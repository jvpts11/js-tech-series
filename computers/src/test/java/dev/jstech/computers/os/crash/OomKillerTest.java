/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.crash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.os.RamLedger;
import java.util.List;
import org.junit.jupiter.api.Test;

class OomKillerTest {

    @Test
    void plan_endsNothingWhenWhatIsHeldFits() {
        final RamLedger ledger = base(512).add("jsc:calculator", 64, RamLedger.Kind.WINDOW, "jsc:calculator");
        final OomKiller.Plan plan = OomKiller.plan(ledger, 0);
        assertTrue(plan.fits());
        assertTrue(plan.victims().isEmpty());
    }

    @Test
    void plan_endsTheHeaviestFirst() {
        final RamLedger ledger = base(512)
                .add("jsc:calculator", 64, RamLedger.Kind.WINDOW, "jsc:calculator")
                .add("Messenger", 200, RamLedger.Kind.SERVICE, "jsc:messenger_service")
                .add("loop", 32, 0L, RamLedger.Kind.PROCESS, 3)
                .add("jsc:paint", 150, RamLedger.Kind.WINDOW, "jsc:paint");
        final OomKiller.Plan plan = OomKiller.plan(ledger, 0);
        assertTrue(plan.fits());
        assertEquals(List.of("Messenger"), names(plan));
    }

    @Test
    void plan_endsTheLaterOfTwoThatHoldTheSame() {
        final RamLedger ledger = base(256)
                .add("first", 64, 0L, RamLedger.Kind.PROCESS, 1)
                .add("jsc:calculator", 64, RamLedger.Kind.WINDOW, "jsc:calculator")
                .add("second", 64, 0L, RamLedger.Kind.PROCESS, 2);
        final OomKiller.Plan plan = OomKiller.plan(ledger, 0);
        assertEquals(List.of("second"), names(plan));
    }

    @Test
    void plan_keepsEndingUntilWhatIsAskedForFits() {
        final RamLedger ledger = base(512)
                .add("jsc:calculator", 64, RamLedger.Kind.WINDOW, "jsc:calculator")
                .add("jsc:paint", 150, RamLedger.Kind.WINDOW, "jsc:paint")
                .add("jsc:files", 100, RamLedger.Kind.WINDOW, "jsc:files");
        // 128 + 314 held of 512 leaves 70: asking for 300 takes the two heaviest.
        final OomKiller.Plan plan = OomKiller.plan(ledger, 300);
        assertTrue(plan.fits());
        assertEquals(List.of("jsc:paint", "jsc:files"), names(plan));
    }

    @Test
    void plan_neverEndsTheSystemItsDesktopOrItsGraphics() {
        final RamLedger ledger = new RamLedger(256)
                .add("Debian", 128, RamLedger.Kind.SYSTEM)
                .add("KDE Plasma", 96, RamLedger.Kind.DESKTOP)
                .add("graphics", 64, RamLedger.Kind.GRAPHICS);
        final OomKiller.Plan plan = OomKiller.plan(ledger, 0);
        assertFalse(plan.fits());
        assertTrue(plan.victims().isEmpty());
    }

    @Test
    void plan_saysItDoesNotFitOnceNothingIsLeftToKill() {
        final RamLedger ledger = base(256)
                .add("loop", 32, 0L, RamLedger.Kind.PROCESS, 1)
                .add("Mirror", 16, RamLedger.Kind.SERVICE, "jsc:mirror");
        final OomKiller.Plan plan = OomKiller.plan(ledger, 200);
        assertFalse(plan.fits());
        assertEquals(List.of("loop", "Mirror"), names(plan));
    }

    /* A machine of {@code totalMb} whose system holds 128 of it. */
    private static RamLedger base(final int totalMb) {
        return new RamLedger(totalMb).add("Debian", 128, RamLedger.Kind.SYSTEM);
    }

    private static List<String> names(final OomKiller.Plan plan) {
        return plan.victims().stream().map(RamLedger.Entry::name).toList();
    }
}
