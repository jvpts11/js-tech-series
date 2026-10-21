/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Mechanical power for any mod, in real units: rotation carried by shafts, gears, gearboxes, belts and chains from
 * water wheels, windmills, steam engines, hand cranks and motors to the machines that turn with it, its speed in
 * revolutions a minute, its torque in newton-metres and its power in watts. The kit is the Core's; the blocks a mod
 * makes of it, their models and their recipes, are the mod's.
 *
 * <p>Whatever turns is an {@link IMechanicalPart}: it says how each of its faces meets its neighbour
 * ({@link ShaftContact}: by an axle, by a gear's teeth, or not at all), what it is joined to across a distance
 * ({@link ShaftJoin}), what it weighs in turning and holds back by friction, and what it drives or asks
 * ({@link ITorque}); a source gives its {@link TorqueCurve}. A block entity that is one keeps itself on its
 * network with {@link MechanicalPorts}, which offers it with nothing to register; one that holds a part without
 * being one is an {@link IMechanicalHolder}. Each dimension's {@link LevelMechanics} joins the parts into a
 * {@link ShaftNetwork} again only when one is placed, broken, loaded or changes how it meets its neighbours,
 * and turns each network once a tick as a whole, by the physics of {@link RotationMath}: a network with too
 * little power for its loads turns slower, and one whose gears disagree is jammed. The Core's own parts are a
 * {@link MechanicalPartBlock} of a {@link MechanicalPartSpec} (a shaft, a flywheel, a gear, a gearbox, a clutch, a
 * brake, a speed controller, a shear pin, a pulley, a torque meter) with a {@link MechanicalPartBlockEntity}, and a
 * belt or a chain is a {@link ShaftLinkItem}; a machine of the Core's machine kit turns as a motor or a dynamo through
 * {@link MachineShaft}.
 *
 * <p>How it works and how to build its parts, from zero, is in {@code core/docs/MECHANICAL_POWER.md}. An example
 * that compiles is the test mod's parts, with its hand crank, its electric motor, its dynamo and its mills
 * ({@code tests/src/main/java/dev/jstech/tests/TestMechanical.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.mechanical;

import dev.jstech.core.machine.MachineShaft;
import org.jetbrains.annotations.ApiStatus;
