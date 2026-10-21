/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Machines and their recipes for any mod: the machine with an inventory and energy, the processing machine with its
 * inputs, outputs, tanks and upgrades, and the processing recipe every kind of machine shares, its kinds declared as
 * recipe types and its files written by a builder.
 *
 * <p>A machine is a block entity that extends {@link MachineBlockEntity}. The kit saves its slots and its energy,
 * offers both to pipes and cables, gives it the standard screen of five tabs ({@link MachineMenu}), an ON/OFF switch,
 * an answer to redstone, health, an owner and a state always in view ({@link MachineStatus}), and ticks it only while
 * it has something to do. What the machine does is its {@code work}, whose answer is its state. A machine that turns
 * some things into others extends {@link ProcessingMachineBlockEntity} instead, says how its slots are laid out and
 * which {@link ProcessingKind} of recipes it works, and the kit does the rest: it finds the recipe, takes the inputs,
 * spends the energy over the recipe's time and puts the outputs away.
 *
 * <p>How to build one, from zero, is in {@code core/docs/MACHINES.md}. Examples that compile are J's Example's
 * generator, battery and crusher ({@code example/src/main/java/dev/jstech/example/machine}), and J's Industrial's
 * machines.
 */
@ApiStatus.Experimental
package dev.jstech.core.machine;

import org.jetbrains.annotations.ApiStatus;
