/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.hazard;

import net.minecraft.world.entity.LivingEntity;

/**
 * A bubble of normal laws round a body, which keeps dissonance from building up in it: an armour module, a block that
 * makes a zone. It spends its own energy; without energy it says it shields no one, and dissonance starts counting.
 */
@FunctionalInterface
public interface IDissonanceShield {

    /** Whether {@code body} stands in a bubble of normal laws this second. */
    boolean shields(LivingEntity body);
}
