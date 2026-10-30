/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.plan;

/**
 * Where a robot arm on a rail is, each moment of laying one block: it runs along its rail to its feeder while it
 * reaches down into it, grips the block waiting there, swings it over to its place running on to the nearest point of
 * its rail, sets it down, and welds it; it waits there with its tool lifted, and after a while folds back up to rest
 * where it stands. The same on the server, which sets the block down once the swing is over, and on the players' games, which
 * draw it. Points are where the middle of the block it carries is, or would be. Pure, so it is tested without the game.
 */
public final class ArmMotion {

    /** How long each step takes, in ticks: reaching the feeder, gripping, swinging, welding; then resting, folding. */
    public static final int FETCH = 12;
    public static final int GRIP = 4;
    public static final int SWING = 20;
    public static final int WELD = 14;
    public static final int REST = 40;
    public static final int FOLD = 20;
    /** When the block is set down, and when the arm is free again, in ticks from the start of its work. */
    public static final int SET = FETCH + GRIP + SWING;
    public static final int END = SET + WELD;
    /** How high over its rail an arm's shoulder stands, and over the block it carries its wrist, in blocks. */
    public static final double SHOULDER = 0.75;
    public static final double WRIST = 0.65;
    /* How high a swing lifts the block over the straight line between its ends; how high the tool welds over it. */
    private static final double LIFT = 1.5;
    private static final double WELD_HIGH = 0.45;
    private static final double LIFTED = 0.6;
    /* How far the tool sways about the seam it welds, and how fast, in turns a tick. */
    private static final double SWAY = 0.18;
    private static final double SWAY_RATE = 0.11;
    private static final int AXES = 3;

    private ArmMotion() {
    }

    /**
     * Where the arm {@code rig} is {@code ticks} into laying a block at {@code place}, having begun {@code rail} along
     * its rail with its tool's block at {@code tool}.
     */
    public static State of(final Rig rig, final double rail, final double[] tool, final double[] place,
                           final double ticks) {
        final double atFeeder = rig.along(rig.feeder());
        final double atPlace = rig.along(place);
        if (ticks < FETCH) {
            final double s = ease(ticks / FETCH);
            return new State(lerp(rail, atFeeder, s), arc(tool, rig.feeder(), s), false, false);
        }
        if (ticks < FETCH + GRIP) {
            return new State(atFeeder, rig.feeder().clone(), ticks >= FETCH + GRIP / 2.0, false);
        }
        if (ticks < SET) {
            final double s = ease((ticks - FETCH - GRIP) / SWING);
            return new State(lerp(atFeeder, atPlace, s), arc(rig.feeder(), place, s), true, false);
        }
        if (ticks < END) {
            final double turn = (ticks - SET) * SWAY_RATE * 2 * Math.PI;
            return new State(atPlace, new double[] {place[0] + Math.cos(turn) * SWAY, place[1] + WELD_HIGH,
                place[2] + Math.sin(turn) * SWAY}, false, true);
        }
        final double[] lifted = {place[0], place[1] + LIFTED, place[2]};
        if (ticks < END + REST) {
            return new State(atPlace, lifted, false, false);
        }
        final double s = ease(Math.min(1, (ticks - END - REST) / FOLD));
        return new State(atPlace, lerp(lifted, rig.restAt(atPlace), s), false, false);
    }

    /** Where the arm is with no block yet laid: at rest by its feeder. */
    public static State idle(final Rig rig) {
        final double atFeeder = rig.along(rig.feeder());
        return new State(atFeeder, rig.restAt(atFeeder), false, false);
    }

    /* Slow away and slow in. */
    private static double ease(final double s) {
        final double t = Math.max(0, Math.min(1, s));
        return t * t * (3 - 2 * t);
    }

    private static double lerp(final double a, final double b, final double s) {
        return a + (b - a) * s;
    }

    private static double[] lerp(final double[] a, final double[] b, final double s) {
        final double[] out = new double[AXES];
        for (int axis = 0; axis < AXES; axis++) {
            out[axis] = lerp(a[axis], b[axis], s);
        }
        return out;
    }

    /* From {@code a} to {@code b}, lifted over the line between them most in the middle. */
    private static double[] arc(final double[] a, final double[] b, final double s) {
        final double[] out = lerp(a, b, s);
        out[1] += Math.sin(Math.PI * s) * LIFT;
        return out;
    }

    /**
     * An arm on its rail.
     *
     * @param from   one end of its rail, where its base stands at nought along it
     * @param to     the other end
     * @param feeder where the block waiting for it in its feeder stands
     * @param rest   where its tool's block stands at rest, from where its base stands
     * @param upper  its upper link's length, in blocks
     * @param fore   its forearm's length
     */
    public record Rig(double[] from, double[] to, double[] feeder, double[] rest, double upper, double fore) {

        /** How far along its rail, from nought to one, its base comes nearest {@code point}. */
        public double along(final double[] point) {
            double dot = 0;
            double length = 0;
            for (int axis = 0; axis < AXES; axis++) {
                final double way = this.to[axis] - this.from[axis];
                dot += (point[axis] - this.from[axis]) * way;
                length += way * way;
            }
            return length <= 0 ? 0 : Math.max(0, Math.min(1, dot / length));
        }

        /** Where its base stands {@code along} its rail. */
        public double[] base(final double along) {
            return ArmMotion.lerp(this.from, this.to, along);
        }

        /** Where its shoulder stands {@code along} its rail. */
        public double[] shoulder(final double along) {
            final double[] base = base(along);
            base[1] += SHOULDER;
            return base;
        }

        /** Where its tool's block stands at rest, its base {@code along} its rail. */
        public double[] restAt(final double along) {
            final double[] base = base(along);
            for (int axis = 0; axis < AXES; axis++) {
                base[axis] += this.rest[axis];
            }
            return base;
        }

        /** Whether, its base the nearest it comes, it reaches down to set a block at {@code place}. */
        public boolean reaches(final double[] place) {
            return ArmReach.reaches(shoulder(along(place)), new double[] {place[0], place[1] + WRIST, place[2]},
                    this.upper, this.fore);
        }
    }

    /**
     * Where an arm is.
     *
     * @param rail    how far along its rail its base stands, from nought to one
     * @param tool    where its tool's block stands, the block it carries or would
     * @param holding whether it holds the block
     * @param welding whether it welds the block it set down
     */
    public record State(double rail, double[] tool, boolean holding, boolean welding) {
    }
}
