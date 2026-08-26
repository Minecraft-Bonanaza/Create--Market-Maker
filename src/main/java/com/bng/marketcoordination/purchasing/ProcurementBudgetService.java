package com.bng.marketcoordination.purchasing;

import com.bng.marketcoordination.market.MarketState;

public class ProcurementBudgetService {
    public long remainingSpurs(MarketState market) {
        return market.remainingBudgetSpurs();
    }

    public boolean canAfford(MarketState market, long spurCost) {
        return remainingSpurs(market) >= spurCost;
    }

    public void spend(MarketState market, long spurCost) {
        if (canAfford(market, spurCost)) {
            market.recordSpend(spurCost);
        }
    }
}
