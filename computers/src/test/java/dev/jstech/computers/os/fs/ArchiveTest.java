/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.fs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

class ArchiveTest {

    private static StoredFile file(final String name, final FileType type, final String content) {
        return new StoredFile(name, type, content);
    }

    private static int bytes(final String text) {
        return text.getBytes(StandardCharsets.UTF_8).length;
    }

    @Test
    void pack_thenUnpack_givesBackExactlyWhatWentIn() {
        final List<StoredFile> in = List.of(
                file("notes.txt", FileType.TXT, "the smelter stalled again, bay 3"),
                file("router.cfg", FileType.CFG, "uplink=hbw\nport=4\n"),
                file("plant.sgs", FileType.SGS, "int floor = 10000;\nConsole.Write(floor);\n"));
        final List<StoredFile> out = Archive.unpack(Archive.pack(in));
        assertEquals(in.size(), out.size());
        for (int i = 0; i < in.size(); i++) {
            assertEquals(in.get(i).path(), out.get(i).path());
            assertEquals(in.get(i).type(), out.get(i).type());
            assertEquals(in.get(i).content(), out.get(i).content());
        }
    }

    @Test
    void pack_survivesTextThatIsNotPlainAscii() {
        /*
         * Built from code points rather than written out, because the point is that letters costing more
         * than one byte in UTF-8 survive the round trip, and the source file itself stays plain ASCII.
         */
        final String awkward = "an accent " + Character.toString(0x00E9)
                + ", a cedilla " + Character.toString(0x00E7)
                + ", a greek capital " + Character.toString(0x03A3)
                + ", a tab\there\nand a newline";
        final List<StoredFile> out = Archive.unpack(Archive.pack(List.of(
                file("odd.txt", FileType.TXT, awkward))));
        assertEquals(1, out.size());
        assertEquals(awkward, out.get(0).content());
    }

    @Test
    void pack_survivesAnEmptyFile() {
        final List<StoredFile> out = Archive.unpack(Archive.pack(List.of(
                file("empty.txt", FileType.TXT, ""),
                file("after.txt", FileType.TXT, "still here"))));
        assertEquals(2, out.size());
        assertEquals("", out.get(0).content());
        assertEquals("still here", out.get(1).content());
    }

    @Test
    void anArchive_weighsLessThanWhatWentIntoIt() {
        /*
         * A log is the case the program exists for: lines that repeat pack down to almost nothing, which is
         * how a machine short of room gets its disk back.
         */
        final StringBuilder log = new StringBuilder();
        for (int i = 0; i < 400; i++) {
            log.append("[12:0").append(i % 10).append("] smelter: waiting on steel_ingot\n");
        }
        final String content = log.toString();
        final String packed = Archive.pack(List.of(file("plant.log", FileType.LOG, content)));
        assertTrue(bytes(packed) < bytes(content) / 2,
                "a repetitive log should pack to less than half: " + bytes(packed) + " of " + bytes(content));
    }

    @Test
    void twoSimilarFiles_costBarelyMoreThanOne() {
        // They are compressed together, which is why the second one is nearly free.
        final StringBuilder text = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            text.append("the same line over and over again, line ").append(i).append('\n');
        }
        final String one = text.toString();
        final int single = bytes(Archive.pack(List.of(file("a.txt", FileType.TXT, one))));
        final int both = bytes(Archive.pack(List.of(
                file("a.txt", FileType.TXT, one), file("b.txt", FileType.TXT, one))));
        assertTrue(both < single * 3 / 2,
                "the second copy should be nearly free: " + single + " became " + both);
    }

    @Test
    void entries_readTheListingWithoutUnpacking() {
        final String packed = Archive.pack(List.of(
                file("notes.txt", FileType.TXT, "hello"),
                file("plant.log", FileType.LOG, "a log line")));
        final List<Archive.Entry> entries = Archive.entries(packed);
        assertEquals(2, entries.size());
        assertEquals("notes.txt", entries.get(0).name());
        assertEquals(FileType.TXT, entries.get(0).type());
        assertEquals(5, entries.get(0).originalBytes());
        assertEquals("plant.log", entries.get(1).name());
        assertEquals(FileType.LOG, entries.get(1).type());
    }

    @Test
    void originalBytes_addsUpWhatWentIn() {
        final String packed = Archive.pack(List.of(
                file("a.txt", FileType.TXT, "12345"),
                file("b.txt", FileType.TXT, "678")));
        assertEquals(8, Archive.originalBytes(packed));
    }

    @Test
    void unpackOne_findsAFileByName() {
        final String packed = Archive.pack(List.of(
                file("a.txt", FileType.TXT, "first"),
                file("b.txt", FileType.TXT, "second")));
        final StoredFile found = Archive.unpackOne(packed, "b.txt");
        assertNotNull(found);
        assertEquals("second", found.content());
        assertNull(Archive.unpackOne(packed, "missing.txt"));
    }

    @Test
    void isArchive_saysNoToOrdinaryText() {
        assertFalse(Archive.isArchive("just some notes"));
        assertFalse(Archive.isArchive(""));
        assertFalse(Archive.isArchive(null));
        assertTrue(Archive.isArchive(Archive.pack(List.of(file("a.txt", FileType.TXT, "x")))));
    }

    @Test
    void entries_ofSomethingThatIsNotAnArchiveIsEmpty() {
        assertTrue(Archive.entries("hello").isEmpty());
        assertTrue(Archive.unpack("hello").isEmpty());
    }

    @Test
    void unpack_refusesAnArchiveWhoseBodyWasDamaged() {
        final String packed = Archive.pack(List.of(file("a.txt", FileType.TXT, "hello there")));
        final int split = packed.indexOf("\n\n");
        final String broken = packed.substring(0, split + 2) + "not base64 at all!!!";
        assertTrue(Archive.unpack(broken).isEmpty(), "a damaged archive should give nothing back");
        // Its listing is still readable, which is what keeps a damaged file explainable.
        assertEquals(1, Archive.entries(broken).size());
    }

    @Test
    void unpack_refusesAListingThatPromisesMoreThanTheBodyHolds() {
        final String packed = Archive.pack(List.of(file("a.txt", FileType.TXT, "hello")));
        final String lying = packed.replace("a.txt\ttxt\t5", "a.txt\ttxt\t500");
        assertTrue(Archive.unpack(lying).isEmpty());
    }

    @Test
    void unpack_refusesASizeSoLargeItWouldWrapRound() {
        /*
         * A listing edited by hand can name a size at the very top of what a number holds. Added to the
         * position it would wrap round to a negative one, pass a check written as a sum, and then read
         * off the end of what is there.
         */
        final String packed = Archive.pack(List.of(file("a.txt", FileType.TXT, "hello")));
        final String lying = packed.replace("a.txt\ttxt\t5", "a.txt\ttxt\t" + Integer.MAX_VALUE);
        assertTrue(Archive.unpack(lying).isEmpty(), "a size that would wrap round must be refused");
    }

    @Test
    void pack_refusesNothingAtAll() {
        assertThrows(IllegalArgumentException.class, () -> Archive.pack(List.of()));
        assertThrows(IllegalArgumentException.class, () -> Archive.pack(null));
    }

    @Test
    void pack_refusesTwoFilesThatWouldShareAName() {
        assertThrows(IllegalArgumentException.class, () -> Archive.pack(List.of(
                file("logs/plant.log", FileType.LOG, "a"),
                file("old/plant.log", FileType.LOG, "b"))));
    }

    @Test
    void pack_refusesANameHoldingASeparator() {
        assertThrows(IllegalArgumentException.class, () -> Archive.pack(List.of(
                file("od\td.txt", FileType.TXT, "x"))));
    }

    @Test
    void pack_keepsOnlyTheNameOfAFileInAFolder() {
        final String packed = Archive.pack(List.of(
                file("logs/plant.log", FileType.LOG, "a line")));
        assertEquals("plant.log", Archive.entries(packed).get(0).name());
        assertEquals("plant.log", Archive.unpack(packed).get(0).path());
    }

    @Test
    void leaf_takesTheNameOffAPath() {
        assertEquals("a.txt", Archive.leaf("folder/deeper/a.txt"));
        assertEquals("a.txt", Archive.leaf("a.txt"));
    }

    @Test
    void entry_refusesNonsense() {
        assertThrows(IllegalArgumentException.class, () -> new Archive.Entry("", FileType.TXT, 1));
        assertThrows(IllegalArgumentException.class, () -> new Archive.Entry("a", null, 1));
        assertThrows(IllegalArgumentException.class, () -> new Archive.Entry("a", FileType.TXT, -1));
    }

    @Test
    void pack_refusesMoreFilesThanAnArchiveHolds() {
        final StoredFile[] many = new StoredFile[Archive.MAX_ENTRIES + 1];
        for (int i = 0; i < many.length; i++) {
            many[i] = file("f" + i + ".txt", FileType.TXT, "x");
        }
        assertThrows(IllegalArgumentException.class, () -> Archive.pack(List.of(many)));
    }

    @Test
    void ark_isAFileTypeThatCannotBeEditedByHand() {
        assertEquals(FileType.ARK, FileType.of(Archive.EXTENSION));
        assertFalse(FileType.ARK.userEditable(), "a compressed file must not be typed into");
    }
}
