/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.style;

import java.util.Optional;

/**
 * Where a style finds the palette it takes its colours from: the declared palettes in the game, a fixed set in a test.
 * The record is asked for each time, so a palette a resource pack recolours is seen at once.
 */
@FunctionalInterface
public interface IPaletteSource {

    /** No palettes at all. */
    IPaletteSource NONE = id -> Optional.empty();

    /** The palette {@code id} ({@code "jscore:era/vintage"}) as it stands now, or none. */
    Optional<Record> palette(String id);
}
