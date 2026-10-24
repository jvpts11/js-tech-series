/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BuildOrderTest {

    private static final String BRICK = "test:brick";
    private static final String GLASS = "test:glass";
    private static final String BARS = "test:bars";
    private static final String POWER = "test:power_entry";
    private static final String LINK = "test:link_socket";

    @Test
    void plan_takesTheChosenBlockFirstAndCountsWhatItUses() {
        final List<BuildOrder.Want> wants = List.of(want(0, BRICK), want(1, GLASS, BARS), want(2, GLASS, BARS));

        final BuildOrder order = BuildOrder.plan(wants, Map.of(GLASS + "," + BARS, BARS),
                Map.of(BRICK, 5, GLASS, 5, BARS, 5)::get);

        assertEquals(List.of(BRICK, BARS, BARS), order.steps().stream().map(BuildOrder.Step::block).toList());
        assertEquals(Map.of(BRICK, 1, BARS, 2), order.uses());
        assertTrue(order.ready());
    }

    @Test
    void plan_turnsToAnotherBlockThatFitsWhenTheChosenRunsOut() {
        final List<BuildOrder.Want> wants = List.of(want(0, GLASS, BARS), want(1, GLASS, BARS));

        final BuildOrder order = BuildOrder.plan(wants, Map.of(GLASS + "," + BARS, GLASS),
                Map.of(GLASS, 1, BARS, 1)::get);

        assertEquals(List.of(GLASS, BARS), order.steps().stream().map(BuildOrder.Step::block).toList());
        assertTrue(order.ready());
    }

    @Test
    void plan_countsWhatTheSourceLacksAndMarksItsSteps() {
        final List<BuildOrder.Want> wants = List.of(want(0, BRICK), want(1, BRICK), want(2, BRICK));

        final BuildOrder order = BuildOrder.plan(wants, Map.of(), block -> BRICK.equals(block) ? 1 : 0);

        assertFalse(order.ready());
        assertEquals(Map.of(BRICK, 2), order.lacks());
        assertEquals(Map.of(BRICK, 3), order.uses());
        assertFalse(order.steps().getFirst().lacking());
        assertTrue(order.steps().get(2).lacking());
    }

    @Test
    void plan_placesTheLeastOfEachCountFirstInAPlaceThatTakesIt() {
        // Six rear places taking casing or an entry; the player chose casing for them all, but one power entry is a
        // must, so the first of them gets it.
        final List<BuildOrder.Want> wants = List.of(want(0, BRICK), want(1, BRICK, POWER, LINK),
                want(2, BRICK, POWER, LINK), want(3, BRICK, POWER, LINK));
        final List<Quota> quotas = List.of(new Quota(POWER, IBlockMatcher.exact(POWER), Limit.exactly(1)),
                new Quota(LINK, IBlockMatcher.exact(LINK), new Limit(0, 1)));

        final BuildOrder order = BuildOrder.plan(wants, Map.of(BRICK + "," + POWER + "," + LINK, BRICK),
                Map.of(BRICK, 9, POWER, 1, LINK, 1)::get, quotas, Map.of());

        assertEquals(List.of(BRICK, POWER, BRICK, BRICK), order.steps().stream().map(BuildOrder.Step::block)
                .toList());
        assertTrue(order.ready());
    }

    @Test
    void plan_neverPassesACountsMostAndCountsWhatAlreadyStands() {
        final List<BuildOrder.Want> wants = List.of(want(0, POWER, BRICK), want(1, POWER, BRICK));
        final List<Quota> quotas = List.of(new Quota(POWER, IBlockMatcher.exact(POWER), Limit.exactly(1)));

        final BuildOrder chosen = BuildOrder.plan(wants, Map.of(POWER + "," + BRICK, POWER),
                Map.of(POWER, 5, BRICK, 5)::get, quotas, Map.of());
        assertEquals(List.of(POWER, BRICK), chosen.steps().stream().map(BuildOrder.Step::block).toList(),
                "the chosen entry once, as the most is one");

        final BuildOrder standing = BuildOrder.plan(wants, Map.of(POWER + "," + BRICK, POWER),
                Map.of(POWER, 5, BRICK, 5)::get, quotas, Map.of(POWER, 1));
        assertEquals(List.of(BRICK, BRICK), standing.steps().stream().map(BuildOrder.Step::block).toList(),
                "none at all once one stands");
    }

    @Test
    void plan_countsAMissingMustAsLacking() {
        final List<BuildOrder.Want> wants = List.of(want(0, BRICK, POWER));
        final List<Quota> quotas = List.of(new Quota(POWER, IBlockMatcher.exact(POWER), Limit.exactly(1)));

        final BuildOrder order = BuildOrder.plan(wants, Map.of(), Map.of(BRICK, 5, POWER, 0)::get, quotas,
                Map.of());

        assertEquals(POWER, order.steps().getFirst().block());
        assertTrue(order.steps().getFirst().lacking());
        assertEquals(Map.of(POWER, 1), order.lacks());
    }

    @Test
    void want_takesABlockAtLeastAndKeysItsChoiceByWhatFits() {
        assertThrows(IllegalArgumentException.class, () -> new BuildOrder.Want(0, 0, 0, List.of()));
        assertEquals(GLASS + "," + BARS, want(0, GLASS, BARS).key());
    }

    private static BuildOrder.Want want(final int x, final String... fits) {
        return new BuildOrder.Want(x, 0, 0, List.of(fits));
    }
}
