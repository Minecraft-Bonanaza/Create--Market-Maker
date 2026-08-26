package com.bng.marketcoordination.market;

public enum MarketTier {
    OUTPOST("Outpost Market"),
    HAMLET("Hamlet Market"),
    VILLAGE("Village Market"),
    TOWN("Town Market"),
    CITY("City Market");

    private final String displayName;

    MarketTier(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static MarketTier fromNormalizedScore(double score) {
        if (score >= 0.85) return CITY;
        if (score >= 0.65) return TOWN;
        if (score >= 0.45) return VILLAGE;
        if (score >= 0.25) return HAMLET;
        return OUTPOST;
    }
}
