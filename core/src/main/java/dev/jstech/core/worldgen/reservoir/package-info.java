/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Reservoirs for any mod: oil, gas and brines as physical things in the ground, each a lens of porous rock soaked in
 * its fluid under a dome of cap rock, laid by the world's generation where the world's seed says, drained by wells a
 * block at a time, and the same for every mod that reads them.
 *
 * <p>A mod declares a kind of reservoir once, with {@link Reservoirs}: {@code Reservoirs.reservoir(id)} with its fluid,
 * its soaked, drained and cap rocks, how much each block holds, its heights, frequency, size, capping and biomes, then
 * {@code declare}. The data generation writes it as a file under {@code data/<namespace>/jstech/reservoirs/}, read as a
 * {@link ReservoirType}, which a data pack can change, and a data pack can add kinds of its own there. Where each
 * reservoir lies is worked out from the world's seed in zones eight chunks a side, and what has been drawn from each is
 * kept in the dimension's data over areas ({@code Reservoirs.DRAWN}), so every mod reads the same blocks left:
 * {@code near} finds the reservoirs round a place, {@code at} what a block of one holds, {@code drain} takes from it,
 * and a block that holds no more turns back into plain rock. A reservoir lays no liquid and replaces only the rock it
 * says, never air, a liquid or another block, so digging into one floods nothing and caves stay as they were. The
 * world's generation lays them chunk by chunk, before the ores ({@link ReservoirFeature}), and {@link ReservoirPainter}
 * lays one anywhere else it is wanted.
 *
 * <p>How to declare and drain one, from zero, is in {@code core/docs/WORLD.md}. An example that compiles is the test
 * mod's brines in mud under a cap of smooth basalt ({@code tests/src/main/java/dev/jstech/tests/TestWorldGen.java}),
 * laid and drained by the Core's tests of reservoirs
 * ({@code tests/src/main/java/dev/jstech/tests/gametest/core/ReservoirGameTests.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.worldgen.reservoir;

import org.jetbrains.annotations.ApiStatus;
