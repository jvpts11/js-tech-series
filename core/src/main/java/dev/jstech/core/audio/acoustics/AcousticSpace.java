/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.acoustics;

/**
 * What a place sounds like, from its walls: how far a sound goes before it meets one, how much of the space round the
 * listener is walled in, how much of a sound the walls drink, and, in a sealed room, its exact size. Its echo follows
 * from these as a hall's does, by Eyring's formula: the bigger the space and the harder its walls, the longer it rings.
 *
 * @param meanFreePath how far a sound goes, on average, before it meets a wall, in metres
 * @param enclosure    the share of directions in which a wall stands within reach: 0 in the open air, 1 walled all
 *                     round
 * @param absorption   the share of a sound the walls drink, on average: near 0 for stone and metal, near 1 for wool
 * @param volume       a sealed room's volume in cubic metres, or 0 for a space known only by its walls
 * @param surface      a sealed room's walls, in square metres, or 0
 */
public record AcousticSpace(double meanFreePath, double enclosure, double absorption, double volume,
                            double surface) {

    /** The open air: nothing near to answer. */
    public static final AcousticSpace OPEN = new AcousticSpace(0, 0, 0, 0, 0);
    /* Sabine's constant: the reverberation time of a space, in seconds, per metre of its volume over its walls. */
    private static final double SABINE = 0.161;
    /* The speed of sound in the air, in metres a second. */
    private static final double SPEED = 343.0;
    /* The least and the most a wall drinks: no wall reflects everything, and none drinks all. */
    private static final double LEAST_DRUNK = 0.01;
    private static final double MOST_DRUNK = 0.99;
    /* The longest the first echoes and the tail can wait, as the sound card's reverberation allows. */
    private static final double MOST_REFLECTIONS_DELAY = 0.3;
    private static final double MOST_LATE_DELAY = 0.1;
    /* How little of its high notes the echo of the softest walls keeps. */
    private static final double LEAST_HIGHS = 0.1;

    public AcousticSpace {
        if (meanFreePath < 0 || enclosure < 0 || enclosure > 1 || absorption < 0 || absorption > 1 || volume < 0
                || surface < 0) {
            throw new IllegalArgumentException("not a space: path " + meanFreePath + ", enclosure " + enclosure
                    + ", absorption " + absorption + ", volume " + volume + ", surface " + surface);
        }
    }

    /** The same walls, as a sealed room of {@code cubicMetres} walled all round by {@code squareMetres}. */
    public AcousticSpace inRoom(final double cubicMetres, final double squareMetres) {
        return new AcousticSpace(this.meanFreePath, 1, this.absorption, cubicMetres, squareMetres);
    }

    /** Whether it is a sealed room, whose size is known exactly. */
    public boolean sealed() {
        return this.volume > 0 && this.surface > 0;
    }

    /** How it answers a sound: nothing in the open air, a long ring in a big hall of stone. */
    public Reverb reverb() {
        if (this.enclosure <= 0) {
            return Reverb.DRY;
        }
        final double drunk = Math.clamp(this.absorption, LEAST_DRUNK, MOST_DRUNK);
        // A sealed room's volume over its walls is known; elsewhere it is a quarter of the way a sound goes.
        final double volumeOverWalls = sealed() ? this.volume / this.surface : this.meanFreePath / 4;
        final double decay = Math.clamp(SABINE * volumeOverWalls / -Math.log(1 - drunk), Reverb.SHORTEST,
                Reverb.LONGEST);
        final double path = 4 * volumeOverWalls;
        // Only walled directions answer, and the softer the walls, the fainter what comes back.
        final double wet = Math.clamp(this.enclosure * this.enclosure * (1 - drunk / 2), 0, 1);
        return new Reverb(decay, wet, Math.min(MOST_REFLECTIONS_DELAY, path / SPEED),
                Math.min(MOST_LATE_DELAY, 2 * path / SPEED), Math.clamp(1 - drunk, LEAST_HIGHS, 1));
    }
}
