/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * What a computer's case takes away on its own, and the fan seats it has (computers/docs/design/temperature.md): at the
 * rear, on the top and at the front, numbered in that order. Its coolers' and fans' watts add to its own.
 *
 * <p>A water cooler's radiator takes a pair of fan seats: the two on the top when the case has them, otherwise the
 * first two at the front; a second water cooler takes the next pair.
 *
 * @param caseWatts the heat the case takes away on its own, in watts
 * @param rearFans  its fan seats at the rear
 * @param topFans   on the top
 * @param frontFans at the front
 */
@ApiStatus.Experimental
public record CaseCooling(int caseWatts, int rearFans, int topFans, int frontFans) {

    /** A computer cooled from outside it, as a server is by its rack: nothing of its own and no fan seats. */
    public static final CaseCooling NONE = new CaseCooling(0, 0, 0, 0);
    /** The Vintage's AT minitower. */
    public static final CaseCooling VINTAGE = new CaseCooling(60, 0, 0, 1);
    /** The Legacy's ATX tower. */
    public static final CaseCooling LEGACY = new CaseCooling(100, 1, 0, 1);
    /** The Transition's mid tower. */
    public static final CaseCooling TRANSITION = new CaseCooling(150, 1, 0, 2);
    /** The Neutral case of the Standard and the Advanced. */
    public static final CaseCooling NEUTRAL = new CaseCooling(200, 1, 0, 2);
    /** The High Performance case: a mesh front and the most fans. */
    public static final CaseCooling HIGH_PERFORMANCE = new CaseCooling(300, 1, 2, 3);
    /** The Aesthetic case: glass panels, handsome and hotter. */
    public static final CaseCooling AESTHETIC = new CaseCooling(150, 1, 1, 2);
    /** The Mainframe's cabinet, which cools its processors' coolers too and has no fan seats. */
    public static final CaseCooling MAINFRAME = new CaseCooling(1_500, 0, 0, 0);
    /**
     * The most fan seats one of this mod's cases has: a tower keeps as many slots for fans whatever its case, as it
     * keeps slots for the most bays, and its case says how many of them it really has.
     */
    public static final int MOST_FANS = 6;

    private static final int PAIR = 2;

    public CaseCooling {
        if (caseWatts < 0 || rearFans < 0 || topFans < 0 || frontFans < 0) {
            throw new IllegalArgumentException("a case takes away and seats no less than nothing");
        }
    }

    /** Its fan seats in all. */
    public int fans() {
        return rearFans + topFans + frontFans;
    }

    /** The slots a computer in this case keeps for fans: none for a case with no seats, the most any case has else. */
    public int fanSlots() {
        return fans() == 0 ? 0 : Math.max(MOST_FANS, fans());
    }

    /** Where fan seat {@code seat} is. */
    public Side side(final int seat) {
        if (seat < rearFans) {
            return Side.REAR;
        }
        return seat < rearFans + topFans ? Side.TOP : Side.FRONT;
    }

    /** The first fan seat of each pair a water cooler's radiator can take, in the order they are taken. */
    public List<Integer> radiatorPairs() {
        final List<Integer> pairs = new ArrayList<>(2);
        if (topFans >= PAIR) {
            pairs.add(rearFans);
        }
        if (frontFans >= PAIR) {
            pairs.add(rearFans + topFans);
        }
        return pairs;
    }

    /**
     * The fan seats the radiators of {@code coolers} take, the coolers over the processors in order and null for a
     * processor with none: the first water cooler takes the first pair, the next the next; one past the pairs the case
     * has takes none, which no case lets happen.
     */
    public Set<Integer> radiatorSeats(final List<CoolerSpec> coolers) {
        final List<Integer> pairs = radiatorPairs();
        final Set<Integer> taken = new HashSet<>();
        int pair = 0;
        for (final CoolerSpec cooler : coolers) {
            if (cooler == null || cooler.radiatorFans() == 0) {
                continue;
            }
            if (pair < pairs.size()) {
                for (int seat = 0; seat < cooler.radiatorFans(); seat++) {
                    taken.add(pairs.get(pair) + seat);
                }
            }
            pair++;
        }
        return taken;
    }

    /**
     * Where the radiator of the cooler over processor {@code index} lies, of {@code coolers} over the processors in
     * order (null for none), by the same rule as {@link #radiatorSeats}; null when that cooler is no water cooler or
     * finds no pair left.
     */
    public @Nullable Side radiatorSide(final List<CoolerSpec> coolers, final int index) {
        final CoolerSpec cooler = coolers.get(index);
        if (cooler == null || cooler.radiatorFans() == 0) {
            return null;
        }
        int pair = 0;
        for (int i = 0; i < index; i++) {
            final CoolerSpec before = coolers.get(i);
            pair += before != null && before.radiatorFans() > 0 ? 1 : 0;
        }
        final List<Integer> pairs = radiatorPairs();
        return pair < pairs.size() ? side(pairs.get(pair)) : null;
    }

    /**
     * What a computer in this case takes away, in watts: what the case takes on its own, with {@code coolers} over its
     * processors (null for none) and {@code fans} in its seats (null for an empty one or one a radiator takes).
     */
    public int budgetWatts(final List<CoolerSpec> coolers, final List<CaseFanSpec> fans) {
        int watts = caseWatts;
        for (final CoolerSpec cooler : coolers) {
            watts += cooler == null ? 0 : cooler.takesAwayWatts();
        }
        for (final CaseFanSpec fan : fans) {
            watts += fan == null ? 0 : fan.takesAwayWatts();
        }
        return watts;
    }

    /** Where a fan seat stands in its case. */
    public enum Side {
        REAR,
        TOP,
        FRONT
    }
}
