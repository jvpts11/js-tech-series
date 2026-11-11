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

/**
 * What the Craft Planner's window says, kept apart from the window so the language generator can read it on a
 * server too, where windows do not exist.
 */
@TextHolder
final class CraftPlannerTexts {

    static final TextKey SEARCH_ITEM = TextKey.of("jsc.craft_planner.search_item", "search item...");
    static final TextKey LOADING = TextKey.of("jsc.craft_planner.loading", "loading...");
    static final TextKey NO_MATCH = TextKey.of("jsc.craft_planner.no_match", "no match");
    static final TextKey PICK_AN_ITEM = TextKey.of("jsc.craft_planner.pick_an_item", "Pick an item to plan.");
    static final TextKey PLANNING = TextKey.of("jsc.craft_planner.planning", "planning...");
    static final TextKey NOT_CRAFTABLE =
            TextKey.of("jsc.craft_planner.not_craftable", "No pattern on the network makes this.");
    static final TextKey CRAFTABLE = TextKey.of("jsc.craft_planner.craftable", "Craftable");
    static final TextKey PARTIAL = TextKey.of("jsc.craft_planner.partial", "Partial");
    static final TextKey SUMMARY = TextKey.of("jsc.craft_planner.summary", "max %s  -  %s stages");
    static final TextKey STEPS = TextKey.of("jsc.craft_planner.steps", "Steps");
    static final TextKey TREE = TextKey.of("jsc.craft_planner.tree", "Tree");
    static final TextKey STAGES = TextKey.of("jsc.craft_planner.stages", "STAGES");
    static final TextKey INGREDIENTS = TextKey.of("jsc.craft_planner.ingredients", "INGREDIENTS");
    static final TextKey CRAFT_TREE = TextKey.of("jsc.craft_planner.craft_tree", "CRAFT TREE");
    static final TextKey WHEEL_TO_SCROLL = TextKey.of("jsc.craft_planner.wheel_to_scroll", "wheel to scroll");
    static final TextKey CRAFT = TextKey.of("jsc.craft_planner.craft", "Craft %s");
    static final TextKey MACHINE_RUNS = TextKey.of("jsc.craft_planner.machine_runs", "machine x%s");
    static final TextKey BENCH_RUNS = TextKey.of("jsc.craft_planner.bench_runs", "bench x%s");
    static final TextKey HAVE = TextKey.of("jsc.craft_planner.have", "have %s");
    /* An ingredient the network lacks: what the plan needs of it and what the network holds. */
    static final TextKey NEED_HAVE = TextKey.of("jsc.craft_planner.need_have", "need %s, have %s");
    /* The pill of a plan the network lacks ingredients for, and how many of them it lacks. */
    static final TextKey MISSING = TextKey.of("jsc.craft_planner.missing", "Missing %s");
    static final TextKey ONE_SHORT = TextKey.of("jsc.craft_planner.one_short", "%s short");
    static final TextKey MANY_SHORT = TextKey.of("jsc.craft_planner.many_short", "%s and %s more short");
    /* The plan on paper: its title, the target, a stage by its number, the columns and what it still lacks. */
    static final TextKey PRINT_TITLE = TextKey.of("jsc.craft_planner.print.title", "Craft plan");
    static final TextKey PRINT_TARGET = TextKey.of("jsc.craft_planner.print.target", "%s x %s");
    static final TextKey PRINT_STAGE = TextKey.of("jsc.craft_planner.print.stage", "%s. %s");
    static final TextKey PRINT_NEED = TextKey.of("jsc.craft_planner.print.need", "NEED");
    static final TextKey PRINT_HAVE = TextKey.of("jsc.craft_planner.print.have", "HAVE");
    static final TextKey PRINT_MISSING = TextKey.of("jsc.craft_planner.print.missing", "MISSING");
    static final TextKey PRINTED_FROM = TextKey.of("jsc.craft_planner.print.from", "Printed from the %s, %s");
    static final TextKey PRINTED_AT = TextKey.of("jsc.craft_planner.print.at", "Day %s, %s");
    /* How many of a thing the tree needs at that point: "12x Iron Ingot". */
    static final TextKey TREE_NODE = TextKey.of("jsc.craft_planner.tree_node", "%sx %s");
    static final TextKey RAW = TextKey.of("jsc.craft_planner.raw", "raw");

    private CraftPlannerTexts() {
    }
}
