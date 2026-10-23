/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.formula;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The formulas of fluids: what a tank holds, a liquid by its inner volume and a gas by its volume times its pressure;
 * what a run of pipes carries; the oil in reservoir rock; the air's shares as an air separation plant takes them
 * apart. A block is one cubic metre, and 1,000 mB are one cubic metre (industrial/docs/design/formulas.md).
 *
 * <p>Pure: no game types.
 */
public final class FluidFormulas {

    /** The millibuckets in a block, a cubic metre. */
    public static final long PER_BLOCK = 1_000L;
    /** The oil a block of reservoir rock holds, in mB: real reservoir rock is about a fifth pores. */
    public static final long OIL_PER_BLOCK = 200L;
    /** How many times its real share each of the air's rare gases is counted at. */
    public static final double RARE_GASES = 1_000.0;

    /* The air's shares by volume, the four main gases and then the four rare ones, as the Earth's air holds them. */
    private static final String[] GASES = {"nitrogen", "oxygen", "argon", "carbon_dioxide", "neon", "helium",
        "krypton", "xenon"};
    private static final double[] SHARES = {0.780_8, 0.209_5, 0.009_3, 0.000_4, 18.18e-6, 5.24e-6, 1.14e-6,
        0.087e-6};
    private static final int FIRST_RARE = 4;

    private FluidFormulas() {
    }

    /** The inner blocks of a tank of {@code width} × {@code height} × {@code depth} blocks, walls included. */
    public static long innerBlocks(final int width, final int height, final int depth) {
        return (long) Math.max(0, width - 2) * Math.max(0, height - 2) * Math.max(0, depth - 2);
    }

    /** What a liquid tank of {@code innerBlocks} holds, in mB. */
    public static long liquidTank(final long innerBlocks) {
        return Math.multiplyExact(Math.max(0L, innerBlocks), PER_BLOCK);
    }

    /** What a gas tank of {@code innerBlocks} holds at {@code bar}, in mB: its volume times its pressure. */
    public static long gasTank(final long innerBlocks, final double bar) {
        return (long) Math.floor(liquidTank(innerBlocks) * Math.max(0.0, bar));
    }

    /** What a run of pipes carries a tick: its worst pipe's top flow, with no loss and no limit by length. */
    public static long run(final long... topFlows) {
        long worst = Long.MAX_VALUE;
        for (final long flow : topFlows) {
            worst = Math.min(worst, flow);
        }
        return topFlows.length == 0 ? 0L : Math.max(0L, worst);
    }

    /** The oil {@code blocks} of reservoir rock hold, in mB. */
    public static long oil(final long blocks) {
        return Math.multiplyExact(Math.max(0L, blocks), OIL_PER_BLOCK);
    }

    /**
     * The share of each gas an air separation plant takes out of the air, by the gas's material's id's path: the main
     * four at their real shares and the rare gases at a thousand times theirs, all of them together the whole, so
     * nothing is made of nothing.
     */
    public static Map<String, Double> airShares() {
        final double[] counted = new double[SHARES.length];
        double total = 0.0;
        for (int i = 0; i < SHARES.length; i++) {
            counted[i] = i >= FIRST_RARE ? SHARES[i] * RARE_GASES : SHARES[i];
            total += counted[i];
        }
        final Map<String, Double> out = new LinkedHashMap<>();
        for (int i = 0; i < GASES.length; i++) {
            out.put(GASES[i], counted[i] / total);
        }
        return Collections.unmodifiableMap(out);
    }
}
