package com.bng.marketcoordination.integration.vc.mixin;

import com.bng.marketcoordination.MarketServices;
import com.bng.marketcoordination.integration.vc.VcIntegration;
import com.bng.marketcoordination.purchasing.StallSelectionService;
import com.beakbock.createvillagercommerce.blockentity.MerchantStallBlockEntity;
import com.beakbock.createvillagercommerce.event.CommonEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Enforces and records a per-villager daily spending cap around VC's purchase completion.
 * Combined with the villager giving up on shopping once capped (see StallSelectionService),
 * this makes population the throughput lever: draining a large market budget requires more
 * villagers, which in turn raises trade volume and market growth.
 */
@Mixin(value = CommonEvents.class, remap = false)
public class CommonEventsCompletePurchaseMixin {

    @Inject(method = "completePurchase", at = @At("HEAD"), cancellable = true, require = 0)
    private static void marketcoord$enforceVillagerDailyCap(
            ServerLevel level,
            Villager villager,
            BlockPos stallPos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (villager == null) {
            return;
        }
        long cost = stallCost(level, stallPos);
        if (!MarketServices.VILLAGER_BUDGET.canSpend(villager.getUUID(), cost)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "completePurchase", at = @At("RETURN"), require = 0)
    private static void marketcoord$onPurchaseComplete(
            ServerLevel level,
            Villager villager,
            BlockPos stallPos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (villager == null) {
            return;
        }
        boolean success = Boolean.TRUE.equals(cir.getReturnValue());
        long cost = stallCost(level, stallPos);
        if (success) {
            MarketServices.VILLAGER_BUDGET.recordSpend(villager.getUUID(), cost);
            StallSelectionService.onPurchaseResult(level, villager.getUUID(), stallPos, true);
        } else if (MarketServices.VILLAGER_BUDGET.canSpend(villager.getUUID(), cost)) {
            // Failure was not our daily-cap gate, so the stall itself could not fulfil the trade
            // (out of stock / on cooldown / disallowed). Park it and let the villager try elsewhere.
            StallSelectionService.onPurchaseResult(level, villager.getUUID(), stallPos, false);
        } else {
            // Budget-capped for this item: release the target without penalizing the stall.
            StallSelectionService.releaseTarget(villager.getUUID());
        }
    }

    private static long stallCost(ServerLevel level, BlockPos stallPos) {
        BlockEntity be = level.getBlockEntity(stallPos);
        return be instanceof MerchantStallBlockEntity stall ? VcIntegration.offerSpurCost(stall) : 0L;
    }
}
