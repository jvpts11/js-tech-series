/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.diagnostic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TickLedgerTest {

    private long now = 1_000L;
    private final TickLedger ledger = new TickLedger(() -> this.now);

    @Test
    void count_addsEachKindUpInItsTick() {
        this.ledger.count("jscore:menu_sync", 40);
        this.ledger.count("jscore:menu_sync", 60);
        this.ledger.count("minecraft:level_chunk_with_light", 9_000);

        final TickLedger.Tick tick = this.ledger.tick(this.now).orElseThrow();

        assertEquals(List.of(new TickLedger.Sent("minecraft:level_chunk_with_light", 1, 9_000, 9_000),
                new TickLedger.Sent("jscore:menu_sync", 2, 100, 60)), tick.sent());
        assertEquals(9_100, tick.bytes());
        assertEquals(3, tick.packets());
    }

    @Test
    void ticks_keepTheLastHundredTheNewestFirst() {
        this.ledger.count("a:b", 10);
        this.now++;
        this.ledger.count("a:b", 500);
        this.now += 2;
        this.ledger.count("a:b", 20);

        assertEquals(List.of(1_003L, 1_001L, 1_000L), this.ledger.ticks().stream().map(TickLedger.Tick::tick)
                .toList());
        assertEquals(1_001L, this.ledger.busiest().orElseThrow().tick());

        this.now += TickLedger.TICKS;
        assertTrue(this.ledger.tick(1_003L).isEmpty(), "a tick past the last hundred is let go");
        this.ledger.count("a:b", 1);
        assertEquals(1, this.ledger.tick(this.now).orElseThrow().bytes(), "and its place is taken by the new one");
    }
}
