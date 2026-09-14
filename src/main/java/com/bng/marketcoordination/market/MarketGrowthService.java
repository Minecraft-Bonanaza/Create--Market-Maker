package com.bng.marketcoordination.market;

import com.bng.marketcoordination.config.MarketCoordConfig;

/**
 * Market growth is driven by how fully the market spent its daily procurement budget:
 * <ul>
 *   <li>Utilization above the break-even threshold grows the market, scaling up to the
 *       maximum daily growth at 100% utilization.</li>
 *   <li>Utilization below the threshold decays the market, scaling up to the (deliberately
 *       gentler) maximum daily decay at 0% utilization.</li>
 *   <li>Decay is floored at a fraction of the market's highest achieved score, so time away
 *       erodes progress only so far.</li>
 * </ul>
 * Because a single villager can only spend so much per day, higher utilization — and therefore
 * faster growth — is reached by having more villagers trading in the market.
 */
public class MarketGrowthService {

    /** Fraction (0..1) of the current daily budget that villagers actually spent today. */
    public double computeBudgetUtilization(MarketState market) {
        long budget = market.dailyBudgetSpurs();
        if (budget <= 0L) {
            return 0.0;
        }
        double utilization = market.spentTodaySpurs() / (double) budget;
        return Math.max(0.0, Math.min(1.0, utilization));
    }

    public void applyDailyCycle(MarketState market, int villagerCount) {
        market.activityWindow().setVillagerCount(villagerCount);

        // Measure how fully today's budget was spent before the day is rolled over/reset.
        double utilization = computeBudgetUtilization(market);

        market.activityWindow().finalizeDay();
        market.issuance().finalizeDay();

        double threshold = MarketCoordConfig.GROWTH.utilizationGrowthThreshold.get();
        double maxGrowth = MarketCoordConfig.GROWTH.maximumDailyGrowth.get();
        double maxDecay = MarketCoordConfig.GROWTH.maximumDailyDecay.get();

        double oldScore = market.activityScore();
        double delta;
        if (utilization >= threshold) {
            // 0 at the threshold, +maxGrowth at full utilization.
            double t = threshold >= 1.0 ? 0.0 : (utilization - threshold) / (1.0 - threshold);
            delta = maxGrowth * t;
        } else {
            // 0 at the threshold, -maxDecay at zero utilization.
            double t = threshold <= 0.0 ? 0.0 : (threshold - utilization) / threshold;
            delta = -maxDecay * t;
        }

        double newScore = oldScore + delta;

        // Never decay below a fraction of the highest score this market has ever reached.
        double peak = market.peakActivityScore();
        double floor = MarketCoordConfig.GROWTH.decayFloorFraction.get() * peak;
        newScore = Math.max(newScore, floor);
        newScore = Math.max(0.0, Math.min(1.0, newScore));

        market.setPreviousActivityScore(oldScore);
        market.setActivityScore(newScore);
        market.setPeakActivityScore(Math.max(peak, newScore));
        market.setTier(MarketTier.fromNormalizedScore(newScore));

        long minBudgetCogs = MarketCoordConfig.MARKET.startingDailyBudget.get();
        long maxBudgetCogs = MarketCoordConfig.MARKET.maximumDailyBudget.get();
        long budgetCogs = minBudgetCogs + Math.round((maxBudgetCogs - minBudgetCogs) * newScore);
        market.setDailyBudgetSpurs(budgetCogs * MarketRegistry.CoinSpurUnits.COG_VALUE_SPURS);
        market.resetDailySpending();
    }
}
