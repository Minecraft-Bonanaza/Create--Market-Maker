package com.bng.marketcoordination.market;

import com.bng.marketcoordination.MarketServices;
import com.bng.marketcoordination.config.MarketCoordConfig;
import com.bng.marketcoordination.integration.StockMarketPriceCache;
import com.bng.marketcoordination.integration.VillagerCommerceAdapter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

public final class MarketDayCycleHandler {
    private static long lastProcessedDay = -1L;
    private static int staggerIndex = 0;
    private static List<MarketState> pendingMarkets = List.of();
    private static final MarketGrowthService GROWTH = new MarketGrowthService();
    private static final VillagerCommerceAdapter VC = new VillagerCommerceAdapter();

    private MarketDayCycleHandler() {}

    public static void reset() {
        lastProcessedDay = -1L;
        staggerIndex = 0;
        pendingMarkets = List.of();
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0) {
            return;
        }

        StockMarketPriceCache.refreshIfNewDay(server);

        if (!pendingMarkets.isEmpty()) {
            processStaggeredBatch(server);
            return;
        }

        var overworld = server.overworld();
        long dayTime = overworld.getDayTime();
        long currentDay = dayTime / 24000L;
        if (currentDay == lastProcessedDay) {
            return;
        }
        if (dayTime % 24000L > 1200L) {
            return;
        }

        lastProcessedDay = currentDay;
        MarketServices.COMMODITY_CAPS.resetDaily();
        MarketServices.CATEGORY_BUDGET.resetDaily();
        MarketServices.ANTI_ABUSE.resetDaily();
        MarketServices.VILLAGER_BUDGET.resetDaily();
        // Roll each player's current-day volume into the graph history before the new day begins.
        MarketServices.TRADER_HISTORY.finalizeDay();
        pendingMarkets = new ArrayList<>(MarketRegistry.all());
        staggerIndex = 0;
        if (pendingMarkets.isEmpty()) {
            return;
        }
        processStaggeredBatch(server);
    }

    private static void processStaggeredBatch(MinecraftServer server) {
        int maxTicks = MarketCoordConfig.PERFORMANCE.dayCycleStaggerTicks.get();
        int batchSize = Math.max(1, (pendingMarkets.size() + maxTicks - 1) / maxTicks);
        int end = Math.min(staggerIndex + batchSize, pendingMarkets.size());
        ServerLevel overworld = server.overworld();
        for (int i = staggerIndex; i < end; i++) {
            MarketState market = pendingMarkets.get(i);
            int villagers = VC.registeredVillagerCount(overworld, market.ledgerPos());
            GROWTH.applyDailyCycle(market, villagers);
        }
        staggerIndex = end;
        if (staggerIndex >= pendingMarkets.size()) {
            pendingMarkets = List.of();
            staggerIndex = 0;
            MarketRegistry.persist();
        }
    }
}
