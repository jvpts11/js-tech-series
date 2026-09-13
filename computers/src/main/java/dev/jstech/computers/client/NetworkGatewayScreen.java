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
import dev.jstech.core.client.gui.theme.EraTheme;
import dev.jstech.core.client.gui.theme.EraThemes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

/**
 * The Network Gateway's own screen, in the hardware theme: which Gateway this is and who it belongs to,
 * two lights for the two links, the item buffer and the player's inventory. Nothing is configured here;
 * that is the Gateway Manager's job on the host.
 *
 * <p>Each status line is clipped to the panel; a clipped line shows its full text as a tooltip.
 */
public class NetworkGatewayScreen extends AbstractContainerScreen<NetworkGatewayMenu> {

    private static final int LINES = 3;
    private static final String TITLE = "NETWORK GATEWAY";

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
        for (int i = 0; i < NetworkGatewayLayout.BUFFER_SLOTS; i++) {
            theme.slot(g, x + NetworkGatewayLayout.bufferX(i) + 1, y + NetworkGatewayLayout.BUFFER_Y + 1);
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                theme.slot(g, x + NetworkGatewayLayout.INV_X + col * 18, y + NetworkGatewayLayout.INV_Y + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            theme.slot(g, x + NetworkGatewayLayout.INV_X + col * 18, y + NetworkGatewayLayout.INV_Y + 58);
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
        line(g, 0, linked ? name + " on " + host + ", " + be.linkKind() : name + ", not linked: plug the cable in the back",
                ix, iy, linked ? theme.text() : theme.amber());
        line(g, 1, be == null ? "" : "Seen from CC as " + be.peripheralName(), ix, iy + lh,
                cc ? theme.text() : theme.dim());
        line(g, 2, "Managed in Gateway Manager", ix, iy + lh * 2, theme.dim());
        small(g, "ITEM BUFFER", x + NetworkGatewayLayout.BUFFER_X, y + NetworkGatewayLayout.BUFFER_CAPTION_Y, theme.dim());
        small(g, "pull lands here, push leaves", x + NetworkGatewayLayout.BUFFER_HINT_X,
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
        g.drawString(font, TITLE, NetworkGatewayLayout.TITLE_X, NetworkGatewayLayout.TITLE_Y, theme.text(), false);
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
        renderTooltip(g, mouseX, mouseY);
        final int ix = leftPos + NetworkGatewayLayout.INFO_X;
        final int iy = topPos + NetworkGatewayLayout.INFO_Y;
        if (mouseX >= ix && mouseX < ix + NetworkGatewayLayout.INFO_W) {
            for (int i = 0; i < LINES; i++) {
                final int ly = iy + i * NetworkGatewayLayout.LINE_H;
                if (clippedLines[i] != null && mouseY >= ly && mouseY < ly + NetworkGatewayLayout.LINE_H) {
                    g.renderTooltip(font, Component.literal(clippedLines[i]), mouseX, mouseY);
                }
            }
        }
    }
}
