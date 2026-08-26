package com.bng.marketcoordination.config;

import com.bng.marketcoordination.MarketCoordinationMod;
import com.bng.marketcoordination.economy.CommodityCategory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class CategoryBudgetConfig {
    private static final Gson GSON = new Gson();
    private static final ResourceLocation DATA_ID = ResourceLocation.fromNamespaceAndPath(
            MarketCoordinationMod.MOD_ID,
            "category_budget_weights.json"
    );

    private static final EnumMap<CommodityCategory, Double> WEIGHTS = new EnumMap<>(CommodityCategory.class);

    static {
        resetDefaults();
    }

    private CategoryBudgetConfig() {}

    public static void load(ResourceManager resourceManager) {
        resetDefaults();
        Optional<Resource> resource = resourceManager.getResource(DATA_ID);
        if (resource.isEmpty()) {
            return;
        }
        try (InputStreamReader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) {
                return;
            }
            for (CommodityCategory category : CommodityCategory.values()) {
                if (root.has(category.id())) {
                    WEIGHTS.put(category, root.get(category.id()).getAsDouble());
                }
            }
            normalizeWeights();
        } catch (Exception exception) {
            MarketCoordinationMod.LOGGER.error("Failed to load category budget weights", exception);
        }
    }

    public static double weight(CommodityCategory category) {
        return WEIGHTS.getOrDefault(category, 0.0);
    }

    public static Map<CommodityCategory, Double> allWeights() {
        return Map.copyOf(WEIGHTS);
    }

    private static void resetDefaults() {
        WEIGHTS.clear();
        WEIGHTS.put(CommodityCategory.FOOD, 0.25);
        WEIGHTS.put(CommodityCategory.FUEL, 0.15);
        WEIGHTS.put(CommodityCategory.MATERIALS, 0.25);
        WEIGHTS.put(CommodityCategory.TEXTILES, 0.10);
        WEIGHTS.put(CommodityCategory.TOOLS, 0.15);
        WEIGHTS.put(CommodityCategory.MISC, 0.10);
    }

    private static void normalizeWeights() {
        double total = WEIGHTS.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total <= 0.0) {
            resetDefaults();
            return;
        }
        for (CommodityCategory category : CommodityCategory.values()) {
            WEIGHTS.computeIfPresent(category, (key, value) -> value / total);
        }
    }
}
