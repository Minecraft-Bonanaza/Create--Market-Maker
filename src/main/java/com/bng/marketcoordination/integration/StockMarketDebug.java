package com.bng.marketcoordination.integration;

import by.deokma.stockmarket.market.MarketData;
import by.deokma.stockmarket.market.MarketEntry;
import by.deokma.stockmarket.shop.ShopEntry;
import by.deokma.stockmarket.shop.VendorRegistry;
import com.beakbock.createvillagercommerce.blockentity.MerchantStallBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Read-only diagnostics for the Create Stock Market bridge, isolated in its own class so the Stock
 * Market / Villager Commerce types are only resolved when both mods are present (the caller guards on
 * {@code ModPresence}). Confirms that Merchant Stalls are being indexed as vendor shops and shows the
 * shared global reference prices the demand curve reads.
 */
public final class StockMarketDebug {

    private static final int MAX_STALLS = 30;
    private static final int MAX_PRICES = 40;

    private StockMarketDebug() {}

    public static int sendPrices(CommandSourceStack source) {
        MinecraftServer server = source.getServer();

        List<ShopEntry> shops = VendorRegistry.getAll();
        int totalShops = shops == null ? 0 : shops.size();

        // Detect which indexed shops are our Merchant Stalls by probing the block entity at each pos.
        Map<String, Long> stallPricePerItem = new LinkedHashMap<>();
        int stallCount = 0;
        if (shops != null) {
            for (ShopEntry shop : shops) {
                if (!isMerchantStall(server, shop)) {
                    continue;
                }
                stallCount++;
                String itemId = idOf(shop.sellingItem().getItem());
                if (stallCount <= MAX_STALLS) {
                    source.sendSuccess(() -> Component.literal(
                            "    " + itemId + " @ " + shop.totalPriceInSpurs() + " spurs/ea"
                                    + "  (" + shortDim(shop.dimensionId()) + " "
                                    + shop.pos().getX() + "," + shop.pos().getY() + "," + shop.pos().getZ() + ")")
                            .withStyle(ChatFormatting.GREEN), false);
                }
                stallPricePerItem.merge(itemId, (long) shop.totalPriceInSpurs(), Math::min);
            }
        }

        final int stalls = stallCount;
        source.sendSuccess(() -> Component.literal("\u2550\u2550 Stock Market bridge \u2550\u2550")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        source.sendSuccess(() -> Component.literal("  Indexed shops: ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(stalls + " merchant stalls / " + totalShops + " total")
                        .withStyle(ChatFormatting.WHITE)), false);

        if (stallCount == 0) {
            source.sendSuccess(() -> Component.literal(
                    "  No Merchant Stalls indexed yet. Ensure stalls are in loaded chunks, have a sale"
                            + " item + Spur price, then wait for the next Stock Market snapshot.")
                    .withStyle(ChatFormatting.GRAY), false);
        }

        // Global aggregated prices from the Stock Market index (what StockMarketAdapter reads).
        List<MarketEntry> entries = MarketData.get();
        Map<String, MarketEntry> byItem = new TreeMap<>();
        if (entries != null) {
            for (MarketEntry entry : entries) {
                if (entry.itemId() != null) {
                    byItem.put(entry.itemId().toString(), entry);
                }
            }
        }

        source.sendSuccess(() -> Component.literal("  Global reference prices (avg / min):")
                .withStyle(ChatFormatting.AQUA), false);
        if (byItem.isEmpty()) {
            source.sendSuccess(() -> Component.literal("    (index empty — no vendor shops aggregated yet)")
                    .withStyle(ChatFormatting.DARK_GRAY), false);
            return stallCount;
        }

        int shown = 0;
        for (Map.Entry<String, MarketEntry> e : byItem.entrySet()) {
            if (shown >= MAX_PRICES) {
                int remaining = byItem.size() - shown;
                source.sendSuccess(() -> Component.literal("    ... and " + remaining + " more")
                        .withStyle(ChatFormatting.DARK_GRAY), false);
                break;
            }
            shown++;
            MarketEntry entry = e.getValue();
            boolean fromStall = stallPricePerItem.containsKey(e.getKey());
            String line = "    " + e.getKey() + ": avg " + entry.avgPrice()
                    + ", min " + entry.minPrice() + "  [" + entry.trend() + "]";
            source.sendSuccess(() -> Component.literal(line)
                    .withStyle(fromStall ? ChatFormatting.GREEN : ChatFormatting.GRAY), false);
        }
        return stallCount;
    }

    private static boolean isMerchantStall(MinecraftServer server, ShopEntry shop) {
        if (server == null || shop == null) {
            return false;
        }
        ResourceLocation dimId = ResourceLocation.tryParse(shop.dimensionId());
        if (dimId == null) {
            return false;
        }
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimId));
        if (level == null) {
            return false;
        }
        BlockPos pos = shop.pos();
        if (!level.isLoaded(pos)) {
            return false;
        }
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof MerchantStallBlockEntity;
    }

    private static String idOf(net.minecraft.world.item.Item item) {
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
        return id == null ? "unknown" : id.toString();
    }

    private static String shortDim(String dimensionId) {
        int colon = dimensionId.indexOf(':');
        return colon >= 0 ? dimensionId.substring(colon + 1) : dimensionId;
    }
}
