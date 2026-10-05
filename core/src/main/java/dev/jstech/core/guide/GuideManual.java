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
 * So a mod's entries are written once and show in the mod's own manual and in the series' one, each in its own look.
 *
 * @param id          the manual's id, {@code namespace:path}
 * @param titleKey    its title, printed on its cover and at the foot of every page
 * @param coverKeys   the further lines of its cover's label, above the title (the publisher, the series)
 * @param edition     the line under the title on the cover, such as "First Edition", or empty
 * @param partNumber  the part number printed on the cover, data rather than words, or empty
 * @param style       the style it is drawn in, {@code namespace:path}
 * @param chapters    the namespaces of the chapters it holds, in the order the chapters say; {@code *} holds every one
 * @param aboutKeys   the paragraphs of its "About this manual" page, or the notes under a drawing list
 * @param priority    which manual the manual key opens when several hold an item's entry: the highest
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

    /** Whether the manual holds the chapter of that namespace. */
    public boolean holds(final String namespace) {
        return this.chapters.contains(EVERY_CHAPTER) || this.chapters.contains(namespace);
    }
}
