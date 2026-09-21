/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.fs;

import dev.jstech.computers.os.FilesystemKind;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrashFolderTest {

    @Test
    void of_putsEachTrashWhereItsDesktopKeepsIt() {
        assertEquals("RECYCLER", TrashFolder.of(TrashKind.RECYCLER, "").path());
        assertEquals("home/player/.local/share/Trash", TrashFolder.of(TrashKind.FREEDESKTOP, "home/player").path());
        assertEquals("usr/player/.dt/Trash", TrashFolder.of(TrashKind.CDE, "usr/player").path());
    }

    @Test
    void of_picksTheHabitByFamilyAndDesktop() {
        assertEquals(TrashKind.RECYCLER, TrashKind.of(false, false));
        assertEquals(TrashKind.FREEDESKTOP, TrashKind.of(true, false));
        assertEquals(TrashKind.CDE, TrashKind.of(true, true));
    }

    @Test
    void directories_listParentsBeforeChildren() {
        assertEquals(List.of("home", "home/player", "home/player/.local", "home/player/.local/share",
                        "home/player/.local/share/Trash", "home/player/.local/share/Trash/files",
                        "home/player/.local/share/Trash/info"),
                TrashFolder.of(TrashKind.FREEDESKTOP, "home/player").directories());
        assertEquals(List.of("RECYCLER"), TrashFolder.of(TrashKind.RECYCLER, "").directories());
    }

    @Test
    void storedPath_keepsFreedesktopFilesApartFromTheirNotes() {
        final TrashFolder trash = TrashFolder.of(TrashKind.FREEDESKTOP, "home/player");
        assertEquals("home/player/.local/share/Trash/files/notes.txt", trash.storedPath("notes.txt"));
        assertEquals("home/player/.local/share/Trash/info/notes.txt.trashinfo", trash.recordPath("notes.txt"));
        assertFalse(trash.indexed());
    }

    @Test
    void recordPath_isOneIndexOnFramesAndCde() {
        assertEquals("RECYCLER/INFO2", TrashFolder.of(TrashKind.RECYCLER, "").recordPath("Dc1.txt"));
        assertEquals("usr/player/.dt/Trash/.trashinfo", TrashFolder.of(TrashKind.CDE, "usr/player").recordPath("a"));
    }

    @Test
    void storedName_numbersFramesFilesAndKeepsTheirExtension() {
        final TrashFolder bin = TrashFolder.of(TrashKind.RECYCLER, "");
        assertEquals("Dc1.txt", bin.storedName("notes.txt", name -> false));
        assertEquals("Dc2.txt", bin.storedName("notes.txt", Set.of("Dc1.txt")::contains));
        assertEquals("Dc1", bin.storedName("old", name -> false));
    }

    @Test
    void storedName_keepsTheNameOnUnixDesktopsAndNumbersASecond() {
        final TrashFolder trash = TrashFolder.of(TrashKind.FREEDESKTOP, "home/player");
        assertEquals("notes.txt", trash.storedName("notes.txt", name -> false));
        assertEquals("notes.2.txt", trash.storedName("notes.txt", Set.of("notes.txt")::contains));
        assertEquals("notes.3.txt", trash.storedName("notes.txt", Set.of("notes.txt", "notes.2.txt")::contains));
    }

    @Test
    void storedName_neverTakesTheIndexName() {
        final TrashFolder can = TrashFolder.of(TrashKind.CDE, "usr/player");
        assertEquals(".trashinfo.2", can.storedName(".trashinfo", name -> false));
    }

    @Test
    void storedName_cutsALongNameSoItStaysAName() {
        final TrashFolder trash = TrashFolder.of(TrashKind.FREEDESKTOP, "home/player");
        final String longest = "a".repeat(60) + ".txt";
        final String second = trash.storedName(longest, Set.of(longest)::contains);
        assertTrue(FsPaths.isValidName(second));
        assertTrue(second.endsWith(".2.txt"));
    }

    @Test
    void holds_coversTheFolderAndWhatIsInIt() {
        final TrashFolder bin = TrashFolder.of(TrashKind.RECYCLER, "");
        assertTrue(bin.holds("RECYCLER"));
        assertTrue(bin.holds("RECYCLER/Dc1.txt"));
        assertFalse(bin.holds("RECYCLERS/a.txt"));
        assertFalse(bin.holds("Users/Public/Desktop/a.txt"));
    }

    @Test
    void readIndex_readsBackWhatIndexLineWrote() {
        final String index = TrashFolder.indexLine("Dc1.txt", "Users/Public/Desktop/notes.txt")
                + TrashFolder.indexLine("Dc2", "Users/Public/Desktop/old");
        assertEquals(Map.of("Dc1.txt", "Users/Public/Desktop/notes.txt", "Dc2", "Users/Public/Desktop/old"),
                TrashFolder.readIndex(index));
        assertEquals(Map.of("Dc2", "Users/Public/Desktop/old"),
                TrashFolder.readIndex(TrashFolder.withoutEntry(index, "Dc1.txt")));
    }

    @Test
    void withoutEntry_leavesALineWhoseNameOnlyStartsTheSame() {
        final String index = TrashFolder.indexLine("a", "x/a") + TrashFolder.indexLine("ab", "x/ab");
        assertEquals(Map.of("ab", "x/ab"), TrashFolder.readIndex(TrashFolder.withoutEntry(index, "a")));
    }

    @Test
    void readNote_readsBackWhatNoteWrote() {
        assertEquals(Optional.of("home/player/Desktop/notes.txt"),
                TrashFolder.readNote(TrashFolder.note("home/player/Desktop/notes.txt")));
        assertEquals(Optional.empty(), TrashFolder.readNote("[Trash Info]\n"));
    }

    @Test
    void noteSubject_namesTheFileANoteDescribes() {
        assertEquals(Optional.of("notes.txt"), TrashFolder.noteSubject("notes.txt.trashinfo"));
        assertEquals(Optional.empty(), TrashFolder.noteSubject(".trashinfo"));
        assertEquals(Optional.empty(), TrashFolder.noteSubject("notes.txt"));
    }

    @Test
    void freePlace_numbersANameWhosePlaceWasTakenSince() {
        assertEquals("Desktop/notes.txt", TrashFolder.freePlace("Desktop/notes.txt", path -> false));
        assertEquals("Desktop/notes (2).txt",
                TrashFolder.freePlace("Desktop/notes.txt", Set.of("Desktop/notes.txt")::contains));
        assertEquals("old (2)", TrashFolder.freePlace("old", Set.of("old")::contains));
    }

    @Test
    void freePlace_staysAPathTheFilesystemWillTake() {
        /*
         * Numbering a name makes it longer, and a long name in a deep folder would make a path longer than
         * a path may be, which the filesystem refuses: the thing would never come back out of the trash.
         */
        final String dir = "a".repeat(FsPaths.MAX_NAME_LENGTH) + "/" + "b".repeat(FsPaths.MAX_NAME_LENGTH);
        final String original = dir + "/" + "c".repeat(
                FsPaths.MAX_PATH_LENGTH - dir.length() - 1 - ".txt".length()) + ".txt";
        assertTrue(FsPaths.isValidPath(original, FilesystemKind.HIERARCHICAL), "the fixture is a real path");
        final String back = TrashFolder.freePlace(original, Set.of(original)::contains);
        assertTrue(FsPaths.isValidPath(back, FilesystemKind.HIERARCHICAL),
                "put back at " + back.length() + " characters, which is not a path");
        assertTrue(back.endsWith(" (2).txt"), "it should still be numbered; got " + back);
    }
}
