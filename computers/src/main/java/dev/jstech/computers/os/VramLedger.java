/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableNames;
import dev.jstech.core.tier.HardwareEra;
import java.util.ArrayList;
import java.util.List;

/**
 * The video memory of one computer: what its graphics memory is spent on, in kilobytes, and whether one more thing
 * fits. Pure arithmetic, like the RAM's ledger beside it.
 *
 * <p>Two things hold video memory. A lit monitor holds it by its size in blocks and by its era's colours: a CGA tube's
 * sixteen colours take four times a monochrome tube's memory, a panel of forty-eight Standard monitors forty-eight
 * times one. A graphics window (a paint program, a canvas, a game) holds a quarter of what a monitor block of its
 * machine's era does, and a whole block when it fills the screen. What the machine promised one thing it cannot
 * promise another: a monitor that does not fit stays dark until there is room, and a graphics window that does not
 * fit does not open.
 *
 * <p>The sizes are first estimates, to be tuned in play.
 */
public final class VramLedger {

    private final long totalKb;
    private final List<Entry> entries = new ArrayList<>();

    /** The kilobytes in a megabyte. */
    public static final long KB_PER_MB = 1024L;

    public VramLedger(final long totalKb) {
        this.totalKb = Math.max(0L, totalKb);
    }

    /** What an entry is: a lit monitor or a graphics window. */
    public enum Kind implements IStableName {
        MONITOR("monitor"),
        WINDOW("window");

        private static final StableNames<Kind> NAMES = StableNames.of(Kind.class);

        private final String serializedName;

        Kind(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return serializedName;
        }

        /** The kind a name stands for, or null for a name no kind declares. */
        public static Kind find(final String name) {
            return NAMES.find(name);
        }
    }

    /** One thing holding video memory: a label to show, the kilobytes it holds, and what it is. */
    public record Entry(String name, long kb, Kind kind) {
    }

    /** Records {@code kb} held by {@code name}; nothing is recorded for a zero or negative size. */
    public VramLedger add(final String name, final long kb, final Kind kind) {
        if (kb > 0L) {
            entries.add(new Entry(name, kb, kind));
        }
        return this;
    }

    public long totalKb() {
        return totalKb;
    }

    public long usedKb() {
        long sum = 0L;
        for (final Entry entry : entries) {
            sum += entry.kb();
        }
        return sum;
    }

    /** The kilobytes held by entries of one kind. */
    public long usedKb(final Kind kind) {
        long sum = 0L;
        for (final Entry entry : entries) {
            if (entry.kind() == kind) {
                sum += entry.kb();
            }
        }
        return sum;
    }

    public long freeKb() {
        return Math.max(0L, totalKb - usedKb());
    }

    /** Whether {@code kb} more would still fit. */
    public boolean fits(final long kb) {
        return kb <= totalKb - usedKb();
    }

    public List<Entry> entries() {
        return List.copyOf(entries);
    }

    /**
     * The kilobytes one block of a lit monitor of {@code era} holds: 64 for a monochrome tube of the Vintage, 256 for
     * its sixteen-colour one, then 4 MB, 16 MB, 64 MB and 256 MB from the Legacy to the Advanced, each era after
     * four times the one before.
     */
    public static long monitorKbPerBlock(final HardwareEra era, final boolean sixteenColours) {
        return switch (era) {
            case VINTAGE -> sixteenColours ? 256L : 64L;
            case LEGACY -> 4L * KB_PER_MB;
            case TRANSITION -> 16L * KB_PER_MB;
            case STANDARD -> 64L * KB_PER_MB;
            case ADVANCED -> 256L * KB_PER_MB;
            case EXA -> 1024L * KB_PER_MB;
            case SINGULARITY -> 4096L * KB_PER_MB;
        };
    }

    /**
     * The kilobytes a graphics window of a machine of {@code era} holds: a quarter of a colour monitor block of that
     * era, or a whole block while it fills the screen.
     */
    public static long windowKb(final HardwareEra era, final boolean fullScreen) {
        final long block = monitorKbPerBlock(era, true);
        return fullScreen ? block : block / 4L;
    }

    /** Kilobytes as a person reads them: KB while there are few, then MB, then GB with one decimal. */
    public static String label(final long kb) {
        if (kb < KB_PER_MB) {
            return kb + " KB";
        }
        if (kb < KB_PER_MB * KB_PER_MB) {
            return kb / KB_PER_MB + " MB";
        }
        final long tenths = kb * 10L / (KB_PER_MB * KB_PER_MB);
        return tenths / 10L + "." + tenths % 10L + " GB";
    }
}
