/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.data;

import com.mojang.blaze3d.platform.InputConstants;

/** Which keys a screen declared in a file hands its components before the game's screen sees them. */
final class DataKeys {

    private DataKeys() {
    }

    /**
     * Whether {@code key} goes to the view first: every key but Escape and Tab while a field has the keyboard, so
     * typing an E does not close the screen and Tab still leaves the field; and Escape while a list is open, which it
     * closes before it closes the screen.
     */
    static boolean toView(final DataScreenView view, final int key) {
        if (view.typing() && key != InputConstants.KEY_ESCAPE && key != InputConstants.KEY_TAB) {
            return true;
        }
        return key == InputConstants.KEY_ESCAPE && view.overlayOpen();
    }
}
