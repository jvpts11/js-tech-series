/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Multipart blocks: thin parts mounted on a block's faces, kinds of them registered by any mod, and a bundle of wires
 * running through the block's middle beside them, kept by the block's entity as fields of its own; and with them
 * microblocks, pieces of any full block of any mod cut with the saw, and the game's own torches, levers, buttons and
 * pressure plates, all sharing one block space.
 *
 * <p>A block space has 27 places ({@link SpaceSlots}): its centre, where a cable's wires run, its six faces,
 * its twelve edges and its eight corners, each holding one part at most, and a part fits only where it shares
 * no room with another ({@link SpaceFit}). The Core's cable block is one kind of space and the space of parts
 * alone ({@link SpaceBlock}) another; both keep their parts in a {@link SpaceParts}, and their block entities
 * are each an {@link ISpaceHolder} and the {@link IPartHost} of their parts. A mod's device on a face implements
 * {@link IFacePart}: it is attached to its host and its face, saves and loads itself, may tick, gives redstone, light
 * and a comparator's reading, answers a player's use, offers capabilities on its face, and gives back its item. Its
 * kind is a {@link PartType}, declared with the mod's content ({@code ModContent.part}) with its {@link PartItem},
 * or registered in {@link CoreParts} by the mod's own deferred register. A block of a mod's own that holds parts
 * answers their capabilities through {@link PartCapabilities} and their redstone through {@link PartRedstone}. A
 * {@link Microblock} is a piece of a {@link MicroShape} and a thickness in a place, carried by the one
 * {@link MicroblockItem}, and cut and joined in the crafting grid as {@link Cuts} says.
 *
 * <p>How a block space and its parts work, from zero, is in {@code core/docs/MULTIPART.md}, and parts on a cable in
 * {@code core/docs/CABLES.md}. An example that compiles is J's Example's level sensor, a plate on a cable's face that
 * gives out how full the block beside it is
 * ({@code example/src/main/java/dev/jstech/example/part/LevelSensorPart.java}), declared in
 * {@code example/src/main/java/dev/jstech/example/ExampleContent.java}.
 */
@ApiStatus.Experimental
package dev.jstech.core.multipart;

import org.jetbrains.annotations.ApiStatus;
