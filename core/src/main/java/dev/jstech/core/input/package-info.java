/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Keys a mod declares once for both sides: what the game's controls call the key, the key it starts on, and what the
 * server does when it is pressed. The player's game makes its key binding from the declaration, and a press of a key
 * the server has a part in reaches the server by itself, with no packet of the mod's own. These classes name nothing of
 * the client, so they load on a dedicated server too.
 *
 * <p>A mod declares each key as a constant, from its constructor while the mods load, with {@link KeyActions#declare}
 * on a {@link KeyAction#builder}: its id and its name, then {@code key} for the key it starts on (none unless given,
 * since the game and the mods beside it take nearly every key), {@code category} for its heading in the controls (the
 * series' own unless given), and {@code onServer} for what the server does with the player who pressed it and whether
 * shift was held. What a press does on the player's own game, and when a press is worth sending, the mod's client code
 * says with {@code KeyActionsClient}, in {@code dev.jstech.core.client.input}. A {@link KeyHold} tells a key held over
 * one thing long enough from a stray press. The Core's own keys are in {@link CoreKeys}.
 *
 * <p>How to declare a key is in {@code core/docs/SCREENS.md}. An example that compiles is the test of a press reaching
 * the server, on the Core's own key that changes the mode of the item in hand
 * ({@code tests/src/main/java/dev/jstech/tests/clienttest/core/KeyActionClientTests.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.input;

import org.jetbrains.annotations.ApiStatus;
