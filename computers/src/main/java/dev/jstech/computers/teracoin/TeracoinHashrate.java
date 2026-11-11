/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.teracoin;

import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.hardware.CpuSpec;
import dev.jstech.computers.hardware.GpuSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A computer's hashrate, in hashes a second, from the formulas the hardware already has
 * (computers/docs/design/teracoin.md, "Mining"): a processor mines at its capacity, a graphics card at its power times
 * ten. In a computer's own slots a card is cut by an older slot, as for everything else; on a Mining Computer's risers
 * it is not, as real mining is not. Pure.
 */
public final class TeracoinHashrate {

    /** What a graphics card's power is worth in hashes a second. */
    public static final long PER_POWER = 10L;

    private TeracoinHashrate() {
    }

    /** The hashrate of {@code build}, its cards cut by their slots unless {@code onRisers}. */
    public static long of(final ComputerBuild build, final boolean onRisers) {
        long total = 0L;
        for (final CpuSpec cpu : build.cpus()) {
            total += cpu.orchestrationCapacity();
        }
        for (final long card : cards(build, onRisers)) {
            total += card;
        }
        return total;
    }

    /** The processors' hashrate together. */
    public static long processors(final ComputerBuild build) {
        long total = 0L;
        for (final CpuSpec cpu : build.cpus()) {
            total += cpu.orchestrationCapacity();
        }
        return total;
    }

    /** Each graphics card's hashrate, in the order they sit. */
    public static List<Long> cards(final ComputerBuild build, final boolean onRisers) {
        final List<Long> out = new ArrayList<>();
        for (final GpuSpec gpu : build.gpus()) {
            final double cut = onRisers ? 1.0
                    : gpu.bus().bandwidthFactorIn(build.motherboard().slotFor(gpu.bus()));
            out.add(Math.round(gpu.power() * PER_POWER * cut));
        }
        return out;
    }

    /** {@code hashes} a second written as a miner shows it: 230 H/s, 3.31 kH/s, 1.90 MH/s. */
    public static String format(final long hashes) {
        if (hashes < 1_000L) {
            return hashes + " H/s";
        }
        if (hashes < 1_000_000L) {
            return String.format(Locale.ROOT, "%.1f kH/s", hashes / 1_000.0);
        }
        return String.format(Locale.ROOT, "%.2f MH/s", hashes / 1_000_000.0);
    }
}
