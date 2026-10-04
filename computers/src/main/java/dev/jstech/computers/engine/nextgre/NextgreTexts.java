/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.engine.nextgre;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What NextgreIQL says: of the plans it weighs, of the steps it shows, of its rules and its statistics. */
@TextHolder
public final class NextgreTexts {

    // The plans weighed.
    public static final TextKey AS_THE_NETWORK = TextKey.of("jsc.nextgre.plan.as_the_network",
            "bench recipes first, as the network plans");
    public static final TextKey MACHINES_FIRST = TextKey.of("jsc.nextgre.plan.machines_first",
            "machine recipes first");
    public static final TextKey WITH_RECIPE = TextKey.of("jsc.nextgre.plan.with_recipe", "with recipe %s of %s");
    public static final TextKey FASTEST_SOURCES = TextKey.of("jsc.nextgre.plan.fastest_sources",
            "raw materials from the fastest servers");
    public static final TextKey ONE_AT_A_TIME = TextKey.of("jsc.nextgre.plan.one_at_a_time",
            "one stage at a time");
    public static final TextKey SET_ASIDE_BY = TextKey.of("jsc.nextgre.plan.set_aside_by", "set aside by %s");
    public static final TextKey CANNOT_BE_MADE = TextKey.of("jsc.nextgre.plan.cannot_be_made",
            "cannot be made: %s %s missing");
    public static final TextKey PARTIAL = TextKey.of("jsc.nextgre.plan.partial", "makes %s of the %s asked for");

    // The plan's tree.
    public static final TextKey CRAFT = TextKey.of("jsc.nextgre.node.craft", "Craft %s x%s");
    public static final TextKey PROCESS = TextKey.of("jsc.nextgre.node.process", "Process %s x%s");
    public static final TextKey PULL = TextKey.of("jsc.nextgre.node.pull", "Pull %s x%s");
    public static final TextKey MISSING = TextKey.of("jsc.nextgre.node.missing", "Missing %s x%s");
    public static final TextKey SEEK = TextKey.of("jsc.nextgre.node.seek", "Index Seek %s x%s");
    public static final TextKey AT_A_BENCH = TextKey.of("jsc.nextgre.node.at_a_bench", "at a bench, %s runs");
    public static final TextKey ON_A_MACHINE = TextKey.of("jsc.nextgre.node.on_a_machine", "on a machine, %s runs");
    public static final TextKey FROM = TextKey.of("jsc.nextgre.node.from", "from %s");
    public static final TextKey IN_PARALLEL = TextKey.of("jsc.nextgre.node.in_parallel",
            "%s steps, %s at once");
    public static final TextKey ONE_STEP = TextKey.of("jsc.nextgre.node.one_step", "one step");
    public static final TextKey HELD_NOWHERE = TextKey.of("jsc.nextgre.node.held_nowhere",
            "nothing holds or makes it");

    // What a statement answers.
    public static final TextKey EXPLAINED = TextKey.of("jsc.nextgre.said.explained",
            "plan %s of %s chosen, cost %s ticks; planning took %s ms");
    public static final TextKey ANALYZING = TextKey.of("jsc.nextgre.said.analyzing",
            "plan %s of %s started as Operation %s; the Nextgre Planner Studio shows what it takes as it runs");
    public static final TextKey CRAFT_STARTED = TextKey.of("jsc.nextgre.said.craft_started",
            "craft of %s %s started as Operation %s, plan %s of %s");
    public static final TextKey NOTHING_MAKES = TextKey.of("jsc.nextgre.said.nothing_makes",
            "nothing on the network knows how to make %s");
    public static final TextKey EVERY_PLAN_SET_ASIDE = TextKey.of("jsc.nextgre.said.every_plan_set_aside",
            "every plan for %s was set aside");
    public static final TextKey EXPLAINS_ONLY = TextKey.of("jsc.nextgre.said.explains_only",
            "EXPLAIN shows the plans of CRAFT and SELECT");
    public static final TextKey BAD_PARALLEL = TextKey.of("jsc.nextgre.said.bad_parallel",
            "MAX PARALLEL takes a whole number above nought");
    public static final TextKey NO_SERVER = TextKey.of("jsc.nextgre.said.no_server",
            "no server on the network is called %s");
    public static final TextKey UNKNOWN_ITEM = TextKey.of("jsc.nextgre.said.unknown_item", "no item is called %s");
    public static final TextKey HINTS_ON_CRAFT = TextKey.of("jsc.nextgre.said.hints_on_craft",
            "hints change the plan of a CRAFT");
    public static final TextKey NOT_STARTED = TextKey.of("jsc.nextgre.said.not_started",
            "the network could not start the craft of %s");
    public static final TextKey SEEK_PLANNED = TextKey.of("jsc.nextgre.said.seek_planned",
            "%s of %s would be read from %s servers, cost %s ticks");
    public static final TextKey ANALYZED = TextKey.of("jsc.nextgre.said.analyzed",
            "statistics gathered: %s item types on %s servers, %s recipes timed");
    public static final TextKey EXPLAIN_NEEDS = TextKey.of("jsc.nextgre.said.explain_needs",
            "EXPLAIN needs a statement to explain");
    /** A step's name moved in under the step above it, as a plan's lines show it. */
    public static final TextKey INDENTED = TextKey.of("jsc.nextgre.said.indented", "%s%s");
    public static final TextKey TICKS = TextKey.of("jsc.nextgre.said.ticks", "%s ticks");

    // The rules of the engine's own planner.
    public static final TextKey RULE_MACHINES = TextKey.of("jsc.nextgre.rule.weigh_machines",
            "Weigh machines against benches");
    public static final TextKey RULE_MACHINES_TELLS = TextKey.of("jsc.nextgre.rule.weigh_machines.tells",
            "Also plans the craft with machine recipes before bench recipes, and keeps the cheaper.");
    public static final TextKey RULE_RECIPES = TextKey.of("jsc.nextgre.rule.weigh_recipes",
            "Weigh every recipe of the result");
    public static final TextKey RULE_RECIPES_TELLS = TextKey.of("jsc.nextgre.rule.weigh_recipes.tells",
            "Plans the craft once with each bench recipe that makes what was asked for, and keeps the cheapest.");
    public static final TextKey RULE_MEASURED = TextKey.of("jsc.nextgre.rule.measured_times", "Use measured times");
    public static final TextKey RULE_MEASURED_TELLS = TextKey.of("jsc.nextgre.rule.measured_times.tells",
            "Reckons each recipe at the time it was measured to take on this network, rather than at a guess.");
    public static final TextKey RULE_PARALLEL = TextKey.of("jsc.nextgre.rule.parallel_steps",
            "Count steps that run at once");
    public static final TextKey RULE_PARALLEL_TELLS = TextKey.of("jsc.nextgre.rule.parallel_steps.tells",
            "Reckons steps that do not wait on each other as running side by side.");
    public static final TextKey RULE_FASTEST = TextKey.of("jsc.nextgre.rule.fastest_sources",
            "Weigh the fastest servers");
    public static final TextKey RULE_FASTEST_TELLS = TextKey.of("jsc.nextgre.rule.fastest_sources.tells",
            "Also plans taking the raw materials from the servers that hand them over fastest.");

    // The hints of the dialect.
    public static final TextKey HINT_PREFER_SOURCE = TextKey.of("jsc.nextgre.hint.prefer_source",
            "Takes the raw materials from the named server first.");
    public static final TextKey HINT_AVOID_SOURCE = TextKey.of("jsc.nextgre.hint.avoid_source",
            "Takes nothing from the named server while another one has it.");
    public static final TextKey HINT_MAX_PARALLEL = TextKey.of("jsc.nextgre.hint.max_parallel",
            "Runs at most that many stages of the craft at once.");
    public static final TextKey HINT_PREFER_MACHINE = TextKey.of("jsc.nextgre.hint.prefer_machine",
            "Makes on a machine whatever both a machine and a bench make.");
    public static final TextKey HINT_PREFER_BENCH = TextKey.of("jsc.nextgre.hint.prefer_bench",
            "Makes at a bench whatever both a machine and a bench make.");

    // The statistics.
    public static final TextKey STAT_ITEM_TYPES = TextKey.of("jsc.nextgre.stat.item_types", "Item types indexed");
    public static final TextKey STAT_SERVERS = TextKey.of("jsc.nextgre.stat.servers", "Servers");
    public static final TextKey STAT_TIMED = TextKey.of("jsc.nextgre.stat.timed", "Recipes timed");
    public static final TextKey STAT_MEASURED = TextKey.of("jsc.nextgre.stat.measured", "Crafts measured");
    public static final TextKey STAT_ANALYZED = TextKey.of("jsc.nextgre.stat.analyzed", "Last ANALYZE");
    public static final TextKey STAT_NEVER = TextKey.of("jsc.nextgre.stat.never", "never");
    public static final TextKey STAT_MINUTES_AGO = TextKey.of("jsc.nextgre.stat.minutes_ago", "%s min ago");
    public static final TextKey STAT_AT_A_BENCH = TextKey.of("jsc.nextgre.stat.at_a_bench", "%s at a bench");
    public static final TextKey STAT_ON_A_MACHINE = TextKey.of("jsc.nextgre.stat.on_a_machine", "%s on a machine");
    public static final TextKey STAT_PER_RUN = TextKey.of("jsc.nextgre.stat.per_run",
            "%s ticks a run, %s crafts");

    private NextgreTexts() {
    }

    /** What the engine's own hint {@code keyword} does. */
    public static Text hintTells(final String keyword) {
        return switch (keyword) {
            case NextgreStatement.PREFER_SOURCE -> HINT_PREFER_SOURCE.text();
            case NextgreStatement.AVOID_SOURCE -> HINT_AVOID_SOURCE.text();
            case NextgreStatement.MAX_PARALLEL -> HINT_MAX_PARALLEL.text();
            case NextgreStatement.PREFER_MACHINE -> HINT_PREFER_MACHINE.text();
            case NextgreStatement.PREFER_BENCH -> HINT_PREFER_BENCH.text();
            default -> Text.EMPTY;
        };
    }
}
