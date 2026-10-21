/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * What a screen declared in a file is: the file, {@code assets/<namespace>/jstech/screens/<name>.json} in a resource
 * pack, read into a tree of components, and every kind of component such a file may hold. The file says what the screen
 * holds and where; there is no style sheet, since how the screen looks is the style the player picks. These classes use
 * no client types, so a mod declares its kinds where both sides see them and a file can be read and checked without a
 * player's game; what builds and draws a screen on the player's game is in {@code dev.jstech.core.client.gui.data}.
 *
 * <p>A {@link ScreenFile} is the screen's width, its title, the style its mod wants it in when the player chose none,
 * and its root {@link ElementSpec}. Each component in the tree is its kind, the {@link CommonProps} every component has
 * (its id, its sizes and its share of room, its margin, a tooltip, a name for the narrator, the values it is shown and
 * usable by), the {@link ContainerProps} of one that holds others, and its own fields. The Core's kinds are in
 * {@link CoreElements} (the boxes that lay others out, the title, labels, buttons, slots, the player's inventory, a
 * bar, a picture, spacers, separators, a check box and a field) and {@link ControlElements} (sliders, switches, option
 * buttons, drop-down lists, colours, trees, tables, charts, graphs, 3D views, maps, tanks, energy bars and ghost
 * slots), and a file names them by their short name. A mod declares a kind of its own, an {@link ElementType}, with
 * {@link ElementTypes#declare} from its constructor, names it in a file by its id, and registers on the player's game
 * what builds it.
 *
 * <p>Every kind and field a file may hold is in {@code core/docs/SCREENS.md}. Examples are J's Example's logbook
 * ({@code example/src/main/resources/assets/jsexample/jstech/screens/logbook.json}) and the test mod's screens
 * ({@code tests/src/main/resources/assets/jstests/jstech/screens}), which
 * {@code tests/src/main/java/dev/jstech/tests/gametest/core/ScreenFileGameTests.java} reads as the game does.
 */
@ApiStatus.Experimental
package dev.jstech.core.gui.screen;

import org.jetbrains.annotations.ApiStatus;
