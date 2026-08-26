package com.bng.marketcoordination.integration;

import by.deokma.stockmarket.market.MarketData;
import by.deokma.stockmarket.market.MarketEntry;
import com.bng.marketcoordination.MarketCoordinationMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;

public final class StockMarketPriceCache {
    private static final Map<ResourceLocation, Long> AVG_SPURS = new HashMap<>();
    private static long lastRefreshDay = -1L;

    private StockMarketPriceCache() {}

    public static void refreshIfNewDay(MinecraftServer server) {
        long day = server.overworld().getDayTime() / 24000L;
        if (day == lastRefreshDay) {
            return;
        }
        refresh(server);
        lastRefreshDay = day;
    }

    public static void refresh(MinecraftServer server) {
        AVG_SPURS.clear();
        if (!ModPresence.isStockMarketLoaded()) {
            return;
        }
        try {
            for (MarketEntry entry : MarketData.get()) {
                if (entry.avgPrice() > 0) {
                    AVG_SPURS.put(entry.itemId(), (long) entry.avgPrice());
                } else if (entry.minPrice() > 0) {
                    AVG_SPURS.put(entry.itemId(), (long) entry.minPrice());
                }
            }
            MarketCoordinationMod.LOGGER.debug("Stock Market price cache refreshed ({} items)", AVG_SPURS.size());
        } catch (Exception exception) {
            MarketCoordinationMod.LOGGER.warn("Failed to refresh Stock Market price cache", exception);
        }
    }

    public static OptionalLong cachedSpurs(ResourceLocation itemId) {
        Long value = AVG_SPURS.get(itemId);
        return value == null ? OptionalLong.empty() : OptionalLong.of(value);
    }

    public static OptionalLong cachedSpurs(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return OptionalLong.empty();
        }
        return cachedSpurs(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }
}
