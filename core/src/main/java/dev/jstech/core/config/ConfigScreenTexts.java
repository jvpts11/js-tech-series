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

    /** The screen's title, the mod's name over what the screen is and the mod's version. */
    public static final TextKey TITLE = TextKey.of("jscore.config_screen.title", "%s settings");
    public static final TextKey SUBTITLE = TextKey.of("jscore.config_screen.subtitle", "Settings · version %s");
    /** The tabs of a mod's files, by whose settings they hold, and where each kind is kept. */
    public static final TextKey WORLD = TextKey.of("jscore.config_screen.world", "World");
    public static final TextKey PLAYER = TextKey.of("jscore.config_screen.player", "This player");
    public static final TextKey EVERY_GAME = TextKey.of("jscore.config_screen.every_game", "Every game");
    public static final TextKey WORLD_NOTE = TextKey.of("jscore.config_screen.world_note",
            "Saved with the world. Every player in it has the same.");
    public static final TextKey PLAYER_NOTE = TextKey.of("jscore.config_screen.player_note",
            "Saved on this game. Only you see these.");
    public static final TextKey EVERY_GAME_NOTE = TextKey.of("jscore.config_screen.every_game_note",
            "Saved on this game, for every world it plays.");
    /** The settings of a file that sit in no section. */
    public static final TextKey GENERAL = TextKey.of("jscore.config_screen.general", "General");
    /** The box that finds settings by their names and what they do, and what it shows. */
    public static final TextKey SEARCH = TextKey.of("jscore.config_screen.search", "Search settings");
    public static final TextKey RESULTS = TextKey.of("jscore.config_screen.results", "Settings matching \"%s\"");
    public static final TextKey NO_RESULTS = TextKey.of("jscore.config_screen.no_results",
            "No setting's name or description has these words.");
    /** A setting's line under its description: its default, and the bounds of a number. */
    public static final TextKey DEFAULT_IS = TextKey.of("jscore.config_screen.default_is", "Default: %s");
    public static final TextKey DEFAULT_IN_RANGE = TextKey.of("jscore.config_screen.default_in_range",
            "Default %s · from %s to %s");
    /** A switch's two states, beside it. */
    public static final TextKey ON = TextKey.of("jscore.config_screen.on", "On");
    public static final TextKey OFF = TextKey.of("jscore.config_screen.off", "Off");
    /** The mark of a setting changed and not saved, and the button that puts one back to its default. */
    public static final TextKey CHANGED = TextKey.of("jscore.config_screen.changed", "CHANGED");
    public static final TextKey TO_DEFAULT = TextKey.of("jscore.config_screen.to_default", "Default");
    public static final TextKey SECTION_DEFAULTS = TextKey.of("jscore.config_screen.section_defaults",
            "Section defaults");
    public static final TextKey CANCEL = TextKey.of("jscore.config_screen.cancel", "Cancel");
    public static final TextKey SAVE = TextKey.of("jscore.config_screen.save", "Save");
    /** How many changes wait to be saved. */
    public static final TextKey NOTHING_CHANGED = TextKey.of("jscore.config_screen.nothing_changed",
            "Nothing changed");
    public static final TextKey ONE_CHANGE = TextKey.of("jscore.config_screen.one_change", "1 change not saved");
    public static final TextKey CHANGES = TextKey.of("jscore.config_screen.changes", "%s changes not saved");
    /** Why a world's settings cannot be changed from here. */
    public static final TextKey WORLD_ONLY = TextKey.of("jscore.config_screen.world_only",
            "These are the world's settings. Open them from inside the world to change them.");
    /** A setting that holds a list or a map, which its file changes. */
    public static final TextKey IN_FILE = TextKey.of("jscore.config_screen.in_file", "Changed in its file");

    private ConfigScreenTexts() {
    }
}
