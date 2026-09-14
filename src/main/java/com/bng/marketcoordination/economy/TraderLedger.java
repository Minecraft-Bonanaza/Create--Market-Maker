package com.bng.marketcoordination.economy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-market ledger of stall owners: how much each owner's stalls have sold (their revenue / profit
 * in Spurs) and how many trades they have completed. Attribution uses the stall's real owner
 * (Villager Commerce's market owner id), so an unowned stall contributes to market volume but not to
 * any single trader.
 */
public final class TraderLedger {

    /** Aggregated stats for one stall owner within a market. */
    public static final class Trader {
        private final UUID id;
        private String name;
        private long volumeSpurs;
        private long trades;

        Trader(UUID id, String name) {
            this.id = id;
            this.name = name;
        }

        public UUID id() {
            return id;
        }

        public String name() {
            return (name == null || name.isBlank()) ? id.toString().substring(0, 8) : name;
        }

        public long volumeSpurs() {
            return volumeSpurs;
        }

        public long trades() {
            return trades;
        }
    }

    private final Map<UUID, Trader> traders = new LinkedHashMap<>();

    public void record(UUID ownerId, String ownerName, long spurs) {
        if (ownerId == null || spurs <= 0L) {
            return;
        }
        Trader trader = traders.computeIfAbsent(ownerId, id -> new Trader(id, ownerName));
        if (ownerName != null && !ownerName.isBlank()) {
            trader.name = ownerName;
        }
        trader.volumeSpurs += spurs;
        trader.trades += 1;
    }

    public Collection<Trader> traders() {
        return traders.values();
    }

    public int traderCount() {
        return traders.size();
    }

    public long totalVolumeSpurs() {
        long total = 0L;
        for (Trader trader : traders.values()) {
            total += trader.volumeSpurs;
        }
        return total;
    }

    public void writeToTag(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Trader trader : traders.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("id", trader.id);
            if (trader.name != null && !trader.name.isBlank()) {
                entry.putString("name", trader.name);
            }
            entry.putLong("volume", trader.volumeSpurs);
            entry.putLong("trades", trader.trades);
            list.add(entry);
        }
        tag.put("traders", list);
    }

    public void importFromTag(CompoundTag tag) {
        traders.clear();
        if (!tag.contains("traders", Tag.TAG_LIST)) {
            return;
        }
        ListTag list = tag.getList("traders", Tag.TAG_COMPOUND);
        for (Tag entryTag : list) {
            CompoundTag entry = (CompoundTag) entryTag;
            if (!entry.hasUUID("id")) {
                continue;
            }
            Trader trader = new Trader(entry.getUUID("id"), entry.contains("name") ? entry.getString("name") : null);
            trader.volumeSpurs = entry.getLong("volume");
            trader.trades = entry.getLong("trades");
            traders.put(trader.id, trader);
        }
    }
}
