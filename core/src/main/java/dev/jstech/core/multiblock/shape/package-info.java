/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * The shapes of formed structures' blocks, taken from their formed models: a block that merges into its structure's
 * model is collided with, outlined and aimed at as its part of the model is drawn, on the ground and aboard a moving
 * structure, in place of a whole block.
 *
 * <p>A mod declares a formed model formed from its structure's pattern, {@code ModelBuilder.formedFrom(pattern)}, and
 * the data run does the rest: it reads the model at rest ({@link ModelTriangles}), shares its faces out among the
 * blocks of the pattern ({@link MeshCells}), works out each block's boxes on a grid of sixteenths, in eighths for a
 * block that would have too many ({@link MeshVoxels}), and writes the model's shape file ({@link FormedShape}), which
 * the server reads, since it has no models, and sends to the players. The run stops when a block of the pattern has no
 * face of the model, or faces lie more than a block outside the pattern. As a structure merges, each of its blocks is
 * told which block of the model it is and how the structure stands ({@link FormedPlace}), and takes that block's shape,
 * turned ({@link CellSpace}); the shape is worked out once for each block and way of standing, and shared.
 *
 * <p>On a player's game, where the models are known, a line of sight that finds such a block is followed again against
 * the model's faces ({@link FormedAim}, {@link MeshPick}), and the block aimed at is outlined by the model's own edges,
 * its sharp edges and the cuts at the block's walls ({@link MeshOutline}), not by its boxes.
 *
 * <p>How to use it is in {@code core/docs/MULTIBLOCKS.md}, under "Shaped as the model". A working example is the test
 * turbine of the test mod ({@code tests/src/main/java/dev/jstech/tests/TestModels.java}), with its game test
 * ({@code tests/src/main/java/dev/jstech/tests/gametest/core/FormedShapeGameTests.java}) and the test of what a player
 * sees ({@code tests/src/main/java/dev/jstech/tests/clienttest/core/FormedShapeClientTests.java}); the Computational
 * Research Centre of J's Industrial is shaped the same way.
 */
@ApiStatus.Experimental
package dev.jstech.core.multiblock.shape;

import org.jetbrains.annotations.ApiStatus;
