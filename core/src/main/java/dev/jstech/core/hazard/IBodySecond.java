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

/** What a mod works out of a body each second, before the body's hazards are weighed: its warmth, its breath. */
@FunctionalInterface
public interface IBodySecond {

    /** A second of {@code body} in {@code level}. */
    void second(LivingEntity body, ServerLevel level);
}
