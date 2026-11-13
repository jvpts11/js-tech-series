/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.launch;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.core.text.GameText;
import dev.jstech.space.launch.LaunchPanel;
import dev.jstech.space.launch.LaunchPanelMenu;
import dev.jstech.space.launch.LaunchTexts;
import dev.jstech.space.pad.PadServicePortBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

/**
 * The Launch Panel's console on the player's screen: the approved LCC-1, as large as the window allows, its switches,
 * key and buttons pressed with the mouse. Its pad's plate, clicked, takes a name for the pad typed on it: Enter keeps
 * it, Esc leaves the name as it was, and a click elsewhere on the console keeps it too; while it is typed, no key
 * closes the console.
 */
public final class LaunchPanelScreen extends AbstractContainerScreen<LaunchPanelMenu> {

    private float scale = 1;
    /* The name being typed on the pad's plate, or null while nobody is naming the pad. */
    private @Nullable StringBuilder naming;

    /* How far the console keeps from the window's edges, in its pixels. */
    private static final int MARGIN = 10;
    private static final int HIDDEN = -10_000;
    /* How wide the plate's hint is wrapped, in the screen's pixels. */
    private static final int HINT_WIDTH = 220;

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

    /** The name being typed on the pad's plate, or null while nobody is naming the pad. */
    public @Nullable String naming() {
        return this.naming == null ? null : this.naming.toString();
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (this.naming == null && LaunchPanelFace.onPlate(this.font, (mouseX - this.leftPos) / this.scale,
                (mouseY - this.topPos) / this.scale)) {
            graphics.renderTooltip(this.font, this.font.split(GameText.component(LaunchTexts.NAME_PAD), HINT_WIDTH),
                    mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (button == InputConstants.MOUSE_BUTTON_LEFT) {
            final double x = (mouseX - this.leftPos) / this.scale;
            final double y = (mouseY - this.topPos) / this.scale;
            if (LaunchPanelFace.onPlate(this.font, x, y)) {
                if (this.naming == null) {
                    this.naming = new StringBuilder(this.menu.panel().padName());
                }
                return true;
            }
            keepName();
            final int control = LaunchPanelFace.controlAt(this.menu.panel(), x, y);
            if (control >= 0) {
                LaunchPanel.PRESS.send(control);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        if (this.naming == null) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        // While the pad is named, every key is the plate's: none closes the console or reaches the game.
        switch (keyCode) {
            case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> keepName();
            case InputConstants.KEY_ESCAPE -> this.naming = null;
            case InputConstants.KEY_BACKSPACE -> {
                if (!this.naming.isEmpty()) {
                    this.naming.deleteCharAt(this.naming.length() - 1);
                }
            }
            default -> {
                // The rest arrive as the characters they type.
            }
        }
        return true;
    }

    @Override
    public boolean charTyped(final char typed, final int modifiers) {
        if (this.naming == null) {
            return super.charTyped(typed, modifiers);
        }
        if (this.naming.length() < PadServicePortBlockEntity.MOST_NAME && PadServicePortBlockEntity.nameable(typed)) {
            this.naming.append(typed);
        }
        return true;
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
        LaunchPanelFace.paint(graphics, this.font, this.imageWidth, this.menu.panel(), naming());
        graphics.pose().popPose();
    }

    @Override
    protected void renderLabels(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        // The console writes everything it shows.
    }

    /* Sends the name typed on the plate for the pad, if one is being typed, and leaves the plate. */
    private void keepName() {
        if (this.naming != null) {
            LaunchPanel.NAME_PAD.send(this.naming.toString());
            this.naming = null;
        }
    }
}
