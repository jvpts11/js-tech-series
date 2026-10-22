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
 * What a kind of hazard that is measured rather than built up stands at in a body: read once a second in place of its
 * sources and its recovery, as how far a body's core is past the edge of its normal warmth.
 */
@FunctionalInterface
public interface IHazardGauge {

    /** How far {@code body} stands in the hazard now, in its unit. */
    double level(LivingEntity body, ServerLevel level);
}
