package com.bng.marketcoordination.integration.sm.mixin;

import by.deokma.stockmarket.neoforge.client.GuiTextures;
import by.deokma.stockmarket.neoforge.client.StockMarketScreen;
import by.deokma.stockmarket.neoforge.client.UIHelper;
import com.bng.marketcoordination.client.ClientTraderHistory;
import com.bng.marketcoordination.network.TraderHistoryRequestPayload;
import com.bng.marketcoordination.ui.client.TraderGraphRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds a fourth "Volume" tab to Create Stock Market's screen that shows our per-player daily
 * trade-volume line graph.
 *
 * <p>Stock Market's screen keeps its tab index in {@code activeTab} and dispatches tab content with a
 * {@code switch (0..2)} that has an empty default, so an index of 3 renders the panel + tab strip but
 * no content — exactly the hole we fill. We mirror SM's own tab geometry so the extra tab lines up with
 * the built-in ones, draw it with SM's active/inactive tab textures (no icon, as requested), route
 * clicks on it to {@code activeTab = 3}, and paint the graph into the panel's content rectangle.
 * Registered in a non-required mixin config, so if Stock Market is absent the mixin is simply skipped.
 */
@Mixin(value = StockMarketScreen.class, remap = false)
public abstract class StockMarketGraphMixin {

    @Unique private static final int MARKETCOORD_VOLUME_TAB = 3;
    @Unique private static final String MARKETCOORD_VOLUME_LABEL = "Volume";
    @Unique private static final String[] MARKETCOORD_BASE_TABS = {"Shops", "Market", "Top Sellers"};

    @Shadow private int activeTab;
    @Shadow private static int lastTab;

    @Shadow protected abstract int panelX();

    @Shadow protected abstract int panelY();

    @Shadow protected abstract int panelW();

    @Shadow protected abstract int panelH();

    @Shadow protected abstract int tabHeight();

    /** X coordinate where the extra "Volume" tab begins (immediately after SM's built-in tabs). */
    @Unique
    private int marketcoord$volumeTabX(Font font) {
        int tabH = tabHeight();
        int iconSize = Math.min(16, tabH - 6);
        int x = panelX() + 8;
        for (String label : MARKETCOORD_BASE_TABS) {
            int tabW = iconSize + font.width(label) + 14;
            x += tabW + 4;
        }
        return x;
    }

    @Unique
    private int marketcoord$volumeTabWidth(Font font) {
        return font.width(MARKETCOORD_VOLUME_LABEL) + 14; // text-only tab, no icon
    }

    @Inject(method = "drawTabs", at = @At("TAIL"), require = 0)
    private void marketcoord$drawVolumeTab(
            GuiGraphics graphics, int panelX, int panelY, int mouseX, int mouseY, CallbackInfo ci) {
        Font font = Minecraft.getInstance().font;
        int tabH = tabHeight();
        int x = marketcoord$volumeTabX(font);
        int tabW = marketcoord$volumeTabWidth(font);
        boolean active = activeTab == MARKETCOORD_VOLUME_TAB;

        UIHelper.blitScaled(
                graphics,
                active ? GuiTextures.TAB_ACTIVE : GuiTextures.TAB_INACTIVE,
                x, panelY + 5, tabW, tabH - 4, 64, 14);

        int textX = x + (tabW - font.width(MARKETCOORD_VOLUME_LABEL)) / 2;
        int textY = panelY + 5 + ((tabH - 4) - 8) / 2;
        graphics.drawString(font, MARKETCOORD_VOLUME_LABEL, textX, textY, 0xFF111111, false);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, require = 0)
    private void marketcoord$clickVolumeTab(
            double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        Font font = Minecraft.getInstance().font;
        int tabH = tabHeight();
        int x = marketcoord$volumeTabX(font);
        int tabW = marketcoord$volumeTabWidth(font);
        int top = panelY() + 5;
        int bottom = panelY() + tabH - 1;
        if (mouseX >= x && mouseX < x + tabW && mouseY >= top && mouseY < bottom) {
            if (activeTab != MARKETCOORD_VOLUME_TAB) {
                activeTab = MARKETCOORD_VOLUME_TAB;
                lastTab = MARKETCOORD_VOLUME_TAB;
                PacketDistributor.sendToServer(TraderHistoryRequestPayload.INSTANCE);
            }
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void marketcoord$renderVolumeContent(
            GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (activeTab != MARKETCOORD_VOLUME_TAB) {
            return;
        }
        if (ClientTraderHistory.shouldRequest()) {
            PacketDistributor.sendToServer(TraderHistoryRequestPayload.INSTANCE);
        }
        int tabH = tabHeight();
        int gx = panelX() + 2;
        int gy = panelY() + tabH + 2;
        int gw = panelW() - 4;
        int gh = panelH() - tabH - 4;
        if (gw > 20 && gh > 20) {
            TraderGraphRenderer.render(graphics, Minecraft.getInstance().font, gx, gy, gw, gh);
        }
    }
}
