/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Ores in veins for any mod: the world cut into zones three chunks a side, each holding at most one vein, dense at its
 * middle and thinning to nothing at its edge, its kind picked by the kinds' frequencies and its place worked out from
 * the world's seed alone; each block of a vein the ore of one of its materials in the rock it was; and what an
 * instrument can learn of the veins round it.
 *
 * <p>A mod declares a kind of vein, a deposit type, once, with {@link Deposits}: {@code Deposits.deposit(id)} with the
 * materials it brings together and their weights, its heights, frequency, size, density, biomes and the rocks it
 * crosses, then {@code declare}. The data generation writes it as a file under
 * {@code data/<namespace>/jstech/deposits/}, read as a {@link DepositType}, which a data pack can change or add to. A
 * vein lays only the ores some installed mod uses in that rock ({@link OreTable}), so a mod may declare a kind for
 * materials it does not use itself. While ores lie in veins the game's scattered iron, copper and gold are not laid,
 * and the server can try moving other mods' scattered ores into veins of their own ({@link MovedOres}); the server's
 * balance file says how often, how large and how dense each kind is ({@link VeinBalance}). An instrument asks
 * {@link Prospecting}: {@code prospect} gives the direction of each vein near and how strongly it shows, never where it
 * is, and {@code sample}, standing in one, what it is made of and how large it is; both answer from the seed, so a vein
 * mined out still shows. {@link VeinField} is where the veins lie, worked out without the game, and {@link VeinPainter}
 * lays one anywhere it is wanted.
 *
 * <p>How to declare a kind and prospect, from zero, is in {@code core/docs/WORLD.md}. Examples that compile are the
 * test mod's kinds of vein ({@code tests/src/main/java/dev/jstech/tests/TestWorldGen.java}), found by the Core's tests
 * of veins ({@code tests/src/main/java/dev/jstech/tests/gametest/core/VeinGameTests.java}), and the Core's own veins of
 * iron, copper and gold ({@code core/src/main/java/dev/jstech/core/jstechseries/SeriesDeposits.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.worldgen.vein;

import org.jetbrains.annotations.ApiStatus;
