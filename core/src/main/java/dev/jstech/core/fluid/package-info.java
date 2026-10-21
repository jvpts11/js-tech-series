/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Fluids and pipes: the marks a pipe has to be made for (a gas, a corrosive fluid), what a pipe stands and takes,
 * and the fluid grid that moves fluids through pipes between the blocks they plug into. Fluids themselves are declared
 * with a mod's content.
 *
 * <p>A pipe is a cable of the fluid grid, declared as any cable is; its throughput is the millibuckets it moves a
 * tick, the pressure it holds. What it is made for is its {@link PipeLimits}: the coldest and the hottest fluid it
 * stands, in kelvin, and which of the marks in {@link CoreFluidTags} it takes; the marks are tags, so a data pack puts
 * them on any mod's fluids. A tank is a block entity's fluid field marked {@code exposed()}, which a pipe beside it
 * reaches. Each dimension's {@link LevelFluids}, found through {@link FluidGrids}, moves the fluids after the machines
 * have ticked: a machine's output feeds the inputs, then fills the tanks, and the tanks feed what the inputs still
 * want, each end by its mode, priority, rate, channel and filter, and two tanks alone never pour into each other. A
 * part of the grid moves no more in a tick than its slowest pipe carries, and only the fluids every pipe of it is made
 * for, any other staying where it is; a liquid freezes and stays put where a pipe of it that is not insulated runs
 * through a place colder than its melting point.
 *
 * <p>How tanks and pipes work, from zero, is in {@code core/docs/ENERGY_AND_RESOURCES.md}, and how a pipe is declared
 * in {@code core/docs/CABLES.md}. Examples that compile are J's Example's fluid pipe
 * ({@code example/src/main/java/dev/jstech/example/ExampleContent.java}) and its tank, which a pipe reaches on every
 * side ({@code example/src/main/java/dev/jstech/example/machine/TankBlockEntity.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.fluid;

import org.jetbrains.annotations.ApiStatus;
