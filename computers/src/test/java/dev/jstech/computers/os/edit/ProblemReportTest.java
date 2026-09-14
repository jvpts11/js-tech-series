/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.edit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.language.IProgrammingLanguage;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProblemReportTest {

    private static IProgrammingLanguage.Complaint at(final int line, final String message) {
        return new IProgrammingLanguage.Complaint("f", line, 1, "CN0001", message);
    }

    private static List<String> names(final List<ProblemReport.Row> rows) {
        return rows.stream().map(ProblemReport.Row::name).toList();
    }

    @Test
    void of_returnsNothingForAFolderWithNothingWrong() {
        final Map<String, List<IProgrammingLanguage.Complaint>> byFile = new LinkedHashMap<>();
        byFile.put("progs/a.sgs", List.of());
        byFile.put("progs/b.sgs", List.of());
        assertTrue(ProblemReport.of(byFile).isEmpty());
    }

    @Test
    void of_putsTheWorstFileFirst() {
        final Map<String, List<IProgrammingLanguage.Complaint>> byFile = new LinkedHashMap<>();
        byFile.put("progs/one.sgs", List.of(at(4, "one thing")));
        byFile.put("progs/many.sgs", List.of(at(1, "a"), at(2, "b"), at(3, "c")));
        assertEquals(List.of("many.sgs", "many.sgs", "many.sgs", "one.sgs"),
                names(ProblemReport.of(byFile)));
    }

    @Test
    void of_keepsTheCompilersOwnOrderWithinAFile() {
        final Map<String, List<IProgrammingLanguage.Complaint>> byFile = new LinkedHashMap<>();
        byFile.put("progs/a.sgs", List.of(at(9, "later"), at(2, "earlier")));
        assertEquals(List.of(9, 2),
                ProblemReport.of(byFile).stream().map(r -> r.complaint().line()).toList());
    }

    @Test
    void of_breaksATieByNameSoTheOrderNeverWobbles() {
        final Map<String, List<IProgrammingLanguage.Complaint>> byFile = new LinkedHashMap<>();
        byFile.put("progs/zeta.sgs", List.of(at(1, "x")));
        byFile.put("progs/alpha.sgs", List.of(at(1, "x")));
        assertEquals(List.of("alpha.sgs", "zeta.sgs"), names(ProblemReport.of(byFile)));
    }

    @Test
    void of_carriesTheWholePathSoARowCanBeOpened() {
        final Map<String, List<IProgrammingLanguage.Complaint>> byFile = new LinkedHashMap<>();
        byFile.put("progs/deep/a.sgs", List.of(at(1, "x")));
        assertEquals("progs/deep/a.sgs", ProblemReport.of(byFile).get(0).path());
        assertEquals("a.sgs", ProblemReport.of(byFile).get(0).name());
    }

    @Test
    void of_leavesOutTheFilesThatCompiled() {
        final Map<String, List<IProgrammingLanguage.Complaint>> byFile = new LinkedHashMap<>();
        byFile.put("progs/good.sgs", List.of());
        byFile.put("progs/bad.sgs", List.of(at(1, "x")));
        assertEquals(List.of("bad.sgs"), names(ProblemReport.of(byFile)));
    }

    @Test
    void brokenFiles_countsFilesRatherThanComplaints() {
        final Map<String, List<IProgrammingLanguage.Complaint>> byFile = new LinkedHashMap<>();
        byFile.put("progs/a.sgs", List.of(at(1, "x"), at(2, "y"), at(3, "z")));
        byFile.put("progs/b.sgs", List.of(at(1, "x")));
        byFile.put("progs/c.sgs", List.of());
        assertEquals(2, ProblemReport.brokenFiles(byFile));
        assertEquals(4, ProblemReport.of(byFile).size());
    }

    @Test
    void brokenFiles_isZeroForAFolderThatCompiles() {
        final Map<String, List<IProgrammingLanguage.Complaint>> byFile = new LinkedHashMap<>();
        byFile.put("progs/a.sgs", List.of());
        assertEquals(0, ProblemReport.brokenFiles(byFile));
        assertEquals(0, ProblemReport.brokenFiles(new LinkedHashMap<>()));
    }

    @Test
    void nameOf_readsTheLastSegmentOfAPath() {
        assertEquals("a.sgs", ProblemReport.nameOf("progs/a.sgs"));
        assertEquals("a.sgs", ProblemReport.nameOf("a.sgs"));
        assertEquals("progs/", ProblemReport.nameOf("progs/"));
    }
}
