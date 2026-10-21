/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Values a data pack gives, for any mod: every JSON file under a folder of {@code data/<namespace>/} one value, read
 * with a codec and read again whenever the server reloads its data, and sent to the players when the mod asks; and
 * things of one sort a mod declares in code, which the files of a data pack add to or change.
 *
 * <p>A mod declares a {@link DataRegistry} once, beside what reads it, with its id, its folder and its codec,
 * {@code synced()} when the players' side needs it too and {@code onReload} to hear each new set of values. Each file
 * is known by its id ({@code data/jsc/recipe_machines/vanilla.json} is {@code jsc:vanilla}): {@code get} and
 * {@code entries} read what the server read, {@code clientEntries} what a player's game was sent, and
 * {@code entries(level)} the side a level is on. A file that does not read is left out, with a line in the log saying
 * why. A {@link DeclaredData} holds things a mod declares in code with {@code declare}, which a file of the same id
 * wins over, so a pack adds or changes one with no code; its files are sent to every player, so both sides read the
 * same. {@link DataRegistries} holds every registry and sends the synced ones to the players as they join and after
 * every reload.
 *
 * <p>How to read values from data packs is in {@code core/docs/STATE_AND_SAVES.md}. An example that compiles is the
 * test mod's notes, a synced registry that counts its reloads
 * ({@code tests/src/main/java/dev/jstech/tests/TestData.java}); the Core's kinds of vein and of reservoir are
 * registries of files too.
 */
@ApiStatus.Experimental
package dev.jstech.core.data;

import org.jetbrains.annotations.ApiStatus;
