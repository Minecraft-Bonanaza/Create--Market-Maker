package com.bng.marketcoordination.economy;

import com.bng.marketcoordination.integration.NumismaticsAdapter;
import com.bng.marketcoordination.market.MarketId;
import com.bng.marketcoordination.market.MarketRegistry;
import com.bng.marketcoordination.market.MarketState;

public final class NationStatisticsService {
    private NationStatisticsService() {}

    public static NationStatistics summarize(NationState nation) {
        long totalVolume = 0L;
        long totalIssuance = 0L;
        int marketCount = 0;
        int capitalTierScore = 0;

        for (MarketId marketId : nation.memberMarkets()) {
            MarketState market = MarketRegistry.get(marketId).orElse(null);
            if (market == null) {
                continue;
            }
            marketCount++;
            totalVolume += market.activityWindow().totalTradeVolumeSpurs();
            totalIssuance += market.issuance().lifetimeSpurs();
            if (marketId.equals(nation.capitalMarketId())) {
                capitalTierScore = market.tier().ordinal();
            }
        }

        return new NationStatistics(
                nation.displayName(),
                marketCount,
                totalVolume,
                totalIssuance,
                capitalTierScore
        );
    }

    public record NationStatistics(
            String nationName,
            int linkedMarkets,
            long totalVolumeSpurs,
            long totalIssuanceSpurs,
            int capitalTierOrdinal
    ) {
        public String formattedVolume() {
            return NumismaticsAdapter.formatSpurs(totalVolumeSpurs);
        }

        public String formattedIssuance() {
            return NumismaticsAdapter.formatSpurs(totalIssuanceSpurs);
        }
    }
}
