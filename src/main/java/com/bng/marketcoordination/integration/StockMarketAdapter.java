package com.bng.marketcoordination.integration;

import by.deokma.stockmarket.market.MarketData;
import by.deokma.stockmarket.market.MarketEntry;
import com.bng.marketcoordination.MarketServices;
import com.bng.marketcoordination.config.MarketCoordConfig;
import com.bng.marketcoordination.market.MarketId;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.OptionalLong;

public final class StockMarketAdapter {
    private StockMarketAdapter() {}

    public static OptionalLong referenceSpurs(MarketId marketId, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return OptionalLong.empty();
        }
        return referenceSpurs(marketId, BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static OptionalLong referenceSpurs(MarketId marketId, ResourceLocation itemId) {
        if (itemId == null) {
            return OptionalLong.empty();
        }

        if (MarketCoordConfig.PURCHASING.useStockMarketCeiling.get()) {
            OptionalLong cached = StockMarketPriceCache.cachedSpurs(itemId);
            if (cached.isPresent()) {
                return cached;
            }
            OptionalLong live = liveReferenceSpurs(itemId);
            if (live.isPresent()) {
                return live;
            }
        }

        if (marketId != null) {
            Item item = BuiltInRegistries.ITEM.get(itemId);
            long observed = MarketServices.OBSERVED_PRICES.medianSpurs(marketId, item);
            if (observed > 0L) {
                return OptionalLong.of(observed);
            }
        }

        return OptionalLong.empty();
    }

    public static OptionalLong referenceSpurs(ResourceLocation itemId) {
        return referenceSpurs(null, itemId);
    }

    public static OptionalLong referenceSpurs(ItemStack stack) {
        return referenceSpurs(null, stack);
    }

    private static OptionalLong liveReferenceSpurs(ResourceLocation itemId) {
        if (!ModPresence.isStockMarketLoaded()) {
            return OptionalLong.empty();
        }
        for (MarketEntry entry : MarketData.get()) {
            if (itemId.equals(entry.itemId())) {
                int avg = entry.avgPrice();
                if (avg > 0) {
                    return OptionalLong.of(avg);
                }
                int min = entry.minPrice();
                if (min > 0) {
                    return OptionalLong.of(min);
                }
            }
        }
        return OptionalLong.empty();
    }
}
