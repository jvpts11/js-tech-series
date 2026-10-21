/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * States: what a mod keeps with a world beyond any block, for the whole server, each dimension, each player or each
 * team, saved in a file of its own in the version of its layout, and sent to the players whose it is to see.
 *
 * <p>A state is declared once, as a constant, with the builder of {@link CoreState}: its id, the codec that writes its
 * value and its default, then the scope it ends with, which gives a {@link ServerState}, a {@link DimensionState}, a
 * {@link PlayerState} or a {@link TeamState}. It is registered once, on both sides, with {@link CoreStates}, and read
 * and changed on the server's thread with {@code get}, {@code set} and {@code update}, each change handed over as the
 * next value rather than made to the old one in place. A player's value is kept whether they are online or not, and a
 * player's or a team's value equal to the default takes no room in the file. A state declared {@code synced} is sent at
 * the end of the tick it changed in, to the players it belongs to, and again as they join, change dimension, respawn or
 * change team; their game reads it with {@code client()}. Its {@code version} and its {@code upgrade} steps bring an
 * older file up to today's layout, and a file a newer mod wrote is read as it is, a copy of it kept.
 *
 * <p>How to keep a value with the world, from zero, is in {@code core/docs/STATE_AND_SAVES.md}. An example that
 * compiles is J's Example's state, one number for the server and one for each player
 * ({@code example/src/main/java/dev/jstech/example/ExampleState.java}), changed by its generator and its logbook.
 */
@ApiStatus.Experimental
package dev.jstech.core.state;

import org.jetbrains.annotations.ApiStatus;
