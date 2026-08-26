package com.bng.marketcoordination.client;

import com.bng.marketcoordination.network.MarketSummaryPayload;
import com.bng.marketcoordination.ui.MarketSummary;
import com.bng.marketcoordination.ui.client.MarketSummaryScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class MarketClientNetworking {
    private MarketClientNetworking() {}

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
}
