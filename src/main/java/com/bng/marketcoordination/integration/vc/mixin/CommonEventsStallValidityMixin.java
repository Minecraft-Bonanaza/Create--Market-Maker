package com.bng.marketcoordination.integration.vc.mixin;

import com.bng.marketcoordination.integration.vc.VcIntegration;
import com.beakbock.createvillagercommerce.blockentity.MerchantStallBlockEntity;
import com.beakbock.createvillagercommerce.event.CommonEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ensures a stall that our market gates currently block (budget, category budget, commodity cap,
 * or price ceiling) is treated as invalid for shopping. Without this, a villager can latch onto a
 * cached target stall that VC still considers valid but our mod refuses to complete, soft-locking
 * the villager into repeatedly pathing to a stall it can never buy from instead of moving on to
 * other available goods.
 */
@Mixin(value = CommonEvents.class, remap = false)
public class CommonEventsStallValidityMixin {
    @Inject(method = "isStallValidForShopping", at = @At("RETURN"), cancellable = true, require = 0)
    private static void marketcoord$rejectGatedStall(
            ServerLevel level,
            BlockPos stallPos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        // Only demote a stall VC already considers valid; never resurrect an invalid one.
        if (!Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }

        BlockEntity be = level.getBlockEntity(stallPos);
        if (be instanceof MerchantStallBlockEntity stall) {
            // Quiet check: this runs frequently during villager AI ticks.
            if (!VcIntegration.allowPurchase(level, stall, false)) {
                cir.setReturnValue(false);
            }
        }
    }
}
