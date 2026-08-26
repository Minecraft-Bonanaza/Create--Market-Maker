package com.bng.marketcoordination.network;

import com.bng.marketcoordination.MarketCoordinationMod;
import com.bng.marketcoordination.ui.CategoryBudgetLine;
import com.bng.marketcoordination.ui.MarketSummary;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record MarketSummaryPayload(MarketSummary summary) implements CustomPacketPayload {
    public static final Type<MarketSummaryPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MarketCoordinationMod.MOD_ID, "market_summary")
    );

    public static final StreamCodec<FriendlyByteBuf, MarketSummaryPayload> STREAM_CODEC = StreamCodec.of(
            MarketSummaryPayload::write,
            MarketSummaryPayload::read
    );

    private static void write(FriendlyByteBuf buf, MarketSummaryPayload payload) {
        MarketSummary summary = payload.summary();
        buf.writeUtf(summary.marketName());
        buf.writeUtf(summary.tierName());
        buf.writeVarInt(summary.activityPercent());
        buf.writeVarLong(summary.budgetRemainingSpurs());
        buf.writeVarLong(summary.budgetTotalSpurs());
        buf.writeVarLong(summary.spentTodaySpurs());
        buf.writeVarLong(summary.volume7DaySpurs());
        buf.writeVarInt(summary.registeredStalls());
        buf.writeVarInt(summary.activeSellers());
        buf.writeUtf(summary.growthTrend());
        buf.writeVarLong(summary.ledgerPos());
        buf.writeUtf(summary.dominantCategory());
        buf.writeUtf(summary.nationName());
        buf.writeVarLong(summary.lifetimeIssuanceSpurs());

        buf.writeVarInt(summary.categoryLines().size());
        for (CategoryBudgetLine line : summary.categoryLines()) {
            buf.writeUtf(line.categoryId());
            buf.writeVarLong(line.remainingSpurs());
            buf.writeVarLong(line.totalSpurs());
            buf.writeVarLong(line.spentSpurs());
        }

        List<Long> history = summary.history7DaySpurs();
        buf.writeVarInt(history.size());
        for (Long volume : history) {
            buf.writeVarLong(volume == null ? 0L : volume);
        }
    }

    private static MarketSummaryPayload read(FriendlyByteBuf buf) {
        String marketName = buf.readUtf();
        String tierName = buf.readUtf();
        int activityPercent = buf.readVarInt();
        long budgetRemaining = buf.readVarLong();
        long budgetTotal = buf.readVarLong();
        long spentToday = buf.readVarLong();
        long volume7Day = buf.readVarLong();
        int registeredStalls = buf.readVarInt();
        int activeSellers = buf.readVarInt();
        String growthTrend = buf.readUtf();
        long ledgerPos = buf.readVarLong();
        String dominantCategory = buf.readUtf();
        String nationName = buf.readUtf();
        long lifetimeIssuance = buf.readVarLong();

        int categoryCount = buf.readVarInt();
        List<CategoryBudgetLine> categoryLines = new ArrayList<>(categoryCount);
        for (int i = 0; i < categoryCount; i++) {
            categoryLines.add(new CategoryBudgetLine(
                    buf.readUtf(),
                    buf.readVarLong(),
                    buf.readVarLong(),
                    buf.readVarLong()
            ));
        }

        int historyCount = buf.readVarInt();
        List<Long> history = new ArrayList<>(historyCount);
        for (int i = 0; i < historyCount; i++) {
            history.add(buf.readVarLong());
        }

        return new MarketSummaryPayload(new MarketSummary(
                marketName,
                tierName,
                activityPercent,
                budgetRemaining,
                budgetTotal,
                spentToday,
                volume7Day,
                registeredStalls,
                activeSellers,
                growthTrend,
                ledgerPos,
                dominantCategory,
                nationName,
                lifetimeIssuance,
                categoryLines,
                history
        ));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
