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
 * What speeds a body's recovery from a hazard while it is away from the hazard's sources: a medicine taken, a bed, a
 * shower, a clean room. Asked once a second for each body recovering.
 */
@FunctionalInterface
public interface IHazardRecovery {

    /**
     * How much faster {@code body} recovers now, on top of its own pace: 0 for no faster, 1 for twice as fast.
     * Every recovery of a hazard adds up.
     */
    double faster(LivingEntity body, ServerLevel level);
}
