/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.language;

/**
 * How much a machine's programs get to run in a tick, as one place every machine reads it from.
 *
 * <p>Two things decide it and they never mix. A machine's processors decide how many instructions it gets a tick,
 * at the fixed rate below; the times decide how long the server is willing to spend running them. A fast machine
 * keeps its pace over a slow one, and a busy server shortens every machine's tick without changing who is faster.
 * The times are design estimates that the server config overwrites when it loads and again on a reload; the rate
 * is not in the config.
 */
public final class ExecutionBalance {

    /** Megahertz of processor, cores times frequency over every processor, that buy one instruction a tick. */
    public static final int MEGAHERTZ_PER_INSTRUCTION = 8;

    /** The fewest instructions a machine with a processor gets a tick, so even the oldest one gets somewhere. */
    public static final int LEAST_INSTRUCTIONS_PER_TICK = 32;

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
