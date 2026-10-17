/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio;

import dev.jstech.core.client.audio.acoustics.Walls;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * How much of a running sound gets through the walls between it and the listener, by what each is made of
 * ({@link Walls}): a solid block of stone lets through what {@code Occlusion} says one block does, wool less, metal
 * more.
 */
final class OcclusionProbe {

    private OcclusionProbe() {
    }

    /** The share of a sound at {@code source} that reaches a listener at {@code listener}. */
    static float through(final Level level, final Vec3 listener, final Vec3 source) {
        return Walls.through(level, listener, source);
    }
}
