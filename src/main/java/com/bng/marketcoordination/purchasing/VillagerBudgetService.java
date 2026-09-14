package com.bng.marketcoordination.purchasing;

import com.bng.marketcoordination.config.MarketCoordConfig;
import com.bng.marketcoordination.market.MarketRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Meters how much each villager may spend per Minecraft day. This makes population the
 * lever for market throughput: a single villager can only drain so much of the market's
 * daily budget, so fully spending a large budget (and thus maximising growth) requires
 * more villagers.
 */
public final class VillagerBudgetService {
    private final Map<UUID, Long> spentTodaySpurs = new HashMap<>();

    public long dailyCapSpurs() {
        long capCogs = MarketCoordConfig.POPULATION.perVillagerDailySpendCap.get();
        return capCogs * MarketRegistry.CoinSpurUnits.COG_VALUE_SPURS;
    }

    public long spentToday(UUID villagerId) {
        if (villagerId == null) {
            return 0L;
        }
        return spentTodaySpurs.getOrDefault(villagerId, 0L);
    }

    /** True if this villager could spend {@code spurCost} more without exceeding its daily cap. */
    public boolean canSpend(UUID villagerId, long spurCost) {
        long cap = dailyCapSpurs();
        if (cap <= 0L || villagerId == null) {
            return true; // 0 = unlimited
        }
        return spentToday(villagerId) + Math.max(0L, spurCost) <= cap;
    }

    /** True if this villager has already exhausted its daily spending allowance. */
    public boolean hasReachedCap(UUID villagerId) {
        long cap = dailyCapSpurs();
        if (cap <= 0L || villagerId == null) {
            return false;
        }
        return spentToday(villagerId) >= cap;
    }

    public void recordSpend(UUID villagerId, long spurCost) {
        if (villagerId == null || spurCost <= 0L) {
            return;
        }
        spentTodaySpurs.merge(villagerId, spurCost, Long::sum);
    }

    public void resetDaily() {
        spentTodaySpurs.clear();
    }
}
