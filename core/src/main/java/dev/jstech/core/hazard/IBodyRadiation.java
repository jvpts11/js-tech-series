/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import net.minecraft.world.entity.LivingEntity;

/** Radiation a body carries with it: a radioactive thing in its pockets, a source strapped to it. */
@FunctionalInterface
public interface IBodyRadiation {

    /** How much of it reaches {@code body} from what it carries, in the unit its kind's rate is shown in. */
    double of(LivingEntity body);
}
