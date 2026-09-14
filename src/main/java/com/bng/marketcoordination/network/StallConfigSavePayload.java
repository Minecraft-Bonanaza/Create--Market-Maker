package com.bng.marketcoordination.network;

import com.bng.marketcoordination.MarketCoordinationMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Sent from the stall-config GUI to persist the chosen sale quantity and price (in Spurs). */
public record StallConfigSavePayload(int saleQuantity, long priceSpurs) implements CustomPacketPayload {

    public static final Type<StallConfigSavePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MarketCoordinationMod.MOD_ID, "stall_config_save")
    );

    public static final StreamCodec<FriendlyByteBuf, StallConfigSavePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.saleQuantity());
                buf.writeVarLong(payload.priceSpurs());
            },
            buf -> new StallConfigSavePayload(buf.readVarInt(), buf.readVarLong())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
