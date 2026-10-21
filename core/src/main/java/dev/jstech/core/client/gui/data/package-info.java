/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Screens declared in files, built on the player's game: a resource pack's file says what a screen holds and where, and
 * these classes build its components, lay them out, draw them in the style the player picked, move the menu's slots
 * under the frames the file placed and route the pointer and the keys to them. The Java a mod writes for such a screen
 * keeps only what the screen does. These classes load only on a player's game, never on a dedicated server.
 *
 * <p>A menu's screen is handed to the game from client setup with {@link DataScreens#register}, which makes a
 * {@link DataContainerScreen} of the file {@code assets/<namespace>/jstech/screens/<path>.json}; a screen with no menu,
 * an instrument's or a block's settings, opens with {@link DataScreens#open} as a {@link DataScreen}. What the screen
 * does is its {@link IScreenLogic}, which hands a {@link ScreenWiring} by name the actions its file's buttons name, the
 * values it shows beyond those its menu declares, and the handlers of what its controls hand back; a screen with
 * nothing to do takes {@link IScreenLogic#none}. When the file is read again, as a resource pack changes or, in a
 * development run, as the file is saved, the open screen is built again from it. A mod that declares a kind of
 * component of its own registers what builds it, an {@link IElementBuilder}, with {@link ElementBuilders} from client
 * setup.
 *
 * <p>How to write a screen's file and its code, from zero, is in {@code core/docs/SCREENS.md}. An example that compiles
 * is J's Example's logbook: its file ({@code example/src/main/resources/assets/jsexample/jstech/screens/logbook.json}),
 * its menu, whose values the file binds by name
 * ({@code example/src/main/java/dev/jstech/example/screen/LogbookMenu.java}), and its screen handed to the game with no
 * code of its own ({@code example/src/main/java/dev/jstech/example/client/JsExampleClient.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.client.gui.data;

import org.jetbrains.annotations.ApiStatus;
