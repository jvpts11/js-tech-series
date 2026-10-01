/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config.format;

/**
 * The formats a settings file can be written in, each one instance shared by every file of it.
 *
 * <p>NBT is not among them: it is the game's own format, and a class naming only these can be used where the game is
 * not loaded (by a tool, or a test). It is {@link NbtConfigFormat#INSTANCE}.
 */
public final class ConfigFormats {

    /** TOML, the format NeoForge's own settings are in; the only one its settings screen shows. */
    public static final IConfigFormat TOML = new TomlConfigFormat();
    /** Plain JSON, with no comments. */
    public static final IConfigFormat JSON = new JsonConfigFormat();
    /** JSON5, the JSON a person writes by hand, with comments. */
    public static final IConfigFormat JSON5 = new Json5ConfigFormat();
    /** YAML, with comments. */
    public static final IConfigFormat YAML = new YamlConfigFormat();

    private ConfigFormats() {
    }
}
