package com.bng.marketcoordination.integration.vc.mixin;

import com.bng.marketcoordination.integration.vc.VcIntegration;
import com.beakbock.createvillagercommerce.blockentity.MerchantStallBlockEntity;
import com.beakbock.createvillagercommerce.server.MarketLedgerManager;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MarketLedgerManager.class, remap = false)
public class MarketLedgerManagerRecordMixin {
    @Inject(method = "recordPurchase", at = @At("TAIL"), require = 0)
    private static void marketcoord$recordMarketActivity(
            ServerLevel level,
            MerchantStallBlockEntity stall,
            CallbackInfo ci
    ) {
        VcIntegration.onPurchaseRecorded(level, stall);
    }
}
