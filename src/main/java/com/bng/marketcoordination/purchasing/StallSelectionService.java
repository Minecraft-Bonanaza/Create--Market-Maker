package com.bng.marketcoordination.purchasing;

import com.bng.marketcoordination.MarketServices;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

public final class StallSelectionService {
    private StallSelectionService() {}

    public static BlockPos selectForVillager(ServerLevel level, Villager villager, BlockPos origin, int radius) {
        BlockPos ledgerPos = MarketLedgerVillagerManager.getRegisteredLedgerPos(level, villager);
        List<BlockPos> candidates = ledgerPos != null
                ? stallsNearLedger(level, ledgerPos)
                : MerchantStallManager.getNearbyStalls(level, origin, radius, 16);

        List<SellerSelectionService.Offer> offers = new ArrayList<>();
        for (BlockPos stallPos : candidates) {
            MerchantStallBlockEntity stall = stallAt(level, stallPos);
            if (stall == null) {
                continue;
            }
            if (!MarketLedgerManager.canPurchase(level, stall)) {
                continue;
            }
            MarketState market = marketForStall(level, stall);
            if (market != null && !VcIntegration.passesMarketGates(market, stall)) {
                continue;
            }
            long price = VcIntegration.offerSpurCost(stall);
            offers.add(new SellerSelectionService.Offer(price, stallPos.immutable()));
        }

        SellerSelectionService.Offer selected = MarketServices.SELLER_SELECTION.select(offers);
        return selected == null ? null : selected.stallPos();
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
