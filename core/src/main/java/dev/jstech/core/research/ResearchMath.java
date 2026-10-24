/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.research;

/**
 * The arithmetic of research, with nothing of the game in it: how far a discovery goes on a number of points, and the
 * data it spends on the way. A discovery costs points, and of each kind of data it asks for, a count, each one covering
 * an equal share of the way: the first of eight is spent as the research starts, the second as it passes an eighth,
 * and so on, so a research goes only as far as the data it holds covers.
 */
public final class ResearchMath {

    /* How near a share may come to the edge of a part and still count as on it, against the rounding of many steps. */
    private static final double EDGE = 1e-9;

    private ResearchMath() {
    }

    /** How many of {@code count} data a research at {@code share} has spent: one for each part it has entered. */
    public static int spent(final double share, final int count) {
        if (share <= EDGE) {
            return 0;
        }
        return (int) Math.min(count, Math.ceil(share * count - EDGE));
    }

    /**
     * Where a research at {@code share} of a discovery costing {@code points} comes to on {@code worked} points more,
     * asking for {@code counts[k]} data of each kind and holding {@code held[k]} of each: the share it reaches, never
     * past the edge of the last part its data pays for, and how many of each kind it spends to get there.
     */
    public static Step advance(final double share, final double worked, final long points, final int[] counts,
                               final int[] held) {
        if (counts.length != held.length) {
            throw new IllegalArgumentException("a count for each kind of data, and as many held");
        }
        if (points < 1) {
            throw new IllegalArgumentException("a discovery costs at least a point; got " + points);
        }
        double reached = Math.min(1.0, share + Math.max(0.0, worked) / points);
        for (int k = 0; k < counts.length; k++) {
            final int before = spent(share, counts[k]);
            if (spent(reached, counts[k]) - before > held[k]) {
                reached = Math.min(reached, (double) (before + held[k]) / counts[k]);
            }
        }
        reached = Math.max(share, reached);
        final int[] spends = new int[counts.length];
        for (int k = 0; k < counts.length; k++) {
            spends[k] = spent(reached, counts[k]) - spent(share, counts[k]);
        }
        return new Step(reached, spends);
    }

    /**
     * What each of a group of research computers working together gives, in the order given: the strongest its whole
     * power (the first of them, when two are as strong), each other {@code eachOther} of its own, what coordinating
     * costs as in any real parallel computing.
     */
    public static double[] groupShares(final double[] powers, final double eachOther) {
        if (eachOther < 0 || eachOther > 1) {
            throw new IllegalArgumentException("each other computer gives a share from 0 to 1 of its power; got "
                    + eachOther);
        }
        int strongest = -1;
        for (int i = 0; i < powers.length; i++) {
            if (powers[i] < 0) {
                throw new IllegalArgumentException("a computer's power is never below none; got " + powers[i]);
            }
            if (strongest < 0 || powers[i] > powers[strongest]) {
                strongest = i;
            }
        }
        final double[] gives = new double[powers.length];
        for (int i = 0; i < powers.length; i++) {
            gives[i] = i == strongest ? powers[i] : powers[i] * eachOther;
        }
        return gives;
    }

    /** The power of a group of research computers working together: what each gives ({@link #groupShares}), summed. */
    public static double groupPower(final double[] powers, final double eachOther) {
        double sum = 0;
        for (final double gives : groupShares(powers, eachOther)) {
            sum += gives;
        }
        return sum;
    }

    /**
     * A step of research.
     *
     * @param share  the share of the discovery reached, from 0 to 1
     * @param spends how many of each kind of data were spent on the way, in the order they were asked for
     */
    public record Step(double share, int[] spends) {

        /** Whether the discovery is made: the whole of it reached. */
        public boolean made() {
            return this.share >= 1.0 - EDGE;
        }
    }
}
