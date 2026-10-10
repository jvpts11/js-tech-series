/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.projectile;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ProjectileSpecTest {

    @Test
    void constructor_refusesANotANumberGravity() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProjectileSpec(1.0F, Double.NaN, 0, 0.0, 0.0F, 0.0F, false, 10));
    }

    @Test
    void constructor_refusesANotANumberDamage() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProjectileSpec(Float.NaN, 0.0, 0, 0.0, 0.0F, 0.0F, false, 10));
    }

    @Test
    void constructor_refusesAnInfiniteKnockback() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProjectileSpec(1.0F, 0.0, 0, Double.POSITIVE_INFINITY, 0.0F, 0.0F, false, 10));
    }

    @Test
    void constructor_refusesANotANumberExplosion() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProjectileSpec(1.0F, 0.0, 0, 0.0, 0.0F, Float.NaN, false, 10));
    }
}
