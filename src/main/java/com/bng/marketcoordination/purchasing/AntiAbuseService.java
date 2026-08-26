package com.bng.marketcoordination.purchasing;

import com.bng.marketcoordination.config.MarketCoordConfig;
import com.bng.marketcoordination.market.MarketId;
import net.minecraft.world.item.Item;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AntiAbuseService {
    private record TradeKey(UUID sellerId, Item item) {}

    private record TimedTrade(long gameTime, TradeKey key, long spurAmount) {}

    private final Deque<TimedTrade> recentTrades = new ArrayDeque<>();
    private final Map<TradeKey, Integer> repeatCounts = new HashMap<>();

    public double growthWeight(long gameTime, MarketId marketId, UUID sellerId, Item item, long spurAmount) {
        if (sellerId == null || item == null) {
            return 1.0;
        }
        purgeOld(gameTime);
        TradeKey key = new TradeKey(sellerId, item);
        int repeats = repeatCounts.getOrDefault(key, 0);
        repeatCounts.put(key, repeats + 1);
        recentTrades.addLast(new TimedTrade(gameTime, key, spurAmount));

        if (repeats >= MarketCoordConfig.ANTI_ABUSE.maxRepeatsBeforeDiminish.get()) {
            return MarketCoordConfig.ANTI_ABUSE.diminishedGrowthMultiplier.get();
        }
        return 1.0;
    }

    public boolean countsAsUniqueSeller(UUID sellerId, Map<UUID, UUID> seenOwners) {
        if (!MarketCoordConfig.ANTI_ABUSE.countUniquePlayerUuid.get()) {
            return true;
        }
        if (sellerId == null) {
            return false;
        }
        return seenOwners.putIfAbsent(sellerId, sellerId) == null;
    }

    public void resetDaily() {
        recentTrades.clear();
        repeatCounts.clear();
    }

    private void purgeOld(long gameTime) {
        long window = MarketCoordConfig.ANTI_ABUSE.repeatWindowTicks.get();
        while (!recentTrades.isEmpty() && gameTime - recentTrades.peekFirst().gameTime() > window) {
            TimedTrade removed = recentTrades.removeFirst();
            repeatCounts.computeIfPresent(removed.key(), (key, count) -> count <= 1 ? null : count - 1);
        }
    }
}
