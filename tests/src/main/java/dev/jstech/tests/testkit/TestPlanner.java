/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit;

import dev.jstech.computers.api.ComputersRegisterEvent;
import dev.jstech.computers.api.planner.IExplainNode;
import dev.jstech.computers.api.planner.IPlannerOperator;
import dev.jstech.computers.api.planner.IPlannerRule;
import dev.jstech.computers.api.planner.IPlannerStatistic;
import dev.jstech.computers.api.planner.PlanCandidate;
import dev.jstech.computers.api.planner.PlanStep;
import dev.jstech.tests.JsTests;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;

/**
 * What another mod adds to NextgreIQL's planner, added through the computers' register event the way that mod would:
 * a rule that adds a fixed cost to every plan, a hint that adds the cost it is given, a statistic and a note under
 * every step. The planner tests look for each of them in the plans and the lists the studio shows.
 */
public final class TestPlanner {

    /** The rule's id, which a Mainframe switches it on and off by. */
    public static final ResourceLocation RULE = ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "test_rule");
    /** What the rule adds to every plan, in ticks. */
    public static final long RULE_COST = 7L;
    /** What the rule says it did. */
    public static final String RULE_NOTE = "the test rule added 7 ticks";
    /** The hint's words. */
    public static final String HINT = "TEST BOOST";
    /** What the statistic reads. */
    public static final String STATISTIC_VALUE = "42 widgets";
    /** How each note under a step begins. */
    public static final String NOTE = "test note for ";

    private TestPlanner() {
    }

    /** Adds the four while the game loads. */
    public static void register(final IEventBus modEventBus) {
        modEventBus.addListener(ComputersRegisterEvent.class, event -> {
            event.plannerRule(new IPlannerRule() {
                @Override
                public ResourceLocation id() {
                    return RULE;
                }

                @Override
                public Component name() {
                    return Component.literal("Test rule");
                }

                @Override
                public Component description() {
                    return Component.literal("Adds a few ticks to every plan.");
                }

                @Override
                public void weigh(final PlanCandidate candidate) {
                    candidate.addCost(RULE_COST, Component.literal(RULE_NOTE));
                }
            });
            event.plannerOperator(new IPlannerOperator() {
                @Override
                public ResourceLocation id() {
                    return ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "test_boost");
                }

                @Override
                public String keyword() {
                    return HINT;
                }

                @Override
                public boolean takesValue() {
                    return true;
                }

                @Override
                public Component description() {
                    return Component.literal("Adds the ticks it is given to every plan.");
                }

                @Override
                public void apply(final String value, final PlanCandidate candidate) {
                    // The value is what a player typed, so a word where a number belongs sets the plan aside
                    // instead of throwing on the server's thread.
                    final long ticks;
                    try {
                        ticks = Long.parseLong(value.strip());
                    } catch (final NumberFormatException notANumber) {
                        candidate.setAside(Component.literal("not a number of ticks: " + value));
                        return;
                    }
                    candidate.addCost(ticks, Component.literal("boosted by " + value));
                }
            });
            event.plannerStatistic(new IPlannerStatistic() {
                @Override
                public ResourceLocation id() {
                    return ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "test_statistic");
                }

                @Override
                public Component name() {
                    return Component.literal("Test statistic");
                }

                @Override
                public Component read(final ServerLevel level, final BlockPos mainframe) {
                    return Component.literal(STATISTIC_VALUE);
                }
            });
            event.explainNode(new IExplainNode() {
                @Override
                public ResourceLocation id() {
                    return ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "test_note");
                }

                @Override
                public List<Component> notes(final PlanStep step) {
                    return List.of(Component.literal(NOTE + step.item().getPath()));
                }
            });
        });
    }
}
