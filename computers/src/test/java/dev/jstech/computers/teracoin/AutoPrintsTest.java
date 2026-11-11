/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.teracoin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AutoPrintsTest {

    @Test
    void toPrint_printsAsManyAsTheBalancePassesWhatItKeeps() {
        assertEquals(4, AutoPrints.toPrint(24_143_000_000L, Trc.of(200), Trc.of(10), 0, 64));
    }

    @Test
    void toPrint_countsWhatAlreadyWaitsUnpaid() {
        assertEquals(2, AutoPrints.toPrint(24_143_000_000L, Trc.of(200), Trc.of(10), 2, 64));
        assertEquals(0, AutoPrints.toPrint(24_143_000_000L, Trc.of(200), Trc.of(10), 4, 64));
    }

    @Test
    void toPrint_printsNothingShortOfOnePrice() {
        assertEquals(0, AutoPrints.toPrint(Trc.of(209), Trc.of(200), Trc.of(10), 0, 64));
        assertEquals(0, AutoPrints.toPrint(Trc.of(100), Trc.of(200), Trc.of(10), 0, 64));
    }

    @Test
    void toPrint_neverPrintsMoreThanTheMost() {
        assertEquals(64, AutoPrints.toPrint(Trc.of(1_000_000), 0L, Trc.of(1), 0, 64));
    }

    @Test
    void toPrint_printsNothingWithoutAPrice() {
        assertEquals(0, AutoPrints.toPrint(Trc.of(1000), 0L, 0L, 0, 64));
    }

    @Test
    void set_replacesTheWalletsAutoPrint() {
        final AutoPrints prints = new AutoPrints();
        prints.set(auto("a.wallet", "minecraft:diamond"));

        assertTrue(prints.set(auto("a.wallet", "minecraft:emerald")));

        assertEquals(1, prints.all().size());
        assertEquals("minecraft:emerald", prints.of("a.wallet").orElseThrow().item());
    }

    @Test
    void set_refusesAWalletPastTheMost() {
        final AutoPrints prints = new AutoPrints();
        for (int i = 0; i < AutoPrints.MOST; i++) {
            assertTrue(prints.set(auto(i + ".wallet", "minecraft:diamond")));
        }

        assertFalse(prints.set(auto("one-more.wallet", "minecraft:diamond")));
        assertTrue(prints.set(auto("0.wallet", "minecraft:emerald")));
    }

    @Test
    void clear_turnsOnlyThatWalletOff() {
        final AutoPrints prints = new AutoPrints();
        prints.set(auto("a.wallet", "minecraft:diamond"));
        prints.set(auto("b.wallet", "minecraft:diamond"));

        prints.clear("a.wallet");

        assertTrue(prints.of("a.wallet").isEmpty());
        assertTrue(prints.of("b.wallet").isPresent());
    }

    @Test
    void secondDue_comesOnceEveryTwentyTicks() {
        final AutoPrints prints = new AutoPrints();
        for (int i = 0; i < 19; i++) {
            assertFalse(prints.secondDue());
        }

        assertTrue(prints.secondDue());
        assertFalse(prints.secondDue());
    }

    @Test
    void restore_keepsAtMostTheMost() {
        final List<AutoPrints.Auto> saved = new ArrayList<>();
        for (int i = 0; i < AutoPrints.MOST + 3; i++) {
            saved.add(auto(i + ".wallet", "minecraft:diamond"));
        }
        final AutoPrints prints = new AutoPrints();

        prints.restore(saved);

        assertEquals(AutoPrints.MOST, prints.all().size());
    }

    @Test
    void printed_remembersWhenHowManyAndTheBlock() {
        final AutoPrints.Auto printed = auto("a.wallet", "minecraft:diamond").printed(96_000L, 4, 1844);

        assertEquals(96_000L, printed.lastDayTime());
        assertEquals(4, printed.lastCount());
        assertEquals(1844, printed.lastBlock());
        assertEquals("minecraft:diamond", printed.item());
    }

    private static AutoPrints.Auto auto(final String wallet, final String item) {
        return new AutoPrints.Auto(wallet, item, 42L, Trc.of(200), 0L, 0, 0);
    }
}
