/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * The settings screen the Core draws for any mod that keeps its settings the Core's way, reached from the game's Mods
 * list: a menu over the whole game in the style the player wears, with a tab for each kind of file the mod keeps, the
 * open tab's sections down the left, and each setting with the control its kind of value calls for, its description,
 * and a button back to its default. These classes load only on a player's game, never on a dedicated server.
 *
 * <p>A mod hands {@link CoreConfigScreen} to the game as its settings screen, from its client entry point:
 * {@code container.registerExtensionPoint(IConfigScreenFactory.class, CoreConfigScreen::new)}. The screen reads
 * everything it shows from the mod's settings files: the settings' names, descriptions and units, their sections,
 * presets and rules. A mod may put its mark in the header's corner, a small drawing in one colour
 * ({@link IConfigBadge}); one that hands none gets {@link IConfigBadge#CHIP}. A setting whose values a word or a number
 * cannot show names a chooser of its own, an {@link IConfigChooser} registered with {@link ConfigChoosers} from client
 * setup, and the screen opens it on a click. What a chooser picks goes into the screen's changes, kept with Done or
 * dropped with Escape.
 *
 * <p>The screen and how a mod gives it, from zero, are in {@code core/docs/SETTINGS.md}. Examples that compile are J's
 * Computers' client entry point, which hands the screen its mark
 * ({@code computers/src/main/java/dev/jstech/computers/JsComputersClient.java}), and the test mod's, which hands it
 * none ({@code tests/src/main/java/dev/jstech/tests/JsTestsClient.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.client.config;

import org.jetbrains.annotations.ApiStatus;
