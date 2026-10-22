/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.unify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UnifyPlanTest {

    private static final List<String> PREFER = List.of("minecraft", "jscore");

    @Test
    void of_letsTheFirstPreferredModsItemStand() {
        final UnifyPlan plan = UnifyPlan.of(Map.of("c:ingots/tin",
                List.of("othermod:tin_ingot", "jscore:tin_ingot", "anothermod:tin_ingot")), PREFER, Set.of());

        assertEquals("jscore:tin_ingot", plan.chosen().get("c:ingots/tin"));
        assertEquals(Map.of("othermod:tin_ingot", "jscore:tin_ingot", "anothermod:tin_ingot", "jscore:tin_ingot"),
                plan.replace());
    }

    @Test
    void of_putsTheGamesOwnItemBeforeTheCores() {
        final UnifyPlan plan = UnifyPlan.of(Map.of("c:ingots/copper",
                List.of("jscore:copper_ingot", "minecraft:copper_ingot")), PREFER, Set.of());

        assertEquals(Map.of("jscore:copper_ingot", "minecraft:copper_ingot"), plan.replace());
    }

    @Test
    void of_takesTheFirstByIdWhenNoPreferredModHasOne() {
        final UnifyPlan plan = UnifyPlan.of(Map.of("c:ingots/osmium",
                List.of("zmod:osmium_ingot", "amod:osmium_ingot")), PREFER, Set.of());

        assertEquals("amod:osmium_ingot", plan.chosen().get("c:ingots/osmium"));
        assertEquals(Map.of("zmod:osmium_ingot", "amod:osmium_ingot"), plan.replace());
    }

    @Test
    void of_leavesATagOfOneItemAlone() {
        final UnifyPlan plan = UnifyPlan.of(Map.of("c:ingots/tin", List.of("jscore:tin_ingot"),
                "c:dusts/tin", List.of()), PREFER, Set.of());

        assertTrue(plan.isEmpty());
        assertTrue(plan.chosen().isEmpty());
    }

    @Test
    void of_neverReplacesAKeptItem() {
        final UnifyPlan plan = UnifyPlan.of(Map.of("c:ingots/tin",
                List.of("othermod:tin_ingot", "jscore:tin_ingot", "anothermod:tin_ingot")), PREFER,
                Set.of("othermod:tin_ingot"));

        assertEquals(Map.of("anothermod:tin_ingot", "jscore:tin_ingot"), plan.replace());
    }

    @Test
    void of_neverReplacesAnItemThatStandsForAnEarlierTag() {
        // amod:bronze_ingot stands for c:ingots/a_bronze, taken first, and is a copy in c:ingots/bronze after it.
        final UnifyPlan plan = UnifyPlan.of(Map.of(
                "c:ingots/a_bronze", List.of("zmod:bronze_ingot", "amod:bronze_ingot"),
                "c:ingots/bronze", List.of("amod:bronze_ingot", "jscore:bronze_ingot")), PREFER, Set.of());

        assertEquals("amod:bronze_ingot", plan.chosen().get("c:ingots/a_bronze"));
        assertFalse(plan.replace().containsKey("amod:bronze_ingot"));
        assertEquals("amod:bronze_ingot", plan.replace().get("zmod:bronze_ingot"));
    }

    @Test
    void of_leavesAModsOwnItemsInATagApart() {
        final UnifyPlan plan = UnifyPlan.of(Map.of("c:gems/certus_quartz",
                List.of("ae2:certus_quartz_crystal", "ae2:charged_certus_quartz_crystal")), PREFER, Set.of());

        assertTrue(plan.isEmpty());
        assertTrue(plan.chosen().isEmpty());
    }

    @Test
    void of_unifiesTheOtherModsCopiesBesideAModWithTwo() {
        final UnifyPlan plan = UnifyPlan.of(Map.of("c:gems/certus_quartz", List.of("ae2:certus_quartz_crystal",
                "ae2:charged_certus_quartz_crystal", "bmod:certus_quartz", "amod:certus_quartz")), PREFER, Set.of());

        assertEquals("amod:certus_quartz", plan.chosen().get("c:gems/certus_quartz"));
        assertEquals(Map.of("bmod:certus_quartz", "amod:certus_quartz"), plan.replace());
    }

    @Test
    void of_makesTheSamePlanWhateverOrderTheItemsCameIn() {
        final UnifyPlan one = UnifyPlan.of(Map.of("c:dusts/tin", List.of("bmod:tin_dust", "amod:tin_dust")),
                PREFER, Set.of());
        final UnifyPlan two = UnifyPlan.of(Map.of("c:dusts/tin", List.of("amod:tin_dust", "bmod:tin_dust")),
                PREFER, Set.of());

        assertEquals(one, two);
    }
}
