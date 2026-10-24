/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.research;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GroupPowerTest {

    private static final double CLOSE = 1e-9;

    @Test
    void byTeam_countsEachTeamsOwnStrongestWholeAndTheRestAtNinetyPercent() {
        final List<GroupPower.Member> group = List.of(new GroupPower.Member("ironworks", 24),
                new GroupPower.Member("ironworks", 9), new GroupPower.Member("ore_barons", 18));
        final Map<String, Double> paces = GroupPower.byTeam(group);
        assertEquals(32.1, paces.get("ironworks"), CLOSE);
        assertEquals(18.0, paces.get("ore_barons"), CLOSE, "another team's strongest counts whole too");
    }

    @Test
    void gives_saysWhatEachCrcGivesInTheirOrder() {
        final List<GroupPower.Member> group = List.of(new GroupPower.Member("a", 9), new GroupPower.Member("a", 24),
                new GroupPower.Member("b", 18));
        assertArrayEquals(new double[] {8.1, 24.0, 18.0}, GroupPower.gives(group), CLOSE);
    }

    @Test
    void byTeam_ofOneCrcIsItsWholePower() {
        assertEquals(Map.of("a", 5.0), GroupPower.byTeam(List.of(new GroupPower.Member("a", 5))));
    }
}
