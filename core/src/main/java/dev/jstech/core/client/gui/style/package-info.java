/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * The styles screens are drawn in, on the player's game: read from the resource packs, chosen by the player for every
 * screen or for one mod's, and worn by the components as a skin, so a choice in the player's settings repaints every
 * screen of every mod built on the Core at once. These classes load only on a player's game, never on a dedicated
 * server; what a style is, and its file, are in {@code dev.jstech.core.gui.style}.
 *
 * <p>{@link Styles} gives the style a mod's screens wear, {@link Styles#forMod}: the player's choice for that mod, else
 * their choice for everything, else the mod's default, which the mod sets from client setup with
 * {@link Styles#setModDefault}, else the game's own. A screen draws in a style through its skin, a {@link StyleSkin}
 * from {@link Styles#skin} in a window or {@link Styles#menuSkin} on a menu over the whole game, and every component
 * drawn through it takes the style's pieces, colours, font, spacing and finish. {@link StyleLoader} reads the style
 * files as the resource packs load, {@link StylePainter} draws a style's pieces, and the player picks a style, seen
 * first in a preview, on the {@link StyleChooserScreen} the Core's settings open. The server imposes nothing.
 *
 * <p>The styles that come with the Core, how a player picks one and how a resource pack writes one are in
 * {@code core/docs/SCREENS.md}. An example that compiles is the test mod's pump panel, drawn in the J's Tech style
 * ({@code tests/src/main/java/dev/jstech/tests/clienttest/TestTouchPanelClient.java}); J's Example's logbook wears the
 * style the player picked with no code at all
 * ({@code example/src/main/java/dev/jstech/example/client/JsExampleClient.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.client.gui.style;

import org.jetbrains.annotations.ApiStatus;
