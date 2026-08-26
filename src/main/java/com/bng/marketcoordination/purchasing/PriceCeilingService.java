package com.bng.marketcoordination.purchasing;

import com.bng.marketcoordination.config.MarketCoordConfig;

public class PriceCeilingService {
    public long ceilingSpurs(long referenceSpurs) {
        double multiplier = MarketCoordConfig.PURCHASING.maximumPriceMultiplier.get();
        return Math.round(referenceSpurs * multiplier);
    }

    public boolean isWithinCeiling(long offerSpurs, long referenceSpurs) {
        return offerSpurs <= ceilingSpurs(referenceSpurs);
    }
}
