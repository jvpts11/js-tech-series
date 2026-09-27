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
 * Base screen for mod GUIs that ARE bound to a container menu (slots / inventory): machines with input/output slots, computers with drive bays, etc. Counterpart to {@link CoreScreen} (which is menu-less).
 */
public abstract class CoreContainerScreen<T extends AbstractContainerMenu>
        extends AbstractContainerScreen<T> {

    protected CoreContainerScreen(
            final T menu,
            final Inventory playerInventory,
            final Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    public void render(
            final GuiGraphics graphics,
            final int mouseX,
            final int mouseY,
            final float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        /*
         * Vanilla AbstractContainerScreen renders tooltips for hovered
         * slots when this is called after super.
         */
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
}
