/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.live;

import java.util.Optional;
import net.minecraft.core.BlockPos;

/**
 * What the parts of a block's screen in the world are, on the player's game: the part at a pixel, by its name and what
 * a press on it does. The press carries the name to the server; the action bar says what it does while the crosshair
 * is on it. A {@link TouchPanel} answers for the Core's components it holds.
 */
@FunctionalInterface
public interface ITouchTargets {

    /** The part of the screen of the block at {@code pos} at the pixel {@code x}, {@code y}, or none. */
    Optional<TouchTarget> at(BlockPos pos, int x, int y);
}
