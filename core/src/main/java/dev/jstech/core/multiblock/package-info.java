/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Multiblocks for any mod: a machine built from many blocks placed in a shape, made of its own structural blocks and
 * one terminal, the block the player works it from. The shape is drawn as a pattern or given by a rule and matched
 * against the world from the terminal in each way it may stand; its entries, where things come in and go out, stand
 * where the multiblock says they may and the player puts them; and the whole is shown before it is built.
 *
 * <p>A shape is an {@link IStructureShape}. A {@link MultiblockPattern} draws it in letters, layer by layer from
 * the bottom, each letter a {@link Place}: what fits there ({@link BlockMatch}, blocks and tags as data), what a
 * block there counts and what number it gives, and the entries it takes; the least and the most of each count is a
 * {@link Limit}, and counts that go together a {@link MultiblockPattern.Together}. It is found in its four horizontal
 * turns, and mirrored or lying on its side when it says so ({@link Orientation}). A {@link RuleShape} is a shape of
 * whatever size the player built within its limits: a box, a floor with walls round it, a cuboid of units, its inside
 * read by an {@link InsideRule}. A mod declares its patterns with {@link MultiblockPatterns}, which writes each to a
 * data file a data pack may change, and checking a shape gives a {@link ShapeMatch}: formed, with what it is made of
 * and gives, or the first place that is wrong. A machine of the Core's kit becomes a terminal with
 * {@link MachineStructure}, checked again only when a block changes in the box it watches ({@link LevelStructures}),
 * never every tick.
 *
 * <p>Entries are the multiblock's own blocks declared on the places they may stand ({@link Place#entry}): whichever
 * stands at a place makes it the port of its kind ({@link PortKind}), through which {@link MultiblockPorts} hands pipes
 * and cables what the terminal holds. A pattern may also make a letter's every place a port, and the generic port
 * blocks ({@link MultiblockPortBlock}, {@link RedstonePortBlock}) stand anywhere a shell lets them. {@link FormedParts}
 * merges a formed shell into its terminal's model, and {@link ShapePreview} lays a shape out to be shown before it is
 * built.
 *
 * <p>A block bigger than a block, placed whole, is not a multiblock: that is a big block, in
 * {@code dev.jstech.core.bigblock}.
 *
 * <p>How to build one, from zero, is in {@code core/docs/MULTIBLOCKS.md}. An example that compiles is the test mod's
 * furnace, a pattern with four ports ({@code tests/src/main/java/dev/jstech/tests/TestMultiblocks.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.multiblock;

import dev.jstech.core.machine.MachineStructure;
import org.jetbrains.annotations.ApiStatus;
