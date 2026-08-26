package com.bng.marketcoordination.command;

import com.bng.marketcoordination.MarketServices;
import com.bng.marketcoordination.economy.NationId;
import com.bng.marketcoordination.economy.NationRegistry;
import com.bng.marketcoordination.economy.NationState;
import com.bng.marketcoordination.economy.NationStatisticsService;
import com.bng.marketcoordination.integration.NumismaticsAdapter;
import com.bng.marketcoordination.market.MarketGrowthService;
import com.bng.marketcoordination.market.MarketRegistry;
import com.bng.marketcoordination.market.MarketState;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Locale;
import java.util.Optional;

public final class MarketCoordinationCommands {
    private static final MarketGrowthService GROWTH = new MarketGrowthService();

    private MarketCoordinationCommands() {}

    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("marketcoord")
                .requires(source -> source.hasPermission(2));

        root.then(Commands.literal("info")
                .executes(ctx -> showInfo(ctx.getSource(), null))
                .then(Commands.argument("market", StringArgumentType.greedyString())
                        .executes(ctx -> showInfo(ctx.getSource(), StringArgumentType.getString(ctx, "market")))));

        root.then(Commands.literal("list")
                .executes(ctx -> {
                    if (MarketRegistry.count() == 0) {
                        ctx.getSource().sendSuccess(
                                () -> Component.literal("No markets registered yet."),
                                false);
                        return 0;
                    }
                    for (MarketState market : MarketRegistry.all()) {
                        ctx.getSource().sendSuccess(
                                () -> Component.literal("- " + market.displayName()
                                        + " [" + market.id().uuid() + "] tier=" + market.tier().displayName()
                                        + " nation=" + (market.nationId() == null ? "none" : market.nationId().uuid())),
                                false);
                    }
                    return MarketRegistry.count();
                }));

        root.then(Commands.literal("activity")
                .executes(ctx -> {
                    for (MarketState market : MarketRegistry.all()) {
                        double score = GROWTH.computeActivityScore(market);
                        ctx.getSource().sendSuccess(
                                () -> Component.literal(market.displayName()
                                        + ": activity=" + String.format(Locale.ROOT, "%.2f", score * 100) + "%"
                                        + ", volume=" + NumismaticsAdapter.formatSpurs(
                                                market.activityWindow().totalTradeVolumeSpurs())
                                        + ", sellers=" + market.activityWindow().currentDay().uniqueSellers()),
                                false);
                    }
                    return MarketRegistry.count();
                }));

        root.then(Commands.literal("reset")
                .executes(ctx -> {
                    for (MarketState market : MarketRegistry.all()) {
                        market.resetDailySpending();
                    }
                    MarketServices.COMMODITY_CAPS.resetDaily();
                    MarketServices.CATEGORY_BUDGET.resetDaily();
                    MarketRegistry.persist();
                    ctx.getSource().sendSuccess(
                            () -> Component.literal("Reset daily spending and commodity caps for all markets."),
                            true);
                    return MarketRegistry.count();
                }));

        root.then(Commands.literal("setbudget")
                .then(Commands.argument("cogs", LongArgumentType.longArg(1))
                        .executes(ctx -> {
                            long cogs = LongArgumentType.getLong(ctx, "cogs");
                            long spurs = cogs * MarketRegistry.CoinSpurUnits.COG_VALUE_SPURS;
                            for (MarketState market : MarketRegistry.all()) {
                                market.setDailyBudgetSpurs(spurs);
                            }
                            MarketRegistry.persist();
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("Set daily budget to " + cogs + " Cogs for all markets"),
                                    true);
                            return (int) Math.min(cogs, Integer.MAX_VALUE);
                        })));

        root.then(Commands.literal("debug")
                .executes(ctx -> {
                    for (MarketState market : MarketRegistry.all()) {
                        ctx.getSource().sendSuccess(
                                () -> Component.literal(market.displayName()
                                        + " budget " + NumismaticsAdapter.formatSpurs(market.spentTodaySpurs())
                                        + " / " + NumismaticsAdapter.formatSpurs(market.dailyBudgetSpurs())
                                        + " remaining="
                                        + NumismaticsAdapter.formatSpurs(MarketServices.BUDGET.remainingSpurs(market))
                                        + " | issuance daily="
                                        + NumismaticsAdapter.formatSpurs(market.issuance().dailySpurs())
                                        + " lifetime="
                                        + NumismaticsAdapter.formatSpurs(market.issuance().lifetimeSpurs())
                                        + " | dominant="
                                        + market.regionalProfile().dominantCategory().id()),
                                false);
                    }
                    return MarketRegistry.count();
                }));

        root.then(Commands.literal("nation")
                .then(Commands.literal("list")
                        .executes(ctx -> {
                            if (NationRegistry.all().isEmpty()) {
                                ctx.getSource().sendSuccess(() -> Component.literal("No nations defined."), false);
                                return 0;
                            }
                            for (NationState nation : NationRegistry.all()) {
                                ctx.getSource().sendSuccess(
                                        () -> Component.literal("- " + nation.displayName()
                                                + " [" + nation.id().uuid() + "] markets="
                                                + nation.memberMarkets().size()),
                                        false);
                            }
                            return NationRegistry.all().size();
                        }))
                .then(Commands.literal("info")
                        .then(Commands.argument("nation", StringArgumentType.greedyString())
                                .executes(ctx -> showNationInfo(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "nation")))))
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "name");
                                    NationState nation = NationRegistry.create(NationId.random(), name);
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal("Created nation " + nation.displayName()
                                                    + " [" + nation.id().uuid() + "]"),
                                            true);
                                    return 1;
                                })))
                .then(Commands.literal("link")
                        .then(Commands.argument("nation", StringArgumentType.string())
                                .then(Commands.argument("market", StringArgumentType.string())
                                        .executes(ctx -> linkNation(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "nation"),
                                                StringArgumentType.getString(ctx, "market"),
                                                false))
                                        .then(Commands.literal("capital")
                                                .executes(ctx -> linkNation(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "nation"),
                                                        StringArgumentType.getString(ctx, "market"),
                                                        true)))))));

        dispatcher.register(root);
    }

    private static int showInfo(CommandSourceStack source, String marketName) {
        if (MarketRegistry.count() == 0) {
            source.sendSuccess(() -> Component.literal("No markets registered yet."), false);
            return 0;
        }
        if (marketName == null || marketName.isBlank()) {
            source.sendSuccess(
                    () -> Component.literal("Market Coordination: " + MarketRegistry.count() + " registered market(s)"),
                    false);
            return MarketRegistry.count();
        }
        Optional<MarketState> match = MarketRegistry.all().stream()
                .filter(market -> market.displayName().equalsIgnoreCase(marketName.trim()))
                .findFirst();
        if (match.isEmpty()) {
            source.sendFailure(Component.literal("Unknown market: " + marketName));
            return 0;
        }
        MarketState market = match.get();
        source.sendSuccess(
                () -> Component.literal(market.displayName()
                        + " | tier=" + market.tier().displayName()
                        + " | activity=" + (int) Math.round(market.activityScore() * 100) + "%"
                        + " | budget remaining=" + NumismaticsAdapter.formatSpurs(market.remainingBudgetSpurs())
                        + " | 7d volume=" + NumismaticsAdapter.formatSpurs(market.activityWindow().totalTradeVolumeSpurs())
                        + " | specialization=" + market.regionalProfile().dominantCategory().id()),
                false);
        return 1;
    }

    private static int showNationInfo(CommandSourceStack source, String nationName) {
        Optional<NationState> nation = NationRegistry.getByName(nationName);
        if (nation.isEmpty()) {
            source.sendFailure(Component.literal("Unknown nation: " + nationName));
            return 0;
        }
        NationStatisticsService.NationStatistics stats = NationStatisticsService.summarize(nation.get());
        source.sendSuccess(
                () -> Component.literal(stats.nationName()
                        + " | markets=" + stats.linkedMarkets()
                        + " | volume=" + stats.formattedVolume()
                        + " | issuance=" + stats.formattedIssuance()
                        + " | capital tier=" + stats.capitalTierOrdinal()),
                false);
        return 1;
    }

    private static int linkNation(CommandSourceStack source, String nationName, String marketName, boolean capital) {
        Optional<NationState> nation = NationRegistry.getByName(nationName);
        if (nation.isEmpty()) {
            source.sendFailure(Component.literal("Unknown nation: " + nationName));
            return 0;
        }
        Optional<MarketState> market = MarketRegistry.all().stream()
                .filter(entry -> entry.displayName().equalsIgnoreCase(marketName.trim()))
                .findFirst();
        if (market.isEmpty()) {
            source.sendFailure(Component.literal("Unknown market: " + marketName));
            return 0;
        }
        NationRegistry.linkMarket(nation.get().id(), market.get().id(), capital);
        MarketRegistry.persist();
        source.sendSuccess(
                () -> Component.literal("Linked " + market.get().displayName() + " to nation "
                        + nation.get().displayName() + (capital ? " (capital)" : "")),
                true);
        return 1;
    }
}
