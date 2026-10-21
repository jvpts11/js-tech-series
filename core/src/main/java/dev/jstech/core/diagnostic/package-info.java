/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Diagnostics for any mod: sections of a system's work, timed, shown by name in profilers, watched for stalls and kept
 * to a budget by slowing themselves; what the server sends each player, minute by minute and tick by tick; what each
 * block costs the server; the state of every system for {@code /jstech health}; and what the Core writes into a crash
 * report and into the diagnostic file a player attaches to a bug report.
 *
 * <p>A system declares its work as a {@link Section}, once, as a constant, with its id and the {@link LogSystem} it
 * belongs to, and runs the work through it with {@code run} or {@code call}. Around the work the section times it for
 * the debug screen (F3), runs it in a frame named after it so a profiler that samples the threads shows it by name, and
 * lets the watchdog write down the stack of a run that goes on too long, once, and tell the operators. With a
 * {@code budget}, a section that takes more than that a tick slows itself to run one tick in two, then in four, and
 * speeds back up once well within it; the system asks {@code due} before its work and does {@code stride} ticks' worth
 * at once. A kit adds its part of the systems' state with {@link Health}, read on the server's thread, a part that
 * fails said as failed. Work timed by hand goes to {@link Diagnostics}; a kit with work of its own in one place adds it
 * to {@link CostMap} while a measure runs; and {@link Bandwidth} hands a mod what the server sent, by kind, by mod and
 * by block entity over the last minute ({@link BandwidthLedger}), and kind by kind over each of the last ticks
 * ({@link TickLedger}).
 *
 * <p>Every part is explained, from zero, in {@code core/docs/DIAGNOSTICS.md}. Examples that compile are the probes of
 * the Core's tests of diagnostics, a section with a budget and a part of the health among them
 * ({@code tests/src/main/java/dev/jstech/tests/gametest/core/DiagnosticsGameTests.java}), and the Core's energy grid, a
 * section of its own ({@code core/src/main/java/dev/jstech/core/energy/LevelEnergy.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.diagnostic;

import dev.jstech.core.log.LogSystem;
import org.jetbrains.annotations.ApiStatus;
