/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * What a style is, the look the Core's screens and every screen declared in a file are drawn in: its colours by role,
 * the pieces its screens are cut from, its spacing, its finish, its font and the sound of a press, written as a file of
 * a resource pack, {@code assets/<namespace>/jstech/styles/<name>.json}, that builds on a parent for what it does not
 * set. A pack replaces a style by writing a file of the same name and adds one under a new name. These classes use no
 * client types, so a style's files are read and checked without a player's game; reading them from the resource packs,
 * choosing one and drawing in it are in {@code dev.jstech.core.client.gui.style}.
 *
 * <p>A {@link StyleFile} is what one file sets: its name, its parent, a palette it takes its colours from, its colours
 * by {@link StyleRole}, how it draws each {@link StylePiece} (a {@link StyleSprite}: a sprite of the GUI atlas, a
 * texture cut in nine or laid side by side, flat colours, or nothing), its {@link StyleMetrics} and its
 * {@link StyleFinish}. {@link StyleFiles} follows each file's parents and makes a {@link Style} with every part
 * decided, what no style sets drawn flat in its colours; what is wrong in a file is said, and the style is made all the
 * same, so one slip costs a look, not a screen. A style that names a palette finds it through an
 * {@link IPaletteSource}: the palettes the mods declared ({@link DeclaredPalettes}) in the game, a fixed set in a test.
 * The styles a player chose are kept in their own settings, {@link StyleSettings}; the server imposes nothing.
 *
 * <p>The styles that come with the Core, and how a resource pack writes one, are in {@code core/docs/SCREENS.md}.
 * Examples are the Core's own style files ({@code core/src/main/resources/assets/jscore/jstech/styles}), which
 * {@code tests/src/main/java/dev/jstech/tests/gametest/core/StyleFileGameTests.java} reads and checks as a resource
 * pack's are read.
 */
@ApiStatus.Experimental
package dev.jstech.core.gui.style;

import org.jetbrains.annotations.ApiStatus;
