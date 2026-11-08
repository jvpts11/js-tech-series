/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.computers.hardware.PsuCondition;
import dev.jstech.computers.hardware.PsuConditions;
import dev.jstech.computers.hardware.PsuSpec;
import dev.jstech.computers.hardware.PsuWear;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The wear a running machine puts on its power supply, written onto the supply's item: the time it has been on, worked
 * out once a game minute and when it stops ({@link Clock}), never each tick. A supply's own life is drawn the first
 * time it wears. What it came to says whether the supply just degraded, lost a unit or failed, for the machine to warn
 * of or to go dark.
 */
final class PsuWearing {

    /* A machine over its thermal budget halves its supply's life. */
    private static final double HOT = 0.5;

    private PsuWearing() {
    }

    /**
     * Wears the supply {@code psu} by {@code ticks} on giving {@code partsWatts}, {@code hot} when the machine runs
     * over its thermal budget; null, with nothing written, when it is no supply or the server lets supplies last
     * forever.
     */
    @Nullable
    static Outcome wear(final ItemStack psu, final int partsWatts, final long ticks, final boolean hot,
                        final RandomSource random) {
        final PsuSpec spec = PsuConditions.spec(psu);
        if (spec == null || ticks <= 0L || !ComputersServerConfig.psuFailures()) {
            return null;
        }
        final PsuCondition before = PsuConditions.of(psu).withLife(PsuWear.drawLife(random.nextDouble()));
        final double multiplier = ComputersServerConfig.psuLifeMultiplier() * (hot ? HOT : 1.0);
        final PsuCondition after = PsuWear.worn(before, spec, partsWatts, ticks, multiplier);
        PsuConditions.set(psu, after);
        return new Outcome(spec, before, after);
    }

    /** What a supply's wear came to: its specification, and its condition before and after. */
    record Outcome(PsuSpec spec, PsuCondition before, PsuCondition after) {

        /** Whether the last of its units failed just now: the machine goes dark. */
        boolean failedNow() {
            return after.failed(spec) && !before.failed(spec);
        }

        /** Whether a redundant supply lost a unit just now and the others carry on. */
        boolean unitLost() {
            return !after.failed(spec) && after.failedUnits(spec) > before.failedUnits(spec);
        }

        /** Whether a working unit crossed into degraded just now. */
        boolean degradedNow() {
            return !after.failed(spec) && after.worst(spec) >= PsuWear.DEGRADED
                    && before.worst(spec) < PsuWear.DEGRADED;
        }
    }

    /**
     * When a running machine's supply is due to wear: once a game minute while it runs, and once more when it stops,
     * for the time since. Nothing is kept: a machine unloaded while it runs starts counting again when it is back.
     */
    static final class Clock {

        private long since = STOPPED;

        private static final long STOPPED = -1L;
        private static final long A_MINUTE = 1_200L;

        /** The ticks on to wear for at {@code now}, the machine {@code running}; 0 while nothing is due. */
        long due(final long now, final boolean running) {
            if (!running) {
                final long ticks = since == STOPPED ? 0L : now - since;
                since = STOPPED;
                return Math.max(0L, ticks);
            }
            if (since == STOPPED) {
                since = now;
                return 0L;
            }
            if (now - since < A_MINUTE) {
                return 0L;
            }
            final long ticks = now - since;
            since = now;
            return ticks;
        }

        /** Counts from {@code now} again, as when the parts change under a running machine. */
        void restart(final long now) {
            if (since != STOPPED) {
                since = now;
            }
        }
    }
}
