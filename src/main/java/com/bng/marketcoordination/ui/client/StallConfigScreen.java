package com.bng.marketcoordination.ui.client;

import com.bng.marketcoordination.menu.StallConfigMenu;
import com.bng.marketcoordination.network.StallConfigSavePayload;
import dev.ithundxr.createnumismatics.content.backend.Coin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** Clean configuration GUI for a Merchant Stall: pick the item, set the price, save. */
public class StallConfigScreen extends AbstractContainerScreen<StallConfigMenu> {

    private EditBox qtyBox;
    private EditBox cogBox;
    private EditBox sprocketBox;
    private EditBox bevelBox;
    private EditBox spurBox;

    public StallConfigScreen(StallConfigMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 196;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        long price = menu.initialPriceSpurs();
        int cogs = (int) (price / Coin.COG.value);
        price %= Coin.COG.value;
        int sprockets = (int) (price / Coin.SPROCKET.value);
        price %= Coin.SPROCKET.value;
        int bevels = (int) (price / Coin.BEVEL.value);
        price %= Coin.BEVEL.value;
        int spurs = (int) price;

        qtyBox = numberBox(leftPos + 118, topPos + 18, Integer.toString(menu.initialSaleQuantity()));
        cogBox = numberBox(leftPos + 12, topPos + 72, Integer.toString(cogs));
        sprocketBox = numberBox(leftPos + 52, topPos + 72, Integer.toString(sprockets));
        bevelBox = numberBox(leftPos + 92, topPos + 72, Integer.toString(bevels));
        spurBox = numberBox(leftPos + 132, topPos + 72, Integer.toString(spurs));

        addRenderableWidget(Button.builder(Component.literal("Save"), b -> save())
                .bounds(leftPos + 112, topPos + 40, 54, 16)
                .build());
    }

    private EditBox numberBox(int x, int y, String initial) {
        EditBox box = new EditBox(font, x, y, 36, 16, Component.empty());
        box.setMaxLength(7);
        box.setFilter(s -> s.isEmpty() || s.matches("\\d{0,7}"));
        box.setValue(initial);
        addRenderableWidget(box);
        return box;
    }

    private int parse(EditBox box) {
        try {
            return Math.max(0, Integer.parseInt(box.getValue().trim()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void save() {
        long priceSpurs = (long) parse(cogBox) * Coin.COG.value
                + (long) parse(sprocketBox) * Coin.SPROCKET.value
                + (long) parse(bevelBox) * Coin.BEVEL.value
                + (long) parse(spurBox) * Coin.SPUR.value;
        int qty = Math.max(1, parse(qtyBox));
        PacketDistributor.sendToServer(new StallConfigSavePayload(qty, priceSpurs));
        onClose();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Panel + border.
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF3A3A3A);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFF2B2B2B);
        // Slot backgrounds.
        for (Slot slot : menu.slots) {
            graphics.fill(leftPos + slot.x - 1, topPos + slot.y - 1,
                    leftPos + slot.x + 17, topPos + slot.y + 17, 0xFF1D1D1D);
            graphics.fill(leftPos + slot.x, topPos + slot.y,
                    leftPos + slot.x + 16, topPos + slot.y + 16, 0xFF505050);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFFFFF, false);
        graphics.drawString(font, Component.literal("Inventory"), inventoryLabelX, inventoryLabelY, 0xC0C0C0, false);

        graphics.drawString(font, Component.literal("Item to sell"), 12, 18, 0xE0E0E0, false);
        graphics.drawString(font, Component.literal("Sell qty"), 118, 8, 0xE0E0E0, false);
        graphics.drawString(font, Component.literal("Price per sale:"), 12, 52, 0xFFD37F, false);
        graphics.drawString(font, Component.literal("Cog"), 14, 62, 0xB0B0B0, false);
        graphics.drawString(font, Component.literal("Spr"), 54, 62, 0xB0B0B0, false);
        graphics.drawString(font, Component.literal("Bev"), 94, 62, 0xB0B0B0, false);
        graphics.drawString(font, Component.literal("Spur"), 134, 62, 0xB0B0B0, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        // Show the resulting total in a friendly denomination string under the price row.
        ItemStack item = menu.ghostItem();
        if (!item.isEmpty()) {
            graphics.drawString(font, item.getHoverName(), leftPos + 34, topPos + 33, 0xE0E0E0, false);
        }
    }
}
