package com.bng.marketcoordination.integration.vc;

import com.bng.marketcoordination.MarketCoordinationMod;
import com.bng.marketcoordination.MarketServices;
import com.bng.marketcoordination.config.CommodityCategoryRegistry;
import com.bng.marketcoordination.economy.CommodityCategory;
import com.bng.marketcoordination.integration.NumismaticsAdapter;
import com.bng.marketcoordination.integration.StockMarketAdapter;
import com.bng.marketcoordination.market.MarketId;
import com.bng.marketcoordination.market.MarketRegistry;
import com.bng.marketcoordination.market.MarketState;
import com.beakbock.createvillagercommerce.blockentity.MerchantStallBlockEntity;
import com.beakbock.createvillagercommerce.server.MarketLedgerManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

public final class VcIntegration {
    private VcIntegration() {}

    public static boolean allowPurchase(ServerLevel level, MerchantStallBlockEntity stall) {
        BlockPos ledgerPos = stall.getLinkedMarketLedgerPos();
        if (ledgerPos == null) {
            return true;
        }
        if (!MarketLedgerManager.isValidMarketLedger(level, ledgerPos)) {
            return true;
        }

        Optional<MarketState> market = resolveMarket(level, ledgerPos);
        if (market.isEmpty()) {
            return true;
        }

        if (!passesMarketGates(market.get(), stall)) {
            return false;
        }

        return true;
    }

    public static boolean passesMarketGates(MarketState market, MerchantStallBlockEntity stall) {
        long spurCost = offerSpurCost(stall);
        if (spurCost <= 0L) {
            return true;
        }

        if (!MarketServices.BUDGET.canAfford(market, spurCost)) {
            MarketCoordinationMod.LOGGER.debug(
                    "Blocked VC purchase for {} — insufficient budget (need {} spurs)",
                    market.displayName(),
                    spurCost);
            return false;
        }

        ItemStack saleItem = stall.getSaleItem();
        if (!saleItem.isEmpty()) {
            CommodityCategory category = CommodityCategoryRegistry.categoryOf(saleItem.getItem());
            if (!MarketServices.CATEGORY_BUDGET.canAfford(market, category, spurCost)) {
                MarketCoordinationMod.LOGGER.debug(
                        "Blocked VC purchase for {} — category {} budget exhausted",
                        market.displayName(),
                        category.id());
                return false;
            }

            if (MarketServices.COMMODITY_CAPS.isCapReached(market.id(), saleItem.getItem())) {
                MarketCoordinationMod.LOGGER.debug(
                        "Blocked VC purchase for {} — commodity cap reached for {}",
                        market.displayName(),
                        saleItem.getItem());
                return false;
            }

            long reference = StockMarketAdapter.referenceSpurs(market.id(), saleItem).orElse(spurCost);
            if (!MarketServices.CEILING.isWithinCeiling(spurCost, reference)) {
                MarketCoordinationMod.LOGGER.debug(
                        "Blocked VC purchase for {} — price {} spurs above ceiling (ref {})",
                        market.displayName(),
                        spurCost,
                        reference);
                return false;
            }
        }

        return true;
    }

    public static void onPurchaseRecorded(ServerLevel level, MerchantStallBlockEntity stall) {
        BlockPos ledgerPos = stall.getLinkedMarketLedgerPos();
        if (ledgerPos == null) {
            return;
        }

        resolveMarket(level, ledgerPos).ifPresent(market -> {
            long spurCost = offerSpurCost(stall);
            ItemStack saleItem = stall.getSaleItem();
            UUID sellerId = sellerIdentity(stall);
            CommodityCategory category = saleItem.isEmpty()
                    ? CommodityCategory.MISC
                    : CommodityCategoryRegistry.categoryOf(saleItem.getItem());

            if (spurCost > 0L) {
                MarketServices.BUDGET.spend(market, spurCost);
                MarketServices.CATEGORY_BUDGET.spend(market, category, spurCost);
                double growthWeight = MarketServices.ANTI_ABUSE.growthWeight(
                        level.getGameTime(),
                        market.id(),
                        sellerId,
                        saleItem.isEmpty() ? null : saleItem.getItem(),
                        spurCost);
                market.activityWindow().recordTrade(spurCost, sellerId, category, growthWeight);
                market.issuance().record(spurCost, category);
                market.regionalProfile().record(category, spurCost);
                if (!saleItem.isEmpty()) {
                    MarketServices.OBSERVED_PRICES.record(market.id(), saleItem.getItem(), spurCost);
                }
            }
            if (!saleItem.isEmpty()) {
                MarketServices.COMMODITY_CAPS.recordPurchase(
                        market.id(),
                        saleItem.getItem(),
                        Math.max(1, stall.getSaleQuantity()));
            }
            MarketRegistry.bindStall(market.id(), stall.getBlockPos());
            MarketRegistry.markDirty();
        });
    }

    public static long offerSpurCost(MerchantStallBlockEntity stall) {
        ItemStack currency = stall.getCurrencyItem();
        if (currency.isEmpty()) {
            return 0L;
        }
        long stackSpurs = NumismaticsAdapter.toSpurs(currency);
        if (stackSpurs <= 0L) {
            return 0L;
        }
        int priceQty = Math.max(1, stall.getPriceQuantity());
        return stackSpurs * priceQty / Math.max(1, currency.getCount());
    }

    public static UUID sellerIdentity(MerchantStallBlockEntity stall) {
        String seed = stall.getLevel().dimension().location() + ":" + stall.getBlockPos().asLong();
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }

    private static Optional<MarketState> resolveMarket(ServerLevel level, BlockPos ledgerPos) {
        MarketId id = MarketId.fromLedger(level.dimension(), ledgerPos);
        Optional<MarketState> existing = MarketRegistry.get(id);
        if (existing.isPresent()) {
            return existing;
        }
        String name = MarketLedgerManager.getMarketName(level, ledgerPos);
        if (name == null || name.isBlank()) {
            name = "Unnamed Market";
        }
        return Optional.of(MarketRegistry.getOrCreate(id, name, level, ledgerPos));
    }
}
