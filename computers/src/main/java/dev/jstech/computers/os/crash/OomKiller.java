/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.crash;

import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.RamLedger;
import java.util.ArrayList;
import java.util.List;

/**
 * What a kernel with an out-of-memory killer ends when its memory runs out: the thing holding the most, then the next,
 * until what is asked for fits.
 *
 * <p>Only what the player started can be ended: a window, a program, a service. The system's own share, its desktop
 * and the memory lent to graphics are the kernel's and stay. When everything that could be ended has been and the
 * memory still does not fit, there is nothing left to kill, and the kernel panics.
 *
 * <p>Of two things holding the same, the one started later goes first, as the real killer, walking its processes in the
 * order they were started, keeps the last of equals it finds.
 */
public final class OomKiller {

    private OomKiller() {
    }

    /**
     * What the killer does to make room for {@code needMb} more in {@code ledger}: the entries it ends, in the order it
     * ends them, and whether that made room. A ledger that already has the room ends nothing.
     */
    public record Plan(List<RamLedger.Entry> victims, boolean fits) {

        public Plan {
            victims = List.copyOf(victims);
        }
    }

    /** The plan for {@code needMb} more in {@code ledger}; zero asks only that what is held already fits. */
    public static Plan plan(final RamLedger ledger, final int needMb) {
        long over = (long) ledger.usedMb() + Math.max(0, needMb) - ledger.totalMb();
        final List<RamLedger.Entry> left = new ArrayList<>();
        for (final RamLedger.Entry entry : ledger.entries()) {
            if (killable(entry.kind())) {
                left.add(entry);
            }
        }
        final List<RamLedger.Entry> victims = new ArrayList<>();
        while (over > 0 && !left.isEmpty()) {
            int heaviest = 0;
            for (int i = 1; i < left.size(); i++) {
                if (left.get(i).mb() >= left.get(heaviest).mb()) {
                    heaviest = i;
                }
            }
            final RamLedger.Entry victim = left.remove(heaviest);
            victims.add(victim);
            over -= victim.mb();
        }
        return new Plan(victims, over <= 0);
    }

    /**
     * Whether the kernel of a {@code platform} system has the killer: the Unix kernels do; Frames goes down instead,
     * and MC-DOS and MC-NET, which run one thing at a time, refuse what does not fit.
     */
    public static boolean runsOn(final Platform platform) {
        return switch (platform) {
            case LINUX, FREEBSD, UNIX -> true;
            case FRAMES, MC_DOS, MC_NET -> false;
        };
    }

    /** Whether the killer may end an entry of that kind: only what was started on top of the system. */
    public static boolean killable(final RamLedger.Kind kind) {
        return switch (kind) {
            case WINDOW, PROCESS, SERVICE -> true;
            case SYSTEM, DESKTOP, GRAPHICS -> false;
        };
    }
}
