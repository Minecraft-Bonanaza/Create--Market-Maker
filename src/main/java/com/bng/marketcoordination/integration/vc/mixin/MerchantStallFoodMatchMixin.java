package com.bng.marketcoordination.integration.vc.mixin;

import com.beakbock.createvillagercommerce.blockentity.MerchantStallBlockEntity;
import com.bng.marketcoordination.integration.vc.FoodMatch;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Makes the Merchant Stall match its sale item leniently for food (any freshness/modifier variant
 * counts) across both stock paths — the Create logistics network and a nearby container — so a food
 * stall is never falsely reported empty because of a freshness/quality data component.
 */
@Mixin(value = MerchantStallBlockEntity.class, remap = false)
public class MerchantStallFoodMatchMixin {

    @Redirect(
            method = {
                    "hasEnoughItems",
                    "removeItems",
                    "countAvailableInPackagerLinks",
                    "extractFromPackagerLinksWithRollbackData"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;isSameItemSameComponents(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"
            ),
            require = 0
    )
    private boolean marketcoord$flexibleItemMatch(ItemStack stock, ItemStack sale) {
        return FoodMatch.matches(stock, sale);
    }

    @Redirect(
            method = "tryDirectCreateStockTransaction",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/logistics/packager/InventorySummary;getCountOf(Lnet/minecraft/world/item/ItemStack;)I"
            ),
            require = 0
    )
    private int marketcoord$flexibleSummaryCount(InventorySummary summary, ItemStack sale) {
        return FoodMatch.summaryCount(summary, sale);
    }
}
