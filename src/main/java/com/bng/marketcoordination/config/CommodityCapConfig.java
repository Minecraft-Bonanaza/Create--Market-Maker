package com.bng.marketcoordination.config;

import com.bng.marketcoordination.MarketCoordinationMod;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class CommodityCapConfig {
    private static final Gson GSON = new Gson();
    private static final ResourceLocation DATA_ID = ResourceLocation.fromNamespaceAndPath(
            MarketCoordinationMod.MOD_ID,
            "commodity_caps.json"
    );

    private static int defaultDailyCap = 64;
    private static final Map<Item, Integer> overrides = new HashMap<>();

    private CommodityCapConfig() {}

    public static void load(ResourceManager resourceManager) {
        overrides.clear();
        defaultDailyCap = 64;

        Optional<Resource> resource = resourceManager.getResource(DATA_ID);
        if (resource.isEmpty()) {
            MarketCoordinationMod.LOGGER.warn("Missing commodity caps data at {}", DATA_ID);
            return;
        }

        try (InputStreamReader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) {
                return;
            }
            if (root.has("default_daily_cap")) {
                defaultDailyCap = Math.max(1, root.get("default_daily_cap").getAsInt());
            }
            if (root.has("overrides")) {
                JsonObject items = root.getAsJsonObject("overrides");
                for (Map.Entry<String, JsonElement> entry : items.entrySet()) {
                    ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
                    if (id == null) {
                        continue;
                    }
                    Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
                    if (item != null) {
                        overrides.put(item, Math.max(1, entry.getValue().getAsInt()));
                    }
                }
            }
            MarketCoordinationMod.LOGGER.info(
                    "Loaded commodity caps — default={}, overrides={}",
                    defaultDailyCap,
                    overrides.size()
            );
        } catch (Exception exception) {
            MarketCoordinationMod.LOGGER.error("Failed to load commodity caps from {}", DATA_ID, exception);
        }
    }

    public static int dailyCap(Item item) {
        return overrides.getOrDefault(item, defaultDailyCap);
    }

    public static int defaultDailyCap() {
        return defaultDailyCap;
    }
}
