package com.bng.marketcoordination.ui;

import com.bng.marketcoordination.MarketServices;
import com.bng.marketcoordination.config.MarketCoordConfig;
import com.bng.marketcoordination.economy.CommodityCategory;
import com.bng.marketcoordination.economy.NationRegistry;
import com.bng.marketcoordination.integration.NumismaticsAdapter;
import com.bng.marketcoordination.market.MarketState;
import com.beakbock.createvillagercommerce.server.MarketLedgerManager;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

public final class MarketInfoProvider {
    private MarketInfoProvider() {}

    public static MarketSummary build(ServerLevel level, MarketState market) {
        long remaining = market.remainingBudgetSpurs();
        long total = market.dailyBudgetSpurs();
        long spent = market.spentTodaySpurs();
        double score = market.activityScore();
        int activityPercent = (int) Math.round(score * 100.0);
        String trend = growthTrend(market);
        int registeredStalls = safeCount(() -> MarketLedgerManager.getRegisteredStallCount(level, market.ledgerPos()));
        int activeSellers = Math.max(registeredStalls, market.activityWindow().currentDay().uniqueSellers());
        String nationName = market.nationId() == null
                ? "Unlinked"
                : NationRegistry.get(market.nationId()).map(n -> n.displayName()).orElse("Unknown");

        List<CategoryBudgetLine> categoryLines = new ArrayList<>();
        if (MarketCoordConfig.CATEGORY.enabled.get()) {
            for (CommodityCategory category : CommodityCategory.values()) {
                long categoryTotal = MarketServices.CATEGORY_BUDGET.categoryBudgetSpurs(market, category);
                if (categoryTotal <= 0L) {
                    continue;
                }
                long categorySpent = MarketServices.CATEGORY_BUDGET.categorySpent(market, category);
                categoryLines.add(new CategoryBudgetLine(
                        category.id(),
                        MarketServices.CATEGORY_BUDGET.categoryRemaining(market, category),
                        categoryTotal,
                        categorySpent
                ));
            }
        }

        return new MarketSummary(
                market.displayName(),
                market.tier().displayName(),
                activityPercent,
                remaining,
                total,
                spent,
                market.activityWindow().totalTradeVolumeSpurs(),
                registeredStalls,
                activeSellers,
                trend,
                market.ledgerPos().asLong(),
                market.regionalProfile().dominantCategory().id(),
                nationName,
                market.issuance().lifetimeSpurs(),
                categoryLines,
                market.activityWindow().recentDailyVolumes()
        );
    }

    public static String formatBudgetLine(MarketSummary summary) {
        return NumismaticsAdapter.formatSpurs(summary.budgetRemainingSpurs())
                + " / "
                + NumismaticsAdapter.formatSpurs(summary.budgetTotalSpurs())
                + " remaining";
    }

    private static String growthTrend(MarketState market) {
        double delta = market.activityScore() - market.previousActivityScore();
        if (delta >= 0.03) {
            return "Rising";
        }
        if (delta <= -0.03) {
            return "Falling";
        }
        return "Stable";
    }

    private static int safeCount(IntSupplier supplier) {
        try {
            return Math.max(0, supplier.getAsInt());
        } catch (Exception ignored) {
            return 0;
        }
    }

    @FunctionalInterface
    private interface IntSupplier {
        int getAsInt();
    }
}
