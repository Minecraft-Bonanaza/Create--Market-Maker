package com.bng.marketcoordination.purchasing;

import com.bng.marketcoordination.config.MarketCoordConfig;
import com.bng.marketcoordination.economy.CommodityCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.ToDoubleFunction;

public class SellerSelectionService {
    public record Offer(long priceSpurs, BlockPos stallPos, Item commodity, CommodityCategory category) {}

    /**
     * Chooses what a villager buys as a "basket" spread across commodity categories:
     * <ol>
     *   <li>pick a category, weighted by how much of that category's daily basket is still
     *       unfilled, so demand spreads across food, materials, tools, ... rather than piling
     *       onto whatever single item is cheapest;</li>
     *   <li>pick an item within that category (even spread across whatever is on offer);</li>
     *   <li>pick the cheapest seller of that item within the competitive price band.</li>
     * </ol>
     * Only stalls that are actually buyable right now are ever passed in, so villagers always
     * buy what is available.
     */
    public Offer selectBasket(List<Offer> offers, ToDoubleFunction<CommodityCategory> categoryWeight) {
        if (offers.isEmpty()) {
            return null;
        }

        Map<CommodityCategory, List<Offer>> byCategory = new LinkedHashMap<>();
        for (Offer offer : offers) {
            byCategory.computeIfAbsent(offer.category(), key -> new ArrayList<>()).add(offer);
        }

        List<Offer> categoryOffers = pickWeightedCategory(byCategory, categoryWeight);
        if (categoryOffers == null || categoryOffers.isEmpty()) {
            return null;
        }

        // Even spread across the distinct items available in the chosen category.
        Map<Item, List<Offer>> byItem = new LinkedHashMap<>();
        for (Offer offer : categoryOffers) {
            byItem.computeIfAbsent(offer.commodity(), key -> new ArrayList<>()).add(offer);
        }
        List<Item> items = new ArrayList<>(byItem.keySet());
        List<Offer> itemOffers = byItem.get(items.get(ThreadLocalRandom.current().nextInt(items.size())));

        return cheapestWithinBand(itemOffers);
    }

    private List<Offer> pickWeightedCategory(
            Map<CommodityCategory, List<Offer>> byCategory,
            ToDoubleFunction<CommodityCategory> categoryWeight
    ) {
        List<CommodityCategory> categories = new ArrayList<>(byCategory.keySet());
        double[] weights = new double[categories.size()];
        double totalWeight = 0.0;
        for (int i = 0; i < categories.size(); i++) {
            double weight = Math.max(0.0, categoryWeight.applyAsDouble(categories.get(i)));
            weights[i] = weight;
            totalWeight += weight;
        }

        if (totalWeight <= 0.0) {
            // No basket signal available — spread evenly across categories.
            return byCategory.get(categories.get(ThreadLocalRandom.current().nextInt(categories.size())));
        }

        double roll = ThreadLocalRandom.current().nextDouble(totalWeight);
        double cumulative = 0.0;
        for (int i = 0; i < categories.size(); i++) {
            cumulative += weights[i];
            if (roll < cumulative) {
                return byCategory.get(categories.get(i));
            }
        }
        return byCategory.get(categories.get(categories.size() - 1));
    }

    private Offer cheapestWithinBand(List<Offer> group) {
        long lowest = group.stream().mapToLong(Offer::priceSpurs).min().orElse(Long.MAX_VALUE);
        double band = MarketCoordConfig.PURCHASING.competitivePriceBand.get();
        long ceiling = lowest + Math.round(lowest * band);
        List<Offer> competitive = group.stream()
                .filter(offer -> offer.priceSpurs() <= ceiling)
                .toList();
        if (competitive.isEmpty()) {
            return null;
        }
        return competitive.get(ThreadLocalRandom.current().nextInt(competitive.size()));
    }
}
