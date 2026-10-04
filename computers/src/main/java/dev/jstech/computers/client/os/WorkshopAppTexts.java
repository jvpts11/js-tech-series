/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** The words of the Workshop window, kept apart from it so a server can read them without the window. */
@TextHolder
final class WorkshopAppTexts {

    static final TextKey TITLE = TextKey.of("jsc.workshop_app.title", "Workshop");
    static final TextKey TAB_CRAFTING = TextKey.of("jsc.workshop_app.tab_crafting", "Crafting");
    static final TextKey TAB_FURNACE = TextKey.of("jsc.workshop_app.tab_furnace", "Furnace");
    static final TextKey TAB_ENCHANTING = TextKey.of("jsc.workshop_app.tab_enchanting", "Enchanting");
    static final TextKey TAB_ANVIL = TextKey.of("jsc.workshop_app.tab_anvil", "Anvil");
    static final TextKey NO_CARD = TextKey.of("jsc.workshop_app.no_card", "no card");
    static final TextKey INVENTORY = TextKey.of("jsc.workshop_app.inventory", "INVENTORY");
    static final TextKey LOADING = TextKey.of("jsc.workshop_app.loading", "Loading...");
    static final TextKey NEEDS_CARD = TextKey.of("jsc.workshop_app.needs_card",
            "Put the %s in this computer to use it.");

    static final TextKey CRAFTING_TABLE_CARD = TextKey.of("jsc.workshop_app.crafting_table_card",
            "Crafting Table Card");
    static final TextKey FURNACE_CARD = TextKey.of("jsc.workshop_app.furnace_card", "Furnace Card");
    static final TextKey ENCHANTING_CARD = TextKey.of("jsc.workshop_app.enchanting_card", "Enchanting Card");
    static final TextKey ANVIL_CARD = TextKey.of("jsc.workshop_app.anvil_card", "Anvil Card");

    static final TextKey CRAFTING_TABLE = TextKey.of("jsc.workshop_app.crafting_table", "Crafting Table");
    static final TextKey RECIPE = TextKey.of("jsc.workshop_app.recipe", "RECIPE");
    static final TextKey EMPTY_GRID = TextKey.of("jsc.workshop_app.empty_grid", "Put a recipe in the grid");
    static final TextKey NO_RESULT = TextKey.of("jsc.workshop_app.no_result", "The grid makes nothing");
    static final TextKey IN_INVENTORY = TextKey.of("jsc.workshop_app.in_inventory", "IN YOUR INVENTORY");
    static final TextKey ENOUGH_FOR = TextKey.of("jsc.workshop_app.enough_for", "enough for %s more");
    static final TextKey ENOUGH_FOR_NONE = TextKey.of("jsc.workshop_app.enough_for_none", "not enough for another");
    static final TextKey INGREDIENT = TextKey.of("jsc.workshop_app.ingredient", "%s %s");
    static final TextKey CRAFT = TextKey.of("jsc.workshop_app.craft", "Craft");
    static final TextKey CRAFT_ALL = TextKey.of("jsc.workshop_app.craft_all", "Craft all (%s)");
    static final TextKey CLEAR_GRID = TextKey.of("jsc.workshop_app.clear_grid", "Clear grid");

    static final TextKey FURNACE = TextKey.of("jsc.workshop_app.furnace", "Furnace");
    static final TextKey NO_FUEL = TextKey.of("jsc.workshop_app.no_fuel", "No fuel");
    static final TextKey RUNS_ON_COMPUTER = TextKey.of("jsc.workshop_app.runs_on_computer", "runs on the computer");
    static final TextKey SMELTING = TextKey.of("jsc.workshop_app.smelting", "SMELTING");
    static final TextKey SMELTING_LINE = TextKey.of("jsc.workshop_app.smelting_line", "%s into %s: %s left, %s done");
    static final TextKey NOTHING_SMELTING = TextKey.of("jsc.workshop_app.nothing_smelting",
            "Nothing in the furnace");
    static final TextKey SPEED = TextKey.of("jsc.workshop_app.speed", "SPEED");
    static final TextKey SPEED_LINE = TextKey.of("jsc.workshop_app.speed_line",
            "%sx a furnace: %s s each, all done in %s");
    static final TextKey SPEED_ONLY = TextKey.of("jsc.workshop_app.speed_only", "%sx a furnace");
    static final TextKey WINDOW_CLOSED = TextKey.of("jsc.workshop_app.window_closed", "WITH THE WINDOW CLOSED");
    static final TextKey WINDOW_CLOSED_NOTE = TextKey.of("jsc.workshop_app.window_closed_note",
            "Keeps smelting while the computer is on.");

    static final TextKey NO_LAPIS = TextKey.of("jsc.workshop_app.no_lapis", "No lapis");
    static final TextKey NEEDS_NONE = TextKey.of("jsc.workshop_app.needs_none", "the card needs none");
    static final TextKey YOUR_EXPERIENCE = TextKey.of("jsc.workshop_app.your_experience", "YOUR EXPERIENCE");
    static final TextKey THE_OFFERS = TextKey.of("jsc.workshop_app.the_offers", "THE OFFERS");
    static final TextKey OFFERS_NOTE = TextKey.of("jsc.workshop_app.offers_note",
            "As a table with every bookshelf around it.");
    static final TextKey WHAT_IT_TAKES = TextKey.of("jsc.workshop_app.what_it_takes", "WHAT IT TAKES");
    static final TextKey ENCHANT_COST = TextKey.of("jsc.workshop_app.enchant_cost", "1, 1 and 2 levels, no lapis");
    static final TextKey ENCHANT_WAS = TextKey.of("jsc.workshop_app.enchant_was",
            "a table: 1, 2 and 3 levels, and as many lapis");
    static final TextKey CLUE = TextKey.of("jsc.workshop_app.clue", "%s . . . ?");
    static final TextKey ONE_LEVEL = TextKey.of("jsc.workshop_app.one_level", "1 level");
    static final TextKey LEVELS = TextKey.of("jsc.workshop_app.levels", "%s levels");

    static final TextKey NAME = TextKey.of("jsc.workshop_app.name", "Name");
    static final TextKey REPAIR_RENAME = TextKey.of("jsc.workshop_app.repair_rename", "REPAIR AND RENAME");
    static final TextKey ANVIL_EMPTY = TextKey.of("jsc.workshop_app.anvil_empty", "Put an item on the anvil");
    static final TextKey ANVIL_NOTHING = TextKey.of("jsc.workshop_app.anvil_nothing", "These two make nothing");
    static final TextKey ANVIL_WITH = TextKey.of("jsc.workshop_app.anvil_with", "%s, with %s");
    static final TextKey ANVIL_WAS = TextKey.of("jsc.workshop_app.anvil_was", "an anvil: %s levels");
    static final TextKey TAKE_IT = TextKey.of("jsc.workshop_app.take_it", "Take it");

    static final TextKey CARDS = TextKey.of("jsc.workshop_app.cards", "Cards: %s");
    static final TextKey NO_CARDS = TextKey.of("jsc.workshop_app.no_cards", "No personal-use cards");
    static final TextKey FURNACE_SMELTING = TextKey.of("jsc.workshop_app.furnace_smelting",
            "Furnace: smelting, %sx");
    static final TextKey FURNACE_LEFT = TextKey.of("jsc.workshop_app.furnace_left", "Furnace: %s %s left");
    static final TextKey FURNACE_IDLE = TextKey.of("jsc.workshop_app.furnace_idle", "Furnace: idle");
    static final TextKey LEVEL = TextKey.of("jsc.workshop_app.level", "Level %s");
    static final TextKey ENCHANTING = TextKey.of("jsc.workshop_app.enchanting", "Enchanting");
    static final TextKey ANVIL = TextKey.of("jsc.workshop_app.anvil", "Anvil");

    private WorkshopAppTexts() {
    }
}
