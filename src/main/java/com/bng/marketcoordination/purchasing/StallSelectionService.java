package com.bng.marketcoordination.purchasing;

import com.bng.marketcoordination.MarketServices;
import com.bng.marketcoordination.config.CommodityCategoryRegistry;
import com.bng.marketcoordination.config.MarketCoordConfig;
import com.bng.marketcoordination.integration.vc.StallStockProbe;
import com.bng.marketcoordination.integration.vc.VcIntegration;
import com.bng.marketcoordination.market.MarketId;
import com.bng.marketcoordination.market.MarketRegistry;
import com.bng.marketcoordination.market.MarketState;
import com.beakbock.createvillagercommerce.blockentity.MerchantStallBlockEntity;
import com.beakbock.createvillagercommerce.server.MarketLedgerManager;
import com.beakbock.createvillagercommerce.server.MarketLedgerVillagerManager;
import com.beakbock.createvillagercommerce.server.MerchantStallManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class StallSelectionService {
    private StallSelectionService() {}

    /**
     * VC's shopping tick re-selects a target every check tick and acts on it immediately. Our
     * basket selection is randomized, so without stickiness a villager would thrash between
     * targets and rarely arrive-and-buy. We therefore commit each villager to one target until
     * they either complete a purchase there or it becomes invalid.
     */
    private static final Map<UUID, Long> STICKY_TARGET = new HashMap<>();

    /** Game-time after which a committed target is abandoned even if unreached (anti-stuck). */
    private static final Map<UUID, Long> STICKY_EXPIRY = new HashMap<>();
    private static final long MAX_COMMIT_TICKS = 300L;

    /**
     * Stalls we recently attempted are parked here (packed pos -> game-time expiry), mirroring
     * VC's own per-stall cooldowns so villagers fall back to other stalls (e.g. food when a
     * material stall is out of stock) instead of hammering the same one.
     */
    private static final Map<Long, Long> STALL_COOLDOWN = new HashMap<>();

    private static final long COOLDOWN_AFTER_SUCCESS = 40L;
    private static final long COOLDOWN_AFTER_FAILURE = 200L;

    public static BlockPos selectForVillager(ServerLevel level, Villager villager, BlockPos origin, int radius) {
        // A villager that has spent its daily allowance is done shopping for the day.
        if (villager != null && MarketServices.VILLAGER_BUDGET.hasReachedCap(villager.getUUID())) {
            clearSticky(villager.getUUID());
            return null;
        }

        BlockPos ledgerPos = MarketLedgerVillagerManager.getRegisteredLedgerPos(level, villager);

        MarketState villagerMarket = ledgerPos != null
                ? MarketRegistry.get(MarketId.fromLedger(level.dimension(), ledgerPos)).orElse(null)
                : null;

        // Self-scaling cadence: in very populous markets, have each villager shop
        // proportionally less often so aggregate pathfinding stays bounded. Markets at or
        // below the concurrency cap are never throttled, so population still drives spend.
        if (villagerMarket != null && !passesConcurrencyThrottle(villagerMarket)) {
            return null;
        }

        List<BlockPos> candidates = ledgerPos != null
                ? stallsNearLedger(level, ledgerPos)
                : MerchantStallManager.getNearbyStalls(level, origin, radius, 16);

        long now = level.getGameTime();
        List<SellerSelectionService.Offer> offers = new ArrayList<>();
        Set<BlockPos> buyablePositions = new HashSet<>();
        for (BlockPos stallPos : candidates) {
            MerchantStallBlockEntity stall = stallAt(level, stallPos);
            if (stall == null) {
                continue;
            }
            // Skip stalls we just attempted (out of stock, on VC cooldown, etc.) so villagers
            // spread out instead of fixating on one stall.
            if (isOnCooldown(stallPos, now)) {
                continue;
            }
            if (!MarketLedgerManager.canPurchase(level, stall)) {
                continue;
            }
            MarketState market = marketForStall(level, stall);
            if (market != null && !VcIntegration.passesMarketGates(market, stall, false)) {
                continue;
            }
            long price = VcIntegration.offerSpurCost(stall);
            // Only consider items this villager can still afford today, so when its preferred/
            // first-choice stall is unavailable it moves on to the next item within its limits.
            if (villager != null && !MarketServices.VILLAGER_BUDGET.canSpend(villager.getUUID(), price)) {
                continue;
            }
            // Skip stalls that have no stock / no room for payment right now, so villagers move
            // straight on to the next affordable item instead of walking to an empty stall.
            if (stall instanceof StallStockProbe probe && !probe.marketcoord$hasStockForSale()) {
                continue;
            }
            Item saleItem = stall.getSaleItem().getItem();
            BlockPos immutablePos = stallPos.immutable();
            offers.add(new SellerSelectionService.Offer(
                    price,
                    immutablePos,
                    saleItem,
                    CommodityCategoryRegistry.categoryOf(saleItem)));
            buyablePositions.add(immutablePos);
        }

        // Stay committed to an existing target while it is still buyable, so a villager actually
        // reaches the stall and completes the trade rather than re-rolling a new target each tick.
        UUID villagerId = villager == null ? null : villager.getUUID();
        if (villagerId != null) {
            Long stickyPacked = STICKY_TARGET.get(villagerId);
            Long stickyExpiry = STICKY_EXPIRY.get(villagerId);
            boolean expired = stickyExpiry != null && now >= stickyExpiry;
            if (stickyPacked != null && !expired) {
                BlockPos sticky = BlockPos.of(stickyPacked);
                if (buyablePositions.contains(sticky)) {
                    return sticky;
                }
            }
            if (stickyPacked != null) {
                clearSticky(villagerId);
            }
        }

        // Favour categories whose daily "basket" is least filled so villagers spread purchases
        // across food, materials, tools, ... rather than piling onto the cheapest single item.
        final MarketState weightMarket = villagerMarket;
        SellerSelectionService.Offer selected = MarketServices.SELLER_SELECTION.selectBasket(
                offers,
                category -> weightMarket == null
                        ? 1.0
                        : Math.max(1.0, MarketServices.CATEGORY_BUDGET.categoryRemaining(weightMarket, category)));
        if (selected == null) {
            clearSticky(villagerId);
            return null;
        }
        if (villagerId != null) {
            STICKY_TARGET.put(villagerId, selected.stallPos().asLong());
            STICKY_EXPIRY.put(villagerId, now + MAX_COMMIT_TICKS);
        }
        return selected.stallPos();
    }

    /**
     * Records the outcome of a villager purchase attempt so selection can react: the villager is
     * released from its current target, and the stall is briefly parked (longer on failure, so a
     * genuinely empty/blocked stall is skipped while other stalls remain available).
     */
    public static void onPurchaseResult(ServerLevel level, UUID villagerId, BlockPos stallPos, boolean success) {
        clearSticky(villagerId);
        if (stallPos == null) {
            return;
        }
        long expiry = level.getGameTime() + (success ? COOLDOWN_AFTER_SUCCESS : COOLDOWN_AFTER_FAILURE);
        STALL_COOLDOWN.put(stallPos.asLong(), expiry);
    }

    /** Releases a villager's committed target without applying a stall cooldown. */
    public static void releaseTarget(UUID villagerId) {
        clearSticky(villagerId);
    }

    private static void clearSticky(UUID villagerId) {
        if (villagerId != null) {
            STICKY_TARGET.remove(villagerId);
            STICKY_EXPIRY.remove(villagerId);
        }
    }

    private static boolean isOnCooldown(BlockPos stallPos, long now) {
        Long expiry = STALL_COOLDOWN.get(stallPos.asLong());
        if (expiry == null) {
            return false;
        }
        if (expiry <= now) {
            STALL_COOLDOWN.remove(stallPos.asLong());
            return false;
        }
        return true;
    }

    /**
     * @return true if this villager may proceed to shop this check. In markets larger than the
     *         configured concurrency cap, only a proportional slice of villagers proceed on any
     *         given check, keeping the number of villagers actively pathfinding roughly constant.
     */
    private static boolean passesConcurrencyThrottle(MarketState market) {
        int maxConcurrent = MarketCoordConfig.POPULATION.maxConcurrentShoppersPerMarket.get();
        if (maxConcurrent <= 0) {
            return true;
        }
        int villagers = market.activityWindow().latestVillagerCount();
        if (villagers <= maxConcurrent) {
            return true;
        }
        double proceedChance = maxConcurrent / (double) villagers;
        return ThreadLocalRandom.current().nextDouble() < proceedChance;
    }

    private static List<BlockPos> stallsNearLedger(ServerLevel level, BlockPos ledgerPos) {
        int radius = Math.max(4, com.beakbock.createvillagercommerce.Config.MARKET_LEDGER_RADIUS.get());
        return MerchantStallManager.getNearbyStalls(level, ledgerPos, radius, 16);
    }

    private static MerchantStallBlockEntity stallAt(ServerLevel level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof MerchantStallBlockEntity stall)) {
            return null;
        }
        ItemStack sale = stall.getSaleItem();
        ItemStack currency = stall.getCurrencyItem();
        if (sale.isEmpty() || currency.isEmpty()) {
            return null;
        }
        return stall;
    }

    private static MarketState marketForStall(ServerLevel level, MerchantStallBlockEntity stall) {
        BlockPos ledgerPos = stall.getLinkedMarketLedgerPos();
        if (ledgerPos == null || !MarketLedgerManager.isValidMarketLedger(level, ledgerPos)) {
            return null;
        }
        MarketId id = MarketId.fromLedger(level.dimension(), ledgerPos);
        return MarketRegistry.get(id).orElse(null);
    }
}
