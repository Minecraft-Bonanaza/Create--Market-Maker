package com.bng.marketcoordination.ui;

public record CategoryBudgetLine(
        String categoryId,
        long remainingSpurs,
        long totalSpurs,
        long spentSpurs
) {}
