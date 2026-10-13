/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.api.planner.PlanCandidate;
import dev.jstech.tests.JsTests;
import java.util.List;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The plan a planner rule from another mod is handed: an extreme penalty makes it the dearest and never the cheapest,
 * and a rule that forgets to say why is refused at its own call.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class PlanCandidateGameTests {

    private static final String ARENA = "empty";

    private PlanCandidateGameTests() {
    }

    @GameTest(template = ARENA)
    public static void addCost_saturatesInsteadOfWrappingToNothing(final GameTestHelper helper) {
        final PlanCandidate plan = plan(10L);

        plan.addCost(Long.MAX_VALUE, Component.literal("a huge penalty"));

        helper.assertTrue(plan.cost() == Long.MAX_VALUE, "the penalised plan is the dearest, not free; got "
                + plan.cost());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void addCost_neverTakesTheCostBelowNothing(final GameTestHelper helper) {
        final PlanCandidate plan = plan(10L);

        plan.addCost(-50L, Component.literal("a discount"));

        helper.assertTrue(plan.cost() == 0L, "the cost stops at nothing; got " + plan.cost());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void addCost_refusesARuleThatGivesNoReason(final GameTestHelper helper) {
        final PlanCandidate plan = plan(10L);

        boolean refused = false;
        try {
            plan.addCost(5L, null);
        } catch (final NullPointerException noReason) {
            refused = true;
        }

        helper.assertTrue(refused && plan.cost() == 10L && plan.notes().isEmpty(),
                "the rule's own call fails, and the plan is left as it was");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void setAside_refusesARuleThatGivesNoReason(final GameTestHelper helper) {
        final PlanCandidate plan = plan(10L);

        boolean refused = false;
        try {
            plan.setAside(null);
        } catch (final NullPointerException noReason) {
            refused = true;
        }

        helper.assertTrue(refused && !plan.isSetAside(), "a plan is never quietly left runnable by a missing reason");
        helper.succeed();
    }

    private static PlanCandidate plan(final long cost) {
        return new PlanCandidate(ResourceLocation.withDefaultNamespace("stick"), 1L, List.of(), cost, Set.of());
    }
}
