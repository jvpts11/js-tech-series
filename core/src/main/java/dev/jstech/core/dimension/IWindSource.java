/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.dimension;

import java.util.Optional;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * What says the wind of some places in place of the Core's own: a mod's weather, a world of its own. Asked first, the
 * last handed in first; one that says nothing of a place leaves it to the next. Handed to {@link Winds#source}. It
 * must say the same on the server and on every player's game, from what both know.
 */
@FunctionalInterface
public interface IWindSource {

    /** The wind at {@code at} in {@code level}, if this says it. */
    Optional<Wind> at(Level level, Vec3 at);
}
