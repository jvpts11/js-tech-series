/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import java.util.List;

/**
 * What one manual holds, in the order it is printed: its chapters, each with its sections, each with its entries.
 *
 * <p>Put together from every mod's declarations for the chapters the manual names, before it is laid out.
 */
public record GuideContents(List<Chapter> chapters) {

    public GuideContents {
        chapters = List.copyOf(chapters);
    }

    /** A chapter and its sections, in order. */
    public record Chapter(GuideChapter chapter, List<Section> sections) {

        public Chapter {
            sections = List.copyOf(sections);
        }
    }

    /** A section and its entries, in order. */
    public record Section(GuideSection section, List<GuideEntry> entries) {

        public Section {
            entries = List.copyOf(entries);
        }
    }
}
