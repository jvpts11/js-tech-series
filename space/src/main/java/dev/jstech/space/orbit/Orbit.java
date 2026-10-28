/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.orbit;

/**
 * A world's path round its sun, taken as a circle at its real distance: how long it takes to go round follows from
 * that distance by Kepler's third law, the Earth's year being the game's week, so every planet keeps its real pace
 * against the others. Where it stands at a tick is where it began, turned by the share of its year gone by.
 *
 * @param semiMajorAxis how far it is from its sun, in astronomical units
 * @param startTurn     where it stands on its path as the world begins, in turns from 0 to 1
 */
public record Orbit(double semiMajorAxis, double startTurn) {

    public Orbit {
        if (!(semiMajorAxis > 0)) {
            throw new IllegalArgumentException("an orbit lies some distance from its sun; got " + semiMajorAxis);
        }
    }

    /** How long it takes to go round once, in ticks: the Earth's year times the distance to the power of 3/2. */
    public double periodTicks() {
        return OrbitScale.EARTH_YEAR_TICKS * Math.pow(this.semiMajorAxis, 1.5);
    }

    /** Where it stands at {@code tick}, as an angle round its sun in radians, from 0 to 2 pi. */
    public double angleAt(final long tick) {
        final double turns = this.startTurn + tick / periodTicks();
        return (turns - Math.floor(turns)) * 2 * Math.PI;
    }

    /** How far it is from its sun, in blocks of its system's space. */
    public double radiusBlocks() {
        return this.semiMajorAxis * OrbitScale.BLOCKS_PER_AU;
    }

    /** Where it stands at {@code tick} across its system's space from its sun, in blocks east. */
    public double xAt(final long tick) {
        return radiusBlocks() * Math.cos(angleAt(tick));
    }

    /** Where it stands at {@code tick} across its system's space from its sun, in blocks south. */
    public double zAt(final long tick) {
        return radiusBlocks() * Math.sin(angleAt(tick));
    }
}
