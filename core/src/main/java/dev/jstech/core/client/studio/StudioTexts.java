/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What the studio says. */
@TextHolder
public final class StudioTexts {

    public static final TextKey STUDIO = TextKey.of("gui.jscore.studio", "Studio");
    public static final TextKey SEARCH = TextKey.of("gui.jscore.studio.search", "Find a block, item, fluid...");
    /* How many things have problems. */
    public static final TextKey CHECKS = TextKey.of("gui.jscore.studio.checks", "Checks: %s");
    /* A content pack's namespace. */
    public static final TextKey PACK = TextKey.of("gui.jscore.studio.pack", "Pack: %s");
    public static final TextKey ON_THE_BENCH = TextKey.of("gui.jscore.studio.bench", "On the bench");
    public static final TextKey WORKING = TextKey.of("gui.jscore.studio.bench.working", "Working");
    public static final TextKey IDLE = TextKey.of("gui.jscore.studio.bench.idle", "Idle");
    public static final TextKey JAMMED = TextKey.of("gui.jscore.studio.bench.jammed", "Jammed");
    public static final TextKey BENCH_HINT = TextKey.of("gui.jscore.studio.bench.hint",
            "Drag: walk around it    Wheel: closer");
    public static final TextKey NOTHING_CHOSEN = TextKey.of("gui.jscore.studio.nothing_chosen",
            "Choose a thing on the left");
    public static final TextKey NOTHING_TO_SHOW = TextKey.of("gui.jscore.studio.nothing_to_show",
            "Nothing of it stands in a world");
    public static final TextKey ACROSS = TextKey.of("gui.jscore.studio.across", "Checks across its parts");
    public static final TextKey NOTHING_WRONG = TextKey.of("gui.jscore.studio.nothing_wrong",
            "Nothing is wrong across its parts");
    public static final TextKey MADE_OF = TextKey.of("gui.jscore.studio.made_of", "Made of");
    public static final TextKey OPEN = TextKey.of("gui.jscore.studio.open", "Open");
    public static final TextKey RELOAD_ALL = TextKey.of("gui.jscore.studio.reload_all", "Reload all");
    public static final TextKey RUN_TESTS = TextKey.of("gui.jscore.studio.run_tests", "Run its tests");
    public static final TextKey RECORD = TextKey.of("gui.jscore.studio.record", "Record a test");
    public static final TextKey RECORDING = TextKey.of("gui.jscore.studio.recording",
            "Recording: every click on a screen is kept");
    public static final TextKey DONE = TextKey.of("gui.jscore.studio.done", "Done");
    public static final TextKey MODE_ON = TextKey.of("gui.jscore.studio.mode_on", "Developer mode is on");
    /* The port, and how many clients. */
    public static final TextKey MCP_LINE = TextKey.of("gui.jscore.studio.mcp_line",
            "MCP server on 127.0.0.1:%s, %s clients");
    /* The file, and where it was looked for. */
    public static final TextKey NOT_OPENED = TextKey.of("gui.jscore.studio.not_opened",
            "%s is not among the mod's sources in this run: %s");
    /* The file. */
    public static final TextKey OPENED = TextKey.of("gui.jscore.studio.opened", "Opened %s");
    public static final TextKey RELOADING = TextKey.of("gui.jscore.studio.reloading",
            "Reloading the resources, and the data when the world is yours");
    /* The thing. */
    public static final TextKey TESTS_ASKED = TextKey.of("gui.jscore.studio.tests_asked",
            "Asked the server to run the GameTests of %s near you");

    public static final TextKey MODEL = TextKey.of("gui.jscore.studio.part.model", "Model");
    public static final TextKey SCREEN = TextKey.of("gui.jscore.studio.part.screen", "Screen");
    public static final TextKey EFFECTS = TextKey.of("gui.jscore.studio.part.effects", "Effects");
    public static final TextKey SOUNDS = TextKey.of("gui.jscore.studio.part.sounds", "Sounds");
    public static final TextKey NAMES = TextKey.of("gui.jscore.studio.part.names", "Names");
    public static final TextKey RECIPE = TextKey.of("gui.jscore.studio.part.recipe", "Recipe");
    public static final TextKey MANUAL = TextKey.of("gui.jscore.studio.part.manual", "Manual page");
    public static final TextKey TAGS = TextKey.of("gui.jscore.studio.part.tags", "Tags");
    public static final TextKey DROPS = TextKey.of("gui.jscore.studio.part.drops", "Drops");
    public static final TextKey SETTINGS = TextKey.of("gui.jscore.studio.part.settings", "Settings");
    public static final TextKey TESTS = TextKey.of("gui.jscore.studio.part.tests", "Tests");
    public static final TextKey NONE = TextKey.of("gui.jscore.studio.part.none", "none");
    /* How many recipes, and the first's id. */
    public static final TextKey RECIPES_COUNT = TextKey.of("gui.jscore.studio.part.recipes", "%s, the first %s");
    /* How many recipes. */
    public static final TextKey RECIPES_NEED = TextKey.of("gui.jscore.studio.part.recipes_need",
            "%s recipes need it");
    /* How many GameTests. */
    public static final TextKey TESTS_COUNT = TextKey.of("gui.jscore.studio.part.tests_count", "%s GameTests");
    /* How many settings, and their files. */
    public static final TextKey SETTINGS_COUNT = TextKey.of("gui.jscore.studio.part.settings_count", "%s, in %s");
    /* The first one, and how many more. */
    public static final TextKey AND_MORE = TextKey.of("gui.jscore.studio.part.and_more", "%s and %s more");
    public static final TextKey DROPS_SELF = TextKey.of("gui.jscore.studio.part.drops.self", "itself");
    public static final TextKey DROPS_KEEPING = TextKey.of("gui.jscore.studio.part.drops.keeping",
            "itself, keeping what it holds");
    public static final TextKey DROPS_NOTHING = TextKey.of("gui.jscore.studio.part.drops.nothing",
            "nothing from a table");
    public static final TextKey DROPS_HANDMADE = TextKey.of("gui.jscore.studio.part.drops.handmade",
            "its own loot table");

    /* The language. */
    public static final TextKey NO_NAME_IN = TextKey.of("gui.jscore.studio.check.no_name", "No name in %s");
    /* The key, and the languages it is in. */
    public static final TextKey ONLY_IN = TextKey.of("gui.jscore.studio.check.only_in", "%s is in %s only");
    /* The key. */
    public static final TextKey NAMED_NOWHERE = TextKey.of("gui.jscore.studio.check.named_nowhere",
            "%s is in no language file of the mod");
    public static final TextKey NO_LANGUAGES = TextKey.of("gui.jscore.studio.check.no_languages",
            "The mod has no language file");
    /* The file. */
    public static final TextKey NO_FILE = TextKey.of("gui.jscore.studio.check.no_file", "No file %s");
    public static final TextKey NO_FILE_DETAIL = TextKey.of("gui.jscore.studio.check.no_file.detail",
            "The game looks for it there and does not find it");
    public static final TextKey NO_MODEL = TextKey.of("gui.jscore.studio.check.no_model",
            "The game has no model for it");
    /* The state or the item. */
    public static final TextKey NO_MODEL_DETAIL = TextKey.of("gui.jscore.studio.check.no_model.detail",
            "%s is drawn as the missing model");
    public static final TextKey NO_TEXTURE = TextKey.of("gui.jscore.studio.check.no_texture",
            "Its model names a texture that is not there");
    /* The model. */
    public static final TextKey NO_TEXTURE_DETAIL = TextKey.of("gui.jscore.studio.check.no_texture.detail",
            "%s is drawn with the missing texture");
    public static final TextKey MISSING_BONE = TextKey.of("gui.jscore.studio.check.missing_bone",
            "An animation moves a missing bone");
    /* The animation, the bone and the mod. */
    public static final TextKey MISSING_BONE_DETAIL = TextKey.of("gui.jscore.studio.check.missing_bone.detail",
            "\"%s\" moves %s, a bone no model of %s has");
    public static final TextKey SCREEN_BROKEN = TextKey.of("gui.jscore.studio.check.screen_broken",
            "Its screen file does not load");
    public static final TextKey SLOTS_BEYOND = TextKey.of("gui.jscore.studio.check.slots_beyond",
            "Its screen shows slots its menu does not have");
    /* The slots, and how many the menu has. */
    public static final TextKey SLOTS_BEYOND_DETAIL = TextKey.of("gui.jscore.studio.check.slots_beyond.detail",
            "The screen shows slots %s; the menu has %s");
    public static final TextKey SLOTS_NOWHERE = TextKey.of("gui.jscore.studio.check.slots_nowhere",
            "Its screen leaves out slots of its menu");
    /* The slots. */
    public static final TextKey SLOTS_NOWHERE_DETAIL = TextKey.of("gui.jscore.studio.check.slots_nowhere.detail",
            "The menu's slots %s are shown nowhere");
    public static final TextKey SLOTS_TWICE = TextKey.of("gui.jscore.studio.check.slots_twice",
            "Its screen shows slots twice");
    /* The slots. */
    public static final TextKey SLOTS_TWICE_DETAIL = TextKey.of("gui.jscore.studio.check.slots_twice.detail",
            "The slots %s are shown more than once");
    public static final TextKey SLOTS_NO_INVENTORY = TextKey.of("gui.jscore.studio.check.slots_no_inventory",
            "Its screen leaves out the player's inventory");
    /* How many of the menu's slots are the player's. */
    public static final TextKey SLOTS_NO_INVENTORY_DETAIL = TextKey.of(
            "gui.jscore.studio.check.slots_no_inventory.detail",
            "The menu holds the player's %s slots, and the screen has no inventory to show them");
    public static final TextKey IN_NO_TAB = TextKey.of("gui.jscore.studio.check.no_tab", "In no creative tab");
    /* The item. */
    public static final TextKey IN_NO_TAB_DETAIL = TextKey.of("gui.jscore.studio.check.no_tab.detail",
            "%s is in none of the tabs a player in creative sees");
    public static final TextKey NO_LOOT = TextKey.of("gui.jscore.studio.check.no_loot",
            "Breaking it gives nothing");
    /* The file. */
    public static final TextKey NO_LOOT_DETAIL = TextKey.of("gui.jscore.studio.check.no_loot.detail",
            "It is said to drop by a table, and %s is not there");
    public static final TextKey READER_FAILED = TextKey.of("gui.jscore.studio.check.reader_failed",
            "A part could not be read");
    /* The reader, and what went wrong. */
    public static final TextKey READER_FAILED_DETAIL = TextKey.of("gui.jscore.studio.check.reader_failed.detail",
            "%s: %s");

    private StudioTexts() {
    }
}
