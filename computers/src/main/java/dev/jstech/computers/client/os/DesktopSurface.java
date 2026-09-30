/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.menu.DesktopMenu;
import org.jetbrains.annotations.Nullable;

/**
 * What a desktop is drawn on: the screen a player opens at a monitor, or anything else that shows one, such as a
 * monitor's face drawn in the world. The desktop asks it only the few things that differ between the two, so all the
 * rest of a desktop works the same whether anyone has its screen open or not.
 */
interface DesktopSurface {

    /** How wide the surface is, in the game's screen pixels; the monitor's glass is centred in it. */
    int surfaceWidth();

    int surfaceHeight();

    /**
     * The container that carries the player's inventory into a window that has an inventory zone, or null for a
     * surface that has none, where such a window shows its zone empty.
     */
    @Nullable
    DesktopMenu container();

    /** Leaves the desktop without touching the machine, which is what logging off is. */
    void leave();
}
