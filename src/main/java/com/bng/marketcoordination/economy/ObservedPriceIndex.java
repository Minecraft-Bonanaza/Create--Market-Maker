package com.bng.marketcoordination.economy;

import com.bng.marketcoordination.market.MarketId;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ObservedPriceIndex {
    private static final int MAX_SAMPLES = 32;
    private final Map<MarketId, Map<ResourceLocation, List<Long>>> samples = new HashMap<>();

    public void record(MarketId marketId, Item item, long spurPrice) {
        if (spurPrice <= 0L) {
            return;
        }
        ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
        samples
                .computeIfAbsent(marketId, ignored -> new HashMap<>())
                .computeIfAbsent(itemId, ignored -> new ArrayList<>())
                .add(spurPrice);
        trim(marketId, itemId);
    }

    public long medianSpurs(MarketId marketId, Item item) {
        ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
        List<Long> prices = samples.getOrDefault(marketId, Map.of()).get(itemId);
        if (prices == null || prices.isEmpty()) {
            return 0L;
        }
        List<Long> sorted = new ArrayList<>(prices);
        Collections.sort(sorted);
        return sorted.get(sorted.size() / 2);
    }

    public void clearMarket(MarketId marketId) {
        samples.remove(marketId);
    }

    private void trim(MarketId marketId, ResourceLocation itemId) {
        List<Long> prices = samples.get(marketId).get(itemId);
        while (prices.size() > MAX_SAMPLES) {
            prices.removeFirst();
        }
    }
}
