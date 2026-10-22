/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.physics;

import dev.jstech.core.physics.Laws;
import org.jetbrains.annotations.Nullable;

/**
 * Something besides the laws of the place that changes how the player sees, as the laws of light do: a hazard built
 * up in the body, a potion of a mod's. What it gives is drawn with the place's own light, by the same shader
 * ({@link LightLawsClient#see}). On the player's game only.
 */
@FunctionalInterface
public interface ISight {

    /** What it does to the player's sight now, as laws of light, at {@code seconds} of the game; none for nothing. */
    @Nullable Laws.Light now(double seconds);
}
