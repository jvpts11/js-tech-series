/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Moving structures for any mod: structures of real blocks, of any mod, that move through the world, from a
 * drawbridge to an airship. Their blocks are kept in a region of each dimension set aside for them, the yard, where
 * they run as on the ground: machines run, computers boot, networks work and crops grow. They are drawn and collided
 * where the structure is, carry what stands on them, are hit by arrows and every other ray, survive a restart in the
 * middle of a move, and go to another dimension with everyone aboard.
 *
 * <p>{@link MovingStructures} is where a mod starts. Its {@code assemble} makes a {@link MovingStructure} of the
 * blocks in a box, or of every block joined to one, and answers a {@link StructureResult} that says why when it is
 * refused; its {@code move} sets the rule that moves the structure, an {@link IStructureMotion} such as
 * {@link TowardMotion}, {@link SpinMotion}, {@link TurnAboutMotion}, {@link PathMotion}, {@link FaceMotion} or
 * {@link RailsMotion}, or a mod's own, registered with {@link StructureMotions}; and its {@code setDown} lays the
 * blocks back on the world's grid. The same calls mount one structure on another, dock two, join them for good and
 * take one to another dimension. Where a structure is and how it is turned is its {@link StructurePose}. What a block
 * does aboard (a seat, a helm, a docking clamp, a decoupler, a tool) is data, {@link BlockAboard}, and a helm or a
 * {@link StructureAutopilot} flies a structure by its {@link StructureControls}. A block entity that keeps the places
 * of other blocks follows them through {@link IMovesWithStructures}, and a mod that cannot change its block entities
 * listens for {@link StructureBlocksMovedEvent}; what happens to a structure, a blow, a parting, a docking, is posted
 * as a {@link MovingStructureEvent}. How a structure moves by its physics is in {@code dev.jstech.core.moving.physics}.
 *
 * <p>How to use it, from zero, is in {@code core/docs/MOVING_STRUCTURES.md}. An example that compiles is the Core's
 * game tests of moving structures
 * ({@code tests/src/main/java/dev/jstech/tests/gametest/core/MovingStructureGameTests.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.moving;

import org.jetbrains.annotations.ApiStatus;
