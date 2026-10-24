/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.research;

import dev.jstech.core.research.ResearchMath;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The rule of a group of Computational Research Centres joined by the Research Link Cable: each team counts its own
 * CRCs, the strongest whole and each other at 90% of its power, and works the group's discovery at that pace with
 * its own progress; so a group of several teams is several groups working side by side, and cutting the cable loses
 * no team anything (industrial/docs/design/science.md). Pure.
 */
public final class GroupPower {

    /** What each CRC but a team's strongest gives of its power. */
    public static final double EACH_OTHER = 0.9;

    private GroupPower() {
    }

    /** What each CRC of {@code members} gives its team, in their order: its whole power or 90% of it. */
    public static double[] gives(final List<Member> members) {
        final Map<String, List<Integer>> byTeam = new LinkedHashMap<>();
        for (int i = 0; i < members.size(); i++) {
            byTeam.computeIfAbsent(members.get(i).team(), team -> new ArrayList<>()).add(i);
        }
        final double[] gives = new double[members.size()];
        for (final List<Integer> team : byTeam.values()) {
            final double[] powers = new double[team.size()];
            for (int k = 0; k < powers.length; k++) {
                powers[k] = members.get(team.get(k)).power();
            }
            final double[] shares = ResearchMath.groupShares(powers, EACH_OTHER);
            for (int k = 0; k < shares.length; k++) {
                gives[team.get(k)] = shares[k];
            }
        }
        return gives;
    }

    /** The pace each team of {@code members} works at, in points a second, by team in the order they first come. */
    public static Map<String, Double> byTeam(final List<Member> members) {
        final double[] gives = gives(members);
        final Map<String, Double> paces = new LinkedHashMap<>();
        for (int i = 0; i < members.size(); i++) {
            paces.merge(members.get(i).team(), gives[i], Double::sum);
        }
        return paces;
    }

    /**
     * A CRC of a group.
     *
     * @param team  the team of its owner
     * @param power the research it makes, in points a second
     */
    public record Member(String team, double power) {

        public Member {
            Objects.requireNonNull(team, "team");
        }
    }
}
