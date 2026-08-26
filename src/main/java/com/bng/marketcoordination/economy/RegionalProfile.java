package com.bng.marketcoordination.economy;

import com.bng.marketcoordination.config.CategoryBudgetConfig;
import com.bng.marketcoordination.economy.CommodityCategory;
import com.bng.marketcoordination.market.MarketState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.EnumMap;
import java.util.Map;

public final class RegionalProfile {
    private final EnumMap<CommodityCategory, Long> categoryVolume = new EnumMap<>(CommodityCategory.class);

    public void record(CommodityCategory category, long spurAmount) {
        if (spurAmount <= 0L) {
            return;
        }
        categoryVolume.merge(category, spurAmount, Long::sum);
    }

    public CommodityCategory dominantCategory() {
        CommodityCategory best = CommodityCategory.MISC;
        long bestAmount = 0L;
        for (Map.Entry<CommodityCategory, Long> entry : categoryVolume.entrySet()) {
            if (entry.getValue() > bestAmount) {
                bestAmount = entry.getValue();
                best = entry.getKey();
            }
        }
        return best;
    }

    public double categoryShare(CommodityCategory category) {
        long total = categoryVolume.values().stream().mapToLong(Long::longValue).sum();
        if (total <= 0L) {
            return 0.0;
        }
        return categoryVolume.getOrDefault(category, 0L) / (double) total;
    }

    public void writeToTag(CompoundTag tag) {
        CompoundTag volumes = new CompoundTag();
        categoryVolume.forEach((category, amount) -> volumes.putLong(category.id(), amount));
        tag.put("volumes", volumes);
    }

    public static RegionalProfile readFromTag(CompoundTag tag) {
        RegionalProfile profile = new RegionalProfile();
        profile.importFromTag(tag);
        return profile;
    }

    public void importFromTag(CompoundTag tag) {
        categoryVolume.clear();
        if (tag.contains("volumes", Tag.TAG_COMPOUND)) {
            CompoundTag volumes = tag.getCompound("volumes");
            for (String key : volumes.getAllKeys()) {
                categoryVolume.put(CommodityCategory.fromId(key), volumes.getLong(key));
            }
        }
    }
}
