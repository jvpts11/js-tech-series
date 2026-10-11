/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import java.util.Objects;

/**
 * A chapter of a mod's: a run of its sections under one title, with the colour of its tab at the edge of the manual
 * and a paragraph that opens it.
 *
 * <p>A mod may write several. Its first, its main chapter, is named by the mod's id alone; any other by {@code
 * namespace:path}. A manual holds whole chapters, so a chapter written once can be bound into several books, such as
 * a chapter on the shell that every book of a Unix-like system carries.
 *
 * @param id          the chapter's id: the mod's id for its main chapter, {@code namespace:path} for any other
 * @param order       where the chapter sits among the chapters of a manual holding several, smallest first
 * @param titleKey    its title's sentence, such as "J's Computers"
 * @param tab         the colour of its tab: the id of a declared palette whose role {@code tab} it is, so a resource
 *                    pack can recolour it, or a colour written {@code #AARRGGBB}; empty for the style's own
 * @param aboutKey    the paragraph on its opening page, saying what the chapter is about, or empty
 * @param inReference whether a manual that holds every chapter, such as the series' reference, holds this one: a
 *                    chapter written for one of the mod's own books only, such as a book on one operating system, is
 *                    left out of it
 */
public record GuideChapter(String id, int order, String titleKey, String tab, String aboutKey, boolean inReference) {

    public GuideChapter {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(titleKey, "titleKey");
        tab = tab == null ? "" : tab;
        aboutKey = aboutKey == null ? "" : aboutKey;
    }

    /** A chapter the manuals holding every chapter hold too. */
    public GuideChapter(final String id, final int order, final String titleKey, final String tab,
                        final String aboutKey) {
        this(id, order, titleKey, tab, aboutKey, true);
    }

    /** The mod the chapter belongs to: its id up to the colon, or the whole id of a mod's main chapter. */
    public String namespace() {
        return namespaceOf(this.id);
    }

    /** The mod a chapter id belongs to. */
    public static String namespaceOf(final String chapterId) {
        final int colon = chapterId.indexOf(':');
        return colon < 0 ? chapterId : chapterId.substring(0, colon);
    }
}
