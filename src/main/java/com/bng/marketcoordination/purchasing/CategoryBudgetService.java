package com.bng.marketcoordination.purchasing;

import com.bng.marketcoordination.config.CategoryBudgetConfig;
import com.bng.marketcoordination.config.MarketCoordConfig;
import com.bng.marketcoordination.economy.CommodityCategory;
import com.bng.marketcoordination.market.MarketId;
import com.bng.marketcoordination.market.MarketState;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class CategoryBudgetService {
    private final Map<MarketId, EnumMap<CommodityCategory, Long>> spentToday = new HashMap<>();

    public boolean canAfford(MarketState market, CommodityCategory category, long spurCost) {
        if (!MarketCoordConfig.CATEGORY.enabled.get()) {
            return market.remainingBudgetSpurs() >= spurCost;
        }
        long categoryBudget = categoryBudgetSpurs(market, category);
        long categorySpent = spentToday.getOrDefault(market.id(), new EnumMap<>(CommodityCategory.class))
                .getOrDefault(category, 0L);
        return market.remainingBudgetSpurs() >= spurCost && categorySpent + spurCost <= categoryBudget;
    }

    public void spend(MarketState market, CommodityCategory category, long spurCost) {
        spentToday
                .computeIfAbsent(market.id(), ignored -> new EnumMap<>(CommodityCategory.class))
                .merge(category, spurCost, Long::sum);
    }

    public long categoryBudgetSpurs(MarketState market, CommodityCategory category) {
        double weight = CategoryBudgetConfig.weight(category);
        if (MarketCoordConfig.CATEGORY.specializationBias.get() > 0.0) {
            double share = market.regionalProfile().categoryShare(category);
            weight += share * MarketCoordConfig.CATEGORY.specializationBias.get();
        }
        return Math.round(market.dailyBudgetSpurs() * weight);
    }

    public long categoryRemaining(MarketState market, CommodityCategory category) {
        return Math.max(0L, categoryBudgetSpurs(market, category)
                - spentToday.getOrDefault(market.id(), new EnumMap<>(CommodityCategory.class))
                        .getOrDefault(category, 0L));
    }

    public long categorySpent(MarketState market, CommodityCategory category) {
        return spentToday.getOrDefault(market.id(), new EnumMap<>(CommodityCategory.class))
                .getOrDefault(category, 0L);
    }

    /**
     * Fraction (0..1) of this category's daily money quota already spent today. Used by the demand
     * curve so willingness-to-pay falls as the quota fills.
     */
    public double categoryFillFraction(MarketState market, CommodityCategory category) {
        long budget = categoryBudgetSpurs(market, category);
        if (budget <= 0L) {
            return 1.0;
        }
        double fill = (double) categorySpent(market, category) / (double) budget;
        return Math.max(0.0, Math.min(1.0, fill));
    }

    public void resetDaily() {
        spentToday.clear();
    }
}
