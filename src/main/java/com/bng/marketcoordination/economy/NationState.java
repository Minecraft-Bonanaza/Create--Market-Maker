package com.bng.marketcoordination.economy;

import com.bng.marketcoordination.market.MarketId;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class NationState {
    private final NationId id;
    private String displayName;
    private MarketId capitalMarketId;
    private final Set<MarketId> memberMarkets = new HashSet<>();
    private final ProcurementPool procurementPool = new ProcurementPool();

    public NationState(NationId id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public NationId id() { return id; }
    public String displayName() { return displayName; }
    public MarketId capitalMarketId() { return capitalMarketId; }
    public Set<MarketId> memberMarkets() { return Set.copyOf(memberMarkets); }
    public ProcurementPool procurementPool() { return procurementPool; }

    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setCapitalMarketId(MarketId capitalMarketId) { this.capitalMarketId = capitalMarketId; }

    public void addMarket(MarketId marketId) {
        memberMarkets.add(marketId);
    }

    public void removeMarket(MarketId marketId) {
        memberMarkets.remove(marketId);
        if (marketId.equals(capitalMarketId)) {
            capitalMarketId = null;
        }
    }
}
