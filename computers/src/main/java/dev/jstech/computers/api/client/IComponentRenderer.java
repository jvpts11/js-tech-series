/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.api.client;

import dev.jstech.core.api.client.ISkin;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.ApiStatus;

/**
 * What draws a kind of generic component in a program's window, on the client.
 *
 * <p>It is handed what the program put in the component, as plain values (see {@link IComponentActions}), and the
 * skin of the system the machine runs, so it can draw in that system's look. Every viewer of the window is sent the
 * same value, so a renderer whose picture follows from the value alone shows every viewer the same thing, which is
 * why a component is drawn for everyone. A renderer that throws shows the placeholder from then on, with one line in
 * the log.
 */
@ApiStatus.Experimental
public interface IComponentRenderer {

    /** How wide the component is when the program asks for no width. */
    default int width() {
        return 80;
    }

    /** How tall the component is when the program asks for no height. */
    default int height() {
        return 40;
    }

    /**
     * Draws the component.
     *
     * @param data what the program put in it, or null before it put anything
     */
    void draw(GuiGraphics graphics, Font font, ISkin skin, int x, int y, int width, int height, Object data,
              int mouseX, int mouseY);

    /** A click inside it, in the window's pixels; true when it was taken. */
    default boolean mouseClicked(final double mouseX, final double mouseY, final int button, final Object data,
                                 final IComponentActions actions) {
        return false;
    }

    /** A key pressed while it has the keyboard, which it has from the click that took. */
    default boolean keyPressed(final int key, final int scanCode, final int modifiers, final Object data,
                               final IComponentActions actions) {
        return false;
    }

    /** A character typed while it has the keyboard. */
    default boolean charTyped(final char typed, final Object data, final IComponentActions actions) {
        return false;
    }
}
