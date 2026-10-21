/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.component;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.function.Supplier;

/**
 * A push button. It fires on the press, as every control of the desktops does, and stays drawn pressed until
 * the button is released so the press reads; a disabled button is drawn faded and takes nothing.
 */
public final class Button extends UiComponent {

    private Supplier<String> label;
    private Runnable onPress;
    private boolean primary;
    private boolean pressed;
    private float labelScale = 1f;
    /* Room kept clear at the right end, for a mark drawn there (a drop-down's caret); the label centres in the rest. */
    private int labelInsetRight;
    private boolean focusable;

    public Button(final String label, final Runnable onPress) {
        this(() -> label, onPress);
    }

    public Button(final Supplier<String> label, final Runnable onPress) {
        this.label = label;
        this.onPress = onPress;
    }

    public Button setLabel(final String value) {
        label = () -> value;
        return this;
    }

    public Button setLabel(final Supplier<String> value) {
        label = value;
        return this;
    }

    public String label() {
        return label.get();
    }

    public Button setOnPress(final Runnable action) {
        onPress = action;
        return this;
    }

    /** Whether this is the default action of its panel, drawn as such. */
    public Button setPrimary(final boolean value) {
        primary = value;
        return this;
    }

    /** Draws the label smaller than the font, for a dense row of buttons; {@code 1} is the font's own size. */
    public Button setLabelScale(final float scale) {
        labelScale = scale;
        return this;
    }

    /**
     * Keeps {@code pixels} at the button's right end clear of the label, for a mark drawn there, such as the caret of a
     * button that opens a list: the label centres in what is left, so the two never meet.
     */
    public Button setLabelInsetRight(final int pixels) {
        labelInsetRight = Math.max(0, pixels);
        return this;
    }

    @Override
    public void render(final GuiGraphics g, final UiContext ctx) {
        final String text = label.get();
        final boolean lit = hovered(ctx) || isFocused();
        if (labelScale == 1f && labelInsetRight == 0) {
            ctx.skin().button(g, ctx.font(), x(), y(), width(), height(), text, lit, pressed, primary, enabled());
        } else {
            // The skin draws the face; the label is centred on what the inset leaves of it, by hand.
            ctx.skin().button(g, ctx.font(), x(), y(), width(), height(), "", lit, pressed, primary, enabled());
            final int tw = Math.round(ctx.skin().textWidth(ctx.font(), text) * labelScale);
            final int th = Math.round(7 * labelScale);
            ctx.skin().drawText(g, ctx.font(), text, x() + (width() - labelInsetRight - tw) / 2,
                    y() + (height() - th) / 2 + (pressed ? 1 : 0), enabled() ? ctx.skin().text() : ctx.skin().dim(),
                    labelScale);
        }
    }

    @Override
    public boolean mouseClicked(final double mx, final double my, final int button) {
        if (button != 0) {
            return false;
        }
        pressed = true;
        onPress.run();
        return true;
    }

    @Override
    public boolean mouseReleased(final double mx, final double my, final int button) {
        pressed = false;
        return true;
    }

    /** Whether the keyboard reaches the button, which Enter or Space then press; off unless asked for. */
    public Button setFocusable(final boolean value) {
        focusable = value;
        return this;
    }

    @Override
    public boolean focusable() {
        return focusable;
    }

    /** Presses the button, as a click or a key does, when it can be pressed. */
    public void press() {
        if (enabled() && visible()) {
            onPress.run();
        }
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_SPACE) {
            press();
            return true;
        }
        return false;
    }

    @Override
    public Component narration() {
        return Narration.button(spoken() != null ? spoken() : Component.literal(label.get()));
    }

    @Override
    public Component narrationUsage() {
        return Narration.PRESS;
    }
}
