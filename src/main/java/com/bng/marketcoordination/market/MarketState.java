package com.bng.marketcoordination.market;

import com.bng.marketcoordination.economy.IssuanceTracker;
import com.bng.marketcoordination.economy.NationId;
import com.bng.marketcoordination.economy.RegionalProfile;
import com.bng.marketcoordination.economy.TraderLedger;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.time.Instant;

public class MarketState {
    private final MarketId id;
    private String displayName;
    private ResourceKey<Level> dimension;
    private BlockPos ledgerPos;
    private Instant createdAt;
    private double activityScore;
    private double previousActivityScore;
    private double peakActivityScore;
    private MarketTier tier;
    private long dailyBudgetSpurs;
    private long spentTodaySpurs;
    private NationId nationId;
    private boolean capitalMarket;
    private final RollingActivityWindow activityWindow;
    private final IssuanceTracker issuance = new IssuanceTracker();
    private final RegionalProfile regionalProfile = new RegionalProfile();
    private final TraderLedger traders = new TraderLedger();

    public MarketState(MarketId id, String displayName, ResourceKey<Level> dimension, BlockPos ledgerPos) {
        this.id = id;
        this.displayName = displayName;
        this.dimension = dimension;
        this.ledgerPos = ledgerPos;
        this.createdAt = Instant.now();
        this.activityScore = 0.0;
        this.previousActivityScore = 0.0;
        this.peakActivityScore = 0.0;
        this.tier = MarketTier.OUTPOST;
        this.dailyBudgetSpurs = 0L;
        this.spentTodaySpurs = 0L;
        this.activityWindow = new RollingActivityWindow(7);
    }

    public MarketId id() { return id; }
    public String displayName() { return displayName; }
    public ResourceKey<Level> dimension() { return dimension; }
    public BlockPos ledgerPos() { return ledgerPos; }
    public Instant createdAt() { return createdAt; }
    public double activityScore() { return activityScore; }
    public double previousActivityScore() { return previousActivityScore; }
    public double peakActivityScore() { return peakActivityScore; }
    public MarketTier tier() { return tier; }
    public long dailyBudgetSpurs() { return dailyBudgetSpurs; }
    public long spentTodaySpurs() { return spentTodaySpurs; }
    public NationId nationId() { return nationId; }
    public boolean capitalMarket() { return capitalMarket; }
    public RollingActivityWindow activityWindow() { return activityWindow; }
    public IssuanceTracker issuance() { return issuance; }
    public RegionalProfile regionalProfile() { return regionalProfile; }
    public TraderLedger traders() { return traders; }

    public long remainingBudgetSpurs() {
        return Math.max(0L, dailyBudgetSpurs - spentTodaySpurs);
    }

    public void setActivityScore(double activityScore) { this.activityScore = activityScore; }
    public void setPreviousActivityScore(double previousActivityScore) {
        this.previousActivityScore = previousActivityScore;
    }
    public void setPeakActivityScore(double peakActivityScore) {
        this.peakActivityScore = Math.max(0.0, peakActivityScore);
    }
    public void setTier(MarketTier tier) { this.tier = tier; }
    public void setDailyBudgetSpurs(long dailyBudgetSpurs) { this.dailyBudgetSpurs = dailyBudgetSpurs; }
    public void resetDailySpending() { this.spentTodaySpurs = 0L; }
    public void recordSpend(long spurAmount) { this.spentTodaySpurs += Math.max(0L, spurAmount); }

    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setSpentTodaySpurs(long spentTodaySpurs) { this.spentTodaySpurs = Math.max(0L, spentTodaySpurs); }
    public void setNationId(NationId nationId) { this.nationId = nationId; }
    public void setCapitalMarket(boolean capitalMarket) { this.capitalMarket = capitalMarket; }
}
