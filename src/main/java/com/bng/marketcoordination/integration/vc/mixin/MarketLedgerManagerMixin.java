package com.bng.marketcoordination.integration.vc.mixin;

import com.bng.marketcoordination.integration.vc.VcIntegration;
import com.beakbock.createvillagercommerce.blockentity.MerchantStallBlockEntity;
import com.beakbock.createvillagercommerce.server.MarketLedgerManager;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MarketLedgerManager.class, remap = false)
public class MarketLedgerManagerMixin {
    @Inject(
            method = "canPurchase",
            at = @At("RETURN"),
            cancellable = true,
            require = 0
    )
    private static void marketcoord$gateByBudget(
            ServerLevel level,
            MerchantStallBlockEntity stall,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }
        if (!VcIntegration.allowPurchase(level, stall)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = "getDailyPurchaseLimit",
            at = @At("RETURN"),
            cancellable = true,
            require = 0
    )
    private static void marketcoord$neutralizeVcCountCap(
            ServerLevel level,
            net.minecraft.core.BlockPos ledgerPos,
            CallbackInfoReturnable<Integer> cir
    ) {
        cir.setReturnValue(Integer.MAX_VALUE);
    }
}
