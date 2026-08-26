package com.bng.marketcoordination.economy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.EnumMap;
import java.util.Map;

public final class IssuanceTracker {
    private long dailySpurs;
    private long weeklySpurs;
    private long lifetimeSpurs;
    private final EnumMap<CommodityCategory, Long> byCategory = new EnumMap<>(CommodityCategory.class);

    public void record(long spurAmount, CommodityCategory category) {
        if (spurAmount <= 0L) {
            return;
        }
        dailySpurs += spurAmount;
        weeklySpurs += spurAmount;
        lifetimeSpurs += spurAmount;
        byCategory.merge(category, spurAmount, Long::sum);
    }

    public void finalizeDay() {
        dailySpurs = 0L;
    }

    public void finalizeWeek() {
        weeklySpurs = 0L;
    }

    public long dailySpurs() { return dailySpurs; }
    public long weeklySpurs() { return weeklySpurs; }
    public long lifetimeSpurs() { return lifetimeSpurs; }

    public long categoryTotal(CommodityCategory category) {
        return byCategory.getOrDefault(category, 0L);
    }

    public void writeToTag(CompoundTag tag) {
        tag.putLong("daily", dailySpurs);
        tag.putLong("weekly", weeklySpurs);
        tag.putLong("lifetime", lifetimeSpurs);
        CompoundTag categories = new CompoundTag();
        byCategory.forEach((category, amount) -> categories.putLong(category.id(), amount));
        tag.put("categories", categories);
    }

    public static IssuanceTracker readFromTag(CompoundTag tag) {
        IssuanceTracker tracker = new IssuanceTracker();
        tracker.importFromTag(tag);
        return tracker;
    }

    public void importFromTag(CompoundTag tag) {
        dailySpurs = tag.getLong("daily");
        weeklySpurs = tag.getLong("weekly");
        lifetimeSpurs = tag.getLong("lifetime");
        byCategory.clear();
        if (tag.contains("categories", Tag.TAG_COMPOUND)) {
            CompoundTag categories = tag.getCompound("categories");
            for (String key : categories.getAllKeys()) {
                byCategory.put(CommodityCategory.fromId(key), categories.getLong(key));
            }
        }
    }
}
