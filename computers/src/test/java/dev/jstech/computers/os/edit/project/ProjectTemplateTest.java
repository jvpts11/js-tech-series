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
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.sigma.LanguageLevel;
import dev.jstech.computers.sigma.SigmaCompiler;
import dev.jstech.computers.sigma.SourceFile;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProjectTemplateTest {

    private static final LanguageLevel SHARP = LanguageLevel.SIGMA_SHARP;
    private static final LanguageLevel SIGMA = LanguageLevel.SIGMA;

    @Test
    void project_startsWithOneSourceNamedAfterTheProject() {
        final ProjectFile file = ProjectTemplate.CONSOLE_APP.project("Sorter", SHARP);
        assertEquals("Sorter", file.name());
        assertEquals(ProjectFile.Kind.CONSOLE, file.kind());
        assertEquals(List.of("Sorter.sgs"), file.sources());
        assertEquals("build/Sorter.asm", file.entry());
        assertEquals("jsc:sigma_sharp", file.language());
        assertEquals("Sorter.sgsproj", file.fileName());
    }

    /** A project in the smaller language is its own kind of file, of its own sources, for the oldest machines. */
    @Test
    void project_inTheSmallerLanguage_isItsOwnKindOfFileBuiltForTheOldestMachines() {
        final ProjectFile file = ProjectTemplate.CONSOLE_APP.project("Sorter", SIGMA);
        assertEquals(List.of("Sorter.sg"), file.sources());
        assertEquals("jsc:sigma", file.language());
        assertEquals("Sorter.sgproj", file.fileName());
        assertEquals("jsc:x86_16", file.platform());
        assertEquals("jsc:x86", ProjectTemplate.CONSOLE_APP.project("Sorter", SHARP).platform());
        assertEquals(file, ProjectFile.read(file.write()), "and it reads back as it was written");
    }

    @Test
    void isProjectFile_knowsBothKindsAndNothingElse() {
        assertTrue(ProjectFile.isProjectFile("Projects/Farm/Farm.sgsproj"));
        assertTrue(ProjectFile.isProjectFile("Projects/Farm/Farm.SGPROJ"));
        assertFalse(ProjectFile.isProjectFile("Projects/Farm/Farm.sln"));
        assertFalse(ProjectFile.isProjectFile("Projects/Farm/Farm.sg"));
        assertEquals("Farm/Farm.sgproj",
                SolutionFile.projectPath(ProjectTemplate.EMPTY_PROJECT.project("Farm", SIGMA)));
    }

    @Test
    void project_ofALibraryOrEmptyHasNoEntry() {
        assertEquals("", ProjectTemplate.CLASS_LIBRARY.project("Helpers", SHARP).entry());
        final ProjectFile empty = ProjectTemplate.EMPTY_PROJECT.project("Blank", SHARP);
        assertEquals("", empty.entry());
        assertTrue(empty.sources().isEmpty());
        assertEquals("", ProjectTemplate.EMPTY_PROJECT.firstSource("Blank", SHARP));
    }

    @Test
    void source_namesTheClassAfterTheProject() {
        assertTrue(ProjectTemplate.CONSOLE_APP.source("Sorter", SHARP).contains("class Sorter {"));
        assertTrue(ProjectTemplate.CONSOLE_APP.source("Sorter", SHARP).contains("static void Main()"));
        assertTrue(ProjectTemplate.SCRIPT.source("Watch", SHARP).contains("class Watch : IScript"));
        assertTrue(ProjectTemplate.SCRIPT.source("Watch", SHARP).contains("OnTick()"));
        assertTrue(ProjectTemplate.CLASS_LIBRARY.source("Helpers", SHARP).contains("class Helpers {"));
        assertEquals("", ProjectTemplate.EMPTY_PROJECT.source("Blank", SHARP));
    }

    /** The smaller language has no interfaces, so its script stands on the class and opens its one namespace. */
    @Test
    void source_inTheSmallerLanguage_standsOnScriptAndOpensItsOneNamespace() {
        final String script = ProjectTemplate.SCRIPT.source("Watch", SIGMA);
        assertTrue(script.contains("class Watch : Script"), script);
        assertTrue(script.contains("public override void OnTick()"), script);
        assertTrue(script.contains("using Standard.*;"), script);
        assertFalse(script.contains("System"), script);
    }

    /**
     * Every line of the studio's list starts as something that already builds, in the language it says it is
     * in. A library has no entry of its own, so it is built the way it always is: into a program that uses it.
     */
    @Test
    void everyOffer_startsAsSomethingThatBuildsInItsOwnLanguage() {
        for (final ProjectTemplate.Offer offer : ProjectTemplate.Offer.all()) {
            if (offer.template() == ProjectTemplate.EMPTY_PROJECT) {
                continue;
            }
            final List<SourceFile> sources = new ArrayList<>();
            sources.add(new SourceFile(offer.firstSource("Sample"), offer.source("Sample")));
            if (offer.template() == ProjectTemplate.CLASS_LIBRARY) {
                sources.add(new SourceFile("Uses.sg", "using Sample.*; namespace Uses; class Uses { "
                        + "static void Main() { int answer = Sample.Answer(); } }"));
            }
            final SigmaCompiler.Result built =
                    SigmaCompiler.compile(sources, offer.project("Sample").platform(), offer.language());
            assertTrue(built.ok(), offer.title() + " in " + offer.language().mark() + ": " + built.diagnostics());
        }
    }

    @Test
    void offers_areEveryShapeInBothLanguagesTheFullOneFirst() {
        final List<ProjectTemplate.Offer> all = ProjectTemplate.Offer.all();
        assertEquals(ProjectTemplate.values().length * 2, all.size());
        assertEquals(new ProjectTemplate.Offer(ProjectTemplate.CONSOLE_APP, SHARP), all.getFirst());
        assertEquals(SIGMA, all.getLast().language());
    }

    @Test
    void tags_carryTheLanguageThePlatformsAndTheKind() {
        assertEquals(List.of("Σ#", "Frames", "Linux", "Console"), ProjectTemplate.CONSOLE_APP.tags(SHARP));
        assertEquals(List.of("Σ#", "Frames", "Linux"), ProjectTemplate.EMPTY_PROJECT.tags(SHARP));
        assertEquals(List.of("Σ", "Frames", "Linux", "Script"), ProjectTemplate.SCRIPT.tags(SIGMA));
    }

    @Test
    void isKind_filtersByTheKindWordOrNotAtAll() {
        assertTrue(ProjectTemplate.SCRIPT.isKind("Script"));
        assertFalse(ProjectTemplate.SCRIPT.isKind("Console"));
        assertTrue(ProjectTemplate.SCRIPT.isKind(""));
        assertTrue(ProjectTemplate.SCRIPT.isKind(null));
    }
}
