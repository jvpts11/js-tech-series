/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit;

import dev.jstech.core.content.ModContent;
import dev.jstech.core.guide.GuideEntry;
import dev.jstech.core.guide.GuideManual;
import dev.jstech.core.guide.GuideSection;
import dev.jstech.core.guide.ModGuide;
import dev.jstech.core.guide.markdown.CodecJsonBlocks;
import dev.jstech.core.guide.markdown.ManualFile;
import dev.jstech.core.guide.markdown.MarkdownManuals;
import dev.jstech.core.guide.markdown.ModManualFiles;
import java.util.ArrayList;
import java.util.List;

/**
 * What every mod writes in the manuals, whichever way it writes them: declared in Java, or in Markdown files read in
 * English from the mods' jars, as a server has them.
 */
public final class TestManuals {

    private TestManuals() {
    }

    /** The Markdown manuals of every mod, in English. */
    public static MarkdownManuals.Gathered markdown() {
        return MarkdownManuals.gather(ModManualFiles.all(), ManualFile.ENGLISH, CodecJsonBlocks.INSTANCE);
    }

    /** Every entry of every mod's manuals. */
    public static List<GuideEntry> entries() {
        final List<GuideEntry> entries = new ArrayList<>(markdown().entries().values());
        for (final ModGuide guide : guides()) {
            entries.addAll(guide.declaredEntries());
        }
        return entries;
    }

    /** Every section of every mod's manuals. */
    public static List<GuideSection> sections() {
        final List<GuideSection> sections = new ArrayList<>(markdown().sections().values());
        for (final ModGuide guide : guides()) {
            sections.addAll(guide.declaredSections());
        }
        return sections;
    }

    /** Every manual of every mod. */
    public static List<GuideManual> manuals() {
        final List<GuideManual> manuals = new ArrayList<>(markdown().manuals().values());
        for (final ModGuide guide : guides()) {
            manuals.addAll(guide.declaredManuals());
        }
        return manuals;
    }

    private static List<ModGuide> guides() {
        final List<ModGuide> guides = new ArrayList<>();
        for (final ModContent content : ModContent.all()) {
            guides.addAll(content.declaredGuides());
        }
        return guides;
    }
}
