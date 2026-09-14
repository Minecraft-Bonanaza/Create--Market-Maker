package com.bng.marketcoordination.data;

import com.bng.marketcoordination.economy.NationId;
import com.bng.marketcoordination.market.MarketId;
import com.bng.marketcoordination.market.MarketRegistry;
import com.bng.marketcoordination.market.MarketState;
import com.bng.marketcoordination.market.MarketTier;
import com.bng.marketcoordination.market.RollingActivityWindow;
import com.bng.marketcoordination.config.MarketCoordConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MarketDataSavedData extends SavedData {
    public static final String DATA_ID = "marketcoordination_markets";

    public static final SavedData.Factory<MarketDataSavedData> FACTORY = new SavedData.Factory<>(
            MarketDataSavedData::new,
            MarketDataSavedData::load,
            null
    );

    private final Map<MarketId, MarketState> markets = new HashMap<>();

    public MarketDataSavedData() {}

    public static MarketDataSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        MarketDataSavedData data = new MarketDataSavedData();
        ListTag list = tag.getList("markets", Tag.TAG_COMPOUND);
        for (Tag entryTag : list) {
            CompoundTag entry = (CompoundTag) entryTag;
            MarketState state = deserializeMarket(entry);
            if (state != null) {
                data.markets.put(state.id(), state);
            }
        }
        if (tag.contains("traderHistory")) {
            com.bng.marketcoordination.MarketServices.TRADER_HISTORY.importFromTag(tag.getCompound("traderHistory"));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (MarketState state : markets.values()) {
            list.add(serializeMarket(state));
        }
        tag.put("markets", list);
        CompoundTag traderHistory = new CompoundTag();
        com.bng.marketcoordination.MarketServices.TRADER_HISTORY.writeToTag(traderHistory);
        tag.put("traderHistory", traderHistory);
        return tag;
    }

    public static MarketDataSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_ID);
    }

    public void hydrateRegistry() {
        markets.values().forEach(MarketRegistry::register);
    }

    public void replaceAll(Map<MarketId, MarketState> source) {
        markets.clear();
        markets.putAll(source);
    }

    public Map<MarketId, MarketState> markets() {
        return markets;
    }

    private static CompoundTag serializeMarket(MarketState state) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", state.id().uuid());
        tag.putString("name", state.displayName());
        tag.putString("dimension", state.dimension().location().toString());
        tag.putLong("ledgerPos", state.ledgerPos().asLong());
        tag.putLong("createdAtEpochMs", state.createdAt().toEpochMilli());
        tag.putDouble("activityScore", state.activityScore());
        tag.putDouble("previousActivityScore", state.previousActivityScore());
        tag.putDouble("peakActivityScore", state.peakActivityScore());
        tag.putString("tier", state.tier().name());
        tag.putLong("dailyBudgetSpurs", state.dailyBudgetSpurs());
        tag.putLong("spentTodaySpurs", state.spentTodaySpurs());
        tag.putBoolean("capitalMarket", state.capitalMarket());
        if (state.nationId() != null) {
            tag.putUUID("nationId", state.nationId().uuid());
        }
        CompoundTag windowTag = new CompoundTag();
        state.activityWindow().writeToTag(windowTag);
        tag.put("activityWindow", windowTag);
        CompoundTag issuanceTag = new CompoundTag();
        state.issuance().writeToTag(issuanceTag);
        tag.put("issuance", issuanceTag);
        CompoundTag regionalTag = new CompoundTag();
        state.regionalProfile().writeToTag(regionalTag);
        tag.put("regionalProfile", regionalTag);
        CompoundTag tradersTag = new CompoundTag();
        state.traders().writeToTag(tradersTag);
        tag.put("traders", tradersTag);
        return tag;
    }

    private static MarketState deserializeMarket(CompoundTag tag) {
        if (!tag.hasUUID("id") || !tag.contains("ledgerPos")) {
            return null;
        }
        MarketId id = new MarketId(tag.getUUID("id"));
        ResourceKey<Level> dimension = ResourceKey.create(
                Registries.DIMENSION,
                ResourceLocation.parse(tag.getString("dimension"))
        );
        BlockPos pos = BlockPos.of(tag.getLong("ledgerPos"));
        MarketState state = new MarketState(id, tag.getString("name"), dimension, pos);
        if (tag.contains("createdAtEpochMs")) {
            state.setCreatedAt(Instant.ofEpochMilli(tag.getLong("createdAtEpochMs")));
        }
        state.setActivityScore(tag.getDouble("activityScore"));
        if (tag.contains("previousActivityScore")) {
            state.setPreviousActivityScore(tag.getDouble("previousActivityScore"));
        }
        // Seed the high-water mark from the current score for saves created before this field existed.
        state.setPeakActivityScore(tag.contains("peakActivityScore")
                ? tag.getDouble("peakActivityScore")
                : state.activityScore());
        if (tag.contains("tier")) {
            state.setTier(MarketTier.valueOf(tag.getString("tier")));
        }
        state.setDailyBudgetSpurs(tag.getLong("dailyBudgetSpurs"));
        state.setSpentTodaySpurs(tag.getLong("spentTodaySpurs"));
        state.setCapitalMarket(tag.getBoolean("capitalMarket"));
        if (tag.hasUUID("nationId")) {
            state.setNationId(new NationId(tag.getUUID("nationId")));
        }
        int windowDays = MarketCoordConfig.MARKET.activityWindowDays.get();
        if (tag.contains("activityWindow")) {
            RollingActivityWindow restored = RollingActivityWindow.readFromTag(
                    tag.getCompound("activityWindow"),
                    windowDays);
            state.activityWindow().importFrom(restored);
        }
        if (tag.contains("issuance")) {
            state.issuance().importFromTag(tag.getCompound("issuance"));
        }
        if (tag.contains("regionalProfile")) {
            state.regionalProfile().importFromTag(tag.getCompound("regionalProfile"));
        }
        if (tag.contains("traders")) {
            state.traders().importFromTag(tag.getCompound("traders"));
        }
        return state;
    }
}
