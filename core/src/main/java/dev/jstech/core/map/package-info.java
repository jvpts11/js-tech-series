/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Markers on the maps for any mod: points and areas of chunks, each with a label and a colour, in layers a map shows or
 * hides as one, kept with the world for each player and for each team, and sent to each player as they change.
 *
 * <p>A mod declares each of its layers once, with {@link MapMarkers}: {@code MapMarkers.layer(id, name, colour, areas)}
 * gives a {@link MarkerLayer}, whose markers are points or, when {@code areas}, areas of chunks. A {@link Marker}, made
 * with {@code Marker.point} or {@code Marker.area}, is shown to one player with {@code MapMarkers.show}, shared with
 * the player's team, whose every member sees it, with {@code MapMarkers.share}, and taken off with
 * {@code MapMarkers.hide}; a marker of the same id takes the place of the old, and {@code coloured} gives one its own
 * colour instead of its layer's. The Core's own layers are {@code MapMarkers.LANDMARKS}, the landmarks a player or
 * their team discovered, and {@code MapMarkers.FOUND}, the places an instrument found. The markers reach the map mods a
 * player has through the Core's adapters, each loaded only while its mod is installed; with none, the series' own maps
 * show them ({@code ClientMarkers}, on the player's game).
 *
 * <p>How to use them is in {@code core/docs/WORLD.md}. Examples that compile are the test mod's layer of ruins
 * ({@code tests/src/main/java/dev/jstech/tests/TestPlaces.java}), shown and shared by the Core's tests of places
 * ({@code tests/src/main/java/dev/jstech/tests/gametest/core/PlaceGameTests.java}), and the Core's layer of the chunks
 * kept loaded ({@code core/src/main/java/dev/jstech/core/region/LoadedChunkMarkers.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.map;

import org.jetbrains.annotations.ApiStatus;
