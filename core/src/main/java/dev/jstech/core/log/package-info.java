/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Logs by system: each part of a mod that writes to the log does it under a name of its own, {@code mod/system}, so a
 * line says which part of which mod wrote it, and how much each part writes can be changed while the game runs, from
 * code or with {@code /jstech log}, to follow a fault in one part without the rest filling the log.
 *
 * <p>A mod declares each of its systems once, as a constant, with {@link LogSystems}:
 * {@code LogSystems.declare(MODID, "machines")} gives a {@link LogSystem}, and the mod writes through its
 * {@code logger()}. {@code LogSystems.setLevel} sets one system's {@link LogLevel} ({@code ERROR}, {@code WARN},
 * {@code INFO}, {@code DEBUG} or {@code TRACE}), and a system at a level writes what is at that level and every level
 * above it; the level lasts until the game stops, as one a server's administrator sets with
 * {@code /jstech log <system> <level>} does. A system also names the work it owns elsewhere in the Core: a section of
 * diagnostics and a part of {@code /jstech health} are each declared with one. The Core's own systems are
 * {@link CoreLogs}.
 *
 * <p>How to declare a system and follow it in the game is in {@code core/docs/DEVELOPING.md}. Examples that compile are
 * the Core's tests of the log by system
 * ({@code tests/src/main/java/dev/jstech/tests/gametest/core/LogSystemGameTests.java}) and the system the tests of
 * diagnostics declare for their probes
 * ({@code tests/src/main/java/dev/jstech/tests/gametest/core/DiagnosticsGameTests.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.log;

import org.jetbrains.annotations.ApiStatus;
