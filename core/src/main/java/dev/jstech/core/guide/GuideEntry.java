/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One entry of a manual: a thing a player can look up, such as a machine, a part or an idea, with its blocks.
 *
 * <p>An entry belongs to a section of its mod's chapter, and shows in every manual that holds that chapter: the
 * mod's own manual and the series' one alike, written once. The items it covers are those whose page a player opens
 * with the manual key while hovering them.
 *
 * @param id       the entry's id, {@code namespace:path}, the namespace being its mod's
 * @param section  the section it sits in, {@code namespace:path}
 * @param order    where it sits among the entries of its section, smallest first
 * @param titleKey its title's sentence
 * @param icon     the item drawn beside it in lists, or empty
 * @param items    the items it is the page of
 * @param shows    the items shown under its title, when they are others than those it is the page of: the parts an
 *                 entry about an idea talks about; empty to show the items it is the page of
 * @param blocks   what it says, in order
 */
public record GuideEntry(String id, String section, int order, String titleKey, String icon, List<String> items,
                         List<String> shows, List<GuideBlock> blocks) {

    public GuideEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(section, "section");
        Objects.requireNonNull(titleKey, "titleKey");
        icon = icon == null ? "" : icon;
        items = List.copyOf(items);
        shows = List.copyOf(shows);
        blocks = List.copyOf(blocks);
    }

    /** An entry that shows the items it is the page of. */
    public GuideEntry(final String id, final String section, final int order, final String titleKey,
                      final String icon, final List<String> items, final List<GuideBlock> blocks) {
        this(id, section, order, titleKey, icon, items, List.of(), blocks);
    }

    /** The items shown under its title: those it names to show, or else those it is the page of. */
    public List<String> shown() {
        return this.shows.isEmpty() ? this.items : this.shows;
    }

    /** The mod the entry belongs to: the namespace of its id. */
    public String namespace() {
        return GuideIds.namespace(this.id);
    }

    /** The words the entry explains, in the order it explains them, which the index lists as the glossary. */
    public List<GuideBlock.Define> terms() {
        final List<GuideBlock.Define> terms = new ArrayList<>();
        for (final GuideBlock block : this.blocks) {
            if (block instanceof GuideBlock.Define define) {
                terms.add(define);
            }
        }
        return terms;
    }
}
