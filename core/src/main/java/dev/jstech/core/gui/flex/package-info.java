/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * The layout engine screens are laid out with: rows, columns, grids and stacks of boxes with gaps, padding, margins,
 * smallest and largest sizes and a share of the room to grow into. Pure Java with no game types, so the layout a screen
 * draws is the one a unit test checks, overlaps and all.
 *
 * <p>A layout is a tree of {@link FlexBox}es, each written as its kind (a {@code leaf} that shows something, a
 * {@code row}, a {@code column}, a {@code grid} of {@link Track} columns, a {@code stack} of boxes laid over each
 * other) followed by what it changes: its padding and margin ({@link Insets}), its gap, its sizes, its grow and shrink,
 * where it stands ({@link Align}) and where the room left goes ({@link Justify}). A leaf says how big it wants to be
 * through an {@link IMeasure}, a text through a {@link TextMeasure} whose widths come from whoever knows the font.
 * {@link FlexLayout#layout} places the tree in a rectangle and gives a {@link Placed}, every box's rectangle, with
 * those whose contents did not fit even at their smallest marked overflowing; {@link Placed#guiLayout} hands it to the
 * overlap check, each box taken as its {@link Role} says. Every size is a whole pixel, and a share of room is cut by
 * largest remainder, so the parts always add up to the room they share.
 *
 * <p>The engine is explained, with an example, in {@code core/docs/SCREENS.md}. An example that compiles is its unit
 * test ({@code core/src/test/java/dev/jstech/core/gui/flex/FlexLayoutTest.java}); every screen declared in a file is
 * laid out with it, J's Example's logbook among them
 * ({@code example/src/main/resources/assets/jsexample/jstech/screens/logbook.json}).
 */
@ApiStatus.Experimental
package dev.jstech.core.gui.flex;

import org.jetbrains.annotations.ApiStatus;
