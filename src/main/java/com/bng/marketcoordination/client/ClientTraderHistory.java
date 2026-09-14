package com.bng.marketcoordination.client;

import com.bng.marketcoordination.economy.TraderHistoryService.PlayerSeries;

import java.util.List;

/**
 * Client-side cache of the latest global per-player trade-volume series received from the server.
 * The Stock Market screen overlay reads this to draw the line graph, and requests a refresh on a
 * throttle so the data stays reasonably current while the screen is open.
 */
public final class ClientTraderHistory {

    private static volatile List<PlayerSeries> series = List.of();
    private static volatile long lastRequestMs;

    private ClientTraderHistory() {}

    public static void set(List<PlayerSeries> value) {
        series = value == null ? List.of() : value;
    }

    public static List<PlayerSeries> get() {
        return series;
    }

    /** Returns true (and records the time) if a fresh request should be sent, throttled to ~2s. */
    public static boolean shouldRequest() {
        long now = System.currentTimeMillis();
        if (now - lastRequestMs < 2000L) {
            return false;
        }
        lastRequestMs = now;
        return true;
    }
}
