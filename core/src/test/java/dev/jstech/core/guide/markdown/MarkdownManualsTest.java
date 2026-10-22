/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide.markdown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.GuideEntry;
import dev.jstech.core.guide.GuideFamilies;
import dev.jstech.core.guide.GuideManual;
import dev.jstech.core.guide.GuideSection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MarkdownManualsTest {

    private static final String KILN = """
            ---
            section: machines
            order: 3
            icon: mymod:kiln
            covers: [mymod:kiln]
            covers_family: mymod:test_kiln_parts
            ---
            # Kiln

            It fires clay.
            """;
    private static final String KILN_PT = """
            ---
            section: machines
            order: 3
            covers: [mymod:kiln]
            ---
            # Forno

            Ele queima argila.
            """;

    @Test
    void gather_readsEachKindOfFileFromItsHeader() {
        final MarkdownManuals.Gathered read = MarkdownManuals.gather(List.of(
                file("en_us", "chapter", """
                        ---
                        kind: chapter
                        order: 40
                        tab: "#FF336699"
                        ---
                        # My Mod

                        Kilns and ovens.
                        """),
                file("en_us", "machines", """
                        ---
                        kind: section
                        icon: mymod:kiln
                        ---
                        # Machines
                        """),
                file("en_us", "handbook", """
                        ---
                        kind: manual
                        style: jscore:binder
                        chapters: [mymod]
                        cover: [My Mod, Reference]
                        priority: 5
                        ---
                        # The Handbook

                        Everything My Mod adds.
                        """),
                file("en_us", "kiln", KILN)), "en_us", IJsonBlocks.NONE);
        assertEquals(new GuideChapter("mymod", 40, "My Mod", "#FF336699", "Kilns and ovens.", true),
                read.chapters().get("mymod"));
        assertEquals(new GuideSection("mymod:machines", 0, "Machines", "mymod:kiln", "mymod"),
                read.sections().get("mymod:machines"));
        assertEquals(new GuideManual("mymod:guide", "The Handbook", List.of("My Mod", "Reference"), "", "",
                "jscore:binder", List.of("mymod"), List.of("Everything My Mod adds."), 5, ""),
                read.manuals().get("mymod:guide"), "a manual is named by its folder");
        final GuideEntry kiln = read.entries().get("mymod:kiln");
        assertEquals("mymod:machines", kiln.section(), "a section named without its namespace is the entry's mod's");
        assertEquals("Kiln", kiln.titleKey());
        assertEquals(3, kiln.order());
        assertTrue(read.problems().stream().noneMatch(problem -> problem.contains("kiln.md")), read.problems()
                .toString());
    }

    @Test
    void gather_readsTheReadersLanguageAndFallsBackOnEnglishFileByFile() {
        final List<ManualFile> files = List.of(file("en_us", "kiln", KILN), file("pt_br", "kiln", KILN_PT),
                file("en_us", "press", KILN.replace("# Kiln", "# Press")));
        final MarkdownManuals.Gathered read = MarkdownManuals.gather(files, "pt_br", IJsonBlocks.NONE);
        assertEquals("Forno", read.entries().get("mymod:kiln").titleKey());
        assertEquals("Press", read.entries().get("mymod:press").titleKey(), "a file the language lacks is English");
        assertEquals("Kiln", MarkdownManuals.gather(files, "de_de", IJsonBlocks.NONE).entries().get("mymod:kiln")
                .titleKey());
    }

    @Test
    void gather_coversTheItemsOfAFamilyAsAskedForNow() {
        GuideFamiliesFixture.registerOnce();
        final GuideEntry kiln = MarkdownManuals.gather(List.of(file("en_us", "kiln", KILN)), "en_us",
                IJsonBlocks.NONE).entries().get("mymod:kiln");
        assertEquals(List.of("mymod:kiln", "mymod:kiln_door", "mymod:kiln_grate"), kiln.items());
    }

    @Test
    void gather_leavesOutWhatIsWrongAndSaysWhere() {
        final MarkdownManuals.Gathered read = MarkdownManuals.gather(List.of(
                file("en_us", "untitled", "---\nsection: machines\n---\nNo heading."),
                file("en_us", "homeless", "# Nowhere"),
                file("en_us", "a/kiln", KILN), file("en_us", "b/kiln", KILN)), "en_us", IJsonBlocks.NONE);
        assertFalse(read.entries().containsKey("mymod:untitled"));
        assertFalse(read.entries().containsKey("mymod:homeless"));
        assertEquals(3, read.problems().size(), read.problems().toString());
        assertTrue(read.problems().get(0).startsWith("assets/mymod/jstech/manuals/guide/en_us/"));
    }

    @Test
    void at_readsAFilesPlaceUnderItsNamespace() {
        assertEquals(Optional.of(new ManualFile("mymod", "guide", "pt_br", "machines/kiln", "x")),
                ManualFile.at("mymod", "jstech/manuals/guide/pt_br/machines/kiln.md", "x"));
        assertEquals(Optional.empty(), ManualFile.at("mymod", "jstech/manuals/guide/kiln.md", "x"));
        assertEquals(Optional.empty(), ManualFile.at("mymod", "jstech/other/guide/en_us/kiln.md", "x"));
    }

    private static ManualFile file(final String language, final String path, final String text) {
        return new ManualFile("mymod", "guide", language, path, text);
    }

    /* A family registered once for the whole run, as a mod registers one while the game loads. */
    private static final class GuideFamiliesFixture {

        private static boolean registered;

        static synchronized void registerOnce() {
            if (!registered) {
                GuideFamilies.register("mymod:test_kiln_parts", () -> List.of("mymod:kiln_door",
                        "mymod:kiln_grate"));
                registered = true;
            }
        }
    }
}
