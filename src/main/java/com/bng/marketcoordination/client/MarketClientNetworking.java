package com.bng.marketcoordination.client;

import com.bng.marketcoordination.network.MarketSummaryPayload;
import com.bng.marketcoordination.network.TraderHistoryPayload;
import com.bng.marketcoordination.ui.MarketSummary;
import com.bng.marketcoordination.ui.client.MarketSummaryScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class MarketClientNetworking {
    private MarketClientNetworking() {}

    /**
     * Registers play-to-client handlers. Must only be called on the physical client —
     * these method references load {@link MarketSummaryScreen}, which extends a
     * client-only class and will crash a dedicated server.
     */
    public static void registerClientHandlers(PayloadRegistrar registrar) {
        registrar.playToClient(
                MarketSummaryPayload.TYPE,
                MarketSummaryPayload.STREAM_CODEC,
                MarketClientNetworking::handleSummary
        );
        registrar.playToClient(
                TraderHistoryPayload.TYPE,
                TraderHistoryPayload.STREAM_CODEC,
                MarketClientNetworking::handleTraderHistory
        );
    }

    public static void handleSummary(MarketSummaryPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            MarketSummary summary = payload.summary();
            if (minecraft.screen instanceof MarketSummaryScreen screen) {
                screen.update(summary);
            } else {
                minecraft.setScreen(new MarketSummaryScreen(summary));
            }
        });
    }

    public static void handleTraderHistory(TraderHistoryPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientTraderHistory.set(payload.series()));
    }
}
