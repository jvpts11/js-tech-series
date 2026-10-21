/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Settings files for any mod: each setting declared once, written through a codec and held to a range or a list of
 * words, in a file of one of the formats of the {@code format} package (TOML, JSON, JSON5, YAML and NBT) with its
 * comment above it, put where its side reads it, changed on the Core's settings screen or by an operator from their own
 * game, and kept the same on every player's game when it is a world's.
 *
 * <p>A setting is a {@link ConfigKey}, made by its kind ({@code flag}, {@code whole}, {@code number}, {@code text},
 * {@code colour}, {@code ids}, or {@code of} with any codec) and kept as a constant. A number outside its range is
 * pulled to the nearer end, and a value that cannot be read, or a word outside its list, takes the default, so a slip
 * in a file costs one setting its value. A file is a {@link ConfigFile}, built with its name, its {@link ConfigSide}
 * (the player's own, every game's, or each world's) and its format, and holding its settings, the rules between them
 * ({@link ConfigRule}) and the presets it ships ({@link ConfigPreset}); its version and its upgrade steps
 * ({@link IConfigUpgrade}) bring an older file up to today's layout, and a file a newer mod wrote is read as far as it
 * can be and never written over. The mod registers the file from its constructor with {@link ConfigFiles}, reads a
 * value with {@code FILE.get(KEY)}, and is told of each change with {@code whenChanged}. A world's settings reach every
 * player as they join and whenever one changes ({@link ConfigSync}), and an operator changes them on the settings
 * screen or with {@code /jstech config}; the presets people save, and settings written out as text to share, are
 * {@link ConfigPresets}.
 *
 * <p>How to declare settings, from zero, is in {@code core/docs/SETTINGS.md}. An example that compiles is J's Example's
 * settings file ({@code example/src/main/java/dev/jstech/example/ExampleSettings.java}), registered from the mod's
 * constructor in {@code JsExample}.
 */
@ApiStatus.Experimental
package dev.jstech.core.config;

import org.jetbrains.annotations.ApiStatus;
