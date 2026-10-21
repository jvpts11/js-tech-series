/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * The formats a settings file is written in, TOML, JSON, JSON5, YAML and NBT: each reads a file's bytes into plain
 * values and writes plain values back, with each setting's comment above it where the format has room for comments.
 *
 * <p>A mod picks one as it builds its {@link ConfigFile}: one of the instances every file shares in
 * {@link ConfigFormats} ({@code TOML}, {@code JSON}, {@code JSON5}, {@code YAML}), or {@code NbtConfigFormat.INSTANCE}
 * for NBT ({@link NbtConfigFormat}), the game's own format, kept apart so a class naming the others can be used where
 * the game is not loaded. Every format is an {@link IConfigFormat} and carries the same plain values
 * ({@link PlainValues}): maps with text keys in their order, lists, text, numbers and booleans; what a setting means is
 * not the format's business. TOML, JSON5 and YAML keep their comments; JSON and NBT have none, and NBT is for settings
 * nobody edits by hand. A file that is not of its format fails with a {@link ConfigFormatException} saying where it
 * stops being one. YAML is read with the safe constructor only, which never builds a class a file names, and held to a
 * few aliases, a shallow nesting and a small size.
 *
 * <p>Which format to pick, and what each keeps, is in {@code core/docs/SETTINGS.md}. Examples that compile are J's
 * Example's settings, written in TOML ({@code example/src/main/java/dev/jstech/example/ExampleSettings.java}), and the
 * Core's own audio settings, a player's file in JSON
 * ({@code core/src/main/java/dev/jstech/core/audio/AudioSettings.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.config.format;

import dev.jstech.core.config.ConfigFile;
import org.jetbrains.annotations.ApiStatus;
