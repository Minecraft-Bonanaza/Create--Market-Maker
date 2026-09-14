package com.bng.marketcoordination.network;

import com.bng.marketcoordination.MarketCoordinationMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client → server request for the global per-player trade-volume history (for the graph overlay). */
public record TraderHistoryRequestPayload() implements CustomPacketPayload {

    public static final TraderHistoryRequestPayload INSTANCE = new TraderHistoryRequestPayload();

    public static final Type<TraderHistoryRequestPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MarketCoordinationMod.MOD_ID, "trader_history_request")
    );

    public static final StreamCodec<FriendlyByteBuf, TraderHistoryRequestPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
