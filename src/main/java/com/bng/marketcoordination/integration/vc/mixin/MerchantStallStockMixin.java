package com.bng.marketcoordination.integration.vc.mixin;

import com.beakbock.createvillagercommerce.blockentity.MerchantStallBlockEntity;
import com.bng.marketcoordination.integration.vc.FoodMatch;
import com.bng.marketcoordination.integration.vc.StallStockProbe;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packagerLink.PackagerLinkBlockEntity;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

/**
 * Exposes a non-mutating stock/fulfilment probe on the Merchant Stall by reusing VC's own
 * private read-only helpers. Mirrors the success conditions of {@code tryVillagerPurchase}
 * (Create logistics network first, then a nearby container) without extracting anything.
 */
@Mixin(value = MerchantStallBlockEntity.class, remap = false)
public abstract class MerchantStallStockMixin implements StallStockProbe {

    @Shadow protected abstract boolean basicTradeIsValid();

    @Shadow public abstract ItemStack getSaleItem();

    @Shadow public abstract ItemStack getCurrencyItem();

    @Shadow public abstract int getSaleQuantity();

    @Shadow public abstract int getPriceQuantity();

    @Shadow protected abstract StockTickerBlockEntity findLinkedStockTicker();

    @Shadow protected abstract List<PackagerLinkBlockEntity> findPackagerLinksForTicker(StockTickerBlockEntity ticker);

    @Shadow protected abstract int countAvailableInPackagerLinks(List<PackagerLinkBlockEntity> links, ItemStack stack);

    @Shadow protected abstract boolean canInsertIntoHandler(IItemHandler handler, ItemStack stack, int amount);

    @Shadow protected abstract Container findNearbyContainer();

    @Shadow protected abstract boolean hasEnoughItems(Container container, ItemStack stack, int amount);

    @Shadow protected abstract boolean hasRoomForItems(Container container, ItemStack stack, int amount);

    @Override
    public boolean marketcoord$hasStockForSale() {
        if (!basicTradeIsValid()) {
            return false;
        }

        // Create logistics network path (matches tryDirectCreateStockTransaction, read-only).
        StockTickerBlockEntity ticker = findLinkedStockTicker();
        if (ticker != null) {
            InventorySummary summary = ticker.getRecentSummary();
            if (summary != null
                    && FoodMatch.summaryCount(summary, getSaleItem()) >= getSaleQuantity()
                    && canInsertIntoHandler(ticker.getReceivedPaymentsHandler(), getCurrencyItem(), getPriceQuantity())) {
                List<PackagerLinkBlockEntity> links = findPackagerLinksForTicker(ticker);
                if (links != null && !links.isEmpty()
                        && countAvailableInPackagerLinks(links, getSaleItem()) >= getSaleQuantity()) {
                    return true;
                }
            }
        }

        // Nearby container path (matches tryNearbyContainerTransaction, read-only).
        Container container = findNearbyContainer();
        return container != null
                && hasEnoughItems(container, getSaleItem(), getSaleQuantity())
                && hasRoomForItems(container, getCurrencyItem(), getPriceQuantity());
    }
}
