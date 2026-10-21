/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Packs a mod makes as the game loads, rather than ships: resources for the player's game or data for the server, whose
 * files (textures drawn from the game's own, models, tags, loot tables) are made from what the installed mods declared,
 * each only when it is first read.
 *
 * <p>A mod declares each pack once, from its constructor, with {@link RuntimePacks}:
 * {@code declare(id, type, title, files)}, its files an {@link IRuntimeFiles} that gives each file by its path in the
 * pack with what makes its bytes, asked again whenever the game reloads its resources or its data. Each pack is always
 * on, below every pack a player or a server adds, so a resource pack or a data pack can still replace any of its files.
 * Files that cost much to make are wrapped in {@link CachedRuntimeFiles}, which keeps them on disk under
 * {@code jstech/cache/} in the game's folder and reads them back on the next load for as long as the pack's fingerprint
 * (the mods installed and their versions, and whatever else the files are drawn from) stays the same. A texture such a
 * pack draws on the player's side, named by a hand-written model, is promised from the mod's common code with
 * {@code RuntimePacks.promise}, so the check of the mod's generated data finds it.
 *
 * <p>How the Core's own packs are made, and how a mod makes one, is in {@code core/docs/CONTENT.md}. An example that
 * compiles is the art of the materials' things, drawn in each material's colour and kept on disk
 * ({@code core/src/main/java/dev/jstech/core/client/material/MaterialArt.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.resource;

import org.jetbrains.annotations.ApiStatus;
