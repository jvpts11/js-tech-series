/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * A mod's data generation, started from what it declared: the block states, models, English, loot tables, tags, sounds,
 * palettes, fonts, manuals, multiblock patterns and world generation its declarations already say, written as the files
 * the game reads; a last check that fails the run when a block or an item lacks one of them; and the Core's own data:
 * the features that lay the veins and the reservoirs, and the game's own blocks' density, conductivity, strength
 * against a blast, seal and the colour of their light.
 *
 * <p>A mod hands its data generation to {@link ContentData}: {@code ContentData.gather}, from the mod's
 * {@code GatherDataEvent} listener, adds a provider for each kind of file its {@link ModContent} declares; the mod adds
 * only what is its own with {@code client} and {@code server} (its recipes, for example); and {@code check()}, called
 * last, looks for every block and item the mod registered and fails the run, saying what to add, when one has no
 * English name, no block state, no model, a model whose texture is nowhere, or no loot table while it drops something.
 * Each provider can be added alone too: {@link ContentLanguageProvider} writes the English of every sentence the mod
 * declares ({@link DeclaredTexts}) and of its blocks, items and tabs, {@link WorldGenProvider} the ores and structures
 * it declared for the world's generation, {@link DeclaredFilesProvider} the kinds of vein and of reservoir declared in
 * code, and {@link ContentPartModelProvider} the models of the parts drawn as plates. A data pack or a resource pack
 * can replace any file written.
 *
 * <p>How to hook it up, from zero, is in {@code core/docs/GETTING_STARTED.md}. Examples that compile are J's Example's
 * entry ({@code example/src/main/java/dev/jstech/example/JsExample.java}) and J's Industrial's, which adds its recipes
 * ({@code industrial/src/main/java/dev/jstech/industrial/datagen/JsIndustrialDataGenerators.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.datagen;

import dev.jstech.core.content.ModContent;
import org.jetbrains.annotations.ApiStatus;
