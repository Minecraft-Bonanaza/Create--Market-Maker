package com.bng.marketcoordination.integration.vc;

import by.deokma.stockmarket.shop.ShopEntry;
import by.deokma.stockmarket.shop.ShopSavedData;
import com.beakbock.createvillagercommerce.blockentity.MerchantStallBlockEntity;
import com.beakbock.createvillagercommerce.server.MarketLedgerManager;
import dev.ithundxr.createnumismatics.content.backend.Coin;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Turns a Create: Villager Commerce merchant stall into a Create Stock Market {@link ShopEntry}.
 *
 * <p>Stalls sell an item for a Spur price, which is exactly how a Numismatics vendor shop behaves, so
 * we index them with the same {@code SELL}/{@code VENDOR} conventions the Stock Market uses for those.
 * The price is normalised to a per-single-item Spur value so a stall selling a stack is comparable
 * with single-item vendors in the shared global price index.
 */
public final class VcStockMarketBridge {

    /** Stock Market treats a shop that sells goods for currency as mode {@code SELL}. */
    private static final String MODE_SELL = "SELL";

    /** Shop type used by spur-priced vendor shops; makes MarketRegistry aggregate our price. */
    private static final String SHOP_TYPE_VENDOR = "VENDOR";

    private VcStockMarketBridge() {}

    /**
     * Writes (or refreshes) a shop entry for the given stall. Returns {@code true} if the stall was a
     * sellable shop and got indexed, {@code false} if it had nothing to sell.
     */
    public static boolean index(ServerLevel level, MerchantStallBlockEntity stall, ShopSavedData data) {
        ItemStack saleItem = stall.getSaleItem();
        if (saleItem == null || saleItem.isEmpty()) {
            return false;
        }
        long totalSpurs = VcIntegration.offerSpurCost(stall);
        if (totalSpurs <= 0L) {
            return false;
        }
        int qty = Math.max(1, stall.getSaleQuantity());
        int perUnitSpurs = (int) Math.min(Integer.MAX_VALUE, Math.max(1L, Math.round((double) totalSpurs / qty)));

        BlockPos pos = stall.getBlockPos();
        String dimensionId = level.dimension().location().toString();

        // Use the stall's real owner so Stock Market shows who owns it; fall back to a stable synthetic
        // identity / market name only when the stall has not been claimed by a player yet.
        UUID ownerId = stall.getMarketOwnerId();
        String ownerName = stall.getMarketOwnerName();

        ShopEntry entry = new ShopEntry(
                pos,
                dimensionId,
                saleItem.copyWithCount(1),
                perUnitSpurs,
                coinStack(stall),
                ownerId != null ? ownerId : VcIntegration.sellerIdentity(stall),
                (ownerName != null && !ownerName.isBlank()) ? ownerName : fallbackName(level, stall),
                MODE_SELL,
                SHOP_TYPE_VENDOR,
                0);
        data.put(makeKey(dimensionId, pos), entry);
        return true;
    }

    private static ItemStack coinStack(MerchantStallBlockEntity stall) {
        ItemStack currency = stall.getCurrencyItem();
        if (currency != null && !currency.isEmpty()) {
            return currency.copyWithCount(1);
        }
        return Coin.SPUR.asStack(1);
    }

    private static String fallbackName(ServerLevel level, MerchantStallBlockEntity stall) {
        BlockPos ledgerPos = stall.getLinkedMarketLedgerPos();
        if (ledgerPos != null) {
            String name = MarketLedgerManager.getMarketName(level, ledgerPos);
            if (name != null && !name.isBlank()) {
                return name;
            }
        }
        return "Merchant Stall";
    }

    /** Unique, VC-namespaced key so we never collide with a real Stock Market shop entry. */
    private static String makeKey(String dimensionId, BlockPos pos) {
        return "vc/" + dimensionId + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}
