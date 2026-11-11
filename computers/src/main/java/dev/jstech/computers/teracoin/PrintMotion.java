/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.teracoin;

import java.util.List;

/**
 * How a 3D printer moves as it prints (computers/docs/design/teracoin.md, "The 3D Printer"), as its approved model
 * moves: the item printed a row of its texels at a time from the bottom, the nozzle travelling to each row's start and
 * sweeping along it, its way turning each row; then the item put out, the Prosa's bed sliding out from under it, the
 * Rattan's belt carrying it off. Every place is in sixteenths of a printer facing north, measured as the model's maker
 * measured them: u from the printer's front left foot (its east side), v up, w from its front. Pure.
 */
public final class PrintMotion {

    /** A texel of the printed item, in sixteenths: an item stands six sixteenths tall. */
    public static final double TEXEL = 0.375;
    /** The Prosa's print sheet's top, the item's foot. */
    public static final double PROSA_BED = 3.25;
    /** Where the Prosa's nozzle and the item stand, front to back. */
    public static final double PROSA_W = 8.0;
    /** The Rattan's nozzle's tip, which stays: its bed sinks under it. */
    public static final double RATTAN_TIP = 9.6;
    /** Where the item stands on the Rattan's belt, front to back. */
    public static final double RATTAN_W = 10.8;
    /** The Rattan's head over the item, front to back. */
    public static final double RATTAN_HEAD_W = RATTAN_W + 0.5;
    /** The Rattan's rollers' radius, and the step of its belt's marks: four to a roller's turn. */
    public static final double BELT_R = 0.45;
    public static final double BELT_P = 2 * Math.PI * BELT_R / 4;
    /** The belt's run for one item: two turns of its rollers. */
    public static final double BELT_RUN = 8 * BELT_P;
    /** How far above the row's top the Prosa's gantry stands. */
    public static final double PROSA_HEAD_LIFT = 1.8;
    /* The first part of each row the head spends travelling to its start. */
    private static final double TRAVEL = 0.18;
    /* The model's own clock: the print it was drawn over, and its put-out keys after it, in seconds. */
    private static final double PROSA_PRINT = 5.0;
    private static final double RATTAN_PRINT = 3.6;
    private static final double MIDDLE = 8.0;

    private PrintMotion() {
    }

    /**
     * A row of the printed item: {@code r}, its height from the sprite's foot in texels, and its texels from
     * {@code a} to {@code b}, columns from the sprite's left.
     */
    public record Row(int r, int a, int b) {
    }

    /**
     * Where the nozzle is while printing: the row it prints, how many texels of it are laid, the way it sweeps (1 or
     * -1), its place along the row (u) and the row's top over the item's foot, in sixteenths.
     *
     * @param row   the row printing, from 0
     * @param laid  how many of its texels from its start are down; -1 while the head still travels to it
     * @param sweep the way it sweeps: 1 left to right, -1 back
     * @param u     the nozzle along the row
     * @param top   the top of the row it prints, over the item's foot
     */
    public record Nozzle(int row, int laid, int sweep, double u, double top) {
    }

    /**
     * A pose of the Prosa: its gantry's height, its head along it, its bed (front negative), its spool's turn, and
     * the item: its rows down, the nozzle's place in the row printing, and, as it falls into the tray, its turn and
     * its shift; {@code gone} once it is in.
     */
    public record Prosa(double gantry, double head, double bed, double spool, double itemW, Nozzle nozzle,
                        boolean whole, double tip, double dropV, double dropW, boolean gone) {
    }

    /** A pose of the Rattan: its bed's top, its head along the row and front to back, its belt's run, the item. */
    public record Rattan(double bed, double head, double headW, double belt, double itemW, double itemFoot,
                         Nozzle nozzle, boolean whole, double tip, double dropV, double dropW, boolean gone) {
    }

    /** Where the item's first column stands along u, its sixteen columns on the bed's middle. */
    public static double itemU0() {
        return MIDDLE - 8 * TEXEL;
    }

    /** Where the nozzle is at {@code progress} (0 to 1) through printing {@code rows}, as {@link Nozzle} says. */
    public static Nozzle printing(final List<Row> rows, final double progress) {
        final int n = rows.size();
        final double t = Math.max(0.0, Math.min(0.999_999, progress)) * n;
        final int k = (int) t;
        final double f = t - k;
        final int sweep = k % 2 == 0 ? 1 : -1;
        final double[] span = span(rows.get(k));
        final double start = sweep > 0 ? span[0] : span[1];
        final double end = sweep > 0 ? span[1] : span[0];
        final double top = top(rows, k);
        if (f < TRAVEL) {
            final double prevU = k == 0 ? start : endOf(rows, k - 1);
            final double prevTop = k == 0 ? top : top(rows, k - 1);
            final double g = smooth(f / TRAVEL);
            return new Nozzle(k, -1, sweep, prevU + (start - prevU) * g, prevTop + (top - prevTop) * g);
        }
        final double g = (f - TRAVEL) / (1 - TRAVEL);
        final double u = start + (end - start) * g;
        return new Nozzle(k, laid(rows.get(k), u, sweep), sweep, u, top);
    }

    /**
     * The Prosa printing at {@code progress}, or putting the item out {@code ejected} seconds after it was done (a
     * negative {@code ejected} while it prints); with no rows it stands ready over the bed's middle.
     */
    public static Prosa prosa(final List<Row> rows, final double progress, final double ejected) {
        if (rows.isEmpty()) {
            return new Prosa(PROSA_BED + TEXEL + PROSA_HEAD_LIFT, MIDDLE, 0.0, 0.0, PROSA_W, null, false, 0, 0, 0,
                    true);
        }
        if (ejected < 0) {
            final Nozzle nozzle = printing(rows, progress);
            return new Prosa(PROSA_BED + nozzle.top() + PROSA_HEAD_LIFT, nozzle.u(), 0.0, 90.0 * progress, PROSA_W,
                    nozzle, false, 0, 0, 0, false);
        }
        final double t = PROSA_PRINT + ejected;
        final double first = PROSA_BED + top(rows, 0) + PROSA_HEAD_LIFT;
        final double last = PROSA_BED + top(rows, rows.size() - 1) + PROSA_HEAD_LIFT;
        final double gantry = keyed(t, new double[][] {{PROSA_PRINT, last}, {PROSA_PRINT + 0.3, last + 0.5},
                {5.9, last + 0.5}, {6.3, 4.85}, {6.9, 4.85}, {7.5, first}});
        final double head = keyed(t, new double[][] {{PROSA_PRINT, endOf(rows, rows.size() - 1)},
                {PROSA_PRINT + 0.3, MIDDLE}, {6.9, MIDDLE}, {7.5, span(rows.get(0))[0]}});
        final double bed = keyed(t, new double[][] {{PROSA_PRINT + 0.3, 0.0}, {5.9, -1.4}, {6.3, -1.4}, {6.9, 0.0}});
        final double contact = bedCrosses(-1.1875);
        final double tipAt = bedCrosses(-0.1875);
        final double w = t < contact ? PROSA_W + bed : PROSA_W - 1.1875;
        if (t < tipAt) {
            return new Prosa(gantry, head, bed, 90.0, w, null, true, 0, 0, 0, false);
        }
        final double tt = t - tipAt;
        final double slide = 6.8 * smooth((tt - 0.3) / 0.35);
        return new Prosa(gantry, head, bed, 90.0, w, null, true, 90.0 * smooth(tt / 0.2),
                -1.9 * smooth((tt - 0.16) / 0.14) - slide * 0.07, slide, tt > 0.7);
    }

    /**
     * The Rattan printing at {@code progress}, or putting the item out {@code ejected} seconds after it was done (a
     * negative {@code ejected} while it prints); with no rows it stands ready, its bed up under the nozzle.
     */
    public static Rattan rattan(final List<Row> rows, final double progress, final double ejected) {
        if (rows.isEmpty()) {
            return new Rattan(RATTAN_TIP - TEXEL, MIDDLE, RATTAN_HEAD_W, 0.0, RATTAN_W, RATTAN_TIP - TEXEL, null, false,
                    0, 0, 0, true);
        }
        if (ejected < 0) {
            final Nozzle nozzle = printing(rows, progress);
            final double bed = RATTAN_TIP - nozzle.top();
            return new Rattan(bed, nozzle.u(), RATTAN_HEAD_W, 0.0, RATTAN_W, bed, nozzle, false, 0, 0, 0, false);
        }
        final double t = RATTAN_PRINT + ejected;
        final double first = RATTAN_TIP - top(rows, 0);
        final double last = RATTAN_TIP - top(rows, rows.size() - 1);
        final double bed = keyed(t, new double[][] {{RATTAN_PRINT, last}, {5.0, last}, {5.6, first}});
        final double head = keyed(t, new double[][] {{RATTAN_PRINT, endOf(rows, rows.size() - 1)}, {3.9, 2.9},
                {5.0, 2.9}, {5.6, span(rows.get(0))[0]}});
        final double headW = keyed(t, new double[][] {{RATTAN_PRINT, RATTAN_HEAD_W}, {3.9, 13.4}, {5.0, 13.4},
                {5.6, RATTAN_HEAD_W}});
        final double belt = keyed(t, new double[][] {{3.9, 0.0}, {5.0, BELT_RUN}});
        final double travel = RATTAN_W - 7.0;
        if (belt < travel) {
            return new Rattan(bed, head, headW, belt, RATTAN_W - belt, bed, null, true, 0, 0, 0, false);
        }
        final double tt = t - beltCrosses(travel);
        final double slide = 6.5 * smooth((tt - 0.34) / 0.36);
        return new Rattan(bed, head, headW, belt, 7.0, bed, null, true, 90.0 * smooth(tt / 0.2),
                -(bed - 1.75) * smooth((tt - 0.16) / 0.16) - slide * 0.085, slide, tt > 0.75);
    }

    /** How many texels of row {@code row}, from its first, are down with the nozzle at {@code u} sweeping so. */
    public static int laid(final Row row, final double u, final int sweep) {
        final double u0 = itemU0();
        if (sweep > 0) {
            return Math.max(0, Math.min(row.b(), (int) Math.floor((u - u0) / TEXEL + 0.5)) - row.a());
        }
        return Math.max(0, row.b() - Math.max(row.a(), (int) Math.ceil((u - u0) / TEXEL - 0.5)));
    }

    /* A row's span along u. */
    private static double[] span(final Row row) {
        final double u0 = itemU0();
        return new double[] {u0 + row.a() * TEXEL, u0 + row.b() * TEXEL};
    }

    /* Where the nozzle leaves row {@code k}: its right end on a row swept to the right, its left otherwise. */
    private static double endOf(final List<Row> rows, final int k) {
        final double[] span = span(rows.get(k));
        return k % 2 == 0 ? span[1] : span[0];
    }

    /* The top of row {@code k} over the item's foot. */
    private static double top(final List<Row> rows, final int k) {
        return (rows.get(k).r() - rows.get(0).r() + 1) * TEXEL;
    }

    /* When the Prosa's bed, sliding back between 6.3 and 6.9 seconds, reaches {@code level}. */
    private static double bedCrosses(final double level) {
        return crosses(6.3, 6.9, -1.4, 0.0, level);
    }

    /* When the Rattan's belt, running between 3.9 and 5.0 seconds, has run {@code run}. */
    private static double beltCrosses(final double run) {
        return crosses(3.9, 5.0, 0.0, BELT_RUN, run);
    }

    /* When a value eased from {@code from} to {@code to} between {@code t0} and {@code t1} reaches {@code level}. */
    private static double crosses(final double t0, final double t1, final double from, final double to,
                                  final double level) {
        final int steps = 600;
        for (int i = 0; i <= steps; i++) {
            final double t = t0 + (t1 - t0) * i / steps;
            if (from + (to - from) * smooth((t - t0) / (t1 - t0)) >= level) {
                return t;
            }
        }
        return t1;
    }

    /** A value from (time, value) keys, eased between them. */
    public static double keyed(final double t, final double[][] keys) {
        if (t <= keys[0][0]) {
            return keys[0][1];
        }
        for (int i = 1; i < keys.length; i++) {
            if (t <= keys[i][0]) {
                final double ta = keys[i - 1][0];
                final double tb = keys[i][0];
                final double f = tb > ta ? smooth((t - ta) / (tb - ta)) : 1.0;
                return keys[i - 1][1] + (keys[i][1] - keys[i - 1][1]) * f;
            }
        }
        return keys[keys.length - 1][1];
    }

    /** Eases {@code x} from 0 to 1, clamped. */
    public static double smooth(final double x) {
        final double c = Math.max(0.0, Math.min(1.0, x));
        return c * c * (3 - 2 * c);
    }
}
