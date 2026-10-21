/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.live;

/**
 * A press on a screen in the world, as the server hears it: the pixel, and the name of the part of the screen the
 * player's game found under the crosshair there (a button's), empty when it found none. The name is what the player's
 * game says; a block that acts on it checks that the part may be pressed now, as it would a click on a menu's button.
 *
 * @param x      the pixel across
 * @param y      the pixel down
 * @param target the name of the part pressed, or empty
 */
public record TouchPress(int x, int y, String target) {

    /** The longest name of a part that a press carries; a longer one is sent as none. */
    public static final int MOST_TARGET = 64;

    public TouchPress {
        target = target == null || target.length() > MOST_TARGET ? "" : target;
    }

    /** Whether the press was on the part named {@code name}. */
    public boolean on(final String name) {
        return !target.isEmpty() && target.equals(name);
    }
}
