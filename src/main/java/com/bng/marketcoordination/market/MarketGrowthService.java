package com.bng.marketcoordination.market;

import com.bng.marketcoordination.config.MarketCoordConfig;

public class MarketGrowthService {
    public double computeActivityScore(MarketState market) {
        RollingActivityWindow window = market.activityWindow();
        long volume = window.totalTradeVolumeSpurs();
        double normalizedVolume = Math.min(1.0, volume / (double) (1000L * MarketRegistry.CoinSpurUnits.COG_VALUE_SPURS));

        double normalizedSellers = Math.min(1.0, window.averageUniqueSellers() / 8.0);
        double normalizedDiversity = Math.min(1.0, window.averageCategoryDiversity() / 5.0);

        int softCap = MarketCoordConfig.POPULATION.softCapVillagers.get();
        int villagers = window.latestVillagerCount();
        double populationFactor = MarketCoordConfig.POPULATION.sqrtScalingFactor.get()
                * Math.sqrt(Math.min(villagers, softCap) / (double) softCap);
        double normalizedPopulation = Math.min(1.0, populationFactor);

        return normalizedVolume * MarketCoordConfig.GROWTH.tradeVolumeWeight.get()
                + normalizedSellers * MarketCoordConfig.GROWTH.uniqueSellerWeight.get()
                + normalizedPopulation * MarketCoordConfig.GROWTH.populationWeight.get()
                + normalizedDiversity * MarketCoordConfig.GROWTH.commodityDiversityWeight.get();
    }

    public void applyDailyCycle(MarketState market, int villagerCount) {
        market.activityWindow().setVillagerCount(villagerCount);
        market.activityWindow().finalizeDay();
        market.issuance().finalizeDay();

        double previousScore = market.activityScore();
        market.setPreviousActivityScore(previousScore);
        double measured = computeActivityScore(market);
        double decay = MarketCoordConfig.GROWTH.activityDecayRate.get();
        double maxGrowth = MarketCoordConfig.GROWTH.maximumDailyGrowth.get();
        double score = previousScore * (1.0 - decay) + measured;
        if (previousScore > 0.0 && measured > previousScore) {
            score = Math.min(previousScore * (1.0 + maxGrowth), score);
        }
        score = Math.max(0.0, Math.min(1.0, score));

        market.setActivityScore(score);
        market.setTier(MarketTier.fromNormalizedScore(score));

        long minBudgetCogs = MarketCoordConfig.MARKET.startingDailyBudget.get();
        long maxBudgetCogs = MarketCoordConfig.MARKET.maximumDailyBudget.get();
        long budgetCogs = minBudgetCogs + Math.round((maxBudgetCogs - minBudgetCogs) * score);
        market.setDailyBudgetSpurs(budgetCogs * MarketRegistry.CoinSpurUnits.COG_VALUE_SPURS);
        market.resetDailySpending();
    }
}
