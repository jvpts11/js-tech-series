/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Radiation that is everywhere in a place rather than given off by a block: the sky's, what lies on a chunk, the
 * minerals in the rock round it. Asked a few times a second for each body and instrument it reaches.
 */
@FunctionalInterface
public interface IAmbientRadiation {

    /** How much of it there is at {@code at} of {@code level}, in the unit its kind's rate is shown in. */
    double at(ServerLevel level, Vec3 at);
}
