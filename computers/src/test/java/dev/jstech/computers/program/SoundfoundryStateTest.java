/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.audio.StereoSide;
import dev.jstech.core.audio.media.MediaId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SoundfoundryStateTest {

    private SoundfoundryState state;

    @BeforeEach
    void setUp() {
        state = new SoundfoundryState();
        state.add(List.of("b.ogg", "a.ogg", "c.ogg", "d.ogg"));
    }

    @Test
    void add_keepsToTheMostAListHolds() {
        final List<String> many = new ArrayList<>();
        for (int i = 0; i < SoundfoundryState.MAX_SONGS; i++) {
            many.add("song" + i + ".ogg");
        }
        assertEquals(SoundfoundryState.MAX_SONGS - 4, state.add(many));
        assertEquals(SoundfoundryState.MAX_SONGS, state.size());
    }

    @Test
    void next_goesDownTheListAndRoundOnlyWhenAsked() {
        state.select(2);
        assertEquals(3, state.next(false, new Random(1)));
        state.select(3);
        assertEquals(-1, state.next(false, new Random(1)), "the end of the last song is silence without repeat");
        assertEquals(0, state.next(true, new Random(1)), "and the start again with it");
    }

    @Test
    void previous_goesUpAndStopsAtTheTopUnlessItRepeats() {
        state.select(0);
        assertEquals(0, state.previous(new Random(1)));
        state.setRepeat(true);
        assertEquals(3, state.previous(new Random(1)));
    }

    @Test
    void next_whileShufflingPlaysEverySongOnceBeforeAnyComesRound() {
        state.setShuffle(true);
        state.select(1);
        final Random random = new Random(7);
        final Set<Integer> played = new HashSet<>(Set.of(1));
        int at = state.next(false, random);
        while (at >= 0) {
            assertEquals(true, played.add(at), "no song twice in one round: " + at);
            state.select(at);
            at = state.next(false, random);
        }
        assertEquals(Set.of(0, 1, 2, 3), played);
        assertNotEquals(-1, state.next(true, random), "and round again with repeat");
    }

    @Test
    void remove_forgetsTheSongItWasOnAndKeepsTheOthersPlace() {
        state.select(2);
        state.remove(Set.of(0));
        assertEquals(1, state.current(), "the song it was on moved up one");
        assertEquals("c.ogg", state.song(state.current()));
        state.remove(Set.of(1));
        assertEquals(-1, state.current());
    }

    @Test
    void keepOnly_keepsTheChosenSongs() {
        state.keepOnly(Set.of(1, 3));
        assertEquals(List.of("a.ogg", "d.ogg"), state.songs());
    }

    @Test
    void sortAndReverse_keepTheSongItWasOn() {
        state.select(0);
        state.sort(Comparator.naturalOrder());
        assertEquals(List.of("a.ogg", "b.ogg", "c.ogg", "d.ogg"), state.songs());
        assertEquals("b.ogg", state.song(state.current()));
        state.reverse();
        assertEquals("b.ogg", state.song(state.current()));
    }

    @Test
    void revision_countsEveryChangeToTheList() {
        final int before = state.revision();
        state.select(1);
        state.setVolume(20);
        assertEquals(before, state.revision(), "choosing a song or a volume is not a change to the list");
        state.reverse();
        assertEquals(before + 1, state.revision());
    }

    @Test
    void gainFor_turnsTheOtherSideDown() {
        state.setBalance(50);
        assertEquals(0.5F, state.gainFor(StereoSide.LEFT));
        assertEquals(1.0F, state.gainFor(StereoSide.RIGHT));
        assertEquals(1.0F, state.gainFor(StereoSide.BOTH));
        state.setBalance(-SoundfoundryState.MAX_BALANCE * 3);
        assertEquals(-SoundfoundryState.MAX_BALANCE, state.balance(), "kept to the most either way");
        assertEquals(0.0F, state.gainFor(StereoSide.RIGHT));
    }

    @Test
    void addDownload_makesRoomByForgettingTheOldestFinished() {
        for (int i = 0; i < SoundfoundryState.MAX_DOWNLOADS; i++) {
            final SongDownload download = download(i);
            if (i == 3) {
                download.kept();
            }
            assertTrue(state.addDownload(download));
        }
        final SongDownload late = download(99);
        assertTrue(state.addDownload(late), "the finished one makes room");
        assertEquals(SoundfoundryState.MAX_DOWNLOADS, state.downloads().size());
        assertFalse(state.downloads().stream().anyMatch(download -> download.status() == SongDownload.Status.DONE));
        assertFalse(state.addDownload(download(100)), "a list full of songs still coming takes no more");
    }

    @Test
    void downloading_isTrueOnlyWhileTheRecordingIsStillComing() {
        final SongDownload download = download(1);
        state.addDownload(download);
        assertTrue(state.downloading(download.media()));
        download.kept();
        assertFalse(state.downloading(download.media()));
    }

    @Test
    void replace_putsTheSongsInPlaceOfTheListOnThePickedOne() {
        final int before = state.revision();
        state.replace(List.of("catalog://config/x/1.ogg", "catalog://config/x/2.ogg"), 1);
        assertEquals(List.of("catalog://config/x/1.ogg", "catalog://config/x/2.ogg"), state.songs());
        assertEquals(1, state.current());
        assertTrue(state.revision() != before, "a window holding the old list is told it changed");
        state.replace(List.of(), 0);
        assertEquals(-1, state.current(), "an empty list is on no song");
    }

    @Test
    void server_isKeptAsPickedAndEmptyForTheFirstFound() {
        assertEquals("", state.server());
        state.setServer("123:4");
        assertEquals("123:4", state.server());
        state.setServer(null);
        assertEquals("", state.server());
    }

    @Test
    void clearFinishedDownloads_leavesThoseStillComing() {
        final SongDownload coming = download(1);
        final SongDownload done = download(2);
        done.kept();
        state.addDownload(coming);
        state.addDownload(done);
        state.clearFinishedDownloads();
        assertEquals(List.of(coming), state.downloads());
        state.removeDownloads(Set.of(0));
        assertTrue(state.downloads().isEmpty());
    }

    private static SongDownload download(final int n) {
        final String hash = String.format("%064x", n);
        return new SongDownload("song " + n + ".ogg", new MediaId(hash, "ogg", 1_000L), SongDownload.FROM_CATALOG,
                "config/Album/" + n + ".ogg", "", false);
    }
}
