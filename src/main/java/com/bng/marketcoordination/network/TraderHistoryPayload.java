package com.bng.marketcoordination.network;

import com.bng.marketcoordination.MarketCoordinationMod;
import com.bng.marketcoordination.economy.TraderHistoryService.PlayerSeries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Server → client global per-player daily trade-volume series that backs the graph. */
public record TraderHistoryPayload(List<PlayerSeries> series) implements CustomPacketPayload {

    public static final Type<TraderHistoryPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MarketCoordinationMod.MOD_ID, "trader_history")
    );

    public static final StreamCodec<FriendlyByteBuf, TraderHistoryPayload> STREAM_CODEC = StreamCodec.of(
            TraderHistoryPayload::write,
            TraderHistoryPayload::read
    );

    private static void write(FriendlyByteBuf buf, TraderHistoryPayload payload) {
        List<PlayerSeries> series = payload.series();
        buf.writeVarInt(series.size());
        for (PlayerSeries s : series) {
            buf.writeUtf(s.name());
            long[] points = s.points();
            buf.writeVarInt(points.length);
            for (long point : points) {
                buf.writeVarLong(point);
            }
        }
    }

    private static TraderHistoryPayload read(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<PlayerSeries> series = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String name = buf.readUtf();
            int len = buf.readVarInt();
            long[] points = new long[len];
            for (int p = 0; p < len; p++) {
                points[p] = buf.readVarLong();
            }
            series.add(new PlayerSeries(name, points));
        }
        return new TraderHistoryPayload(series);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
