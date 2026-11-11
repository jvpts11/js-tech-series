/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.media;

import java.util.Locale;
import org.jetbrains.annotations.Nullable;

/**
 * How the size of a removable medium reads: what its box says it holds, its exact bytes, and what is left on it.
 *
 * <p>The box says what the maker printed, which is not one rule: a floppy disk's 1.44 MB are 1,440 KB of 1,024 bytes,
 * a CD's 700 MB are megabytes of 1,048,576, and a DVD's and a stick's gigabytes are of a thousand million. What is left
 * on a medium is counted in units of 1,024, as a system counts it. Free of the game, so it is tested without it.
 */
public final class MediaSizes {

    private static final long KB = 1024L;
    private static final long MB = KB * KB;
    private static final long GB = MB * KB;
    /* The gigabyte a stick's box counts in. */
    private static final long DECIMAL_GB = 1_000_000_000L;
    /* Below this many of a unit a label keeps a decimal: "4.1 GB", then "25 GB". */
    private static final double ONE_DECIMAL_BELOW = 10.0;

    private MediaSizes() {
    }

    /** What the box of a medium of {@code format} says it holds, or a stick's model's box: "1.44 MB", "16 GB". */
    public static String soldAs(final MediaFormat format, @Nullable final UsbStick stick) {
        if (stick != null) {
            return stick.capacityBytes() / DECIMAL_GB + " GB";
        }
        return switch (format) {
            case FLOPPY -> "1.44 MB";
            case CD -> "700 MB";
            case DVD -> "4.7 GB";
            case USB -> UsbStick.POCKET_32G.capacityBytes() / DECIMAL_GB + " GB";
            case BLU_RAY -> "25 GB";
        };
    }

    /** {@code bytes} with its thousands grouped, as a tooltip writes an exact size: "1,474,560". */
    public static String grouped(final long bytes) {
        return String.format(Locale.ROOT, "%,d", bytes);
    }

    /** A size in bytes as a short label in units of 1,024, with a decimal below ten: "0.5 KB", "1.2 MB", "512 MB". */
    public static String label(final long bytes) {
        final long size = Math.max(0L, bytes);
        if (size >= GB) {
            return amount(size, GB) + " GB";
        }
        if (size >= MB) {
            return amount(size, MB) + " MB";
        }
        return amount(size, KB) + " KB";
    }

    /* How many of {@code unit} {@code bytes} are, rounded down so a label never promises room that is not there. */
    private static String amount(final long bytes, final long unit) {
        final double value = (double) bytes / unit;
        if (value >= ONE_DECIMAL_BELOW) {
            return Long.toString(bytes / unit);
        }
        final double tenths = Math.floor(value * 10.0) / 10.0;
        return tenths == Math.rint(tenths) ? Long.toString((long) tenths) : String.format(Locale.ROOT, "%.1f", tenths);
    }
}
