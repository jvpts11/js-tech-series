/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Grids: cables joined into runs and connected parts, one grid of each kind in each dimension shared by every line of
 * that kind, with the slowest cable on a way and each run's length against its range worked out when the grid changes,
 * never on every tick.
 *
 * <p>Each kind of grid is a {@link GridKind}: one of the Core's (power, fluid, heat, motion, data, peripheral, items
 * and each other state of matter) or a mod's own, declared with an {@link IGridPolicy}, how what the grid carries is
 * shared along a run and what its wires plug into. A mod takes one of {@link GridPolicies} (energy, fluids, matter,
 * items, heat, groups, the data network, or nothing but joining) or gives its own, whose {@link IGridMover} shares
 * what the grid carries in one dimension each tick, after the machines have ticked. A cable or a device stands in
 * a {@link Grid} as a {@link GridMember} at a {@link GridPlace}, put in and taken out through {@link CoreGrids};
 * two cables join only when they are of one line and generation and their colours agree, and only data carries
 * the identity of a network along its grid. A line that routes what it carries shares it among its ends as
 * {@link EndRouter} says, each end by its {@link EndMode}; a line that carries nothing groups the machines each run of
 * it joins ({@link LineGroups}); and what a part of a line did in the last tick is read as a {@link LineSurvey}.
 *
 * <p>How to declare a grid of a mod's own, and lines that group, is in {@code core/docs/CABLES.md}. Examples
 * that compile are J's Example's energy cable and fluid pipe, on the Core's power and fluid grids
 * ({@code example/src/main/java/dev/jstech/example/ExampleContent.java}), and the test mod's grids of its own
 * ({@code tests/src/main/java/dev/jstech/tests/TestCableTypes.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.grid;

import org.jetbrains.annotations.ApiStatus;
