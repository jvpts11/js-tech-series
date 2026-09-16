/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.edit.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.vm.listing.AsmProgram;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProjectFileTest {

    private static ProjectFile stockWatch() {
        return new ProjectFile("StockWatch", ProjectFile.Kind.SCRIPT, "jsc:sigma",
                List.of("StockWatch.sgs", "Helpers.sgs"), List.of("Helpers"), "build/StockWatch.asm");
    }

    @Test
    void write_thenRead_roundTripsEveryFact() {
        final ProjectFile back = ProjectFile.read(stockWatch().write());
        assertEquals(stockWatch(), back);
    }

    @Test
    void write_isOneFactPerLineAPlayerCanRead() {
        final String text = stockWatch().write();
        assertTrue(text.contains("name: StockWatch\n"));
        assertTrue(text.contains("kind: script\n"));
        assertTrue(text.contains("sources: StockWatch.sgs, Helpers.sgs\n"));
        assertTrue(text.contains("references: Helpers\n"));
        assertTrue(text.contains("entry: build/StockWatch.asm\n"));
        assertTrue(text.contains("platform: jsc:x86\n"));
    }

    @Test
    void read_aProjectWithNoPlatformLine_isBuiltForTheOldestThatRunsIt() {
        final ProjectFile back = ProjectFile.read("name: Old\nkind: console\nlanguage: jsc:sigma\n"
                + "sources: Old.sgs\nreferences: \nentry: build/Old.asm\n");
        assertEquals(AsmProgram.DEFAULT_ARCHITECTURE, back.platform());
    }

    @Test
    void withPlatform_buildsForThatOneInstead() {
        assertEquals("jsc:x86_64", stockWatch().withPlatform("jsc:x86_64").platform());
        assertEquals("jsc:x86_64",
                ProjectFile.read(stockWatch().withPlatform("jsc:x86_64").write()).platform());
    }

    @Test
    void withPlatform_theOneItAlreadyHas_isTheSameProject() {
        final ProjectFile project = stockWatch();
        assertSame(project, project.withPlatform(project.platform()));
    }

    /*
     * A project keeps its platform through every other change. Rebuilding the record by hand in each of these is
     * exactly where a field gets dropped, and dropping this one would quietly send a program back to the default.
     */
    @Test
    void everyOtherChange_keepsThePlatform() {
        final ProjectFile built = stockWatch().withPlatform("jsc:x86_64");
        assertEquals("jsc:x86_64", built.withSource("More.sgs").platform());
        assertEquals("jsc:x86_64", built.withoutSource("Helpers.sgs").platform());
        assertEquals("jsc:x86_64", built.withReference("Other").platform());
    }

    @Test
    void read_skipsWhatItCannotReadAndFillsWhatIsMissing() {
        final ProjectFile file = ProjectFile.read("name: A\nthis line is nonsense\nkind: sideways\n");
        assertEquals("A", file.name());
        assertEquals(ProjectFile.Kind.EMPTY, file.kind());
        assertTrue(file.sources().isEmpty());
        assertEquals("", file.entry());
        assertEquals("", ProjectFile.read(null).name());
    }

    @Test
    void read_trimsListsAndDropsEmptyEntries() {
        final ProjectFile file = ProjectFile.read("sources:  a.sgs ,, b.sgs , \n");
        assertEquals(List.of("a.sgs", "b.sgs"), file.sources());
    }

    @Test
    void withSource_addsOnceAndKeepsOrder() {
        final ProjectFile more = stockWatch().withSource("Format.sgs");
        assertEquals(List.of("StockWatch.sgs", "Helpers.sgs", "Format.sgs"), more.sources());
        assertSame(more, more.withSource("Format.sgs"));
    }

    @Test
    void withReference_neverReferencesItself() {
        final ProjectFile file = stockWatch();
        assertSame(file, file.withReference("StockWatch"));
        assertEquals(List.of("Helpers", "Format"), stockWatch().withReference("Format").references());
    }

    @Test
    void buildsAListing_isFalseForALibraryAndForNoEntry() {
        assertTrue(stockWatch().buildsAListing());
        assertFalse(new ProjectFile("L", ProjectFile.Kind.LIBRARY, "jsc:sigma", List.of(), List.of(), "").buildsAListing());
        assertFalse(new ProjectFile("E", ProjectFile.Kind.CONSOLE, "jsc:sigma", List.of(), List.of(), "").buildsAListing());
    }

    @Test
    void kind_readsItsOwnWordsWhateverTheCase() {
        assertEquals(ProjectFile.Kind.CONSOLE, ProjectFile.Kind.of("Console"));
        assertEquals(ProjectFile.Kind.LIBRARY, ProjectFile.Kind.of(" library "));
        assertEquals(ProjectFile.Kind.EMPTY, ProjectFile.Kind.of("bogus"));
        assertEquals(ProjectFile.Kind.EMPTY, ProjectFile.Kind.of(null));
    }

    @Test
    void fileName_andDefaultEntry_followTheName() {
        assertEquals("StockWatch.sgsproj", ProjectFile.fileName("StockWatch"));
        assertEquals("build/StockWatch.asm", ProjectFile.defaultEntry("StockWatch"));
    }
}
