package com.bng.marketcoordination.integration.vc;

import com.beakbock.createvillagercommerce.blockentity.MerchantStallBlockEntity;
import com.bng.marketcoordination.menu.StallConfigMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Opens our custom stall configuration GUI when a Merchant Stall is right-clicked with an empty
 * hand (and not sneaking), pre-empting Create: Villager Commerce's own stall menu.
 */
public final class StallConfigInteraction {

    private StallConfigInteraction() {}

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return; // avoid a double fire from the off hand
        }
        if (!event.getItemStack().isEmpty()) {
            return; // empty-hand interaction only, so held items still bind/place as normal
        }
        if (event.getEntity().isShiftKeyDown()) {
            return;
        }

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof MerchantStallBlockEntity stall)) {
            return;
        }

        // We own this interaction: stop VC from also opening its stall menu.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ItemStack saleItem = stall.getSaleItem();
            int saleQty = Math.max(1, stall.getSaleQuantity());
            long priceSpurs = VcIntegration.offerSpurCost(stall);

            serverPlayer.openMenu(
                    new SimpleMenuProvider(
                            (windowId, inv, player) -> new StallConfigMenu(windowId, inv, stall),
                            Component.literal("Merchant Stall")
                    ),
                    buf -> {
                        buf.writeBlockPos(pos);
                        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf,
                                saleItem == null ? ItemStack.EMPTY : saleItem);
                        buf.writeVarInt(saleQty);
                        buf.writeVarLong(priceSpurs);
                    }
            );
        }
    }
}
