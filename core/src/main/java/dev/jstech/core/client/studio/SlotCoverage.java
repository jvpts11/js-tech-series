/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * What a screen file shows of its menu's slots against what the menu has, which neither file can see alone: a slot
 * shown that the menu has not, a slot of the menu shown nowhere, a slot shown twice, and the player's inventory held by
 * the menu but on no screen. Pure.
 */
final class SlotCoverage {

    private SlotCoverage() {
    }

    /** What is wrong, each with the slots it is about. */
    enum Kind {
        /** The screen shows slots past the last the menu has. */
        BEYOND_THE_MENU,
        /** Slots of the menu, not the player's, that the screen shows nowhere. */
        SHOWN_NOWHERE,
        /** Slots the screen shows more than once. */
        SHOWN_TWICE,
        /** The menu holds the player's inventory and the screen has none; the slots are the player's. */
        NO_INVENTORY
    }

    /**
     * One thing wrong.
     *
     * @param kind  what is wrong
     * @param slots the slots it is about, by their place among the menu's
     */
    record Finding(Kind kind, List<Integer> slots) {

        Finding {
            slots = List.copyOf(slots);
        }
    }

    /**
     * What is wrong with a screen showing {@code shown}, each by its place among the menu's slots, and the player's
     * inventory when {@code inventory}, for a menu of {@code menuSlots} slots of which {@code playerSlots} are the
     * player's; nothing for a screen that shows each of them once.
     */
    static List<Finding> check(final int menuSlots, final Set<Integer> playerSlots, final List<Integer> shown,
                               final boolean inventory) {
        final Map<Integer, Integer> times = new TreeMap<>();
        for (final int slot : shown) {
            times.merge(slot, 1, Integer::sum);
        }
        if (inventory) {
            for (final int slot : playerSlots) {
                times.merge(slot, 1, Integer::sum);
            }
        }
        final List<Integer> beyond = new ArrayList<>();
        final List<Integer> twice = new ArrayList<>();
        times.forEach((slot, count) -> {
            if (slot < 0 || slot >= menuSlots) {
                beyond.add(slot);
            } else if (count > 1) {
                twice.add(slot);
            }
        });
        final List<Integer> nowhere = new ArrayList<>();
        for (int slot = 0; slot < menuSlots; slot++) {
            if (!playerSlots.contains(slot) && !times.containsKey(slot)) {
                nowhere.add(slot);
            }
        }
        final boolean inventoryShown = inventory || playerSlots.stream().anyMatch(times::containsKey);
        final List<Finding> out = new ArrayList<>();
        if (!beyond.isEmpty()) {
            out.add(new Finding(Kind.BEYOND_THE_MENU, beyond));
        }
        if (!nowhere.isEmpty()) {
            out.add(new Finding(Kind.SHOWN_NOWHERE, nowhere));
        }
        if (!twice.isEmpty()) {
            out.add(new Finding(Kind.SHOWN_TWICE, twice));
        }
        if (!playerSlots.isEmpty() && !inventoryShown) {
            out.add(new Finding(Kind.NO_INVENTORY, new ArrayList<>(new TreeSet<>(playerSlots))));
        }
        return out;
    }
}
