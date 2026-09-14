package com.bng.marketcoordination.economy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Global (cross-market) per-player daily trade-volume history, used to draw the per-player line graph
 * in the Stock Market interface. Volume is accumulated per owner for the current day and pushed into a
 * fixed rolling window when the daily cycle finalises, so each player's series is aligned to the same
 * day boundaries.
 */
public final class TraderHistoryService {

    /** Number of finalised days retained. The graph also shows the in-progress day as a live point. */
    public static final int HISTORY_DAYS = 14;

    /** One player's rolling volume series. */
    private static final class Series {
        private String name;
        private final long[] daily = new long[HISTORY_DAYS]; // index 0 = oldest, HISTORY_DAYS-1 = newest finalised
        private long current;

        Series(String name) {
            this.name = name;
        }

        long total() {
            long total = current;
            for (long value : daily) {
                total += value;
            }
            return total;
        }
    }

    /** A client-facing snapshot of one player's series (finalised days + the live current day). */
    public record PlayerSeries(String name, long[] points) {}

    private final Map<UUID, Series> series = new LinkedHashMap<>();

    public void record(UUID ownerId, String ownerName, long spurs) {
        if (ownerId == null || spurs <= 0L) {
            return;
        }
        Series entry = series.computeIfAbsent(ownerId, id -> new Series(fallbackName(id, ownerName)));
        if (ownerName != null && !ownerName.isBlank()) {
            entry.name = ownerName;
        }
        entry.current += spurs;
    }

    /** Push every player's current-day volume into the rolling window and start a fresh day. */
    public void finalizeDay() {
        for (Series entry : series.values()) {
            System.arraycopy(entry.daily, 1, entry.daily, 0, HISTORY_DAYS - 1);
            entry.daily[HISTORY_DAYS - 1] = entry.current;
            entry.current = 0L;
        }
    }

    /**
     * Top players by total volume, each with a points array of length {@code HISTORY_DAYS + 1}
     * (finalised days followed by the live current day). Players with no volume are omitted.
     */
    public List<PlayerSeries> snapshot(int limit) {
        List<Series> ranked = new ArrayList<>(series.values());
        ranked.removeIf(s -> s.total() <= 0L);
        ranked.sort(Comparator.comparingLong(Series::total).reversed());
        List<PlayerSeries> out = new ArrayList<>();
        for (int i = 0; i < ranked.size() && i < limit; i++) {
            Series s = ranked.get(i);
            long[] points = new long[HISTORY_DAYS + 1];
            System.arraycopy(s.daily, 0, points, 0, HISTORY_DAYS);
            points[HISTORY_DAYS] = s.current;
            out.add(new PlayerSeries(s.name, points));
        }
        return out;
    }

    public void writeToTag(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Series> e : series.entrySet()) {
            Series s = e.getValue();
            CompoundTag entry = new CompoundTag();
            entry.putUUID("id", e.getKey());
            if (s.name != null && !s.name.isBlank()) {
                entry.putString("name", s.name);
            }
            entry.putLong("current", s.current);
            entry.putLongArray("daily", s.daily.clone());
            list.add(entry);
        }
        tag.put("series", list);
    }

    public void importFromTag(CompoundTag tag) {
        series.clear();
        if (!tag.contains("series", Tag.TAG_LIST)) {
            return;
        }
        ListTag list = tag.getList("series", Tag.TAG_COMPOUND);
        for (Tag entryTag : list) {
            CompoundTag entry = (CompoundTag) entryTag;
            if (!entry.hasUUID("id")) {
                continue;
            }
            UUID id = entry.getUUID("id");
            Series s = new Series(fallbackName(id, entry.contains("name") ? entry.getString("name") : null));
            s.current = entry.getLong("current");
            long[] stored = entry.getLongArray("daily");
            for (int i = 0; i < HISTORY_DAYS && i < stored.length; i++) {
                s.daily[i] = stored[i];
            }
            series.put(id, s);
        }
    }

    private static String fallbackName(UUID id, String name) {
        return (name == null || name.isBlank()) ? id.toString().substring(0, 8) : name;
    }
}
