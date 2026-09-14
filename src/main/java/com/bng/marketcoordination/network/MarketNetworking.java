package com.bng.marketcoordination.network;

import com.bng.marketcoordination.MarketCoordinationMod;
import com.bng.marketcoordination.integration.VillagerCommerceAdapter;
import com.bng.marketcoordination.market.MarketRegistry;
import com.bng.marketcoordination.market.MarketState;
import com.bng.marketcoordination.ui.MarketInfoProvider;
import com.bng.marketcoordination.ui.MarketSummary;
import com.bng.marketcoordination.client.MarketClientNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class MarketNetworking {
    private static final VillagerCommerceAdapter VC = new VillagerCommerceAdapter();

    private MarketNetworking() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MarketCoordinationMod.MOD_ID);
        registrar.playToClient(
                MarketSummaryPayload.TYPE,
                MarketSummaryPayload.STREAM_CODEC,
                MarketClientNetworking::handleSummary
        );
        registrar.playToServer(
                MarketSummaryRequestPayload.TYPE,
                MarketSummaryRequestPayload.STREAM_CODEC,
                MarketNetworking::handleSummaryRequest
        );
        registrar.playToServer(
                StallConfigSavePayload.TYPE,
                StallConfigSavePayload.STREAM_CODEC,
                MarketNetworking::handleStallConfigSave
        );
        registrar.playToClient(
                TraderHistoryPayload.TYPE,
                TraderHistoryPayload.STREAM_CODEC,
                MarketClientNetworking::handleTraderHistory
        );
        registrar.playToServer(
                TraderHistoryRequestPayload.TYPE,
                TraderHistoryRequestPayload.STREAM_CODEC,
                MarketNetworking::handleTraderHistoryRequest
        );
    }

    public static void handleTraderHistoryRequest(TraderHistoryRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            // Top players keep the graph readable and the packet small.
            player.connection.send(new net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket(
                    new TraderHistoryPayload(
                            com.bng.marketcoordination.MarketServices.TRADER_HISTORY.snapshot(8))
            ));
        });
    }

    public static void handleStallConfigSave(StallConfigSavePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (player.containerMenu instanceof com.bng.marketcoordination.menu.StallConfigMenu menu
                    && menu.stillValid(player)) {
                menu.applyToStall(payload.saleQuantity(), payload.priceSpurs());
            }
        });
    }

    public static void handleSummaryRequest(MarketSummaryRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!(player.level() instanceof ServerLevel level)) {
                return;
            }
            BlockPos pos = BlockPos.of(payload.ledgerPos());
            if (!player.blockPosition().closerThan(pos, 8.0)) {
                return;
            }
            if (!VC.isMarketLedger(level, pos)) {
                return;
            }
            MarketState market = VC.discoverLedger(level, pos).orElse(null);
            if (market == null) {
                market = MarketRegistry.getByLedger(level, pos).orElse(null);
            }
            if (market == null) {
                return;
            }
            MarketSummary summary = MarketInfoProvider.build(level, market);
            sendSummary(player, summary);
        });
    }

    public static void sendSummary(ServerPlayer player, MarketSummary summary) {
        player.connection.send(new net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket(
                new MarketSummaryPayload(summary)
        ));
    }

    public static void sendSummary(ServerPlayer player, BlockPos ledgerPos, ServerLevel level) {
        MarketRegistry.getByLedger(level, ledgerPos).ifPresentOrElse(
                market -> sendSummary(player, MarketInfoProvider.build(level, market)),
                () -> VC.discoverLedger(level, ledgerPos).ifPresent(market ->
                        sendSummary(player, MarketInfoProvider.build(level, market)))
        );
    }
}
