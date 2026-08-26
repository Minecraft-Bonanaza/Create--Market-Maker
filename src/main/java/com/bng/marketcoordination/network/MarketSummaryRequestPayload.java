package com.bng.marketcoordination.network;

import com.bng.marketcoordination.MarketCoordinationMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MarketSummaryRequestPayload(long ledgerPos) implements CustomPacketPayload {
    public static final Type<MarketSummaryRequestPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MarketCoordinationMod.MOD_ID, "market_summary_request")
    );

    public static final StreamCodec<FriendlyByteBuf, MarketSummaryRequestPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeVarLong(payload.ledgerPos()),
            buf -> new MarketSummaryRequestPayload(buf.readVarLong())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
