/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.engine.nextgre;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.api.planner.IExplainNode;
import dev.jstech.computers.api.planner.IPlannerOperator;
import dev.jstech.computers.api.planner.IPlannerRule;
import dev.jstech.computers.api.planner.IPlannerStatistic;
import dev.jstech.computers.os.OsRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;

/**
 * What other mods have added to the planner of an engine that takes extensions: rules that weigh its plans, hints its
 * dialect accepts, statistics it reckons with and notes it shows under a plan's steps.
 *
 * <p>They are added while the game loads, through the computers' register event, and the lists close with the
 * programs' when the loading is done. Two of one kind on the same id are refused.
 */
public final class NextgreExtensions {

    private static final List<IPlannerRule> RULES = new ArrayList<>();
    private static final List<IPlannerOperator> OPERATORS = new ArrayList<>();
    private static final List<IPlannerStatistic> STATISTICS = new ArrayList<>();
    private static final List<IExplainNode> EXPLAIN_NODES = new ArrayList<>();

    private NextgreExtensions() {
    }

    /** Adds a rule. */
    public static void addRule(final IPlannerRule rule) {
        add(RULES, rule, IPlannerRule::id, "rule");
    }

    /** Adds a hint. */
    public static void addOperator(final IPlannerOperator operator) {
        add(OPERATORS, operator, IPlannerOperator::id, "hint");
    }

    /** Adds a statistic. */
    public static void addStatistic(final IPlannerStatistic statistic) {
        add(STATISTICS, statistic, IPlannerStatistic::id, "statistic");
    }

    /** Adds what is shown under a plan's steps. */
    public static void addExplainNode(final IExplainNode node) {
        add(EXPLAIN_NODES, node, IExplainNode::id, "note");
    }

    /** Every rule, in the order they were added. */
    public static List<IPlannerRule> rules() {
        return Collections.unmodifiableList(RULES);
    }

    /** Every hint, in the order they were added. */
    public static List<IPlannerOperator> operators() {
        return Collections.unmodifiableList(OPERATORS);
    }

    /** Every statistic, in the order they were added. */
    public static List<IPlannerStatistic> statistics() {
        return Collections.unmodifiableList(STATISTICS);
    }

    /** Everything shown under a plan's steps, in the order it was added. */
    public static List<IExplainNode> explainNodes() {
        return Collections.unmodifiableList(EXPLAIN_NODES);
    }

    /** The hints as the dialect reads them. */
    public static List<NextgreStatement.Operator> dialectOperators() {
        final List<NextgreStatement.Operator> out = new ArrayList<>();
        for (final IPlannerOperator operator : OPERATORS) {
            out.add(new NextgreStatement.Operator(operator.keyword(), operator.takesValue()));
        }
        return out;
    }

    private static <T> void add(final List<T> into, final T entry, final Function<T, ResourceLocation> id,
                                final String kind) {
        if (OsRegistry.isFrozen()) {
            JsComputers.LOGGER.warn("The planner's {} {} was not added: they are only added while the game loads",
                    kind, id.apply(entry));
            return;
        }
        for (final T existing : into) {
            if (id.apply(existing).equals(id.apply(entry))) {
                throw new IllegalStateException("A planner " + kind + " is already added as " + id.apply(entry));
            }
        }
        into.add(entry);
    }
}
