/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.blockentity.NetworkGatewayBlockEntity;
import dev.jstech.computers.gui.layout.NetworkGatewayLayout;
import dev.jstech.computers.menu.NetworkGatewayMenu;
import dev.jstech.core.client.gui.screen.CoreContainerScreen;
import dev.jstech.core.client.gui.theme.EraTheme;
import dev.jstech.core.client.gui.theme.EraThemes;
import dev.jstech.core.text.GameText;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

/**
 * The Network Gateway's own screen, in the hardware theme: which Gateway this is and who it belongs to,
 * two lights for the two links, the item buffer and the player's inventory. Nothing is configured here;
 * that is the Gateway Manager's job on the host.
 *
 * <p>Each status line is clipped to the panel; a clipped line shows its full text as a tooltip.
 */
public class NetworkGatewayScreen extends CoreContainerScreen<NetworkGatewayMenu> {

    private static final int LINES = 3;

    private final EraTheme theme = EraThemes.STANDARD;
    private final String[] clippedLines = new String[LINES];

    public NetworkGatewayScreen(final NetworkGatewayMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.imageWidth = NetworkGatewayLayout.WIDTH;
        this.imageHeight = NetworkGatewayLayout.HEIGHT;
        this.titleLabelX = -10000;
        this.inventoryLabelY = -10000;
    }

    @Nullable
    private NetworkGatewayBlockEntity gateway() {
        return minecraft != null && minecraft.level != null
                && minecraft.level.getBlockEntity(menu.blockEntityPos()) instanceof NetworkGatewayBlockEntity be
                ? be : null;
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final int x = leftPos;
        final int y = topPos;
        theme.window(g, x, y, imageWidth, imageHeight);
        // Slot frames come straight from the menu's own slots, so a moved slot always draws its frame with it.
        for (final Slot slot : menu.slots) {
            if (slot.isActive()) {
                theme.slot(g, x + slot.x, y + slot.y);
            }
        }

        final NetworkGatewayBlockEntity be = gateway();
        final boolean linked = be != null && be.online();
        final boolean cc = be != null && be.ccOnline();
        g.fill(x + NetworkGatewayLayout.LED_JS_X, y + NetworkGatewayLayout.LED_Y,
                x + NetworkGatewayLayout.LED_JS_X + NetworkGatewayLayout.LED,
                y + NetworkGatewayLayout.LED_Y + NetworkGatewayLayout.LED, linked ? theme.green() : theme.red());
        g.fill(x + NetworkGatewayLayout.LED_CC_X, y + NetworkGatewayLayout.LED_Y,
                x + NetworkGatewayLayout.LED_CC_X + NetworkGatewayLayout.LED,
                y + NetworkGatewayLayout.LED_Y + NetworkGatewayLayout.LED, cc ? theme.green() : theme.dim());

        final int ix = x + NetworkGatewayLayout.INFO_X;
        final int iy = y + NetworkGatewayLayout.INFO_Y;
        final int lh = NetworkGatewayLayout.LINE_H;
        final String name = be == null ? "" : be.name();
        final String host = be == null ? "" : be.hostName();
        line(g, 0, GameText.resolve(linked ? NetworkGatewayTexts.LINKED.with(name, host, be.linkKind())
                        : NetworkGatewayTexts.NOT_LINKED.with(name)),
                ix, iy, linked ? theme.text() : theme.amber());
        line(g, 1, be == null ? "" : GameText.resolve(NetworkGatewayTexts.SEEN_FROM_CC.with(be.peripheralName())),
                ix, iy + lh, cc ? theme.text() : theme.dim());
        line(g, 2, GameText.resolve(NetworkGatewayTexts.MANAGED), ix, iy + lh * 2, theme.dim());
        small(g, GameText.resolve(NetworkGatewayTexts.ITEM_BUFFER), x + NetworkGatewayLayout.BUFFER_X,
                y + NetworkGatewayLayout.BUFFER_CAPTION_Y, theme.dim());
        small(g, GameText.resolve(NetworkGatewayTexts.BUFFER_HINT), x + NetworkGatewayLayout.BUFFER_HINT_X,
                y + NetworkGatewayLayout.BUFFER_CAPTION_Y, theme.dim());
    }

    /** Draws a status line at the small font, clipped to the panel with an ellipsis, remembering the full text. */
    private void line(final GuiGraphics g, final int index, final String text, final int x, final int y,
                      final int color) {
        final float scale = NetworkGatewayLayout.INFO_SCALE;
        final int limit = (int) (NetworkGatewayLayout.INFO_W / scale);
        String shown = text;
        if (font.width(text) > limit) {
            shown = font.plainSubstrByWidth(text, limit - font.width("...")) + "...";
            clippedLines[index] = text;
        } else {
            clippedLines[index] = null;
        }
        small(g, shown, x, y, color);
    }

    private void small(final GuiGraphics g, final String text, final int x, final int y, final int color) {
        final float scale = NetworkGatewayLayout.INFO_SCALE;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1.0f);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
        g.drawString(font, GameText.resolve(NetworkGatewayTexts.TITLE), NetworkGatewayLayout.TITLE_X,
                NetworkGatewayLayout.TITLE_Y, theme.text(), false);
        smallLabel(g, "J's", NetworkGatewayLayout.LED_JS_LABEL_X, NetworkGatewayLayout.TITLE_Y);
        smallLabel(g, "CC", NetworkGatewayLayout.LED_CC_LABEL_X, NetworkGatewayLayout.TITLE_Y);
        g.drawString(font, playerInventoryTitle, NetworkGatewayLayout.INV_X, NetworkGatewayLayout.INV_LABEL_Y,
                theme.dim(), false);
    }

    private void smallLabel(final GuiGraphics g, final String text, final int x, final int y) {
        final float scale = NetworkGatewayLayout.INFO_SCALE;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1.0f);
        g.drawString(font, text, 0, 0, theme.dim(), false);
        g.pose().popPose();
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        for (int i = 0; i < LINES; i++) {
            final int ly = NetworkGatewayLayout.INFO_Y + i * NetworkGatewayLayout.LINE_H;
            if (clippedLines[i] != null && hover(mouseX, mouseY, NetworkGatewayLayout.INFO_X, ly,
                    NetworkGatewayLayout.INFO_W, NetworkGatewayLayout.LINE_H)) {
                g.renderTooltip(font, Component.literal(clippedLines[i]), mouseX, mouseY);
            }
        }
    }
}
