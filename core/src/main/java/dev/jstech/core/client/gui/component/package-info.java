/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * The components a program's content and a screen are built from: buttons, fields, tabs, lists, grids, sliders,
 * switches, drop-down lists, trees, tables, charts, gauges, popups and the panel that holds them. Each keeps its own
 * state and draws through an {@link ISkin}, so a program written once looks like whichever desktop it runs on and a
 * screen of the Core wears the player's style, and a mod or an addon composes them instead of painting pixels and
 * hit-testing rectangles by hand. These classes load only on a player's game, never on a dedicated server.
 *
 * <p>Components are retained and their layout is immediate. A mod creates each {@link UiComponent} once, adds it to a
 * {@link Panel}, places it every frame with {@code setBounds} from the room it has, and renders the panel with a
 * {@link UiContext}: the skin, the font, the pointer and the partial tick. The panel routes clicks, keys, the wheel and
 * the keyboard among its children, the last added in front; a {@link Popup} floats a modal dialog over them.
 * {@link KeyboardFocus} hands the components to the game's own screen as its widgets, so the keyboard, a controller and
 * the narrator move among them. Text is drawn through {@link Draw}, over a ground painted with {@link Grounds}, fitted
 * to a width with {@link Texts} and sized as the player asked with {@link TextScale}.
 *
 * <p>Every component, and how a program is written with them, is in {@code core/docs/UI_COMPONENTS.md}. An example that
 * compiles is the test mod's pump panel, a screen in the world built of two labels and two buttons
 * ({@code tests/src/main/java/dev/jstech/tests/clienttest/TestTouchPanelClient.java}); J's Example's logbook has its
 * components built for it from its screen's file
 * ({@code example/src/main/resources/assets/jsexample/jstech/screens/logbook.json}).
 */
@ApiStatus.Experimental
package dev.jstech.core.client.gui.component;

import dev.jstech.core.api.client.ISkin;
import org.jetbrains.annotations.ApiStatus;
