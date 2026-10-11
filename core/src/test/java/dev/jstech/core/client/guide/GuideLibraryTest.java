/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.guide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.GuideContents;
import dev.jstech.core.guide.GuideEntry;
import dev.jstech.core.guide.GuideManual;
import dev.jstech.core.guide.GuideSection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GuideLibraryTest {

    private GuideLibrary library;

    @BeforeEach
    void setUp() {
        final Map<String, GuideChapter> chapters = new LinkedHashMap<>();
        chapters.put("mod", new GuideChapter("mod", 20, "mod.title", "", ""));
        chapters.put("mod:shell", new GuideChapter("mod:shell", 30, "shell.title", "", "", false));
        chapters.put("mod:frames", new GuideChapter("mod:frames", 40, "frames.title", "", "", false));
        chapters.put("other", new GuideChapter("other", 10, "other.title", "", ""));
        final Map<String, GuideSection> sections = new LinkedHashMap<>();
        sections.put("mod:basics", new GuideSection("mod:basics", 0, "basics", ""));
        sections.put("mod:commands", new GuideSection("mod:commands", 1, "commands", "", "mod:shell"));
        sections.put("mod:editions", new GuideSection("mod:editions", 2, "editions", "", "mod:frames"));
        sections.put("other:things", new GuideSection("other:things", 0, "things", ""));
        final Map<String, GuideEntry> entries = new LinkedHashMap<>();
        entries.put("mod:welcome", entry("mod:welcome", "mod:basics"));
        entries.put("mod:ls", entry("mod:ls", "mod:commands"));
        entries.put("mod:xp", entry("mod:xp", "mod:editions"));
        entries.put("other:thing", entry("other:thing", "other:things"));
        final Map<String, GuideManual> manuals = new LinkedHashMap<>();
        manuals.put("core:reference", manual("core:reference", 100, GuideManual.EVERY_CHAPTER));
        manuals.put("mod:guide", manual("mod:guide", 50, "mod"));
        manuals.put("mod:linux", manual("mod:linux", 50, "mod:shell", "mod"));
        manuals.put("mod:frames_kit", manual("mod:frames_kit", 50, "mod:frames", "mod:shell"));
        this.library = new GuideLibrary(chapters, sections, entries, manuals, Map.of());
    }

    @Test
    void contentsOf_printsTheChaptersABookNamesInTheOrderItNamesThem() {
        assertEquals(List.of("mod:shell", "mod"), chapterIds(this.library.contentsOf(manual("mod:linux"))));
        assertEquals(List.of("mod:frames", "mod:shell"), chapterIds(this.library.contentsOf(
                manual("mod:frames_kit"))));
    }

    @Test
    void contentsOf_bindsOneChapterIntoSeveralBooks() {
        assertTrue(chapterIds(this.library.contentsOf(manual("mod:linux"))).contains("mod:shell"));
        assertTrue(chapterIds(this.library.contentsOf(manual("mod:frames_kit"))).contains("mod:shell"));
    }

    @Test
    void contentsOf_leavesABookOnlyChapterOutOfTheReference() {
        assertEquals(List.of("other", "mod"), chapterIds(this.library.contentsOf(manual("core:reference"))));
    }

    @Test
    void contentsOf_putsEachSectionInItsOwnChapter() {
        final GuideContents linux = this.library.contentsOf(manual("mod:linux"));
        assertEquals("mod:commands", linux.chapters().getFirst().sections().getFirst().section().id());
        assertEquals("mod:basics", linux.chapters().get(1).sections().getFirst().section().id());
    }

    @Test
    void manualFor_opensAnEntryInTheBookThatNamesItsChapter() {
        assertEquals("mod:guide", this.library.manualFor("mod:welcome").orElseThrow().id());
        assertEquals("mod:frames_kit", this.library.manualFor("mod:xp").orElseThrow().id());
        assertEquals("core:reference", this.library.manualFor("other:thing").orElseThrow().id());
    }

    @Test
    void manualFor_breaksATieBetweenBooksNamingTheChapterByTheirIds() {
        assertEquals("mod:frames_kit", this.library.manualFor("mod:ls").orElseThrow().id());
    }

    @Test
    void manualForTarget_findsTheBookOfASectionAndOfAChapter() {
        assertEquals("mod:frames_kit", this.library.manualForTarget("mod:editions").orElseThrow().id());
        assertEquals("mod:frames_kit", this.library.manualForTarget("mod:frames").orElseThrow().id());
        assertTrue(this.library.manualForTarget("mod:nothing").isEmpty());
    }

    @Test
    void chapterOf_followsAnEntryThroughItsSection() {
        assertEquals("mod:shell", this.library.chapterOf("mod:ls").orElseThrow().id());
        assertTrue(this.library.chapterOf("mod:nothing").isEmpty());
    }

    private GuideManual manual(final String id) {
        return this.library.manual(id).orElseThrow();
    }

    private static GuideManual manual(final String id, final int priority, final String... chapters) {
        return new GuideManual(id, id + ".title", List.of(), "", "", "core:binder", List.of(chapters), List.of(),
                priority, "");
    }

    private static GuideEntry entry(final String id, final String section) {
        return new GuideEntry(id, section, 0, id + ".title", "", List.of(), List.of());
    }

    private static List<String> chapterIds(final GuideContents contents) {
        return contents.chapters().stream().map(chapter -> chapter.chapter().id()).toList();
    }
}
