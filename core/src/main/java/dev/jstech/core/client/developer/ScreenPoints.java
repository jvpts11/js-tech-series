/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.developer;

import dev.jstech.core.client.gui.data.IDataScreen;
import java.util.Optional;

/**
 * What lies under a point of a screen declared in a file, by name: how the studio's recorder keeps a click as the
 * component clicked, so the test it writes still clicks it when the screen is laid out another way.
 */
public final class ScreenPoints {

    private ScreenPoints() {
    }

    /** The name of the deepest named component of {@code screen} at the point, on the game's screen; none for none. */
    public static Optional<String> nameAt(final IDataScreen screen, final double x, final double y) {
        return new InspectorModel(screen.view()).namedAt(x, y);
    }
}
