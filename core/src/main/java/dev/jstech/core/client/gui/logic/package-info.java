/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * The logic behind the components, kept apart from their drawing: the text of a field and of a multi-line editor with
 * its caret and its selection, and the editor's undo; the arithmetic of sliders, steppers, colour pickers, charts,
 * scrolling, pagination, radial menus and graph scales; and the models of the data views, a tree, a table and a node
 * graph. Pure Java with no Minecraft dependency: it sits beside the components of the player's game, but its rules are
 * checked by unit tests without the game.
 *
 * <p>The components keep their state in these: a text field edits a {@link TextEditState}, a text area a
 * {@link TextDocument}, a stepper steps by {@link StepperMath}. A mod builds the model of a data view in its screen's
 * code and hands it over by name, as a screen's file binds it: a {@link TreeModel} for a tree, whose open nodes give
 * its rows and their guides; a {@link TableModel} for a table, its columns and its rows as the player sorted and
 * filtered them; a {@link GraphModel} for a node graph editor, whose owner hears every change to its nodes and wires.
 *
 * <p>The components these serve, and the models of the data views, are in {@code core/docs/UI_COMPONENTS.md}. An
 * example that compiles is the test mod's screen of views, whose code builds a tree, a table and a graph
 * ({@code tests/src/main/java/dev/jstech/tests/clienttest/core/TestControlScreens.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.client.gui.logic;

import org.jetbrains.annotations.ApiStatus;
