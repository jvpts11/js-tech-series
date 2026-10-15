/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.diagnostic;

/**
 * What the server's settings say of diagnostics, set each time the world's settings are read: how long a section may
 * run before the watchdog writes it down, whether what the server sends each player is counted, and whether operators
 * are told in chat when a system stalls or slows itself.
 */
public final class DiagnosticBalance {

    /** How long a section may run before the watchdog writes it down, unless the settings say otherwise. */
    public static final int DEFAULT_STALL_MILLIS = 1000;

    private static final long NANOS_PER_MILLI = 1_000_000L;

    private static volatile long stallNanos = DEFAULT_STALL_MILLIS * NANOS_PER_MILLI;
    private static volatile boolean countNetwork = true;
    private static volatile boolean warnOperators = true;

    private DiagnosticBalance() {
    }

    /** How long a section may run before the watchdog writes it down, in nanoseconds. */
    public static long stallNanos() {
        return stallNanos;
    }

    public static void setStallMillis(final int value) {
        stallNanos = value * NANOS_PER_MILLI;
    }

    /** Whether what the server sends each player is counted. */
    public static boolean countNetwork() {
        return countNetwork;
    }

    public static void setCountNetwork(final boolean value) {
        countNetwork = value;
    }

    /** Whether operators are told in chat when a system stalls or slows itself. */
    public static boolean warnOperators() {
        return warnOperators;
    }

    public static void setWarnOperators(final boolean value) {
        warnOperators = value;
    }
}
