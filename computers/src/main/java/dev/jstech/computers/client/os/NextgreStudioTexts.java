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

/** What the Nextgre Planner Studio's window says. */
@TextHolder
public final class NextgreStudioTexts {

    public static final TextKey TITLE = TextKey.of("jsc.nextgre_studio.title", "Nextgre Planner Studio");

    // The tabs.
    public static final TextKey EXPLAIN_TAB = TextKey.of("jsc.nextgre_studio.tab.explain", "Explain");
    public static final TextKey STATISTICS_TAB = TextKey.of("jsc.nextgre_studio.tab.statistics", "Statistics");
    public static final TextKey RULES_TAB = TextKey.of("jsc.nextgre_studio.tab.rules", "Rules");
    public static final TextKey HISTORY_TAB = TextKey.of("jsc.nextgre_studio.tab.history", "History");

    // The Explain tab.
    public static final TextKey EXPLAIN = TextKey.of("jsc.nextgre_studio.explain", "Explain");
    public static final TextKey EXPLAIN_ANALYZE = TextKey.of("jsc.nextgre_studio.explain_analyze",
            "Explain Analyze");
    public static final TextKey NO_PLAN = TextKey.of("jsc.nextgre_studio.no_plan",
            "Write a CRAFT or a SELECT, then press Explain (F7) or Explain Analyze (Shift+F7).");
    public static final TextKey PLANS_WEIGHED = TextKey.of("jsc.nextgre_studio.plans_weighed", "PLANS WEIGHED");
    public static final TextKey ALTERNATIVE = TextKey.of("jsc.nextgre_studio.alternative", "%s. cost %s");
    public static final TextKey ALTERNATIVE_CHOSEN = TextKey.of("jsc.nextgre_studio.alternative_chosen",
            "%s. cost %s, chosen");
    public static final TextKey STATISTICS_HEAD = TextKey.of("jsc.nextgre_studio.statistics_head", "STATISTICS");
    public static final TextKey EST_ACTUAL = TextKey.of("jsc.nextgre_studio.est_actual", "est %s, actual %s");
    public static final TextKey EST_ONLY = TextKey.of("jsc.nextgre_studio.est_only", "est %s");
    public static final TextKey SECONDS = TextKey.of("jsc.nextgre_studio.seconds", "%s s");
    public static final TextKey SO_FAR = TextKey.of("jsc.nextgre_studio.so_far", "%s so far");

    // The status bar.
    public static final TextKey PLANNING = TextKey.of("jsc.nextgre_studio.planning", "Planning %s ms");
    public static final TextKey EXECUTION = TextKey.of("jsc.nextgre_studio.execution", "Execution %s");
    public static final TextKey EXECUTING = TextKey.of("jsc.nextgre_studio.executing", "Executing");
    public static final TextKey ASKING = TextKey.of("jsc.nextgre_studio.asking", "Asking the network");

    // The Statistics tab.
    public static final TextKey STATISTIC = TextKey.of("jsc.nextgre_studio.statistic", "Statistic");
    public static final TextKey VALUE = TextKey.of("jsc.nextgre_studio.value", "Value");
    public static final TextKey GATHER = TextKey.of("jsc.nextgre_studio.gather", "Gather Statistics");

    // The Rules tab.
    public static final TextKey OWN_RULES = TextKey.of("jsc.nextgre_studio.own_rules", "THE PLANNER'S RULES");
    public static final TextKey ADDED_RULES = TextKey.of("jsc.nextgre_studio.added_rules",
            "RULES FROM OTHER MODS");
    public static final TextKey OWN_HINTS = TextKey.of("jsc.nextgre_studio.own_hints", "HINTS");
    public static final TextKey ADDED_HINTS = TextKey.of("jsc.nextgre_studio.added_hints",
            "HINTS FROM OTHER MODS");

    // The History tab.
    public static final TextKey PLAN_NUMBER = TextKey.of("jsc.nextgre_studio.plan_number", "Plan");
    public static final TextKey STATEMENT = TextKey.of("jsc.nextgre_studio.statement", "Statement");
    public static final TextKey COST = TextKey.of("jsc.nextgre_studio.cost", "Cost");
    public static final TextKey TOOK = TextKey.of("jsc.nextgre_studio.took", "Took");
    public static final TextKey STATE = TextKey.of("jsc.nextgre_studio.state", "State");
    public static final TextKey PLANNED = TextKey.of("jsc.nextgre_studio.planned", "planned");
    public static final TextKey RUNNING = TextKey.of("jsc.nextgre_studio.running", "running");
    public static final TextKey DONE = TextKey.of("jsc.nextgre_studio.done", "done");
    public static final TextKey SHORT = TextKey.of("jsc.nextgre_studio.short", "fell short");
    public static final TextKey NO_HISTORY = TextKey.of("jsc.nextgre_studio.no_history",
            "No plans yet. Every craft NextgreIQL plans is kept here.");

    private NextgreStudioTexts() {
    }
}
