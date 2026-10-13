/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.media;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaLedgerTest {

    private static final UUID ALICE = new UUID(1L, 1L);
    private static final UUID BOB = new UUID(2L, 2L);
    private static final long DAY = 24L * 60L * 60L * 1000L;

    @Test
    void brought_countsARecordingOnlyForWhoeverBroughtItFirst() {
        final MediaLedger ledger = new MediaLedger();
        final MediaId song = media('a', 1_000L);
        ledger.brought(song, ALICE, 0L);
        ledger.brought(song, BOB, 10L);
        assertEquals(1_000L, ledger.broughtBytes(ALICE));
        assertEquals(0L, ledger.broughtBytes(BOB), "bringing a song the server keeps costs nothing");
    }

    @Test
    void brought_claimsARecordingNobodyBrought() {
        final MediaLedger ledger = new MediaLedger();
        final MediaId song = media('b', 500L);
        ledger.used(song, 0L);
        ledger.brought(song, ALICE, 5L);
        assertEquals(500L, ledger.broughtBytes(ALICE));
    }

    @Test
    void unusedSince_leavesOutWhatIsKeptAndWhatWasUsedLately() {
        final MediaLedger ledger = new MediaLedger();
        final MediaId old = media('c', 1L);
        final MediaId recent = media('d', 1L);
        final MediaId kept = media('e', 1L);
        ledger.used(old, 0L);
        ledger.used(recent, 10 * DAY);
        ledger.used(kept, 0L);
        assertEquals(List.of(old), ledger.unusedSince(5 * DAY, Set.of(kept)));
    }

    @Test
    void used_writesAUseDownOnlyOnceAnHourHasPassed() {
        final MediaLedger ledger = new MediaLedger();
        final MediaId song = media('f', 1L);
        ledger.used(song, 0L);
        ledger.taken();
        ledger.used(song, MediaLedger.GRAIN_MILLIS - 1L);
        assertFalse(ledger.dirty(), "a use within the hour changes nothing worth keeping");
        ledger.used(song, MediaLedger.GRAIN_MILLIS);
        assertTrue(ledger.dirty());
    }

    @Test
    void reconcile_writesDownWhatTheStoreHoldsAndForgetsWhatIsGone() {
        final MediaLedger ledger = new MediaLedger();
        final MediaId gone = media('1', 1L);
        final MediaId found = media('2', 1L);
        ledger.brought(gone, ALICE, 0L);
        ledger.reconcile(List.of(found), 7L);
        assertEquals(0L, ledger.broughtBytes(ALICE), "a recording no longer held counts for nobody");
        assertEquals(List.of(found), ledger.unusedSince(8L, Set.of()));
    }

    @Test
    void of_givesBackWhatTakenTook() {
        final MediaLedger ledger = new MediaLedger();
        final MediaId song = media('3', 4_096L);
        final MediaId other = media('4', 8L);
        ledger.brought(song, ALICE, 123L);
        ledger.used(other, 456L);
        final MediaLedger again = MediaLedger.of(ledger.taken());
        assertFalse(ledger.dirty(), "what was taken is kept");
        assertEquals(4_096L, again.broughtBytes(ALICE));
        assertEquals(List.of(song), again.unusedSince(124L, Set.of(other)));
        assertEquals(2, again.entries().size());
    }

    @Test
    void read_takesTheTextLedgerOfBefore() {
        final MediaId song = media('5', 4_096L);
        final MediaId other = media('6', 8L);
        final String text = MediaLedger.HEADER + "\n" + song.fileName() + " 4096 123 " + ALICE + "\n"
                + other.fileName() + " 8 456 -\n";
        final MediaLedger read = MediaLedger.read(text);
        assertEquals(4_096L, read.broughtBytes(ALICE));
        assertEquals(List.of(song), read.unusedSince(124L, Set.of(other)));
        assertEquals(2, read.entries().size());
    }

    @Test
    void read_takesNothingFromTextThatIsNoLedger() {
        assertTrue(MediaLedger.read("something else\nabc 1 2 -").entries().isEmpty());
        assertEquals(0, MediaLedger.read(MediaLedger.HEADER + "\nnot a line\n").entries().size());
    }

    private static MediaId media(final char digit, final long bytes) {
        return new MediaId(String.valueOf(digit).repeat(MediaId.HASH_DIGITS), "ogg", bytes);
    }
}
