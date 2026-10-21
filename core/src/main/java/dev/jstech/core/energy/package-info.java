/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Energy for any mod: FE (Forge Energy), the game's own, which cables move from the blocks that give it to the blocks
 * that take it; and an energy of a mod's own, counted in its own unit, with classes of line as voltage classes are,
 * loss by distance, converters to and from FE, protection against a line of the wrong class, and the readings an
 * instrument shows.
 *
 * <p>A mod that only wants FE declares nothing: a block entity's energy store marked {@code exposed()} is reached by
 * any cable of the power grid, and each tick, after the machines have worked, the grid shares what the generators
 * on a run give among what its consumers ask, each its share in proportion, the shares adding up exactly to what
 * was given ({@link LevelEnergy}). An item holds FE through {@link CoreEnergy}. A mod that wants more declares its
 * {@link Energy} once with {@link Energies}: its {@link EnergyUnit} and what that is worth in FE, its classes
 * ({@link EnergyClass}), each a grid of its own whose lines never join another's, what each class loses by
 * distance, and the {@link IWrongClassRule} for a port that meets a live line of another class. A block's port is an
 * {@link IEnergyPort} offered by {@link EnergyPorts}: an {@link EnergyStore}, or a {@link GeneratorPort} whose output
 * follows its load. {@link FeBridge} and {@link EnergyConversion} meet FE, both rounding down, so no energy is ever
 * made; {@link EnergyProtection} holds a machine's fuse and its battery; {@link PartialWork} lets a machine short of
 * energy work slower in proportion; and {@link EnergyReadings} is what an instrument reads of a wire or a block.
 *
 * <p>How energy moves, and how to declare an energy of a mod's own, from zero, is in
 * {@code core/docs/ENERGY_AND_RESOURCES.md}. Examples that compile are J's Example's energy cable
 * ({@code example/src/main/java/dev/jstech/example/ExampleContent.java}) with the generator and battery whose FE it
 * carries ({@code example/src/main/java/dev/jstech/example/machine}), and the test mod's energy of its own, with four
 * classes of line ({@code tests/src/main/java/dev/jstech/tests/TestEnergies.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.energy;

import org.jetbrains.annotations.ApiStatus;
