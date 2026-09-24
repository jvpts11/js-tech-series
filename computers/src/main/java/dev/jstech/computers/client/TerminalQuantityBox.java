/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.core.text.GameText;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import org.jetbrains.annotations.Nullable;

import java.util.function.LongConsumer;

/**
 * How many of a thing is being asked for: a field that takes digits and the steppers that nudge it.
 *
 * <p>It is a thing of its own because a field that is typed into and also set from outside has one trap,
 * and both questions that use it fall into it: writing the value back into the box fires the responder,
 * which reads the box and writes the value, over and over. The flag that says "this edit is mine" belongs
 * with the box rather than with whoever happens to own it that frame.
 *
 * <p>The box itself is not made until {@link #ensure(Font)}, which is called while the screen is being
 * laid out. A screen has no font while its fields are being initialised, and a box built with that null
 * throws the moment anything measures a string in it.
 */
final class TerminalQuantityBox {

    private final int width;
    private final int height;
    private final int maxLength;
    private final LongConsumer onTyped;

    @Nullable
    private EditBox box;

    /** Whether the edit in flight is the code's own, so the responder lets it through unanswered. */
    private boolean syncing;

    TerminalQuantityBox(final int width, final int height, final int maxLength,
                        final LongConsumer onTyped) {
        this.width = width;
        this.height = height;
        this.maxLength = maxLength;
        this.onTyped = onTyped;
    }

    /** Makes the box the first time the screen has a font to make it with. */
    void ensure(final Font font) {
        if (this.box != null) {
            return;
        }
        final EditBox made = new EditBox(font, -4000, -4000, this.width, this.height,
                GameText.component(TerminalGridTexts.QUANTITY));
        made.setMaxLength(this.maxLength);
        made.setFilter(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        made.setResponder(this::typed);
        made.visible = false;
        made.active = false;
        this.box = made;
    }

    /** The widget itself, for the screen to add and for a popup to draw over its own panel. */
    @Nullable
    EditBox widget() {
        return this.box;
    }

    void setTextColor(final int color) {
        if (this.box != null) {
            this.box.setTextColor(color);
        }
    }

    /** Puts the box where the question it belongs to is drawn. */
    void moveTo(final int x, final int y) {
        if (this.box != null) {
            this.box.setX(x);
            this.box.setY(y);
        }
    }

    /** Shows or hides it, taking the keyboard away with it when it goes. */
    void show(final boolean visible) {
        if (this.box == null) {
            return;
        }
        this.box.visible = visible;
        this.box.active = visible;
        if (!visible) {
            this.box.setFocused(false);
        }
    }

    boolean focused() {
        return this.box != null && this.box.isFocused();
    }

    void setFocused(final boolean focused) {
        if (this.box != null) {
            this.box.setFocused(focused);
        }
    }

    /** Writes a value in without the responder answering it back. */
    void set(final long value) {
        if (this.box == null) {
            return;
        }
        this.syncing = true;
        this.box.setValue(String.valueOf(value));
        this.syncing = false;
    }

    /** What is typed in it now, or zero for an empty box or one holding more digits than a long. */
    long value() {
        if (this.box == null) {
            return 0L;
        }
        final String text = this.box.getValue();
        if (text.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(text);
        } catch (final NumberFormatException overlong) {
            return 0L;
        }
    }

    boolean isMouseOver(final double mouseX, final double mouseY) {
        return this.box != null && this.box.isMouseOver(mouseX, mouseY);
    }

    boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        return this.box != null && this.box.mouseClicked(mouseX, mouseY, button);
    }

    boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        return this.box != null && this.box.keyPressed(key, scanCode, modifiers);
    }

    boolean charTyped(final char c, final int modifiers) {
        return this.box != null && this.box.charTyped(c, modifiers);
    }

    void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        if (this.box != null) {
            this.box.render(g, mouseX, mouseY, partialTick);
        }
    }

    private void typed(final String text) {
        if (!this.syncing) {
            this.onTyped.accept(value());
        }
    }
}
