package com.bng.marketcoordination.integration.vc;

import com.simibubi.create.content.logistics.packager.InventorySummary;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/**
 * Item matching that is deliberately lenient for food. Freshness / quality mods stamp extra data
 * components onto food, which makes VC's exact {@link ItemStack#isSameItemSameComponents} matching
 * treat e.g. "Bread @85%" and "Bread @60%" as different items and report a stall as out of stock.
 *
 * <p>For food we therefore match on the {@link net.minecraft.world.item.Item} alone (any modifiers
 * accepted); for everything else we keep VC's strict component-sensitive matching so tools,
 * enchanted items, potions, etc. still behave normally.
 */
public final class FoodMatch {

    private FoodMatch() {}

    public static boolean isFood(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.has(DataComponents.FOOD);
    }

    /** Flexible replacement for {@code ItemStack.isSameItemSameComponents(stock, sale)}. */
    public static boolean matches(ItemStack stock, ItemStack sale) {
        if (stock == null || sale == null || stock.isEmpty() || sale.isEmpty()) {
            return ItemStack.isSameItemSameComponents(stock, sale);
        }
        if (stock.getItem() == sale.getItem() && isFood(sale)) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(stock, sale);
    }

    /** Flexible replacement for {@code InventorySummary.getCountOf(sale)}. */
    public static int summaryCount(InventorySummary summary, ItemStack sale) {
        if (summary == null) {
            return 0;
        }
        if (isFood(sale)) {
            return summary.getTotalOfMatching(stack -> stack != null && !stack.isEmpty() && stack.is(sale.getItem()));
        }
        return summary.getCountOf(sale);
    }
}
