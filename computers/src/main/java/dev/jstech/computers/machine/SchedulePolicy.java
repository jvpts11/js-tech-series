/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.sigma.SystemFacts;
import dev.jstech.core.schedule.TickScheduler;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;

/**
 * How a machine hands its instruction credits out to its programs (computers/docs/design/player-systems.md): as the
 * mod's systems do, in slices of 64 with high run like medium; or as the kernel a player made says: to the program in
 * front alone, to each in turn until it yields, or in slices of its own size with a high program getting two a round,
 * and with the system itself choosing which program has each slice when it answers {@code OnSchedule}.
 *
 * @param kind      how the credits are handed out
 * @param slice     the most one program runs before the next, in slices
 * @param highTwice whether a high program gets two slices a round
 * @param chooses   whether the system chooses which program has each slice
 */
public record SchedulePolicy(Kind kind, int slice, boolean highTwice, boolean chooses) {

    /** As the mod's own systems hand out their credits. */
    public static final SchedulePolicy MOD = new SchedulePolicy(Kind.PREEMPTIVE, TickScheduler.QUANTUM, false, false);
    /** What a system answers to choose which program has the next slice, handed the numbers of those ready to run. */
    public static final String ON_SCHEDULE = "OnSchedule";

    /** As that kernel says, or as the mod's systems do when it is one of the mod's kernels or there is none. */
    public static SchedulePolicy of(@Nullable final SystemFacts.Kernel kernel, final boolean chooses) {
        if (kernel == null || !kernel.own()) {
            return MOD;
        }
        final int slice = Math.clamp(kernel.slice(), SystemFacts.LEAST_SLICE, SystemFacts.MOST_SLICE);
        return new SchedulePolicy(Kind.named(kernel.scheduler()), slice, true, chooses);
    }

    /** How a kernel hands out the credits. */
    public enum Kind {
        /** Only the program in front, and the system itself, run; the others wait, as on DOS. */
        NONE,
        /** A program runs until it yields, waits or ends, as on Frames 3.1. */
        COOPERATIVE,
        /** The programs take slices in turn. */
        PREEMPTIVE;

        /** The kind a kernel names in words; preemptive for any other word, which the compiler never lets through. */
        static Kind named(final String word) {
            return switch (word.toLowerCase(Locale.ROOT)) {
                case "none" -> NONE;
                case "cooperative" -> COOPERATIVE;
                default -> PREEMPTIVE;
            };
        }
    }
}
