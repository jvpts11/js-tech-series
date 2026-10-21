/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Cables for any mod, all laid in the Core's one cable block: each cable a wire in one of nine lanes, so the lines of
 * many mods run side by side through one block without joining, each wire joined to the wires and devices beside it,
 * its ends set one by one, and what it carries shown in its windows and to a wrench.
 *
 * <p>A mod declares a cable as a {@link CableType}, built with {@link CableType.Builder}: the line it carries
 * and its generation, the grid it is a part of, its {@link Lane} or a block to itself, how much it carries a tick
 * and how far a run reaches, what it loses on the way, for a pipe the temperatures and marks it stands, and its
 * jacket and plug. Declared with its content ({@code ModContent.cable}), it becomes a {@link CableEntry}: the cable,
 * registered in the Core's cable registry ({@link CoreCables}), and the item that lays it. The kit does the rest. The
 * {@link CableBlock} and its {@link CableBlockEntity} hold the wires, each a {@link Wire} that joins the wires of
 * its line and generation whose colours agree, and put each in the grid of its kind as it is laid or loads; a wire
 * of energy plugs into any block that offers the game's energy, and a pipe into any block that holds fluids. Where a
 * line routes what it carries, each end is a {@link CableEnd}, with its mode, priority, rate, channel and filter,
 * set with a wrench and on its screen. What befalls a pipe that something it is not made for reaches is its
 * {@link IBreachRule}, and {@link Cables} lays wires and reads them from code.
 *
 * <p>How to declare one, from zero, is in {@code core/docs/CABLES.md}. Examples that compile are J's Example's energy
 * cable and fluid pipe ({@code example/src/main/java/dev/jstech/example/ExampleContent.java}), and J's Industrial's
 * energy cable ({@code industrial/src/main/java/dev/jstech/industrial/IndustrialModule.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.cable;

import org.jetbrains.annotations.ApiStatus;
