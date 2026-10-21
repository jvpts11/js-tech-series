/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Fonts of the mods' own: read from free font sources (bitmap BDF files first), laid out on the sheets the game's
 * bitmap fonts are drawn from, declared by each mod with the licence and the credit they come under, and the pure
 * geometry the grid painter uses to put text on a monospace grid: one character to a cell, and the box and block
 * characters drawn to fill their cells so lines and bars join from cell to cell.
 *
 * <p>A font is declared with the mod's content ({@code ModContent.font}) as a {@link CellFont}: its BDF source, its
 * cell, its baseline, its licence and its credit. The data generation reads the source with the {@link BdfReader} into
 * a {@link BitmapFont}, checks the cell and the baseline against it, lays it out as a {@link FontSheet}, and writes the
 * picture and the font file the game draws from, so any text can be drawn in the font by its id. {@link CoreFonts} are
 * the fonts the Core carries for every mod: Misc Fixed, in the public domain, in four sizes. The grid painter
 * ({@link GridPainter}, on the player's game) lays each row, as its stretches of styled text ({@link GridSpan}), out
 * with {@link GridLayout} into the fewest pieces that keep every character in its own cell, and draws the box lines and
 * the blocks from the rectangles {@link CellGlyphs} gives for a cell of any size.
 *
 * <p>How to declare one and draw on a grid, from zero, is in {@code core/docs/FONTS.md}. Examples that compile are the
 * test mod's grid painter tests
 * ({@code tests/src/main/java/dev/jstech/tests/clienttest/core/GridPainterClientTests.java}), which draw a frame of box
 * lines in the Core's terminal font, and J's Computers' terminals
 * ({@code computers/src/main/java/dev/jstech/computers/client/term}).
 */
@ApiStatus.Experimental
package dev.jstech.core.font;

import dev.jstech.core.client.font.GridPainter;
import org.jetbrains.annotations.ApiStatus;
