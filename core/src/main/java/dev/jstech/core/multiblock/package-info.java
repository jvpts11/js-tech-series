/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Multiblocks for any mod: a machine built from many blocks placed in a shape, the shape drawn as a pattern or given
 * by a rule and matched against the world from its controller in each way it may stand, its ports opened onto pipes
 * and cables, and the whole shown before it is built.
 *
 * <p>A shape is an {@link IStructureShape}. A {@link MultiblockPattern} draws it in letters, layer by layer from
 * the bottom, each letter a {@link Place}: what fits there ({@link BlockMatch}, blocks and tags as data), what a
 * block there counts and what number it gives, the least and the most of each count being a {@link Limit}. It is
 * found in its four horizontal turns, and mirrored or lying on its side when it says so ({@link Orientation}). A
 * {@link RuleShape} is a shape of whatever size the player built within its limits: a box, a floor with walls
 * round it, a cuboid of units, its inside read by an {@link InsideRule}. A mod declares its patterns with
 * {@link MultiblockPatterns}, which writes each to a data file a data pack may change, and checking a shape gives a
 * {@link ShapeMatch}: formed, with what it is made of and gives, or the first place that is wrong. A machine of the
 * Core's kit becomes a controller with {@link MachineStructure}, checked again only when a block changes in the box it
 * watches ({@link LevelStructures}), never every tick; a block that raises a fixed footprint as it is placed extends
 * {@link AbstractMultiblockControllerBlock}. Ports are the places a pattern marks with a {@link PortKind}, or port
 * blocks the player puts anywhere on the shell ({@link MultiblockPortBlock}, {@link RedstonePortBlock}), through which
 * {@link MultiblockPorts} hands pipes and cables what the controller holds. {@link FormedParts} merges a formed shell
 * into its controller's model, and {@link ShapePreview} lays a shape out to be shown before it is built.
 *
 * <p>How to build one, from zero, is in {@code core/docs/MULTIBLOCKS.md}. Examples that compile are the test mod's
 * furnace, a pattern with four ports ({@code tests/src/main/java/dev/jstech/tests/TestMultiblocks.java}), and J's
 * Computers' Mainframe, whose controller raises its footprint as it is placed
 * ({@code computers/src/main/java/dev/jstech/computers/block/MainframeBlock.java}), its pattern in
 * {@code computers/src/main/java/dev/jstech/computers/block/MainframeStructure.java}.
 */
@ApiStatus.Experimental
package dev.jstech.core.multiblock;

import dev.jstech.core.machine.MachineStructure;
import org.jetbrains.annotations.ApiStatus;
