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
        public final ModConfigSpec.DoubleValue tradeVolumeWeight;
        public final ModConfigSpec.DoubleValue uniqueSellerWeight;
        public final ModConfigSpec.DoubleValue populationWeight;
        public final ModConfigSpec.DoubleValue commodityDiversityWeight;
        public final ModConfigSpec.DoubleValue maximumDailyGrowth;
        public final ModConfigSpec.DoubleValue activityDecayRate;

        GrowthSection(ModConfigSpec.Builder builder) {
            builder.push("growth");
            tradeVolumeWeight = builder.defineInRange("trade_volume_weight", 0.40, 0.0, 1.0);
            uniqueSellerWeight = builder.defineInRange("unique_seller_weight", 0.25, 0.0, 1.0);
            populationWeight = builder.defineInRange("population_weight", 0.20, 0.0, 1.0);
            commodityDiversityWeight = builder.defineInRange("commodity_diversity_weight", 0.15, 0.0, 1.0);
            maximumDailyGrowth = builder.defineInRange("maximum_daily_growth", 0.08, 0.0, 1.0);
            activityDecayRate = builder.defineInRange("activity_decay_rate", 0.05, 0.0, 1.0);
            builder.pop();
        }
    }

    public static final class PurchasingSection {
        public final ModConfigSpec.DoubleValue competitivePriceBand;
        public final ModConfigSpec.BooleanValue useStockMarketCeiling;
        public final ModConfigSpec.DoubleValue maximumPriceMultiplier;

        PurchasingSection(ModConfigSpec.Builder builder) {
            builder.push("purchasing");
            competitivePriceBand = builder
                    .comment("Fractional band around lowest price for competitive seller selection")
                    .defineInRange("competitive_price_band", 0.05, 0.0, 1.0);
            useStockMarketCeiling = builder
                    .comment("Use Stock Market reference prices when mod is present")
                    .define("use_stock_market_ceiling", true);
            maximumPriceMultiplier = builder
                    .comment("Maximum allowed price as multiple of reference price")
                    .defineInRange("maximum_price_multiplier", 1.25, 1.0, 10.0);
            builder.pop();
        }
    }

    public static final class PopulationSection {
        public final ModConfigSpec.IntValue softCapVillagers;
        public final ModConfigSpec.DoubleValue sqrtScalingFactor;

        PopulationSection(ModConfigSpec.Builder builder) {
            builder.push("population");
            softCapVillagers = builder
                    .comment("Villager count at which population contribution saturates")
                    .defineInRange("soft_cap_villagers", 32, 1, 512);
            sqrtScalingFactor = builder
                    .comment("Multiplier applied to sqrt(villagers / softCap) for population score")
                    .defineInRange("sqrt_scaling_factor", 1.0, 0.1, 5.0);
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
