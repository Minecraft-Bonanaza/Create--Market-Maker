package com.bng.marketcoordination.economy;

import com.bng.marketcoordination.market.MarketId;
import com.bng.marketcoordination.market.MarketRegistry;
import com.bng.marketcoordination.market.MarketState;
import net.minecraft.server.level.ServerLevel;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class NationRegistry {
    private static final Map<NationId, NationState> NATIONS = new ConcurrentHashMap<>();
    private static ServerLevel storageLevel;

    private NationRegistry() {}

    public static void init(ServerLevel overworld) {
        storageLevel = overworld;
        NATIONS.clear();
        com.bng.marketcoordination.data.NationDataSavedData.get(overworld).hydrateRegistry();
    }

    public static NationState create(NationId id, String name) {
        NationState nation = new NationState(id, name);
        NATIONS.put(id, nation);
        persist();
        return nation;
    }

    public static Optional<NationState> get(NationId id) {
        return Optional.ofNullable(NATIONS.get(id));
    }

    public static Optional<NationState> getByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return NATIONS.values().stream()
                .filter(nation -> nation.displayName().equalsIgnoreCase(name.trim()))
                .findFirst();
    }

    public static Collection<NationState> all() {
        return Collections.unmodifiableCollection(NATIONS.values());
    }

    public static void linkMarket(NationId nationId, MarketId marketId, boolean capital) {
        NationState nation = NATIONS.get(nationId);
        if (nation == null) {
            return;
        }
        nation.addMarket(marketId);
        if (capital) {
            nation.setCapitalMarketId(marketId);
        }
        MarketRegistry.get(marketId).ifPresent(market -> {
            market.setNationId(nationId);
            market.setCapitalMarket(capital);
        });
        persist();
    }

    public static void register(NationState nation) {
        NATIONS.put(nation.id(), nation);
    }

    public static void persist() {
        if (storageLevel == null) {
            MarketRegistry.overworld().ifPresent(level -> {
                storageLevel = level;
                write(level);
            });
            return;
        }
        write(storageLevel);
    }

    private static void write(ServerLevel level) {
        com.bng.marketcoordination.data.NationDataSavedData saved =
                com.bng.marketcoordination.data.NationDataSavedData.get(level);
        saved.replaceAll(NATIONS);
        saved.setDirty();
    }
}
