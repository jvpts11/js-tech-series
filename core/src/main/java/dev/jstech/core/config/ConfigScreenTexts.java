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

    /** The screen's title, the mod's name over what the screen is, which the narrator reads. */
    public static final TextKey TITLE = TextKey.of("jscore.config_screen.title", "%s settings");
    /** The tabs of a mod's files, by whose settings they hold. */
    public static final TextKey WORLD = TextKey.of("jscore.config_screen.world", "World");
    public static final TextKey PLAYER = TextKey.of("jscore.config_screen.player", "This player");
    public static final TextKey EVERY_GAME = TextKey.of("jscore.config_screen.every_game", "Every game");
    /** The settings of a file that sit in no section. */
    public static final TextKey GENERAL = TextKey.of("jscore.config_screen.general", "General");
    /** The row over the settings: the search, the filter of what changed, the presets. */
    public static final TextKey SEARCH = TextKey.of("jscore.config_screen.search", "Search settings...");
    public static final TextKey ONLY_CHANGED = TextKey.of("jscore.config_screen.only_changed", "Only changed");
    public static final TextKey ONLY_CHANGED_TIP = TextKey.of("jscore.config_screen.only_changed_tip",
            "Show only the settings that differ from their defaults");
    public static final TextKey PRESETS = TextKey.of("jscore.config_screen.presets", "Presets...");
    public static final TextKey NO_RESULTS = TextKey.of("jscore.config_screen.no_results",
            "No setting's name or description has these words.");
    public static final TextKey NONE_CHANGED = TextKey.of("jscore.config_screen.none_changed",
            "Every setting here is at its default.");
    /** How many settings a section or a group holds. */
    public static final TextKey ONE_SETTING = TextKey.of("jscore.config_screen.one_setting", "1 setting");
    public static final TextKey SETTINGS = TextKey.of("jscore.config_screen.settings", "%s settings");
    /** A switch's two states, on its button. */
    public static final TextKey ON = TextKey.of("jscore.config_screen.on", "ON");
    public static final TextKey OFF = TextKey.of("jscore.config_screen.off", "OFF");
    /** When a change takes hold, beside the setting's name. */
    public static final TextKey NEEDS_RESTART = TextKey.of("jscore.config_screen.needs_restart", "needs a restart");
    public static final TextKey ON_RELOAD = TextKey.of("jscore.config_screen.on_reload", "on reload");
    /** The setting another is active under: its name, and the words it must be. */
    public static final TextKey ACTIVE_WHEN_ON = TextKey.of("jscore.config_screen.active_when_on",
            "Active when %s is on");
    public static final TextKey ACTIVE_WHEN = TextKey.of("jscore.config_screen.active_when", "Active when %s is %s");
    /** A setting's default, and the bounds of a number. */
    public static final TextKey DEFAULT_IS = TextKey.of("jscore.config_screen.default_is", "Default: %s");
    public static final TextKey DEFAULT_IN_RANGE = TextKey.of("jscore.config_screen.default_in_range",
            "Default: %s, from %s to %s");
    /** A setting that holds a record of its own, which its file changes. */
    public static final TextKey IN_FILE = TextKey.of("jscore.config_screen.in_file", "Changed in its file");
    /** The editors of lists and maps. */
    public static final TextKey ADD = TextKey.of("jscore.config_screen.add", "+ Add");
    public static final TextKey NEW_ENTRY = TextKey.of("jscore.config_screen.new_entry", "New entry");
    public static final TextKey NEW_NAME = TextKey.of("jscore.config_screen.new_name", "Name");
    public static final TextKey NEW_VALUE = TextKey.of("jscore.config_screen.new_value", "Value");
    public static final TextKey REMOVE = TextKey.of("jscore.config_screen.remove", "Take it out");
    public static final TextKey TO_DEFAULT = TextKey.of("jscore.config_screen.to_default", "Back to its default");
    public static final TextKey NOT_TAKEN = TextKey.of("jscore.config_screen.not_taken",
            "That is not a value this setting takes");
    /** The footer's buttons. */
    public static final TextKey DEFAULTS = TextKey.of("jscore.config_screen.defaults", "Defaults");
    public static final TextKey DEFAULTS_TIP = TextKey.of("jscore.config_screen.defaults_tip",
            "Put every setting of this section back to its default");
    public static final TextKey UNDO = TextKey.of("jscore.config_screen.undo", "Undo");
    public static final TextKey REDO = TextKey.of("jscore.config_screen.redo", "Redo");
    public static final TextKey EXPORT = TextKey.of("jscore.config_screen.export", "Export");
    public static final TextKey IMPORT = TextKey.of("jscore.config_screen.import", "Import");
    public static final TextKey DONE = TextKey.of("jscore.config_screen.done", "Done");
    public static final TextKey CANCEL = TextKey.of("jscore.config_screen.cancel", "Cancel");
    /** How many changes wait to be kept, and how many of them a restart. */
    public static final TextKey NOTHING_CHANGED = TextKey.of("jscore.config_screen.nothing_changed",
            "Nothing changed");
    public static final TextKey CHANGED = TextKey.of("jscore.config_screen.changed", "%s changed");
    public static final TextKey CHANGED_ON_RESTART = TextKey.of("jscore.config_screen.changed_on_restart",
            "%s changed, %s on restart");
    public static final TextKey RESTART_DUE = TextKey.of("jscore.config_screen.restart_due",
            "%s kept changes take hold when the game starts again");
    /** The banner under the tabs. */
    public static final TextKey OPERATOR = TextKey.of("jscore.config_screen.operator",
            "The server's settings: as an operator, what you change reaches every player.");
    public static final TextKey SERVER_READ_ONLY = TextKey.of("jscore.config_screen.server_read_only",
            "The server's settings, as it holds them. Only an operator may change them.");
    public static final TextKey WORLD_ONLY = TextKey.of("jscore.config_screen.world_only",
            "These are the world's settings. Open them from inside the world to change them.");
    /** The presets. */
    public static final TextKey PRESETS_TITLE = TextKey.of("jscore.config_screen.presets_title", "Presets");
    public static final TextKey FROM_THE_MOD = TextKey.of("jscore.config_screen.from_the_mod", "From the mod");
    public static final TextKey SAVED_HERE = TextKey.of("jscore.config_screen.saved_here", "Saved on this game");
    public static final TextKey NO_PRESETS = TextKey.of("jscore.config_screen.no_presets",
            "No presets yet: save these settings as one.");
    public static final TextKey PRESET_NAME = TextKey.of("jscore.config_screen.preset_name", "New preset's name");
    public static final TextKey SAVE_PRESET = TextKey.of("jscore.config_screen.save_preset", "Save these");
    public static final TextKey PICK = TextKey.of("jscore.config_screen.pick", "Pick");
    public static final TextKey DELETE = TextKey.of("jscore.config_screen.delete", "Delete");
    public static final TextKey BAD_NAME = TextKey.of("jscore.config_screen.bad_name",
            "A preset is named with letters, digits, spaces and dashes");
    public static final TextKey PRESET_SAVED = TextKey.of("jscore.config_screen.preset_saved", "Saved as %s");
    public static final TextKey PRESET_TAKEN = TextKey.of("jscore.config_screen.preset_taken",
            "%s: %s settings set; keep them with Done");
    /** Export and import as text. */
    public static final TextKey EXPORT_TITLE = TextKey.of("jscore.config_screen.export_title",
            "These settings as text, to share");
    public static final TextKey IMPORT_TITLE = TextKey.of("jscore.config_screen.import_title",
            "Paste settings shared as text");
    public static final TextKey COPY = TextKey.of("jscore.config_screen.copy", "Copy");
    public static final TextKey COPIED = TextKey.of("jscore.config_screen.copied", "Copied");
    public static final TextKey PASTE = TextKey.of("jscore.config_screen.paste", "Paste");
    public static final TextKey TAKE = TextKey.of("jscore.config_screen.take", "Take them");
    public static final TextKey IMPORTED = TextKey.of("jscore.config_screen.imported",
            "%s settings taken; keep them with Done");
    public static final TextKey IMPORT_FAILED = TextKey.of("jscore.config_screen.import_failed",
            "This text is not settings: %s");
    /** The picker of a game's thing. */
    public static final TextKey CHOOSE_ITEM = TextKey.of("jscore.config_screen.choose_item", "Choose an item");
    public static final TextKey CHOOSE_BLOCK = TextKey.of("jscore.config_screen.choose_block", "Choose a block");
    public static final TextKey CHOOSE_FLUID = TextKey.of("jscore.config_screen.choose_fluid", "Choose a fluid");
    public static final TextKey MATCHES = TextKey.of("jscore.config_screen.matches", "%s match \"%s\"");
    public static final TextKey TO_CHOOSE_FROM = TextKey.of("jscore.config_screen.to_choose_from",
            "%s to choose from");
    public static final TextKey CHOOSE = TextKey.of("jscore.config_screen.choose", "Choose");
    /** The link at the end of a description. */
    public static final TextKey READ_MANUAL = TextKey.of("jscore.config_screen.read_manual", "Read it in the manual");

    private ConfigScreenTexts() {
    }
}
