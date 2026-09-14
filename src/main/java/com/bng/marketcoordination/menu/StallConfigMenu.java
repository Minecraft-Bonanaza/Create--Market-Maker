package com.bng.marketcoordination.menu;

import com.beakbock.createvillagercommerce.blockentity.MerchantStallBlockEntity;
import com.bng.marketcoordination.integration.vc.VcIntegration;
import com.bng.marketcoordination.registry.ModMenus;
import dev.ithundxr.createnumismatics.content.backend.Coin;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Container menu backing our custom Merchant Stall configuration GUI.
 *
 * <p>The player picks a sale item into a non-consuming "ghost" slot and enters a price in
 * Numismatics denominations; on save the values are written straight onto the stall's block
 * entity. Prices are stored as a Spur total (currency = Spur, priceQuantity = total Spurs) so
 * an arbitrary mix of cogs/sprockets/bevels/spurs can be represented in VC's single-currency
 * stall model.
 */
public class StallConfigMenu extends AbstractContainerMenu {

    public static final int GHOST_SLOT_INDEX = 0;

    private final Container ghost = new SimpleContainer(1);
    private final BlockPos stallPos;
    private final MerchantStallBlockEntity stall; // server side only; null on client
    private final int initialSaleQuantity;
    private final long initialPriceSpurs;

    /** Client-side constructor (invoked via {@code IContainerFactory} from the open packet). */
    public StallConfigMenu(int windowId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(windowId, playerInventory, null,
                buf.readBlockPos(),
                ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                buf.readVarInt(),
                buf.readVarLong());
    }

    /** Server-side constructor (invoked from the {@code SimpleMenuProvider}). */
    public StallConfigMenu(int windowId, Inventory playerInventory, MerchantStallBlockEntity stall) {
        this(windowId, playerInventory, stall,
                stall.getBlockPos(),
                stall.getSaleItem(),
                stall.getSaleQuantity(),
                VcIntegration.offerSpurCost(stall));
    }

    private StallConfigMenu(int windowId, Inventory playerInventory, MerchantStallBlockEntity stall,
                            BlockPos stallPos, ItemStack saleItem, int saleQuantity, long priceSpurs) {
        super(ModMenus.STALL_CONFIG.get(), windowId);
        this.stall = stall;
        this.stallPos = stallPos;
        this.initialSaleQuantity = Math.max(1, saleQuantity);
        this.initialPriceSpurs = Math.max(0L, priceSpurs);
        this.ghost.setItem(0, saleItem == null || saleItem.isEmpty() ? ItemStack.EMPTY : saleItem.copyWithCount(1));

        // Ghost display slot: never accepts real insertion and never yields items.
        addSlot(new Slot(ghost, 0, 13, 29) {
            @Override
            public boolean mayPlace(ItemStack candidate) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return false;
            }
        });

        // Player inventory (3 rows) + hotbar so the player can pick the item to sell.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 114 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 172));
        }
    }

    public BlockPos stallPos() {
        return stallPos;
    }

    public int initialSaleQuantity() {
        return initialSaleQuantity;
    }

    public long initialPriceSpurs() {
        return initialPriceSpurs;
    }

    public ItemStack ghostItem() {
        return ghost.getItem(0);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // Ghost slot: copy whatever the player is holding on the cursor without consuming it.
        if (slotId == GHOST_SLOT_INDEX
                && (clickType == ClickType.PICKUP || clickType == ClickType.PICKUP_ALL)) {
            ItemStack carried = getCarried();
            ghost.setItem(0, carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
            broadcastChanges();
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // No auto-transfer; the ghost slot is display-only and shift-clicking should do nothing.
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (stall == null) {
            return true; // client side trusts the server
        }
        Level level = stall.getLevel();
        if (level == null || level.getBlockEntity(stallPos) != stall) {
            return false;
        }
        return player.distanceToSqr(
                stallPos.getX() + 0.5, stallPos.getY() + 0.5, stallPos.getZ() + 0.5) <= 64.0;
    }

    /**
     * Server-side application of the configured item + price onto the stall. Called from the save
     * packet handler. The price is expressed as a total number of Spurs.
     */
    public void applyToStall(int saleQuantity, long priceSpurs) {
        if (stall == null) {
            return;
        }
        ItemStack item = ghost.getItem(0);
        int qty = Math.max(1, saleQuantity);

        stall.setSaleItem(item.isEmpty() ? ItemStack.EMPTY : item.copyWithCount(1));
        stall.setSaleQuantity(qty);

        if (item.isEmpty() || priceSpurs <= 0L) {
            stall.setCurrencyItem(ItemStack.EMPTY);
            stall.setPriceQuantity(0);
        } else {
            stall.setCurrencyItem(Coin.SPUR.asStack(1));
            stall.setPriceQuantity((int) Math.min(priceSpurs, Integer.MAX_VALUE));
        }

        stall.setChanged();
        Level level = stall.getLevel();
        if (level != null) {
            BlockEntity be = level.getBlockEntity(stallPos);
            if (be != null) {
                BlockState state = level.getBlockState(stallPos);
                level.sendBlockUpdated(stallPos, state, state, 3);
            }
        }
    }
}
