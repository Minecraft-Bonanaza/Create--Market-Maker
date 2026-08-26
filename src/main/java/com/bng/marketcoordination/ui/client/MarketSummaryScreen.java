package com.bng.marketcoordination.ui.client;

import com.bng.marketcoordination.config.MarketCoordConfig;
import com.bng.marketcoordination.integration.NumismaticsAdapter;
import com.bng.marketcoordination.network.MarketSummaryRequestPayload;
import com.bng.marketcoordination.ui.CategoryBudgetLine;
import com.bng.marketcoordination.ui.MarketInfoProvider;
import com.bng.marketcoordination.ui.MarketSummary;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class MarketSummaryScreen extends Screen {
    private MarketSummary summary;
    private int refreshTicks;

    public MarketSummaryScreen(MarketSummary summary) {
        super(Component.literal(summary.marketName()));
        this.summary = summary;
    }

    public void update(MarketSummary summary) {
        this.summary = summary;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("Close"), button -> onClose())
                .bounds(width / 2 - 40, height - 28, 80, 20)
                .build());
    }

    @Override
    public void tick() {
        super.tick();
        refreshTicks++;
        int interval = MarketCoordConfig.PERFORMANCE.uiRefreshIntervalTicks.get();
        if (refreshTicks >= interval) {
            refreshTicks = 0;
            PacketDistributor.sendToServer(new MarketSummaryRequestPayload(summary.ledgerPos()));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        int x = width / 2 - 120;
        int y = 20;
        graphics.drawCenteredString(font, summary.marketName().toUpperCase(), width / 2, y, 0xFFFFFF);
        y += 14;
        graphics.drawCenteredString(font, summary.tierName(), width / 2, y, 0xC8C8C8);
        y += 16;
        graphics.drawString(font, "Nation: " + summary.nationName(), x, y, 0xE0E0E0);
        y += 12;
        graphics.drawString(font, "Specialization: " + summary.dominantCategory(), x, y, 0xE0E0E0);
        y += 12;
        graphics.drawString(font, "Registered Stalls: " + summary.registeredStalls(), x, y, 0xE0E0E0);
        y += 12;
        graphics.drawString(font, "Active Sellers: " + summary.activeSellers(), x, y, 0xE0E0E0);
        y += 14;
        graphics.drawString(font, "Daily Procurement:", x, y, 0xFFFFFF);
        y += 12;
        graphics.drawString(font, MarketInfoProvider.formatBudgetLine(summary), x, y, 0xC8FFC8);
        y += 14;
        renderBudgetBar(graphics, x, y, 240, 8, summary);
        y += 18;

        if (!summary.categoryLines().isEmpty()) {
            graphics.drawString(font, "Category Budgets:", x, y, 0xFFFFFF);
            y += 12;
            for (CategoryBudgetLine line : summary.categoryLines()) {
                if (y > height - 80) {
                    break;
                }
                graphics.drawString(font,
                        line.categoryId() + ": "
                                + NumismaticsAdapter.formatSpurs(line.remainingSpurs())
                                + " / "
                                + NumismaticsAdapter.formatSpurs(line.totalSpurs()),
                        x + 8,
                        y,
                        0xD0D0D0);
                y += 10;
            }
            y += 4;
        }

        graphics.drawString(font, "7-Day Trade Volume:", x, y, 0xFFFFFF);
        y += 12;
        graphics.drawString(font, NumismaticsAdapter.formatSpurs(summary.volume7DaySpurs()), x, y, 0xE0E0E0);
        y += 12;
        renderHistoryLine(graphics, x, y, summary.history7DaySpurs());
        y += 16;
        graphics.drawString(font, "Market Activity: " + summary.activityPercent() + "%", x, y, 0xE0E0E0);
        y += 12;
        graphics.drawString(font, "Growth Trend: " + summary.growthTrend(), x, y, 0xE0E0E0);
        y += 12;
        graphics.drawString(font, "Lifetime Issuance: " + NumismaticsAdapter.formatSpurs(summary.lifetimeIssuanceSpurs()), x, y, 0xAAAAAA);
        y += 12;
        graphics.drawString(font, "Spent today: " + NumismaticsAdapter.formatSpurs(summary.spentTodaySpurs()), x, y, 0xAAAAAA);
    }

    private void renderHistoryLine(GuiGraphics graphics, int x, int y, List<Long> history) {
        if (history.isEmpty()) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        int shown = Math.min(7, history.size());
        for (int i = shown - 1; i >= 0; i--) {
            if (builder.length() > 0) {
                builder.append(" | ");
            }
            builder.append(formatCompact(history.get(i)));
        }
        graphics.drawString(font, builder.toString(), x, y, 0x909090);
    }

    private static String formatCompact(long spurs) {
        long cogs = spurs / 64L;
        return cogs + "c";
    }

    private void renderBudgetBar(GuiGraphics graphics, int x, int y, int width, int height, MarketSummary summary) {
        graphics.fill(x, y, x + width, y + height, 0xFF303030);
        if (summary.budgetTotalSpurs() <= 0L) {
            return;
        }
        int fillWidth = (int) (width * (summary.budgetRemainingSpurs() / (double) summary.budgetTotalSpurs()));
        graphics.fill(x, y, x + fillWidth, y + height, 0xFF4CAF50);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
