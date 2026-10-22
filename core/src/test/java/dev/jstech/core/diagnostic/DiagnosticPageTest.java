/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.diagnostic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class DiagnosticPageTest {

    private static final String FILE = """
            J's Tech Series diagnostics
            Minecraft 1.21.1, NeoForge 21.1.248

            == Mods ==
            jscore 0.6.0a
            <script>alert(1)</script> 1.0

            == Sections ==
            jscore:world_edits 0.4 ms, budget 2 ms
            jscore:physics stalled 3 times
            """;

    @Test
    void sections_takeTheHeaderAndEachHeadingInOrder() {
        final List<DiagnosticPage.Section> sections = DiagnosticPage.sections(FILE);
        assertEquals(3, sections.size());
        assertEquals("J's Tech Series diagnostics", sections.get(0).title());
        assertEquals(List.of("Minecraft 1.21.1, NeoForge 21.1.248"), sections.get(0).lines());
        assertEquals("Mods", sections.get(1).title());
        assertEquals(2, sections.get(1).lines().size());
        assertEquals("Sections", sections.get(2).title());
    }

    @Test
    void html_foldsEachSectionAndKeepsTheFilesWordsEscaped() {
        final String page = DiagnosticPage.html("diagnostics-1", FILE);
        assertEquals(3, page.split("<details", -1).length - 1);
        assertTrue(page.contains("<summary>Mods<small>2</small></summary>"));
        assertTrue(page.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertFalse(page.contains("<script>alert(1)"));
    }

    @Test
    void marked_findsTheLinesThatWarn() {
        assertTrue(DiagnosticPage.marked("jscore:physics stalled 3 times"));
        assertTrue(DiagnosticPage.marked("WARN something"));
        assertFalse(DiagnosticPage.marked("jscore 0.6.0a"));
        assertTrue(DiagnosticPage.html("d", FILE).contains("<span class=\"marked\">jscore:physics stalled"));
    }
}
