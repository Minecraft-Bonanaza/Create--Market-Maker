package com.bng.marketcoordination.purchasing;

import com.bng.marketcoordination.config.CommodityCapConfig;
import com.bng.marketcoordination.market.MarketId;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

public class CommodityCapService {
    private final Map<MarketId, Map<Item, Integer>> purchasedToday = new HashMap<>();

    public boolean isCapReached(MarketId marketId, Item item) {
        return purchasedToday.getOrDefault(marketId, Map.of()).getOrDefault(item, 0) >= CommodityCapConfig.dailyCap(item);
    }

    public int purchasedToday(MarketId marketId, Item item) {
        return purchasedToday.getOrDefault(marketId, Map.of()).getOrDefault(item, 0);
    }

    public int remaining(MarketId marketId, Item item) {
        return Math.max(0, CommodityCapConfig.dailyCap(item) - purchasedToday(marketId, item));
    }

    /**
     * Fraction (0..1) of this item's daily unit cap already purchased today. Used by the demand
     * curve so willingness-to-pay for an item falls the more of it has been bought.
     */
    public double capFillFraction(MarketId marketId, Item item) {
        int cap = CommodityCapConfig.dailyCap(item);
        if (cap <= 0) {
            return 1.0;
        }
        double fill = (double) purchasedToday(marketId, item) / (double) cap;
        return Math.max(0.0, Math.min(1.0, fill));
    }

    public void recordPurchase(MarketId marketId, Item item, int quantity) {
        if (quantity <= 0) {
            return;
        }
        purchasedToday
                .computeIfAbsent(marketId, id -> new HashMap<>())
                .merge(item, quantity, Integer::sum);
    }

    public void resetDaily() {
        purchasedToday.clear();
    }
}
