package com.bng.marketcoordination.purchasing;

import com.bng.marketcoordination.config.MarketCoordConfig;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class SellerSelectionService {
    public record Offer(long priceSpurs, BlockPos stallPos) {}

    public Offer select(List<Offer> offers) {
        if (offers.isEmpty()) {
            return null;
        }
        long lowest = offers.stream().mapToLong(Offer::priceSpurs).min().orElse(Long.MAX_VALUE);
        double band = MarketCoordConfig.PURCHASING.competitivePriceBand.get();
        long ceiling = lowest + Math.round(lowest * band);
        List<Offer> competitive = offers.stream()
                .filter(offer -> offer.priceSpurs() <= ceiling)
                .toList();
        if (competitive.isEmpty()) {
            return null;
        }
        return competitive.get(ThreadLocalRandom.current().nextInt(competitive.size()));
    }
}
