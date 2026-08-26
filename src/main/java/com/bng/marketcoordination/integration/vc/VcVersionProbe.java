package com.bng.marketcoordination.integration.vc;

import com.bng.marketcoordination.MarketCoordinationMod;
import com.beakbock.createvillagercommerce.server.MarketLedgerManager;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;

public final class VcVersionProbe {
    private static boolean compatible = false;

    private VcVersionProbe() {}

    public static void probe() {
        compatible = false;
        if (!ModList.get().isLoaded("createvillagercommerce")) {
            MarketCoordinationMod.LOGGER.warn("Create: Villager Commerce not loaded — VC mixins will no-op");
            return;
        }

        try {
            Method canPurchase = MarketLedgerManager.class.getMethod(
                    "canPurchase",
                    net.minecraft.server.level.ServerLevel.class,
                    com.beakbock.createvillagercommerce.blockentity.MerchantStallBlockEntity.class);
            Method recordPurchase = MarketLedgerManager.class.getMethod(
                    "recordPurchase",
                    net.minecraft.server.level.ServerLevel.class,
                    com.beakbock.createvillagercommerce.blockentity.MerchantStallBlockEntity.class);
            Method dailyLimit = MarketLedgerManager.class.getMethod(
                    "getDailyPurchaseLimit",
                    net.minecraft.server.level.ServerLevel.class,
                    net.minecraft.core.BlockPos.class);

            compatible = canPurchase != null && recordPurchase != null && dailyLimit != null;
            if (compatible) {
                MarketCoordinationMod.LOGGER.info(
                        "Villager Commerce integration probe OK (createvillagercommerce present)");
            }
        } catch (ReflectiveOperationException exception) {
            MarketCoordinationMod.LOGGER.error(
                    "Villager Commerce API surface changed — update mixins before deploying",
                    exception);
        }
    }

    public static boolean isCompatible() {
        return compatible;
    }
}
