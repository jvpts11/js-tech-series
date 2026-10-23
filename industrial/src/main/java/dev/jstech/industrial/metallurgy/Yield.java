/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.metallurgy;

/**
 * How many of a form come of how many of what it is made from: one ingot into two wires.
 *
 * <p>Pure: no game types.
 *
 * @param in  how many it takes
 * @param out how many it makes
 */
public record Yield(int in, int out) {

    public Yield {
        if (in < 1 || out < 1) {
            throw new IllegalArgumentException("a yield takes and makes one at least, not " + in + " into " + out);
        }
    }

    /**
     * What the same work yields by hand, which always yields less, so the start of the game never waits on a machine
     * and a machine is always worth having: half of what a machine makes when it makes more than it takes, two of
     * three ingots when it makes one of one; the smallest numbers that say it.
     */
    public Yield byHand() {
        if (this.out >= 2 * this.in) {
            return reduced(this.in, this.out / 2);
        }
        return reduced(3 * this.in, 2 * this.out);
    }

    private static Yield reduced(final int in, final int out) {
        int a = in;
        int b = out;
        while (b != 0) {
            final int rest = a % b;
            a = b;
            b = rest;
        }
        return new Yield(in / a, out / a);
    }
}
