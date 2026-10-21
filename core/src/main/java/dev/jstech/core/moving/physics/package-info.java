/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * The physics of moving structures: a structure let go moves as a body of real mass, by its weight, its engines,
 * wings, wheels and balloons, its drag, its floating, levitation and magnets, and the wind, and it meets the world and
 * the other structures, held to them by its contacts and its joints. Its units are metres, seconds, kilograms and
 * newtons.
 *
 * <p>A mod lets a structure go by moving it with a {@link PhysicsMotion}: at rest ({@code PhysicsMotion.AT_REST}), or
 * thrown ({@code PhysicsMotion.going}). It then falls, floats, flies or rests by its physics until another rule is
 * set, and how fast it goes is saved with it. What each block is, for the physics, is a data map,
 * {@link BlockPhysics}, which any mod or data pack fills: its mass and the forces it gives, an engine's thrust, a
 * wing's lift, a wheel's spring, a float's displacement, a levitator's hold, a magnet's pull, a burner's heat in a
 * balloon. A block the map says nothing of weighs its density times the volume of its shape. A structure's
 * {@link StructurePhysics} keeps its mass, centre and inertia as its blocks change ({@link StructureParts}), sums each
 * tick's forces in a {@link ForceSum} and moves its {@link RigidBody} by them. What a place of the world is like for a
 * body moving through it, its pull, its air and its wind, is {@link PhysicsWorld}. The arithmetic beneath is free of
 * the game, so most of it is tested with no game around it: {@link Aerodynamics}, {@link ContactMath},
 * {@link BoxContacts}, {@link ContactSolver}, {@link ConstraintRows}, {@link JointRows}, {@link MassSum} and
 * {@link CompoundMass}.
 *
 * <p>How to use it, from zero, is in {@code core/docs/MOVING_STRUCTURES.md}, under "By its physics". Examples that
 * compile are the test mod's blocks with forces ({@code tests/src/main/java/dev/jstech/tests/TestPhysics.java}),
 * given them by its data map ({@code tests/src/main/resources/data/jscore/data_maps/block/block_physics.json}), and
 * the Core's game tests of the physics
 * ({@code tests/src/main/java/dev/jstech/tests/gametest/core/MovingStructurePhysicsGameTests.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.moving.physics;

import org.jetbrains.annotations.ApiStatus;
