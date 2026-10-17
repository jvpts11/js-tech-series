/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.acoustics;

/**
 * How a sound gets from where it is made to the listener: late, as far as it has to come at the speed of sound, so
 * the flash of a far explosion is seen before its boom arrives; and moving, so what races past drops in pitch as it
 * goes, from how fast it and the listener move.
 */
public final class SoundTravel {

    /** The least delay worth making, in ticks: less is not heard as late. */
    public static final int LEAST_DELAY = 2;
    /** The longest a sound is held back, in ticks: ten seconds, three and a half kilometres of air. */
    public static final int MOST_DELAY = 200;
    /** Faster than this, in metres a second, a move is a jump (a teleport, a ride starting), not a motion. */
    public static final double JUMP = 100.0;
    private static final int TICKS_A_SECOND = 20;

    private SoundTravel() {
    }

    /**
     * How many ticks a sound {@code metres} away takes to arrive at {@code speed} metres a second; none when it is too
     * near to be heard as late, or when nothing carries it.
     */
    public static int delayTicks(final double metres, final double speed) {
        if (speed <= 0 || metres <= 0) {
            return 0;
        }
        final long ticks = Math.round(metres / speed * TICKS_A_SECOND);
        return ticks < LEAST_DELAY ? 0 : (int) Math.min(MOST_DELAY, ticks);
    }

    /**
     * How fast something moved {@code dx, dy, dz} metres in {@code seconds}, in metres a second; standing still for a
     * move too fast to be one, so a jump does not shriek.
     */
    public static Motion velocity(final double dx, final double dy, final double dz, final double seconds) {
        if (seconds <= 0) {
            return Motion.STILL;
        }
        final Motion motion = new Motion(dx / seconds, dy / seconds, dz / seconds);
        return motion.speed() > JUMP ? Motion.STILL : motion;
    }

    /**
     * How much higher a sound is heard than it was made, from 1, as Doppler worked out: {@code towardsListener} is how
     * fast what makes it comes towards the listener, {@code towardsSource} how fast the listener goes towards it, in
     * metres a second, at {@code speed} metres a second through the medium.
     */
    public static double doppler(final double towardsListener, final double towardsSource, final double speed) {
        if (speed <= 0) {
            return 1;
        }
        // Nothing outruns its own sound here: what comes as fast as sound is heard at the highest the game plays.
        final double near = Math.min(towardsListener, speed * 0.95);
        return Math.max(0, (speed + towardsSource) / (speed - near));
    }

    /**
     * A velocity, in metres a second.
     *
     * @param x along the east
     * @param y up
     * @param z along the south
     */
    public record Motion(double x, double y, double z) {

        /** Not moving. */
        public static final Motion STILL = new Motion(0, 0, 0);

        /** How fast, in metres a second. */
        public double speed() {
            return Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
        }
    }
}
