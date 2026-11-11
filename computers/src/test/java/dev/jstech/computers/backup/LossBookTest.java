/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LossBookTest {

    private LossBook<String> book;

    @BeforeEach
    void setUp() {
        this.book = new LossBook<>();
    }

    @Test
    void restore_givesBackTheLesserOfTheBackupAndWhatWasLostSince() {
        book.lose("diamond", 64L, 100L, 100L, LossCause.DEFRAG, "", -1);
        book.lose("redstone", 512L, 120L, 120L, LossCause.DROP, "PC-01", -1);
        book.lose("gold", 300L, 130L, 130L, LossCause.DEAD_DISK, "SRV-01", 3);
        final Map<String, Long> back = book.restore(Map.of("diamond", 1_000L, "redstone", 640L, "gold", 180L,
                "copper", 96L), 50L);
        assertEquals(64L, back.get("diamond"), "the diamonds lost, though the backup holds more");
        assertEquals(512L, back.get("redstone"));
        assertEquals(180L, back.get("gold"), "only what the backup holds of the gold");
        assertTrue(!back.containsKey("copper"), "nothing of what was never lost");
    }

    @Test
    void restore_givesEachLossBackOnce() {
        book.lose("diamond", 64L, 100L, 100L, LossCause.DEFRAG, "", -1);
        assertEquals(64L, book.restore(Map.of("diamond", 100L), 50L).get("diamond"));
        assertTrue(book.restore(Map.of("diamond", 100L), 50L).isEmpty(), "the same backup a second time");
        assertTrue(book.restore(Map.of("diamond", 100L), 10L).isEmpty(), "or an older one");
    }

    @Test
    void restore_ignoresWhatWasLostBeforeTheBackupWasMade() {
        book.lose("diamond", 64L, 40L, 40L, LossCause.RAID0, "SRV-02", -1);
        assertTrue(book.restore(Map.of("diamond", 100L), 50L).isEmpty(),
                "the backup was made after the loss, so it never held those diamonds");
    }

    @Test
    void restore_takesFromTheOldestLossesFirstAndLeavesTheRest() {
        book.lose("gold", 10L, 100L, 100L, LossCause.DEFRAG, "", -1);
        book.lose("gold", 10L, 200L, 200L, LossCause.DEFRAG, "", -1);
        assertEquals(15L, book.restore(Map.of("gold", 15L), 50L).get("gold"));
        assertEquals(0L, book.all().get(0).left());
        assertEquals(5L, book.all().get(1).left());
        assertEquals(5L, book.wouldRestore(Map.of("gold", 15L), 50L).get("gold"), "five are still lost");
    }

    @Test
    void lose_joinsALossWrittenInPartsInTheSameTick() {
        book.lose("stone", 64L, 10L, 10L, LossCause.DROP, "PC-01", -1);
        book.lose("stone", 32L, 10L, 10L, LossCause.DROP, "PC-01", -1);
        book.lose("stone", 8L, 11L, 11L, LossCause.DROP, "PC-01", -1);
        assertEquals(2, book.all().size());
        assertEquals(96L, book.all().getFirst().count());
    }

    @Test
    void lose_keepsTheNewestWhenTheBookIsFull() {
        for (int i = 0; i < LossBook.MOST + 5; i++) {
            book.lose("stone", 1L, i, i, LossCause.DROP, "", -1);
        }
        assertEquals(LossBook.MOST, book.all().size());
        assertEquals(5L, book.all().getFirst().at(), "the five oldest fell off");
    }
}
