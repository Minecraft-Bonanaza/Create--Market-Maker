package com.bng.marketcoordination.market;

import com.bng.marketcoordination.integration.VillagerCommerceAdapter;
import com.bng.marketcoordination.network.MarketNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class MarketDiscoveryHandler {
    private static final VillagerCommerceAdapter VC = new VillagerCommerceAdapter();

    private MarketDiscoveryHandler() {}

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!(level.getBlockEntity(event.getPos()) instanceof LecternBlockEntity)) {
            return;
        }
        if (!VC.isMarketLedger(level, event.getPos())) {
            return;
        }

        if (event.getEntity().isShiftKeyDown()) {
            if (event.getEntity() instanceof ServerPlayer player) {
                VC.discoverLedger(level, event.getPos()).ifPresent(market -> {
                    event.setCanceled(true);
                    MarketNetworking.sendSummary(player, event.getPos(), level);
                });
            }
            return;
        }

        VC.discoverLedger(level, event.getPos()).ifPresent(market ->
                event.getEntity().displayClientMessage(
                        net.minecraft.network.chat.Component.literal(
                                "Registered market: " + market.displayName() + " (shift-click for details)"),
                        true));
    }
}
