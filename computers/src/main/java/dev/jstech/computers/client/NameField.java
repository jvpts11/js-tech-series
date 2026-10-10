/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.theme.JsTechTheme;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/**
 * The name field of a device's screen: an edit box that takes the keyboard and the click that focuses it but is never
 * drawn by the game, since vanilla draws its text with a dark copy of the letters as a shadow, a smear on a light
 * era's field. The screen draws the field itself with {@link #render}, so every device shows its name the same way.
 */
final class NameField {

    private static final long CARET_BLINK_MILLIS = 500L;

    private final EditBox box;

    /**
     * @param onChange told the new name on every edit, to send it to the server
     */
    NameField(final Font font, final int x, final int y, final int width, final int height, final Component label,
              final int maxLength, final String initial, final Consumer<String> onChange) {
        this.box = new EditBox(font, x, y, width, height, label);
        // Unbordered, its text starts where the field draws it, so a click lands the caret on the letter clicked.
        this.box.setBordered(false);
        this.box.setTextShadow(false);
        this.box.setMaxLength(maxLength);
        this.box.setValue(initial);
        this.box.setResponder(onChange);
    }

    /** The widget to hand to the screen so that it receives the clicks. */
    EditBox widget() {
        return this.box;
    }

    /** What is typed so far. */
    String value() {
        return this.box.getValue();
    }

    /**
     * Gives a key to the field while it is focused, so the inventory key types instead of closing the screen.
     *
     * @return whether the field took the key
     */
    boolean keyPressed(final int key, final int scan, final int mods) {
        if (this.box.isFocused() && key != GLFW.GLFW_KEY_ESCAPE) {
            this.box.keyPressed(key, scan, mods);
            return true;
        }
        return false;
    }

    /** Gives a typed character to the field while it is focused; false when it is not. */
    boolean charTyped(final char c, final int mods) {
        return this.box.isFocused() && this.box.charTyped(c, mods);
    }

    /**
     * Draws the name being typed, or {@code defaultName} dimmed while there is none, with a caret where the next
     * letter goes. A name wider than {@code room} shows the part around the caret.
     */
    void render(final GuiGraphics g, final Font font, final int x, final int y, final int room,
                final String defaultName) {
        final String value = this.box.getValue();
        final int cursor = Math.min(this.box.getCursorPosition(), value.length());
        final String before = Texts.tail(font, value.substring(0, cursor), room);
        String after = value.substring(cursor);
        while (!after.isEmpty() && font.width(before + after) > room) {
            after = after.substring(0, after.length() - 1);
        }
        if (value.isEmpty()) {
            Draw.text(g, font, defaultName, x, y, JsTechTheme.dim(), JsTechTheme.slotBg());
        } else {
            Draw.text(g, font, before + after, x, y, JsTechTheme.text(), JsTechTheme.slotBg());
        }
        if (this.box.isFocused() && Util.getMillis() / CARET_BLINK_MILLIS % 2 == 0) {
            final int caretX = x + font.width(before);
            g.fill(caretX, y - 1, caretX + 1, y + 9, JsTechTheme.text());
        }
    }
}
