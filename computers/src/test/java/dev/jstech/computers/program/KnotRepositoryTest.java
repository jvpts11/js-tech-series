/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.program.KnotRepository.DiffLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class KnotRepositoryTest {

    private KnotRepository repo;

    @BeforeEach
    void setUp() {
        repo = new KnotRepository("eastwing");
    }

    private static long countOf(final List<DiffLine> lines, final DiffLine.Kind kind) {
        return lines.stream().filter(line -> line.kind() == kind).count();
    }

    @Test
    void aRepository_keepsItsNameInLowerCase() {
        assertEquals("eastwing", repo.name());
        assertEquals("mixed", new KnotRepository("MiXeD").name());
    }

    @Test
    void aRepository_refusesNoNameAtAll() {
        assertThrows(IllegalArgumentException.class, () -> new KnotRepository(""));
        assertThrows(IllegalArgumentException.class, () -> new KnotRepository(null));
    }

    @Test
    void commit_savesAFileAndNumbersIt() {
        final KnotRepository.Revision first = repo.commit("plant.sgs", "ada", "first pass", "one", 10L);
        assertNotNull(first);
        assertEquals(1, first.number());
        assertEquals("r1", first.label());
        assertEquals("ada", first.author());
        assertEquals(List.of("plant.sgs"), repo.files());
    }

    @Test
    void commit_numbersEveryRevisionAcrossEveryFile() {
        repo.commit("a.sgs", "ada", "one", "1", 1L);
        repo.commit("b.sgs", "grace", "two", "2", 2L);
        final KnotRepository.Revision third = repo.commit("a.sgs", "ada", "three", "3", 3L);
        assertNotNull(third);
        assertEquals(3, third.number(), "revisions are numbered across the repository, not per file");
    }

    @Test
    void commit_refusesTheSameTextTwice() {
        repo.commit("plant.sgs", "ada", "first", "the same", 1L);
        assertNull(repo.commit("plant.sgs", "ada", "again", "the same", 2L),
                "committing unchanged text makes a history nobody can read");
        assertEquals(1, repo.revisionsOf("plant.sgs").size());
    }

    @Test
    void commit_refusesNonsense() {
        assertNull(repo.commit("", "ada", "m", "x", 1L));
        assertNull(repo.commit(null, "ada", "m", "x", 1L));
        assertNull(repo.commit("a.sgs", "ada", "m", null, 1L));
    }

    @Test
    void commit_withNoMessageSaysSoRatherThanBeingBlank() {
        final KnotRepository.Revision revision = repo.commit("a.sgs", "ada", "", "x", 1L);
        assertNotNull(revision);
        assertEquals("no message", revision.message());
    }

    @Test
    void head_isTheNewestTextOfAFile() {
        repo.commit("plant.sgs", "ada", "one", "first", 1L);
        repo.commit("plant.sgs", "ada", "two", "second", 2L);
        assertEquals("second", repo.head("plant.sgs"));
        assertNull(repo.head("nothing.sgs"));
    }

    @Test
    void revisionsOf_areOldestFirst() {
        repo.commit("plant.sgs", "ada", "one", "first", 1L);
        repo.commit("plant.sgs", "grace", "two", "second", 2L);
        final List<KnotRepository.Revision> revisions = repo.revisionsOf("plant.sgs");
        assertEquals(2, revisions.size());
        assertEquals("first", revisions.get(0).content());
        assertEquals("second", revisions.get(1).content());
    }

    @Test
    void recent_isNewestFirstAcrossEveryFile() {
        repo.commit("a.sgs", "ada", "one", "1", 1L);
        repo.commit("b.sgs", "grace", "two", "2", 2L);
        repo.commit("a.sgs", "linus", "three", "3", 3L);
        final List<KnotRepository.Revision> recent = repo.recent(10);
        assertEquals(3, recent.size());
        assertEquals(3, recent.get(0).number());
        assertEquals(1, recent.get(2).number());
    }

    @Test
    void recent_givesAtMostWhatWasAskedFor() {
        for (int i = 0; i < 10; i++) {
            repo.commit("a.sgs", "ada", "m", "content " + i, i);
        }
        assertEquals(4, repo.recent(4).size());
    }

    @Test
    void aFileKeepsOnlyAsManyRevisionsAsItHolds() {
        for (int i = 0; i < KnotRepository.MAX_REVISIONS + 20; i++) {
            repo.commit("a.sgs", "ada", "m", "content " + i, i);
        }
        assertEquals(KnotRepository.MAX_REVISIONS, repo.revisionsOf("a.sgs").size());
        assertEquals("content " + (KnotRepository.MAX_REVISIONS + 19), repo.head("a.sgs"));
    }

    @Test
    void aRepository_refusesMoreFilesThanItHolds() {
        for (int i = 0; i < KnotRepository.MAX_FILES; i++) {
            assertNotNull(repo.commit("file" + i + ".sgs", "ada", "m", "x" + i, i));
        }
        assertNull(repo.commit("one_too_many.sgs", "ada", "m", "x", 1L));
    }

    @Test
    void fileOf_andRevision_findARevisionByItsNumber() {
        repo.commit("a.sgs", "ada", "one", "1", 1L);
        repo.commit("b.sgs", "grace", "two", "2", 2L);
        assertEquals("b.sgs", repo.fileOf(2));
        assertNotNull(repo.revision(2));
        assertEquals("grace", repo.revision(2).author());
        assertNull(repo.fileOf(99));
        assertNull(repo.revision(99));
    }

    @Test
    void bytes_growWithWhatIsKept() {
        final long empty = repo.bytes();
        repo.commit("a.sgs", "ada", "one", "a line of code", 1L);
        final long one = repo.bytes();
        assertTrue(one > empty);
        repo.commit("a.sgs", "ada", "two", "a longer line of code than before", 2L);
        assertTrue(repo.bytes() > one, "keeping a second revision costs more");
    }

    @Test
    void restore_bringsAFileBackAndKeepsNumberingAfterIt() {
        repo.commit("a.sgs", "ada", "one", "1", 1L);
        repo.commit("a.sgs", "ada", "two", "2", 2L);
        final List<KnotRepository.Revision> kept = repo.revisionsOf("a.sgs");

        final KnotRepository other = new KnotRepository("eastwing");
        other.restore("a.sgs", kept);
        assertEquals(2, other.revisionsOf("a.sgs").size());
        final KnotRepository.Revision next = other.commit("a.sgs", "grace", "three", "3", 3L);
        assertNotNull(next);
        assertEquals(3, next.number(), "numbering must carry on from what was restored");
    }

    @Test
    void restore_ofARevisionNumberedAtTheVeryTopStillLetsTheNextCommitThrough() {
        /*
         * A file somebody edited can name a revision right at the largest a number holds. The next number
         * after that wraps round to a negative one, and a revision refuses to be made with it by throwing,
         * which would take the commit down instead of answering it.
         */
        repo.restore("a.sgs", List.of(
                new KnotRepository.Revision(Integer.MAX_VALUE, "ada", "from a damaged file", 1L, "x")));
        final KnotRepository.Revision next = repo.commit("a.sgs", "grace", "after it", "y", 2L);
        assertNotNull(next, "the next commit must be answered rather than throw");
        assertTrue(next.number() >= 1, "and it must be a revision that can exist");
    }

    @Test
    void restore_ofNothingDoesNothing() {
        repo.restore("a.sgs", null);
        repo.restore(null, List.of());
        repo.restore("a.sgs", List.of());
        assertTrue(repo.files().isEmpty());
    }

    @Test
    void diff_marksWhatArrivedAndWhatWentAway() {
        final List<DiffLine> lines = KnotRepository.diff(
                "int floor = 8000;\nrequest(floor);\n",
                "int floor = 10000;\nrequest(floor);\nlog(floor);\n");
        assertEquals(1, countOf(lines, DiffLine.Kind.REMOVED));
        assertEquals(2, countOf(lines, DiffLine.Kind.ADDED));
        assertTrue(countOf(lines, DiffLine.Kind.CONTEXT) >= 1);
    }

    @Test
    void diff_ofTheSameTextIsAllContext() {
        final List<DiffLine> lines = KnotRepository.diff("one\ntwo\n", "one\ntwo\n");
        assertEquals(0, countOf(lines, DiffLine.Kind.ADDED));
        assertEquals(0, countOf(lines, DiffLine.Kind.REMOVED));
    }

    @Test
    void diff_againstNothingIsAllAdded() {
        final List<DiffLine> lines = KnotRepository.diff("", "one\ntwo");
        assertEquals(2, countOf(lines, DiffLine.Kind.ADDED));
    }

    @Test
    void diff_ofNullsDoesNotThrow() {
        assertNotNull(KnotRepository.diff(null, null));
        assertNotNull(KnotRepository.diff(null, "x"));
        assertNotNull(KnotRepository.diff("x", null));
    }

    @Test
    void diff_keepsEveryLineOfTheNewerTextSomewhere() {
        // Nothing may be lost: every line of what is there now has to appear as context or as added.
        final String after = "alpha\nbeta\ngamma\ndelta";
        final List<DiffLine> lines = KnotRepository.diff("alpha\nzeta\ngamma", after);
        for (final String wanted : after.split("\n")) {
            assertTrue(lines.stream().anyMatch(line -> line.text().equals(wanted)
                            && line.kind() != DiffLine.Kind.REMOVED),
                    "the comparison lost the line: " + wanted);
        }
    }

    @Test
    void aRevision_refusesNonsense() {
        assertThrows(IllegalArgumentException.class,
                () -> new KnotRepository.Revision(0, "ada", "m", 1L, "x"));
        assertThrows(IllegalArgumentException.class,
                () -> new KnotRepository.Revision(1, "", "m", 1L, "x"));
        assertThrows(IllegalArgumentException.class,
                () -> new KnotRepository.Revision(1, "ada", "m", 1L, null));
    }
}
