/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.component;

import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * A small box with a label that turns something on or off. The state lives with its owner and is read every
 * frame; a click asks the owner to flip it.
 */
public final class Checkbox extends UiComponent {

    private final Supplier<String> label;
    private final BooleanSupplier checked;
    private final Runnable onToggle;
    private float labelScale = 1f;
    private int box = BOX;
    private boolean focusable;

    private static final int BOX = 7;

    public Checkbox(final Supplier<String> label, final BooleanSupplier checked, final Runnable onToggle) {
        this.label = label;
        this.checked = checked;
        this.onToggle = onToggle;
    }

    /** Draws the label smaller than the font, for a dense list of choices. */
    public Checkbox setLabelScale(final float scale) {
        labelScale = scale;
        return this;
    }

    /** How big the box is drawn, as square; the game's own are 20 pixels, the series' dense ones 7. */
    public Checkbox setBoxSize(final int pixels) {
        box = Math.max(1, pixels);
        return this;
    }

    @Override
    public void render(final GuiGraphics g, final UiContext ctx) {
        final boolean on = checked.getAsBoolean();
        final int by = y() + (height() - box) / 2;
        ctx.skin().checkbox(g, ctx.font(), x(), by, box, on, hovered(ctx) || isFocused());
        final int textX = x() + box + 4;
        final int fits = (int) ((width() - box - 4) / labelScale);
        final String text = Texts.trim(ctx.font(), label.get(), fits);
        final int th = Math.round(7 * labelScale);
        ctx.skin().drawText(g, ctx.font(), text, textX, y() + (height() - th) / 2, ctx.skin().text(), labelScale);
    }

    @Override
    public boolean mouseClicked(final double mx, final double my, final int button) {
        if (button != 0) {
            return false;
        }
        onToggle.run();
        return true;
    }

    /** Whether the keyboard reaches the box, which Enter or Space then tick; off unless asked for. */
    public Checkbox setFocusable(final boolean value) {
        focusable = value;
        return this;
    }

    @Override
    public boolean focusable() {
        return focusable;
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_SPACE) {
            onToggle.run();
            return true;
        }
        return false;
    }
}
