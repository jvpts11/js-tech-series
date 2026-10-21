/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Heat for any mod, in real units: temperature held in blocks, in kelvin, carried between neighbours by the real
 * conductivity of their materials and along heat pipes, and lost to the place around them, to the air as thick as
 * it is and by radiation, which in a vacuum is the only way out; with heaters and coolers, boilers, exchangers, cold
 * liquids that boil off, and the sensors and the sight that read it.
 *
 * <p>A block entity holds heat in a {@link HeatBody}: so many cubic metres of a material, whose {@link Thermal} data
 * says how much heat it holds a kelvin and how well it conducts. It implements {@link IHeatHolder} and keeps its body
 * with {@link HeatPorts}, which offers it to its neighbours and to heat pipes on the heat capability with nothing
 * to register; another mod's block joins by offering an {@link IHeatPort} there. Each dimension's {@link LevelHeat}
 * carries the heat by the physics of {@link HeatMath}, a few hundred blocks a tick, and lets a block in balance with
 * everything round it sleep, so idle heat costs nothing; how hot the place around a block is, and how thick its air,
 * is its {@link HeatClimate}. A heat pipe is a cable of the heat grid, each connected part of it one node that holds
 * no heat ({@link LevelHeatLines}). A block puts heat in or takes it out with a {@link HeatSource}, boils a liquid
 * with a {@link Boiler}, passes one through its wall with a {@link FluidPass}, and loses a cold liquid to the place's
 * heat with {@link BoilOff}; {@link HeatReadings} speaks a temperature in redstone, and {@link ThermalSight} shows
 * heat to a player who wears or holds an item that sees it.
 *
 * <p>How heat works and how a block holds it, from zero, is in {@code core/docs/HEAT.md}. An example that
 * compiles is the test mod's blocks that hold heat, a radiator, a heater, boilers and a heat exchanger among them
 * ({@code tests/src/main/java/dev/jstech/tests/TestHeat.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.heat;

import org.jetbrains.annotations.ApiStatus;
