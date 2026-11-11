/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.monitor;

import net.minecraft.client.gui.GuiGraphics;

/**
 * A machine's screen the player's game can hold on the glass of a touch monitor rather than open over the game: it is
 * laid out on an area just bigger than a monitor's glass, the glass half of what is kept clear round it from the
 * corner, as a monitor's face is drawn, and it is drawn there each frame with the finger where the mouse would be.
 */
public interface ITouchGlass {

    /**
     * Tells the screen it is held on a touch monitor's glass from now on, before it is laid out there: a screen that
     * draws differently under a finger than under a mouse (a pointer of its own, a touch keyboard) does so from now.
     */
    default void holdOnGlass() {
    }

    /**
     * Draws the screen on the glass, with the finger at ({@code fingerX}, {@code fingerY}), in the screen's own
     * coordinates, where the mouse would be: the screen alone, without the game's dimming of the world behind it.
     */
    void paintTouched(GuiGraphics g, int fingerX, int fingerY, float partialTick);

    /** Whether what was last touched on it takes typing: a prompt, a field, a document, which the typing line opens for. */
    boolean takesTyping();
}
