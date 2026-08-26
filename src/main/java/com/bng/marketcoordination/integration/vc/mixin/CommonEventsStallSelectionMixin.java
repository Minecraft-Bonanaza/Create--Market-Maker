package com.bng.marketcoordination.integration.vc.mixin;

import com.bng.marketcoordination.purchasing.StallSelectionService;
import com.beakbock.createvillagercommerce.event.CommonEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CommonEvents.class, remap = false)
public class CommonEventsStallSelectionMixin {
    @Inject(method = "findNearbyAllowedStall", at = @At("HEAD"), cancellable = true, require = 0)
    private static void marketcoord$selectCompetitiveStall(
            ServerLevel level,
            Villager villager,
            BlockPos origin,
            int radius,
            CallbackInfoReturnable<BlockPos> cir
    ) {
        BlockPos selected = StallSelectionService.selectForVillager(level, villager, origin, radius);
        cir.setReturnValue(selected);
        cir.cancel();
    }
}
