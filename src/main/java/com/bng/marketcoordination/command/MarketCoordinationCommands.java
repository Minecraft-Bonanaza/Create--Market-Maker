package com.bng.marketcoordination.command;

import com.bng.marketcoordination.MarketServices;
import com.bng.marketcoordination.economy.CommodityCategory;
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
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import com.bng.marketcoordination.economy.TraderLedger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
                    CommandSourceStack source = ctx.getSource();
                    if (MarketRegistry.count() == 0) {
                        source.sendSuccess(
                                () -> Component.literal("No markets registered yet.").withStyle(ChatFormatting.GRAY),
                                false);
                        return 0;
                    }
                    boolean first = true;
                    for (MarketState market : MarketRegistry.all()) {
                        if (!first) {
                            source.sendSuccess(Component::empty, false);
                        }
                        first = false;
                        sendMarketActivity(source, market);
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
                    CommandSourceStack source = ctx.getSource();
                    if (MarketRegistry.count() == 0) {
                        source.sendSuccess(
                                () -> Component.literal("No markets registered yet.").withStyle(ChatFormatting.GRAY),
                                false);
                        return 0;
                    }
                    boolean first = true;
                    for (MarketState market : MarketRegistry.all()) {
                        if (!first) {
                            source.sendSuccess(Component::empty, false);
                        }
                        first = false;
                        sendMarketDebug(source, market);
                    }
                    return MarketRegistry.count();
                }));

        root.then(Commands.literal("prices")
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();
                    if (!com.bng.marketcoordination.integration.ModPresence.isStockMarketLoaded()) {
                        source.sendSuccess(
                                () -> Component.literal("Stock Market is not loaded — no global price index.")
                                        .withStyle(ChatFormatting.GRAY),
                                false);
                        return 0;
                    }
                    if (!com.bng.marketcoordination.integration.ModPresence.isVillagerCommerceLoaded()) {
                        source.sendSuccess(
                                () -> Component.literal("Villager Commerce is not loaded — no stalls to index.")
                                        .withStyle(ChatFormatting.GRAY),
                                false);
                        return 0;
                    }
                    return com.bng.marketcoordination.integration.StockMarketDebug.sendPrices(source);
                }));

        root.then(Commands.literal("traders")
                .executes(ctx -> showTraders(ctx.getSource())));

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

    private static void sendMarketDebug(CommandSourceStack source, MarketState market) {
        source.sendSuccess(
                () -> Component.literal("\u2550\u2550 " + market.displayName() + " \u2550\u2550")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                false);
        debugLine(source, "Tier",
                market.tier().displayName()
                        + "  (activity " + percent(market.activityScore())
                        + ", peak " + percent(market.peakActivityScore()) + ")");
        debugLine(source, "Budget",
                NumismaticsAdapter.formatSpurs(market.spentTodaySpurs()) + " spent"
                        + "  /  " + NumismaticsAdapter.formatSpurs(market.dailyBudgetSpurs()) + " daily");
        debugLine(source, "Remaining",
                NumismaticsAdapter.formatSpurs(MarketServices.BUDGET.remainingSpurs(market)));
        debugLine(source, "Utilization", percent(GROWTH.computeBudgetUtilization(market)));
        debugLine(source, "Issuance",
                "daily " + NumismaticsAdapter.formatSpurs(market.issuance().dailySpurs())
                        + "  |  lifetime " + NumismaticsAdapter.formatSpurs(market.issuance().lifetimeSpurs()));
        debugLine(source, "Trade volume",
                NumismaticsAdapter.formatSpurs(market.traders().totalVolumeSpurs())
                        + "  across " + market.traders().traderCount() + " trader(s)");
        debugLine(source, "Dominant", market.regionalProfile().dominantCategory().id());

        source.sendSuccess(
                () -> Component.literal("  Baskets (spent / daily budget):").withStyle(ChatFormatting.AQUA),
                false);
        for (CommodityCategory category : CommodityCategory.values()) {
            long spent = MarketServices.CATEGORY_BUDGET.categorySpent(market, category);
            long budget = MarketServices.CATEGORY_BUDGET.categoryBudgetSpurs(market, category);
            String line = "    " + category.id() + ": "
                    + NumismaticsAdapter.formatSpurs(spent) + " / " + NumismaticsAdapter.formatSpurs(budget);
            ChatFormatting color = spent > 0 ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY;
            source.sendSuccess(() -> Component.literal(line).withStyle(color), false);
        }
    }

    /** Aggregated per-owner totals across every market, plus a per-market volume breakdown. */
    private static int showTraders(CommandSourceStack source) {
        if (MarketRegistry.count() == 0) {
            source.sendSuccess(
                    () -> Component.literal("No markets registered yet.").withStyle(ChatFormatting.GRAY),
                    false);
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal("\u2550\u2550 Trade Ledger \u2550\u2550")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                false);

        // Per-market volume.
        source.sendSuccess(
                () -> Component.literal("  Markets (volume / traders):").withStyle(ChatFormatting.AQUA),
                false);
        Map<UUID, long[]> byOwner = new HashMap<>(); // id -> [volumeSpurs, trades]
        Map<UUID, String> names = new HashMap<>();
        for (MarketState market : MarketRegistry.all()) {
            TraderLedger ledger = market.traders();
            String line = "    " + market.displayName() + ": "
                    + NumismaticsAdapter.formatSpurs(ledger.totalVolumeSpurs())
                    + "  (" + ledger.traderCount() + " trader(s))";
            source.sendSuccess(() -> Component.literal(line).withStyle(ChatFormatting.WHITE), false);
            for (TraderLedger.Trader trader : ledger.traders()) {
                long[] totals = byOwner.computeIfAbsent(trader.id(), id -> new long[2]);
                totals[0] += trader.volumeSpurs();
                totals[1] += trader.trades();
                names.put(trader.id(), trader.name());
            }
        }

        // Global leaderboard, highest volume first.
        source.sendSuccess(
                () -> Component.literal("  Top traders (volume / profit):").withStyle(ChatFormatting.AQUA),
                false);
        if (byOwner.isEmpty()) {
            source.sendSuccess(
                    () -> Component.literal(
                            "    (no attributed trades yet — stalls need an owner and a completed sale)")
                            .withStyle(ChatFormatting.DARK_GRAY),
                    false);
            return 0;
        }

        List<Map.Entry<UUID, long[]>> ranked = new ArrayList<>(byOwner.entrySet());
        ranked.sort(Comparator.comparingLong((Map.Entry<UUID, long[]> e) -> e.getValue()[0]).reversed());
        int rank = 0;
        for (Map.Entry<UUID, long[]> e : ranked) {
            if (rank >= 15) {
                int remaining = ranked.size() - rank;
                source.sendSuccess(
                        () -> Component.literal("    ... and " + remaining + " more")
                                .withStyle(ChatFormatting.DARK_GRAY),
                        false);
                break;
            }
            rank++;
            int position = rank;
            String name = names.getOrDefault(e.getKey(), e.getKey().toString().substring(0, 8));
            long volume = e.getValue()[0];
            long trades = e.getValue()[1];
            String line = "    " + position + ". " + name + " \u2014 "
                    + NumismaticsAdapter.formatSpurs(volume) + " across " + trades + " trade(s)";
            source.sendSuccess(() -> Component.literal(line).withStyle(ChatFormatting.GREEN), false);
        }
        return ranked.size();
    }

    private static void sendMarketActivity(CommandSourceStack source, MarketState market) {
        source.sendSuccess(
                () -> Component.literal("\u2550\u2550 " + market.displayName() + " \u2550\u2550")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                false);
        debugLine(source, "Activity",
                percent(market.activityScore()) + "  (peak " + percent(market.peakActivityScore()) + ")");
        debugLine(source, "Utilization", percent(GROWTH.computeBudgetUtilization(market)) + " of today's budget");
        debugLine(source, "7d Volume",
                NumismaticsAdapter.formatSpurs(market.activityWindow().totalTradeVolumeSpurs()));
        debugLine(source, "Sellers today",
                String.valueOf(market.activityWindow().currentDay().uniqueSellers()));
    }

    private static void debugLine(CommandSourceStack source, String label, String value) {
        source.sendSuccess(
                () -> Component.literal("  " + label + ": ").withStyle(ChatFormatting.AQUA)
                        .append(Component.literal(value).withStyle(ChatFormatting.WHITE)),
                false);
    }

    private static String percent(double fraction) {
        return String.format(Locale.ROOT, "%.1f%%", fraction * 100);
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
                () -> Component.literal("\u2550\u2550 " + market.displayName() + " \u2550\u2550")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                false);
        debugLine(source, "Tier", market.tier().displayName()
                + "  (activity " + percent(market.activityScore()) + ")");
        debugLine(source, "Budget remaining",
                NumismaticsAdapter.formatSpurs(market.remainingBudgetSpurs()));
        debugLine(source, "7d Volume",
                NumismaticsAdapter.formatSpurs(market.activityWindow().totalTradeVolumeSpurs()));
        debugLine(source, "Specialization", market.regionalProfile().dominantCategory().id());
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
