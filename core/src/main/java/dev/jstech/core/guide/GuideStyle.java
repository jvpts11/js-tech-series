/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import dev.jstech.core.id.IStableName;
import java.util.Map;
import java.util.Objects;

/**
 * How a manual looks, which is nothing but data: its colours, whether it opens as one page or two, the size of its
 * pages, the fonts it writes in, the binder's rings, and how its pages are numbered.
 *
 * <p>A style never says what a manual holds, so one manual can be redrawn in another style by naming it, and a
 * resource pack can replace a style's file without touching a word of the manuals drawn in it.
 *
 * @param palette    the declared palette whose roles colour it, {@code namespace:path}, or empty
 * @param colours    colours written {@code #AARRGGBB} by role, over the palette's: a style needs no code to have
 *                   colours of its own
 * @param spread     whether it opens as two pages side by side, as a binder does, or as one
 * @param pageWidth  a page's width, in the screen's pixels
 * @param pageHeight a page's height
 * @param margin     the room between a page's edge and its text
 * @param rings      whether a binder's rings are drawn through the pages
 * @param bodyFont   the font its running text is written in, or empty for the game's own
 * @param tableFont  the font its tables are written in, or empty for the game's own
 * @param folios     how its pages are numbered
 */
public record GuideStyle(String palette, Map<String, String> colours, boolean spread, int pageWidth, int pageHeight,
                         int margin, boolean rings, String bodyFont, String tableFont, Folios folios) {

    /**
     * The colour roles a style fills, each named as a palette's record names its components and as style files name
     * them: the binder's cover and its edge, the paper, the gutter between two pages, the ink, faint ink, headings,
     * links, rules, shaded boxes, a warning's box, the rings, the cover's label and its ink, a chapter's tab and the
     * number on it, and a warning's ink.
     */
    public static final String COVER = "cover";
    public static final String COVER_EDGE = "coverEdge";
    public static final String PAPER = "paper";
    public static final String GUTTER = "gutter";
    public static final String INK = "ink";
    public static final String FAINT = "faint";
    public static final String HEADING = "heading";
    public static final String LINK = "link";
    public static final String RULE = "rule";
    public static final String SHADE = "shade";
    public static final String HIGHLIGHT = "highlight";
    public static final String RING = "ring";
    public static final String LABEL = "label";
    public static final String LABEL_INK = "labelInk";
    public static final String TAB = "tab";
    public static final String TAB_INK = "tabInk";
    public static final String WARNING = "warning";

    public GuideStyle {
        palette = palette == null ? "" : palette;
        colours = Map.copyOf(colours);
        bodyFont = bodyFont == null ? "" : bodyFont;
        tableFont = tableFont == null ? "" : tableFont;
        Objects.requireNonNull(folios, "folios");
        if (pageWidth < 64 || pageHeight < 64 || margin < 0 || margin * 2 >= pageWidth) {
            throw new IllegalArgumentException("a page is at least 64 by 64 pixels with room inside its margins; got "
                    + pageWidth + " by " + pageHeight + " with " + margin);
        }
    }

    /** How a manual's pages are numbered. */
    public enum Folios implements IStableName {
        /** By chapter and page within it, as technical manuals number them: 3-14 is page fourteen of chapter 3. */
        CHAPTER_PAGE("chapter_page"),
        /** One count from the first page to the last. */
        SEQUENTIAL("sequential");

        private final String serializedName;

        Folios(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }
    }
}
