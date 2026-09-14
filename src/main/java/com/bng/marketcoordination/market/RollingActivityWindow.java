package com.bng.marketcoordination.market;

import com.bng.marketcoordination.economy.CommodityCategory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class RollingActivityWindow {
    public record DaySnapshot(long tradeVolumeSpurs, int uniqueSellers, int commodityCount, int villagerCount) {
        public static DaySnapshot empty() {
            return new DaySnapshot(0L, 0, 0, 0);
        }
    }

    private final int capacity;
    private final DaySnapshot[] days;
    private int head;
    private int size;
    private long currentVolume;
    private final Set<UUID> currentSellers = new HashSet<>();
    private final Set<CommodityCategory> currentCategories = new HashSet<>();
    private int currentVillagers;

    public RollingActivityWindow(int capacity) {
        this.capacity = Math.max(1, capacity);
        this.days = new DaySnapshot[this.capacity];
        this.head = 0;
        this.size = 0;
    }

    public void recordTrade(long spurAmount, UUID sellerId, CommodityCategory category, double growthWeight) {
        long weighted = Math.round(Math.max(0L, spurAmount) * Math.max(0.0, growthWeight));
        currentVolume += weighted;
        if (sellerId != null) {
            currentSellers.add(sellerId);
        }
        if (category != null) {
            currentCategories.add(category);
        }
    }

    public void recordTrade(long spurAmount) {
        recordTrade(spurAmount, null, null, 1.0);
    }

    public void setVillagerCount(int villagerCount) {
        currentVillagers = Math.max(0, villagerCount);
    }

    public void finalizeDay() {
        push(new DaySnapshot(
                currentVolume,
                currentSellers.size(),
                currentCategories.size(),
                currentVillagers
        ));
        currentVolume = 0L;
        currentSellers.clear();
        currentCategories.clear();
        currentVillagers = 0;
    }

    public void push(DaySnapshot snapshot) {
        days[head] = snapshot;
        head = (head + 1) % capacity;
        if (size < capacity) {
            size++;
        }
    }

    public int size() {
        return size;
    }

    public long totalTradeVolumeSpurs() {
        long total = currentVolume;
        for (int i = 0; i < size; i++) {
            int index = (head - 1 - i + capacity) % capacity;
            DaySnapshot snapshot = days[index];
            if (snapshot != null) {
                total += snapshot.tradeVolumeSpurs();
            }
        }
        return total;
    }

    public double averageDailyVolumeSpurs() {
        long total = currentVolume;
        int count = 1;
        for (int i = 0; i < size; i++) {
            int index = (head - 1 - i + capacity) % capacity;
            DaySnapshot snapshot = days[index];
            if (snapshot != null) {
                total += snapshot.tradeVolumeSpurs();
                count++;
            }
        }
        return total / (double) count;
    }

    public double averageUniqueSellers() {
        return averageInt(snapshot -> snapshot.uniqueSellers());
    }

    public double averageCategoryDiversity() {
        return averageInt(snapshot -> snapshot.commodityCount());
    }

    public int latestVillagerCount() {
        if (currentVillagers > 0) {
            return currentVillagers;
        }
        for (int i = 0; i < size; i++) {
            int index = (head - 1 - i + capacity) % capacity;
            DaySnapshot snapshot = days[index];
            if (snapshot != null && snapshot.villagerCount() > 0) {
                return snapshot.villagerCount();
            }
        }
        return 0;
    }

    public List<Long> recentDailyVolumes() {
        List<Long> volumes = new ArrayList<>(capacity);
        for (int i = 0; i < size; i++) {
            int index = (head - 1 - i + capacity) % capacity;
            DaySnapshot snapshot = days[index];
            volumes.add(snapshot == null ? 0L : snapshot.tradeVolumeSpurs());
        }
        while (volumes.size() < capacity) {
            volumes.add(0L);
        }
        return volumes;
    }

    public DaySnapshot currentDay() {
        return new DaySnapshot(
                currentVolume,
                currentSellers.size(),
                currentCategories.size(),
                currentVillagers
        );
    }

    public void writeToTag(CompoundTag tag) {
        tag.putLong("currentVolume", currentVolume);
        tag.putInt("currentVillagers", currentVillagers);
        ListTag daysTag = new ListTag();
        for (int i = 0; i < size; i++) {
            int index = (head - 1 - i + capacity) % capacity;
            DaySnapshot snapshot = days[index];
            if (snapshot == null) {
                continue;
            }
            daysTag.add(writeSnapshot(snapshot));
        }
        tag.put("days", daysTag);
    }

    public static RollingActivityWindow readFromTag(CompoundTag tag, int capacity) {
        RollingActivityWindow window = new RollingActivityWindow(capacity);
        if (tag.contains("days")) {
            ListTag daysTag = tag.getList("days", Tag.TAG_COMPOUND);
            for (int i = daysTag.size() - 1; i >= 0; i--) {
                window.push(readSnapshot(daysTag.getCompound(i)));
            }
        }
        if (tag.contains("currentVolume")) {
            window.currentVolume = tag.getLong("currentVolume");
        }
        if (tag.contains("currentVillagers")) {
            window.currentVillagers = tag.getInt("currentVillagers");
        }
        return window;
    }

    public void importFrom(RollingActivityWindow source) {
        head = 0;
        size = 0;
        currentVolume = 0L;
        currentSellers.clear();
        currentCategories.clear();
        currentVillagers = 0;
        for (int i = source.size - 1; i >= 0; i--) {
            int index = (source.head - 1 - i + source.capacity) % source.capacity;
            DaySnapshot snapshot = source.days[index];
            if (snapshot != null) {
                push(snapshot);
            }
        }
        currentVolume = source.currentVolume;
        currentVillagers = source.currentVillagers;
    }

    private double averageInt(java.util.function.ToIntFunction<DaySnapshot> extractor) {
        if (size == 0) {
            DaySnapshot current = currentDay();
            return extractor.applyAsInt(current);
        }
        int total = 0;
        int count = 0;
        for (int i = 0; i < size; i++) {
            int index = (head - 1 - i + capacity) % capacity;
            DaySnapshot snapshot = days[index];
            if (snapshot != null) {
                total += extractor.applyAsInt(snapshot);
                count++;
            }
        }
        DaySnapshot current = currentDay();
        total += extractor.applyAsInt(current);
        count++;
        return count == 0 ? 0.0 : total / (double) count;
    }

    private static CompoundTag writeSnapshot(DaySnapshot snapshot) {
        CompoundTag dayTag = new CompoundTag();
        dayTag.putLong("volume", snapshot.tradeVolumeSpurs());
        dayTag.putInt("sellers", snapshot.uniqueSellers());
        dayTag.putInt("categories", snapshot.commodityCount());
        dayTag.putInt("villagers", snapshot.villagerCount());
        return dayTag;
    }

    private static DaySnapshot readSnapshot(CompoundTag dayTag) {
        return new DaySnapshot(
                dayTag.getLong("volume"),
                dayTag.getInt("sellers"),
                dayTag.getInt("categories"),
                dayTag.getInt("villagers")
        );
    }
}
