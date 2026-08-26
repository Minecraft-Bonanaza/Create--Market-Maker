package com.bng.marketcoordination.integration;

import com.bng.marketcoordination.MarketCoordinationMod;
import net.neoforged.fml.ModList;

public final class ModPresence {
    private ModPresence() {}

    public static boolean isStockMarketLoaded() {
        return ModList.get().isLoaded("stockmarket");
    }

    public static boolean isVillagerCommerceLoaded() {
        return ModList.get().isLoaded("createvillagercommerce");
    }

    public static void logStartupPresence() {
        MarketCoordinationMod.LOGGER.info(
                "Integration presence — createvillagercommerce={} stockmarket={}",
                isVillagerCommerceLoaded(),
                isStockMarketLoaded()
        );
    }
}
