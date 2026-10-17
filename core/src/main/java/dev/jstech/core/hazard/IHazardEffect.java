/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import net.minecraft.world.entity.LivingEntity;

/**
 * What a hazard does to a body while what has built up in it stands in a band: a sickness, a slowness, harm. It is
 * done once a second, and, for what must be smooth (air running out), every tick too.
 */
public interface IHazardEffect {

    /** A second of it on {@code body}, with {@code level} of {@code kind} built up. */
    default void second(final LivingEntity body, final HazardKind kind, final double level) {
    }

    /** A tick of it on {@code body}, with {@code level} of {@code kind} built up. */
    default void tick(final LivingEntity body, final HazardKind kind, final double level) {
    }
}
