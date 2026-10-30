/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.launch;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.space.launch.LaunchPanel;
import dev.jstech.space.launch.LaunchPanelMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Launch Panel's console on the player's screen: the approved LCC-1, as large as the window allows, its switches,
 * key and buttons pressed with the mouse.
 */
public final class LaunchPanelScreen extends AbstractContainerScreen<LaunchPanelMenu> {

    private float scale = 1;

    /* How far the console keeps from the window's edges, in its pixels. */
    private static final int MARGIN = 10;
    private static final int HIDDEN = -10_000;

    public LaunchPanelScreen(final LaunchPanelMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.titleLabelX = HIDDEN;
        this.inventoryLabelY = HIDDEN;
    }

    /** How large the console is drawn, its own pixels to the screen's; and where its top left is. */
    public float scale() {
        return this.scale;
    }

    public int left() {
        return this.leftPos;
    }

    public int top() {
        return this.topPos;
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (button == InputConstants.MOUSE_BUTTON_LEFT) {
            final int control = LaunchPanelFace.controlAt(this.menu.panel(), (mouseX - this.leftPos) / this.scale,
                    (mouseY - this.topPos) / this.scale);
            if (control >= 0) {
                LaunchPanel.PRESS.send(control);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void init() {
        this.scale = Math.max(0.5F, Math.min((this.width - 2F * MARGIN) / LaunchPanelFace.WIDTH,
                (this.height - 2F * MARGIN) / LaunchPanelFace.HEIGHT));
        this.imageWidth = Math.round(LaunchPanelFace.WIDTH * this.scale);
        this.imageHeight = Math.round(LaunchPanelFace.HEIGHT * this.scale);
        super.init();
    }

    @Override
    protected void renderBg(final GuiGraphics graphics, final float partialTick, final int mouseX,
                            final int mouseY) {
        graphics.pose().pushPose();
        graphics.pose().translate(this.leftPos, this.topPos, 0);
        LaunchPanelFace.paint(graphics, this.font, this.imageWidth, this.menu.panel());
        graphics.pose().popPose();
    }

    @Override
    protected void renderLabels(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        // The console writes everything it shows.
    }
}
