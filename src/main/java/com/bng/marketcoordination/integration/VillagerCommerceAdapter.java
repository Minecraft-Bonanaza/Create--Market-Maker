package com.bng.marketcoordination.integration;

import com.bng.marketcoordination.MarketCoordinationMod;
import com.bng.marketcoordination.market.MarketId;
import com.bng.marketcoordination.market.MarketRegistry;
import com.bng.marketcoordination.market.MarketSpacingService;
import com.bng.marketcoordination.market.MarketState;
import com.beakbock.createvillagercommerce.menu.MarketLedgerMenu;
import com.beakbock.createvillagercommerce.server.MarketLedgerManager;
import com.beakbock.createvillagercommerce.server.MarketLedgerVillagerManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.ModList;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class VillagerCommerceAdapter {
    public Optional<MarketState> discoverLedger(ServerLevel level, BlockPos pos) {
        if (!isVillagerCommercePresent()) {
            return Optional.empty();
        }
        if (!MarketLedgerManager.isMarketLedgerBook(level, pos)) {
            return Optional.empty();
        }

        MarketId id = MarketId.fromLedger(level.dimension(), pos);
        Optional<MarketState> existing = MarketRegistry.get(id);
        if (existing.isPresent()) {
            MarketLedgerManager.registerKnownMarketLedger(level, pos);
            return existing;
        }

        if (!MarketSpacingService.canRegister(level, pos)) {
            MarketCoordinationMod.LOGGER.warn(
                    "Rejected market ledger at {} — within minimum spacing of another market",
                    pos);
            return Optional.empty();
        }

        MarketLedgerManager.registerKnownMarketLedger(level, pos);
        String marketName = MarketLedgerManager.getMarketName(level, pos);
        if (marketName == null || marketName.isBlank()) {
            marketName = "Unnamed Market";
        }

        return Optional.of(MarketRegistry.getOrCreate(id, marketName, level, pos));
    }

    public List<MarketLedgerMenu.StallInfo> collectStalls(ServerLevel level, BlockPos ledgerPos) {
        if (!isVillagerCommercePresent() || !MarketLedgerManager.isValidMarketLedger(level, ledgerPos)) {
            return Collections.emptyList();
        }
        return MarketLedgerManager.getRegisteredStalls(level, ledgerPos);
    }

    public int registeredVillagerCount(ServerLevel level, BlockPos ledgerPos) {
        if (!isVillagerCommercePresent() || !MarketLedgerManager.isValidMarketLedger(level, ledgerPos)) {
            return 0;
        }
        try {
            return MarketLedgerVillagerManager.getRegisteredVillagerCount(level, ledgerPos);
        } catch (Exception exception) {
            return 0;
        }
    }

    public boolean isMarketLedger(ServerLevel level, BlockPos pos) {
        return isVillagerCommercePresent() && MarketLedgerManager.isMarketLedgerBook(level, pos);
    }

    public boolean isVillagerCommercePresent() {
        return ModList.get().isLoaded("createvillagercommerce");
    }
}
