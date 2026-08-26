package com.bng.marketcoordination.config;

import com.bng.marketcoordination.MarketCoordinationMod;
import com.bng.marketcoordination.economy.CommodityCategory;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class CommodityCategoryRegistry {
    private static final Gson GSON = new Gson();
    private static final ResourceLocation DATA_ID = ResourceLocation.fromNamespaceAndPath(
            MarketCoordinationMod.MOD_ID,
            "commodity_categories.json"
    );

    private static final Map<Item, CommodityCategory> ITEM_MAP = new HashMap<>();
    private static CommodityCategory defaultCategory = CommodityCategory.MISC;

    private CommodityCategoryRegistry() {}

    public static void load(ResourceManager resourceManager) {
        ITEM_MAP.clear();
        defaultCategory = CommodityCategory.MISC;

        Optional<Resource> resource = resourceManager.getResource(DATA_ID);
        if (resource.isEmpty()) {
            MarketCoordinationMod.LOGGER.warn("Missing commodity category data at {}", DATA_ID);
            return;
        }

        try (InputStreamReader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) {
                return;
            }
            if (root.has("default_category")) {
                defaultCategory = CommodityCategory.fromId(root.get("default_category").getAsString());
            }
            if (!root.has("categories")) {
                return;
            }
            JsonObject categories = root.getAsJsonObject("categories");
            for (Map.Entry<String, JsonElement> entry : categories.entrySet()) {
                CommodityCategory category = CommodityCategory.fromId(entry.getKey());
                for (JsonElement itemElement : entry.getValue().getAsJsonArray()) {
                    ResourceLocation id = ResourceLocation.tryParse(itemElement.getAsString());
                    if (id == null) {
                        continue;
                    }
                    Item item = BuiltInRegistries.ITEM.get(id);
                    if (item != null) {
                        ITEM_MAP.put(item, category);
                    }
                }
            }
            MarketCoordinationMod.LOGGER.info("Loaded {} commodity category mappings", ITEM_MAP.size());
        } catch (Exception exception) {
            MarketCoordinationMod.LOGGER.error("Failed to load commodity categories", exception);
        }
    }

    public static CommodityCategory categoryOf(Item item) {
        return ITEM_MAP.getOrDefault(item, defaultCategory);
    }
}
