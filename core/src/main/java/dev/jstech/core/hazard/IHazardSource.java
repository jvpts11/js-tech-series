/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * Where a hazard builds up from: a block, an item carried, a creature, an area, a dimension, a chunk's pollution. Asked
 * once a second how much it gives a body now, before what the body wears takes its share off.
 */
@FunctionalInterface
public interface IHazardSource {

    /** How much of the hazard {@code body} takes from it in a second, in the hazard's unit; 0 where it gives none. */
    double rate(LivingEntity body, ServerLevel level);
}
