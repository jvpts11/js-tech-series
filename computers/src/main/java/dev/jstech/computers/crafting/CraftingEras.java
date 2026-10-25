/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.crafting;

import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.EraTable;

/**
 * How much the autocrafting hardware of each era holds, in one place: how many Crafting Interfaces a Crafting Card
 * drives, how many bench recipes its ROM keeps, and how many {@code .craft} files an interface of that era holds. A
 * later era always holds more; the numbers are estimates a play test may move. An era after the Advanced holds what
 * the Advanced does.
 */
public final class CraftingEras {

    /**
     * The most recipes a Crafting Card's ROM list may hold on the wire. It is above every era's {@link #romPerCard}
     * (a test keeps it so) and leaves room for a card filled by a command, so a synced list is never refused.
     */
    public static final int MAX_ROM_PER_CARD = 16;
    private static final EraTable<Integer> INTERFACES_PER_CARD = EraTable.ofFive(2, 4, 5, 6, 8);
    private static final EraTable<Integer> ROM_PER_CARD = EraTable.ofFive(2, 4, 5, 6, 8);
    private static final EraTable<Integer> PATTERNS_PER_INTERFACE = EraTable.ofFive(3, 6, 8, 9, 12);

    private CraftingEras() {
    }

    /** How many Crafting Interfaces a Crafting Card of {@code era} drives. */
    public static int interfacesPerCard(final Era era) {
        return INTERFACES_PER_CARD.get(era);
    }

    /** How many bench recipes the ROM on a Crafting Card of {@code era} keeps. */
    public static int romPerCard(final Era era) {
        return ROM_PER_CARD.get(era);
    }

    /** How many {@code .craft} files a Crafting Interface of {@code era} holds. */
    public static int patternsPerInterface(final Era era) {
        return PATTERNS_PER_INTERFACE.get(era);
    }
}
