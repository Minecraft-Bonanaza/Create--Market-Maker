package com.bng.marketcoordination.market;

import com.bng.marketcoordination.MarketCoordinationMod;
import com.bng.marketcoordination.config.MarketCoordConfig;
import com.bng.marketcoordination.data.MarketDataSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class MarketRegistry {
    private static final Map<MarketId, MarketState> MARKETS = new ConcurrentHashMap<>();
    private static final Map<MarketId, List<BlockPos>> STALL_CACHE = new ConcurrentHashMap<>();
    private static MinecraftServer server;
    private static boolean dirty;

    private MarketRegistry() {}

    public static void init(MinecraftServer minecraftServer) {
        server = minecraftServer;
        MARKETS.clear();
        STALL_CACHE.clear();
        dirty = false;

        ServerLevel overworld = minecraftServer.overworld();
        MarketDataSavedData saved = MarketDataSavedData.get(overworld);
        saved.hydrateRegistry();

        MarketCoordinationMod.LOGGER.info("MarketRegistry loaded {} market(s)", MARKETS.size());
    }

    public static Optional<MarketState> get(MarketId id) {
        return Optional.ofNullable(MARKETS.get(id));
    }

    public static MarketState register(MarketState state) {
        MARKETS.put(state.id(), state);
        STALL_CACHE.putIfAbsent(state.id(), new ArrayList<>());
        markDirty();
        return state;
    }

    public static Optional<MarketState> getByLedger(ServerLevel level, BlockPos ledgerPos) {
        return get(MarketId.fromLedger(level.dimension(), ledgerPos));
    }

    public static MarketState getOrCreate(MarketId id, String displayName, ServerLevel level, BlockPos ledgerPos) {
        return MARKETS.computeIfAbsent(id, ignored -> {
            if (!MarketSpacingService.canRegister(level, ledgerPos)) {
                throw new IllegalStateException("Market ledger too close to an existing market: " + ledgerPos);
            }
            MarketState created = new MarketState(id, displayName, level.dimension(), ledgerPos);
            long starting = MarketCoordConfig.MARKET.startingDailyBudget.get();
            created.setDailyBudgetSpurs(starting * CoinSpurUnits.COG_VALUE_SPURS);
            STALL_CACHE.put(id, new ArrayList<>());
            markDirty();
            return created;
        });
    }

    public static void markDirty() {
        dirty = true;
    }

    public static void persistIfDirty() {
        if (dirty) {
            persist();
        }
    }

    public static Collection<MarketState> all() {
        return Collections.unmodifiableCollection(MARKETS.values());
    }

    public static int count() {
        return MARKETS.size();
    }

    public static List<BlockPos> stalls(MarketId id) {
        if (!MarketCoordConfig.PERFORMANCE.stallCacheEnabled.get()) {
            return List.of();
        }
        return Collections.unmodifiableList(STALL_CACHE.getOrDefault(id, List.of()));
    }

    public static void invalidateStallCache(MarketId id) {
        STALL_CACHE.remove(id);
    }

    public static void bindStall(MarketId id, BlockPos stallPos) {
        if (!MarketCoordConfig.PERFORMANCE.stallCacheEnabled.get()) {
            return;
        }
        STALL_CACHE.computeIfAbsent(id, ignored -> new ArrayList<>());
        List<BlockPos> stalls = STALL_CACHE.get(id);
        if (!stalls.contains(stallPos)) {
            stalls.add(stallPos.immutable());
        }
    }

    public static void persist() {
        overworld().ifPresent(level -> {
            MarketDataSavedData saved = MarketDataSavedData.get(level);
            saved.replaceAll(MARKETS);
            saved.setDirty();
            dirty = false;
        });
    }

    public static Optional<ServerLevel> overworld() {
        if (server == null) {
            return Optional.empty();
        }
        return Optional.of(server.overworld());
    }

    public static Optional<MinecraftServer> server() {
        return Optional.ofNullable(server);
    }

    /** Spur multiplier: config budgets are expressed in Cogs for pack readability. */
    public static final class CoinSpurUnits {
        public static final long COG_VALUE_SPURS = 64L;

        private CoinSpurUnits() {}
    }
}
