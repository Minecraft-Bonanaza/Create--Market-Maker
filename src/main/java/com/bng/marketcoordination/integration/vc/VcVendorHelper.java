package com.bng.marketcoordination.integration.vc;

import by.deokma.stockmarket.neoforge.shop.NeoForgeVendorHelper;
import by.deokma.stockmarket.platform.IPlatformVendorHelper;
import by.deokma.stockmarket.shop.ShopSavedData;
import by.deokma.stockmarket.shop.VendorRegistry;
import com.beakbock.createvillagercommerce.blockentity.MerchantStallBlockEntity;
import com.bng.marketcoordination.MarketCoordinationMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Bridges Create: Villager Commerce merchant stalls into Create Stock Market's shop index.
 *
 * <p>Stock Market discovers shops through a single, swappable {@link IPlatformVendorHelper}. We wrap
 * SM's own NeoForge helper so its existing Table Cloth / Numismatics vendor discovery is fully
 * preserved, and additionally recognise Merchant Stalls. SM already calls {@code refreshLoaded} every
 * server tick and snapshots the market on an interval, both routed through the active helper, so once
 * installed our stalls are indexed into the shop list and folded into the shared global price index —
 * no extra scanning of our own.
 */
public final class VcVendorHelper implements IPlatformVendorHelper {

    private final IPlatformVendorHelper delegate;

    public VcVendorHelper(IPlatformVendorHelper delegate) {
        this.delegate = delegate;
    }

    /**
     * Installs the bridge by wrapping a fresh copy of SM's NeoForge helper. Safe to call again on each
     * server start; the wrapped helper is stateless. Guarded so a Stock Market API change can't crash
     * our mod — it just logs and leaves SM's vendor index untouched.
     */
    public static void install() {
        try {
            VendorRegistry.setPlatformHelper(new VcVendorHelper(new NeoForgeVendorHelper()));
            MarketCoordinationMod.LOGGER.info(
                    "Registered Merchant Stalls as Stock Market vendor shops (global price index active)");
        } catch (Throwable t) {
            MarketCoordinationMod.LOGGER.warn("Failed to hook Merchant Stalls into Stock Market", t);
        }
    }

    @Override
    public boolean isShopEntity(BlockEntity be) {
        return be instanceof MerchantStallBlockEntity || delegate.isShopEntity(be);
    }

    @Override
    public boolean tryIndex(ServerLevel level, BlockEntity be, ShopSavedData data, Map<UUID, String> nameCache) {
        if (be instanceof MerchantStallBlockEntity stall) {
            return VcStockMarketBridge.index(level, stall, data);
        }
        return delegate.tryIndex(level, be, data, nameCache);
    }

    @Override
    public void forEachLoadedChunk(MinecraftServer server, BiConsumer<ServerLevel, LevelChunk> consumer) {
        delegate.forEachLoadedChunk(server, consumer);
    }
}
