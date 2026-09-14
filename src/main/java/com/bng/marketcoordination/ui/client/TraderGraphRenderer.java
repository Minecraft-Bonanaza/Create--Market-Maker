package com.bng.marketcoordination.ui.client;

import com.bng.marketcoordination.client.ClientTraderHistory;
import com.bng.marketcoordination.economy.TraderHistoryService.PlayerSeries;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Draws a compact per-player daily trade-volume line graph (with legend) as a self-contained panel.
 * Intended to be overlaid onto the Stock Market screen; it owns its whole rectangle so it does not
 * depend on the host screen's internal layout.
 */
public final class TraderGraphRenderer {

    private static final int PANEL_BG = 0xE0141414;
    private static final int PANEL_BORDER = 0xFF3A3A3A;
    private static final int AXIS = 0xFF5A5A5A;
    private static final int TITLE = 0xFFFFD37F;
    private static final int TEXT = 0xFFD0D0D0;
    private static final int MUTED = 0xFF8A8A8A;

    /** Distinct line colours, one per player (cycles if there are more players than colours). */
    private static final int[] PALETTE = {
            0xFF4FC3F7, 0xFFFF8A65, 0xFF81C784, 0xFFBA68C8,
            0xFFFFD54F, 0xFF4DB6AC, 0xFFF06292, 0xFF9575CD
    };

    private TraderGraphRenderer() {}

    public static void render(GuiGraphics g, Font font, int x, int y, int width, int height) {
        // Panel.
        g.fill(x, y, x + width, y + height, PANEL_BG);
        g.fill(x, y, x + width, y + 1, PANEL_BORDER);
        g.fill(x, y + height - 1, x + width, y + height, PANEL_BORDER);
        g.fill(x, y, x + 1, y + height, PANEL_BORDER);
        g.fill(x + width - 1, y, x + width, y + height, PANEL_BORDER);

        g.drawString(font, Component.literal("Trade Volume (daily, Spurs)"), x + 6, y + 5, TITLE, false);

        List<PlayerSeries> series = ClientTraderHistory.get();
        if (series == null || series.isEmpty()) {
            g.drawString(font, Component.literal("No trade volume recorded yet."), x + 6, y + 20, MUTED, false);
            return;
        }

        int points = series.get(0).points().length;
        long max = 1L;
        for (PlayerSeries s : series) {
            for (long v : s.points()) {
                if (v > max) {
                    max = v;
                }
            }
        }

        // Plot area (leave room for the legend column on the right).
        int legendWidth = 78;
        int plotLeft = x + 8;
        int plotRight = x + width - legendWidth - 8;
        int plotTop = y + 18;
        int plotBottom = y + height - 10;
        if (plotRight <= plotLeft || plotBottom <= plotTop) {
            return;
        }

        // Axes.
        g.fill(plotLeft, plotTop, plotLeft + 1, plotBottom + 1, AXIS);
        g.fill(plotLeft, plotBottom, plotRight + 1, plotBottom + 1, AXIS);

        // Peak label at top-left of the plot.
        g.drawString(font, Component.literal("peak " + formatShort(max)), plotLeft + 3, plotTop - 1, MUTED, false);

        int plotWidth = plotRight - plotLeft;
        int plotHeight = plotBottom - plotTop;
        int denom = Math.max(1, points - 1);

        for (int c = 0; c < series.size(); c++) {
            int color = PALETTE[c % PALETTE.length];
            long[] pts = series.get(c).points();
            int prevX = 0;
            int prevY = 0;
            for (int i = 0; i < pts.length; i++) {
                int px = plotLeft + (int) Math.round((double) i / denom * plotWidth);
                int py = plotBottom - (int) Math.round((double) pts[i] / max * plotHeight);
                if (i > 0) {
                    drawLine(g, prevX, prevY, px, py, color);
                }
                // Emphasise the live (last) point.
                if (i == pts.length - 1) {
                    g.fill(px - 1, py - 1, px + 2, py + 2, color);
                }
                prevX = px;
                prevY = py;
            }
        }

        // Legend.
        int legendX = plotRight + 8;
        int legendY = plotTop;
        for (int c = 0; c < series.size(); c++) {
            int color = PALETTE[c % PALETTE.length];
            g.fill(legendX, legendY + 1, legendX + 6, legendY + 7, color);
            long[] pts = series.get(c).points();
            long latest = pts[pts.length - 1];
            String label = truncate(series.get(c).name(), 9) + " " + formatShort(latest);
            g.drawString(font, Component.literal(label), legendX + 9, legendY, TEXT, false);
            legendY += 10;
            if (legendY > plotBottom - 8) {
                break;
            }
        }
    }

    /** Bresenham-style line via 1px fills; fine for a small overlay graph. */
    private static void drawLine(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        int x = x1;
        int y = y1;
        while (true) {
            g.fill(x, y, x + 1, y + 1, color);
            if (x == x2 && y == y2) {
                break;
            }
            int e2 = err * 2;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }

    private static String formatShort(long spurs) {
        if (spurs >= 1_000_000L) {
            return String.format("%.1fM", spurs / 1_000_000.0);
        }
        if (spurs >= 1_000L) {
            return String.format("%.1fk", spurs / 1_000.0);
        }
        return Long.toString(spurs);
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "?";
        }
        return value.length() <= max ? value : value.substring(0, max - 1) + "\u2026";
    }
}
