/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/**
 * Base screen for mod GUIs that ARE bound to a container menu (slots / inventory): machines with input/output slots,
 * computers with drive bays, etc. Counterpart to {@link CoreScreen} (which is menu-less). It draws the tooltips over
 * everything, frames the menu's own slots, tests the mouse against rectangles in the screen's own space and presses
 * the menu's buttons, so no screen writes those again.
 */
public abstract class CoreContainerScreen<T extends AbstractContainerMenu>
        extends AbstractContainerScreen<T> {

    protected CoreContainerScreen(
            final T menu,
            final Inventory playerInventory,
            final Component title) {
        super(menu, playerInventory, title);
    }

    /*
     * The game's own render already dims the world behind the screen before it draws the panel; dimming it again
     * here would turn the world behind the screen almost black.
     */
    @Override
    public void render(
            final GuiGraphics graphics,
            final int mouseX,
            final int mouseY,
            final float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(
            final GuiGraphics graphics,
            final float partialTick,
            final int mouseX,
            final int mouseY) {
        graphics.fill(
                leftPos, topPos,
                leftPos + imageWidth, topPos + imageHeight,
                ScreenPalette.get().containerGround());
    }

    /**
     * Draws a frame under every slot the menu shows, where the menu placed it: a {@code border} one pixel around
     * the slot and a {@code fill} behind its item. The frames come from the menu's own slots, so a screen never
     * repeats a slot position and a frame can never stand where no slot is.
     */
    protected void drawSlotFrames(final GuiGraphics graphics, final int border, final int fill) {
        for (final Slot slot : menu.slots) {
            if (!slot.isActive()) {
                continue;
            }
            final int x = leftPos + slot.x;
            final int y = topPos + slot.y;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, border);
            graphics.fill(x, y, x + 16, y + 16, fill);
        }
    }

    /**
     * Whether the mouse is inside the rectangle at {@code (x, y)} sized {@code width x height}, the rectangle given
     * in this screen's own space, from its top left corner.
     */
    protected boolean hover(final int mouseX, final int mouseY, final int x, final int y, final int width,
                            final int height) {
        final int mx = mouseX - leftPos;
        final int my = mouseY - topPos;
        return mx >= x && mx < x + width && my >= y && my < y + height;
    }

    /** Presses the menu's button of that id, which the menu on the server answers. */
    protected void sendButton(final int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }
}
