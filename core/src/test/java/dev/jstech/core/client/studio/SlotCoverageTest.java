/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class SlotCoverageTest {

    /* A menu of three slots of its own, then the player's 36. */
    private static final int OWN = 3;
    private static final int ALL = OWN + 36;
    private static final Set<Integer> PLAYER = IntStream.range(OWN, ALL).boxed().collect(Collectors.toSet());

    @Test
    void check_findsNothingWhenEachSlotIsShownOnce() {
        assertTrue(SlotCoverage.check(ALL, PLAYER, List.of(0, 1, 2), true).isEmpty());
    }

    @Test
    void check_findsTheSlotsOfTheMenuShownNowhere() {
        final List<SlotCoverage.Finding> found = SlotCoverage.check(ALL, PLAYER, List.of(0, 1), true);
        assertEquals(List.of(new SlotCoverage.Finding(SlotCoverage.Kind.SHOWN_NOWHERE, List.of(2))), found);
    }

    @Test
    void check_findsASlotPastTheLastOfTheMenu() {
        final List<SlotCoverage.Finding> found = SlotCoverage.check(OWN, Set.of(), List.of(0, 1, 2, 3), false);
        assertEquals(List.of(new SlotCoverage.Finding(SlotCoverage.Kind.BEYOND_THE_MENU, List.of(3))), found);
    }

    @Test
    void check_findsASlotShownTwice() {
        final List<SlotCoverage.Finding> found = SlotCoverage.check(ALL, PLAYER, List.of(0, 1, 2, 1, OWN), true);
        assertEquals(List.of(new SlotCoverage.Finding(SlotCoverage.Kind.SHOWN_TWICE, List.of(1, OWN))), found,
                "a slot twice, and a slot of the player's shown beside the inventory that shows it too");
    }

    @Test
    void check_findsThePlayersInventoryOnNoScreen() {
        final List<SlotCoverage.Finding> found = SlotCoverage.check(ALL, PLAYER, List.of(0, 1, 2), false);
        assertEquals(1, found.size());
        assertEquals(SlotCoverage.Kind.NO_INVENTORY, found.get(0).kind());
        assertEquals(PLAYER.size(), found.get(0).slots().size());
    }

    @Test
    void check_takesAMenuWithNoInventoryAsItIs() {
        assertTrue(SlotCoverage.check(OWN, Set.of(), List.of(0, 1, 2), false).isEmpty());
    }
}
