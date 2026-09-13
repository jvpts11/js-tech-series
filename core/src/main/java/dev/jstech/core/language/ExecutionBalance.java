/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.language;

/**
 * How much real time the server lends to running programs, as one place every machine reads it from.
 *
 * <p>A machine's processors decide how many instructions it gets a tick; these decide how long the
 * server is willing to spend running them. The two never mix: a fast machine keeps its pace over a slow
 * one, and a busy server shortens every machine's tick without changing who is faster. The defaults are
 * design estimates; the server config overwrites them when it loads and again on a reload.
 */
public final class ExecutionBalance {

    /** Real time one machine may spend running its programs in a tick, in microseconds. */
    public static final int DEFAULT_MACHINE_MICROS = 1000;

    /** Real time every machine together may spend running programs in a tick, in microseconds. */
    public static final int DEFAULT_SERVER_MICROS = 8000;

    private static final long NANOS_PER_MICRO = 1000L;

    private static volatile long machineNanos = DEFAULT_MACHINE_MICROS * NANOS_PER_MICRO;
    private static volatile long serverNanos = DEFAULT_SERVER_MICROS * NANOS_PER_MICRO;

    private ExecutionBalance() {
    }

    /** How long one machine may run programs in a tick, in nanoseconds. */
    public static long machineNanos() {
        return machineNanos;
    }

    public static void setMachineMicros(final int micros) {
        machineNanos = Math.max(0, micros) * NANOS_PER_MICRO;
    }

    /** How long every machine together may run programs in a tick, in nanoseconds. */
    public static long serverNanos() {
        return serverNanos;
    }

    public static void setServerMicros(final int micros) {
        serverNanos = Math.max(0, micros) * NANOS_PER_MICRO;
    }

    /** Back to the design estimates, for tests. */
    public static void reset() {
        setMachineMicros(DEFAULT_MACHINE_MICROS);
        setServerMicros(DEFAULT_SERVER_MICROS);
    }
}
