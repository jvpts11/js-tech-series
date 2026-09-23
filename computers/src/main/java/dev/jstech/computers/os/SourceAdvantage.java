/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

/**
 * What building a program from source on the machine that runs it buys over installing a built package.
 *
 * <p>A package is built once, for every machine it might land on, so it carries what the least of them needs and
 * asks for what the most of them has. A program compiled on the machine is made for that one processor and nothing
 * else, and so it asks a little less of it in all three of the numbers a program asks for: the memory it holds while
 * it runs, the processor it needs at the least, and the disk it takes. The price is the build, which is paid in the
 * machine's own time, however long its processor takes over it.
 *
 * <p>A small edge on purpose, the same for every number and every system that builds what it installs. The figure
 * is a balancing estimate.
 *
 * <p>Pure: numbers in, numbers out.
 */
public final class SourceAdvantage {

    /** How much less a program built on the machine asks for, in percent. */
    public static final int PERCENT = 10;

    private SourceAdvantage() {
    }

    /**
     * That figure as a program built on the machine asks for it: {@link #PERCENT} less, to the nearest whole, so a
     * small figure that ten percent of does not reach one stays what it was rather than coming out as nothing.
     */
    public static int of(final int figure) {
        return (int) of((long) figure);
    }

    /** The same, for a figure too big for an int. */
    public static long of(final long figure) {
        if (figure <= 0L) {
            return figure;
        }
        return (figure * (100L - PERCENT) + 50L) / 100L;
    }

    /** That figure as the machine asks it of a program: less when it was built there, as it is otherwise. */
    public static int of(final int figure, final boolean builtHere) {
        return builtHere ? of(figure) : figure;
    }

    /** The same, for a figure too big for an int. */
    public static long of(final long figure, final boolean builtHere) {
        return builtHere ? of(figure) : figure;
    }
}
