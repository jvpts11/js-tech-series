/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.projectile;

/**
 * How a projectile behaves in flight and when it hits.
 *
 * @param damage          the harm it does to what it hits
 * @param gravity         how hard it falls each tick: an arrow's is 0.05, a snowball's 0.03, a bolt's nothing
 * @param pierce          how many things it passes through before it stops in one
 * @param knockback       how hard it pushes what it hits away
 * @param fireSeconds     how long what it hits burns; 0 for no fire
 * @param explosion       how big a blast it ends in, as TNT's is 4; 0 for none
 * @param breaksBlocks    whether its blast breaks blocks
 * @param lifetimeTicks   how long it flies before it is gone, hit or not
 */
public record ProjectileSpec(float damage, double gravity, int pierce, double knockback, float fireSeconds,
                             float explosion, boolean breaksBlocks, int lifetimeTicks) {

    public ProjectileSpec {
        if (damage < 0.0F || gravity < 0.0 || pierce < 0 || knockback < 0.0 || fireSeconds < 0.0F
                || explosion < 0.0F || lifetimeTicks < 1) {
            throw new IllegalArgumentException("a projectile does no less than nothing, and flies a tick at least");
        }
    }
}
