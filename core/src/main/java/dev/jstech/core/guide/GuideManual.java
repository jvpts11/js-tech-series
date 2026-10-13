/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import java.util.List;
import java.util.Objects;

/**
 * A manual: a title, a style it is drawn in, and the chapters it holds.
 *
 * <p>A manual says nothing about its entries: it holds whole chapters, and every entry of those chapters is in it.
 * So a mod's entries are written once and show in the mod's own manual and in the series' one, each in its own look,
 * and a chapter can be bound into several of a mod's books at once.
 *
 * @param id          the manual's id, {@code namespace:path}
 * @param titleKey    its title, printed on its cover and at the foot of every page
 * @param coverKeys   the further lines of its cover's label, above the title (the publisher, the series)
 * @param edition     the line under the title on the cover, such as "First Edition", or empty
 * @param partNumber  the part number printed on the cover, data rather than words, or empty
 * @param style       the style it is drawn in, {@code namespace:path}
 * @param chapters    the ids of the chapters it holds, in the order it prints them; {@code *} holds every chapter
 *                    written for the reference, in the order the chapters say
 * @param aboutKeys   the paragraphs of its "About this manual" page, or the notes under a drawing list
 * @param priority    which manual the manual key opens when several alike hold an item's entry: the highest
 * @param icon        the mark printed on its cover, a texture {@code namespace:path} of 32 by 32 pixels, or empty
 */
public record GuideManual(String id, String titleKey, List<String> coverKeys, String edition, String partNumber,
                          String style, List<String> chapters, List<String> aboutKeys, int priority, String icon) {

    /** The chapter list that holds every chapter there is. */
    public static final String EVERY_CHAPTER = "*";

    public GuideManual {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(titleKey, "titleKey");
        Objects.requireNonNull(style, "style");
        coverKeys = List.copyOf(coverKeys);
        edition = edition == null ? "" : edition;
        partNumber = partNumber == null ? "" : partNumber;
        chapters = List.copyOf(chapters);
        aboutKeys = List.copyOf(aboutKeys);
        icon = icon == null ? "" : icon;
    }

    /**
     * Whether the manual holds that chapter: it names it, or it holds every chapter and the chapter is one of those
     * every such manual holds rather than one written for a mod's own book only.
     */
    public boolean holds(final GuideChapter chapter) {
        return this.chapters.contains(chapter.id()) || this.holdsEvery() && chapter.inReference();
    }

    /** Whether the manual names the chapter of that id, rather than holding it as one of every chapter. */
    public boolean names(final String chapterId) {
        return this.chapters.contains(chapterId);
    }

    /** Whether the manual holds every chapter there is, as the series' reference does: its tabs are then one a mod. */
    public boolean holdsEvery() {
        return this.chapters.contains(EVERY_CHAPTER);
    }

    /** Where a chapter it names stands in its list, so a book prints its chapters in the order it binds them. */
    public int placeOf(final String chapterId) {
        final int at = this.chapters.indexOf(chapterId);
        return at < 0 ? Integer.MAX_VALUE : at;
    }
}
