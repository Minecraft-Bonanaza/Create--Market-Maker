package com.bng.marketcoordination.data;

import com.bng.marketcoordination.economy.NationId;
import com.bng.marketcoordination.economy.NationRegistry;
import com.bng.marketcoordination.economy.NationState;
import com.bng.marketcoordination.market.MarketId;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NationDataSavedData extends SavedData {
    public static final String DATA_ID = "marketcoordination_nations";

    public static final SavedData.Factory<NationDataSavedData> FACTORY = new SavedData.Factory<>(
            NationDataSavedData::new,
            NationDataSavedData::load,
            null
    );

    private final Map<NationId, NationState> nations = new HashMap<>();

    public static NationDataSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        NationDataSavedData data = new NationDataSavedData();
        ListTag list = tag.getList("nations", Tag.TAG_COMPOUND);
        for (Tag entryTag : list) {
            CompoundTag entry = (CompoundTag) entryTag;
            if (!entry.hasUUID("id")) {
                continue;
            }
            NationId id = new NationId(entry.getUUID("id"));
            NationState nation = new NationState(id, entry.getString("name"));
            if (entry.hasUUID("capital")) {
                nation.setCapitalMarketId(new MarketId(entry.getUUID("capital")));
            }
            if (entry.contains("members")) {
                ListTag members = entry.getList("members", Tag.TAG_STRING);
                for (Tag memberTag : members) {
                    nation.addMarket(new MarketId(UUID.fromString(memberTag.getAsString())));
                }
            }
            if (entry.contains("procurement")) {
                CompoundTag procurement = entry.getCompound("procurement");
                nation.procurementPool().setPublicWorksBudgetSpurs(procurement.getLong("publicWorks"));
                nation.procurementPool().setMilitaryBudgetSpurs(procurement.getLong("military"));
                nation.procurementPool().setNotes(procurement.getString("notes"));
            }
            data.nations.put(id, nation);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (NationState nation : nations.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("id", nation.id().uuid());
            entry.putString("name", nation.displayName());
            if (nation.capitalMarketId() != null) {
                entry.putUUID("capital", nation.capitalMarketId().uuid());
            }
            ListTag members = new ListTag();
            for (MarketId marketId : nation.memberMarkets()) {
                members.add(net.minecraft.nbt.StringTag.valueOf(marketId.uuid().toString()));
            }
            entry.put("members", members);
            CompoundTag procurement = new CompoundTag();
            procurement.putLong("publicWorks", nation.procurementPool().publicWorksBudgetSpurs());
            procurement.putLong("military", nation.procurementPool().militaryBudgetSpurs());
            procurement.putString("notes", nation.procurementPool().notes());
            entry.put("procurement", procurement);
            list.add(entry);
        }
        tag.put("nations", list);
        return tag;
    }

    public static NationDataSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_ID);
    }

    public void hydrateRegistry() {
        nations.values().forEach(NationRegistry::register);
    }

    public void replaceAll(Map<NationId, NationState> source) {
        nations.clear();
        nations.putAll(source);
    }
}
