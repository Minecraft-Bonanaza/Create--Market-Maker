package com.bng.marketcoordination.ui;

import java.util.List;

public record MarketSummary(
        String marketName,
        String tierName,
        int activityPercent,
        long budgetRemainingSpurs,
        long budgetTotalSpurs,
        long spentTodaySpurs,
        long volume7DaySpurs,
        int registeredStalls,
        int activeSellers,
        String growthTrend,
        long ledgerPos,
        String dominantCategory,
        String nationName,
        long lifetimeIssuanceSpurs,
        List<CategoryBudgetLine> categoryLines,
        List<Long> history7DaySpurs
) {}
