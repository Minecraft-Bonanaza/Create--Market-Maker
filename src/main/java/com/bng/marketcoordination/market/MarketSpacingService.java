package com.bng.marketcoordination.market;

import com.bng.marketcoordination.config.MarketCoordConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class MarketSpacingService {
    private MarketSpacingService() {}

    public static boolean violatesSpacing(ResourceKey<Level> dimension, BlockPos ledgerPos) {
        int minimum = MarketCoordConfig.MARKET.minimumMarketSpacing.get();
        if (minimum <= 0) {
            return false;
        }
        long minDistanceSq = (long) minimum * minimum;
        for (MarketState market : MarketRegistry.all()) {
            if (!market.dimension().equals(dimension)) {
                continue;
            }
            if (market.ledgerPos().equals(ledgerPos)) {
                continue;
            }
            if (market.ledgerPos().distSqr(ledgerPos) < minDistanceSq) {
                return true;
            }
        }
        return false;
    }

    public static boolean canRegister(ServerLevel level, BlockPos ledgerPos) {
        return !violatesSpacing(level.dimension(), ledgerPos);
    }
}
