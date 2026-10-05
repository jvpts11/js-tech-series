/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What the Core's settings screen says around a mod's settings. */
@TextHolder
public final class ConfigScreenTexts {

    /** The screen's title: the mod's name. */
    public static final TextKey TITLE = TextKey.of("jscore.config_screen.title", "%s settings");
    /** The headings of a mod's files in the rail, by whose settings they hold. */
    public static final TextKey WORLD = TextKey.of("jscore.config_screen.world", "WORLD");
    public static final TextKey PLAYER = TextKey.of("jscore.config_screen.player", "PLAYER");
    public static final TextKey EVERY_GAME = TextKey.of("jscore.config_screen.every_game", "EVERY GAME");
    /** The settings of a file that sit in no section. */
    public static final TextKey GENERAL = TextKey.of("jscore.config_screen.general", "General");
    public static final TextKey DEFAULTS = TextKey.of("jscore.config_screen.defaults", "Defaults");
    public static final TextKey CANCEL = TextKey.of("jscore.config_screen.cancel", "Cancel");
    public static final TextKey DONE = TextKey.of("jscore.config_screen.done", "Done");
    /** Why a world's settings cannot be changed from here. */
    public static final TextKey WORLD_ONLY = TextKey.of("jscore.config_screen.world_only",
            "A world's settings are changed from inside it, on the game that runs it.");
    /** A setting that holds a list or a map, which its file changes. */
    public static final TextKey IN_FILE = TextKey.of("jscore.config_screen.in_file", "Changed in its file");

    private ConfigScreenTexts() {
    }
}
