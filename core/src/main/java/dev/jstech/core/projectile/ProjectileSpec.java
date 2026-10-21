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
 * <p>A projectile with a mass flies by real ballistics: the air holds it back by its drag area and the air's density
 * where it flies, thin high up and none in a vacuum, water far more; the wind carries it. One with none slows as the
 * game's own thrown things do. Either way it falls by the pull of the dimension it flies in.
 *
 * @param damage        the harm it does to what it hits
 * @param gravity       how hard it falls each tick under the overworld's pull: an arrow's is 0.05, a snowball's 0.03,
 *                      a bolt's nothing; another dimension's pull scales it
 * @param pierce        how many things it passes through before it stops in one
 * @param knockback     how hard it pushes what it hits away
 * @param fireSeconds   how long what it hits burns; 0 for no fire
 * @param explosion     how big a blast it ends in, as TNT's is 4; 0 for none
 * @param breaksBlocks  whether its blast breaks blocks
 * @param lifetimeTicks how long it flies before it is gone, hit or not
 * @param mass          its mass, in kilograms; 0 to slow as the game's thrown things do
 * @param dragArea      how much the air holds it back: its drag coefficient times its cross-section, in square metres
 * @param penetration   the hardness of blocks it gets through in all: glass is 0.3, planks 2, stone 1.5; 0 for none
 */
public record ProjectileSpec(float damage, double gravity, int pierce, double knockback, float fireSeconds,
                             float explosion, boolean breaksBlocks, int lifetimeTicks, double mass, double dragArea,
                             float penetration) {

    public ProjectileSpec {
        /* Not-a-number and infinite values pass every "<" test, and would put the projectile nowhere at all. */
        if (!Float.isFinite(damage) || !Double.isFinite(gravity) || !Double.isFinite(knockback)
                || !Float.isFinite(fireSeconds) || !Float.isFinite(explosion) || !Double.isFinite(mass)
                || !Double.isFinite(dragArea) || !Float.isFinite(penetration)) {
            throw new IllegalArgumentException("a projectile's numbers must be finite");
        }
        if (damage < 0.0F || gravity < 0.0 || pierce < 0 || knockback < 0.0 || fireSeconds < 0.0F
                || explosion < 0.0F || lifetimeTicks < 1 || mass < 0.0 || dragArea < 0.0 || penetration < 0.0F) {
            throw new IllegalArgumentException("a projectile does no less than nothing, and flies a tick at least");
        }
    }

    /** A projectile that slows as the game's thrown things do and gets through no block. */
    public ProjectileSpec(final float damage, final double gravity, final int pierce, final double knockback,
                          final float fireSeconds, final float explosion, final boolean breaksBlocks,
                          final int lifetimeTicks) {
        this(damage, gravity, pierce, knockback, fireSeconds, explosion, breaksBlocks, lifetimeTicks, 0.0, 0.0, 0.0F);
    }

    /** The same projectile flying by real ballistics, of {@code kilograms} and {@code squareMetres} of drag area. */
    public ProjectileSpec withBallistics(final double kilograms, final double squareMetres) {
        return new ProjectileSpec(damage, gravity, pierce, knockback, fireSeconds, explosion, breaksBlocks,
                lifetimeTicks, kilograms, squareMetres, penetration);
    }

    /** The same projectile getting through blocks of {@code hardness} in all. */
    public ProjectileSpec withPenetration(final float hardness) {
        return new ProjectileSpec(damage, gravity, pierce, knockback, fireSeconds, explosion, breaksBlocks,
                lifetimeTicks, mass, dragArea, hardness);
    }

    /** Whether it flies by real ballistics. */
    public boolean ballistic() {
        return mass > 0.0;
    }
}
