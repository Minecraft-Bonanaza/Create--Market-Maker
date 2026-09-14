package com.bng.marketcoordination.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class MarketCoordConfig {
    private MarketCoordConfig() {}

    public static final ModConfigSpec SPEC;
    public static final MarketSection MARKET;
    public static final GrowthSection GROWTH;
    public static final PurchasingSection PURCHASING;
    public static final PopulationSection POPULATION;
    public static final AntiAbuseSection ANTI_ABUSE;
    public static final CategorySection CATEGORY;
    public static final PerformanceSection PERFORMANCE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        MARKET = new MarketSection(builder);
        GROWTH = new GrowthSection(builder);
        PURCHASING = new PurchasingSection(builder);
        POPULATION = new PopulationSection(builder);
        ANTI_ABUSE = new AntiAbuseSection(builder);
        CATEGORY = new CategorySection(builder);
        PERFORMANCE = new PerformanceSection(builder);

        SPEC = builder.build();
    }

    public static final class MarketSection {
        public final ModConfigSpec.IntValue activityWindowDays;
        public final ModConfigSpec.IntValue minimumMarketSpacing;
        public final ModConfigSpec.IntValue startingDailyBudget;
        public final ModConfigSpec.IntValue maximumDailyBudget;

        MarketSection(ModConfigSpec.Builder builder) {
            builder.push("market");
            activityWindowDays = builder
                    .comment("Rolling activity window length in Minecraft days")
                    .defineInRange("activity_window_days", 7, 1, 30);
            minimumMarketSpacing = builder
                    .comment("Minimum block distance between market ledgers")
                    .defineInRange("minimum_market_spacing", 750, 0, 10000);
            startingDailyBudget = builder
                    .comment("Starting daily procurement budget in Cogs (spur units)")
                    .defineInRange("starting_daily_budget", 20, 1, 100000);
            maximumDailyBudget = builder
                    .comment("Maximum daily procurement budget in Cogs (spur units)")
                    .defineInRange("maximum_daily_budget", 400, 1, 1000000);
            builder.pop();
        }
    }

    public static final class GrowthSection {
        public final ModConfigSpec.DoubleValue utilizationGrowthThreshold;
        public final ModConfigSpec.DoubleValue maximumDailyGrowth;
        public final ModConfigSpec.DoubleValue maximumDailyDecay;
        public final ModConfigSpec.DoubleValue decayFloorFraction;

        GrowthSection(ModConfigSpec.Builder builder) {
            builder.push("growth");
            utilizationGrowthThreshold = builder
                    .comment("Break-even point as a fraction of the daily budget spent.",
                            "Spending more than this fraction grows the market (scaling up to full",
                            "growth at 100% utilization); spending less makes it decay.")
                    .defineInRange("utilization_growth_threshold", 0.50, 0.0, 1.0);
            maximumDailyGrowth = builder
                    .comment("Maximum single-day rise in activity score, reached at 100% budget utilization.")
                    .defineInRange("maximum_daily_growth", 0.08, 0.0, 1.0);
            maximumDailyDecay = builder
                    .comment("Maximum single-day fall in activity score, reached at 0% budget utilization.",
                            "Deliberately smaller than growth so idle/offline days erode progress gently.")
                    .defineInRange("maximum_daily_decay", 0.02, 0.0, 1.0);
            decayFloorFraction = builder
                    .comment("Decay can never push the activity score below this fraction of the market's",
                            "highest achieved score, so players are not punished for being away a while.")
                    .defineInRange("decay_floor_fraction", 0.70, 0.0, 1.0);
            builder.pop();
        }
    }

    public static final class PurchasingSection {
        public final ModConfigSpec.DoubleValue competitivePriceBand;
        public final ModConfigSpec.BooleanValue useStockMarketCeiling;
        public final ModConfigSpec.DoubleValue maximumPriceMultiplier;
        public final ModConfigSpec.BooleanValue demandCurveEnabled;
        public final ModConfigSpec.DoubleValue demandFloorMultiplier;
        public final ModConfigSpec.DoubleValue demandCurveExponent;

        PurchasingSection(ModConfigSpec.Builder builder) {
            builder.push("purchasing");
            competitivePriceBand = builder
                    .comment("Fractional band around lowest price for competitive seller selection")
                    .defineInRange("competitive_price_band", 0.05, 0.0, 1.0);
            useStockMarketCeiling = builder
                    .comment("Use Stock Market reference prices when mod is present")
                    .define("use_stock_market_ceiling", true);
            maximumPriceMultiplier = builder
                    .comment("Maximum allowed price as multiple of reference price (willingness-to-pay when a commodity's daily quota is empty)")
                    .defineInRange("maximum_price_multiplier", 1.25, 1.0, 10.0);
            demandCurveEnabled = builder
                    .comment(
                            "Enable diminishing demand: as a commodity/category daily quota fills, the",
                            "acceptable price per unit slides down so the market expects more quantity per",
                            "unit of money and pricier stalls drop out first.")
                    .define("demand_curve_enabled", true);
            demandFloorMultiplier = builder
                    .comment(
                            "Price multiplier the willingness-to-pay slides down to as a quota approaches full.",
                            "Clamped to at most maximum_price_multiplier; e.g. 0.5 means only offers at half the",
                            "reference price (or cheaper) are still bought once a quota is saturated.")
                    .defineInRange("demand_floor_multiplier", 0.5, 0.0, 10.0);
            demandCurveExponent = builder
                    .comment(
                            "Shape of the demand falloff. 1.0 = linear; >1 keeps demand high early and drops",
                            "sharply near full; <1 drops demand quickly then tapers.")
                    .defineInRange("demand_curve_exponent", 1.0, 0.1, 5.0);
            builder.pop();
        }
    }

    public static final class PopulationSection {
        public final ModConfigSpec.IntValue perVillagerDailySpendCap;
        public final ModConfigSpec.IntValue maxConcurrentShoppersPerMarket;

        PopulationSection(ModConfigSpec.Builder builder) {
            builder.push("population");
            perVillagerDailySpendCap = builder
                    .comment("Maximum Cogs a single villager may spend per Minecraft day (0 = unlimited).",
                            "Villagers buy aggressively until they hit this cap, so a larger market",
                            "budget can only be fully spent by having more villagers — more villagers",
                            "means more trade volume and therefore more market growth.")
                    .defineInRange("per_villager_daily_spend_cap", 16, 0, 100000);
            maxConcurrentShoppersPerMarket = builder
                    .comment("Soft cap on how many villagers actively path to stalls at once per market.",
                            "When a market has more registered villagers than this, each one shops",
                            "proportionally less often so pathfinding cost stays bounded in huge markets.",
                            "Total daily spend is unaffected below this size; 0 disables the throttle.")
                    .defineInRange("max_concurrent_shoppers_per_market", 24, 0, 512);
            builder.pop();
        }
    }

    public static final class AntiAbuseSection {
        public final ModConfigSpec.BooleanValue countUniquePlayerUuid;
        public final ModConfigSpec.IntValue maxRepeatsBeforeDiminish;
        public final ModConfigSpec.DoubleValue diminishedGrowthMultiplier;
        public final ModConfigSpec.IntValue repeatWindowTicks;
        public final ModConfigSpec.BooleanValue ignoreListingVolume;

        AntiAbuseSection(ModConfigSpec.Builder builder) {
            builder.push("anti_abuse");
            countUniquePlayerUuid = builder
                    .comment("Count unique sellers by stall identity (proxy for player UUID)")
                    .define("count_unique_player_uuid", true);
            maxRepeatsBeforeDiminish = builder
                    .comment("Repeated same-seller+item trades before growth weight diminishes")
                    .defineInRange("max_repeats_before_diminish", 3, 1, 64);
            diminishedGrowthMultiplier = builder
                    .comment("Growth weight multiplier after repeat threshold")
                    .defineInRange("diminished_growth_multiplier", 0.25, 0.0, 1.0);
            repeatWindowTicks = builder
                    .comment("Window in ticks for repeat trade detection")
                    .defineInRange("repeat_window_ticks", 24000, 100, 240000);
            ignoreListingVolume = builder
                    .comment("Only completed purchases count toward activity (always true in v0.2+)")
                    .define("ignore_listing_volume", true);
            builder.pop();
        }
    }

    public static final class CategorySection {
        public final ModConfigSpec.BooleanValue enabled;
        public final ModConfigSpec.DoubleValue specializationBias;

        CategorySection(ModConfigSpec.Builder builder) {
            builder.push("categories");
            enabled = builder
                    .comment("Split daily budget across commodity categories")
                    .define("enabled", true);
            specializationBias = builder
                    .comment("Extra category budget weight from regional trade mix (0 disables)")
                    .defineInRange("specialization_bias", 0.15, 0.0, 1.0);
            builder.pop();
        }
    }

    public static final class PerformanceSection {
        public final ModConfigSpec.IntValue dayCycleStaggerTicks;
        public final ModConfigSpec.IntValue uiRefreshIntervalTicks;
        public final ModConfigSpec.BooleanValue stallCacheEnabled;

        PerformanceSection(ModConfigSpec.Builder builder) {
            builder.push("performance");
            dayCycleStaggerTicks = builder
                    .comment("Max ticks to spread daily market processing across")
                    .defineInRange("day_cycle_stagger_ticks", 32, 1, 200);
            uiRefreshIntervalTicks = builder
                    .comment("Client UI refresh interval while screen is open (20 ticks = 1s)")
                    .defineInRange("ui_refresh_interval_ticks", 20, 1, 200);
            stallCacheEnabled = builder
                    .comment("Cache per-market stall lists instead of scanning each purchase")
                    .define("stall_cache_enabled", true);
            builder.pop();
        }
    }
}
