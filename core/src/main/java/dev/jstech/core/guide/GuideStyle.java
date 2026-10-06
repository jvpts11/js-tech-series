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
 * How a manual looks, which is nothing but data: its colours, its pages (one or two side by side, their size, how many
 * columns), what decorates them, the fonts it writes in, how its pages are numbered, and its cover.
 *
 * <p>A style never says what a manual holds, so one manual can be redrawn in another style by naming it, and a
 * resource pack can replace a style's file without touching a word of the manuals drawn in it.
 *
 * @param palette       the declared palette whose roles colour it, {@code namespace:path}, or empty
 * @param colours       colours written {@code #AARRGGBB} by role, over the palette's: a style needs no code to have
 *                      colours of its own
 * @param pages         the pages' shape
 * @param decor         what is drawn on and around the pages
 * @param bodyFont      the font its running text is written in, or empty for the game's own
 * @param tableFont     the font its tables are written in, or empty for the game's own
 * @param folios        how its pages are numbered
 * @param drawingPrefix the letters a drawing's number starts with ({@code JI} for JI-102), for drawing numbers
 * @param cover         how its cover looks
 * @param holdBar       the little bar that fills while the manual key is held over an item to open its page here
 */
public record GuideStyle(String palette, Map<String, String> colours, Pages pages, Decor decor, String bodyFont,
                         String tableFont, Folios folios, String drawingPrefix, Cover cover, HoldBar holdBar) {

    /**
     * The colour roles a style fills, each named as a palette's record names its components and as style files name
     * them: the cover and its edge, the paper, the gutter between two pages, the ink, faint ink, headings, links,
     * rules, shaded boxes, a warning's box, the rings, the cover's label and its ink, a chapter's tab and the number on
     * it, a warning's ink, the accent that marks notes and what can go wrong, the numbers of steps, a cover's band,
     * a drawing's grid, a binder's spine, and the first line of the cover, which names the series or the mod.
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
    public static final String ACCENT = "accent";
    public static final String NUMBER = "number";
    public static final String BAND = "band";
    public static final String GRID = "grid";
    public static final String SPINE = "spine";
    public static final String COVER_LINE = "coverLine";

    public GuideStyle {
        palette = palette == null ? "" : palette;
        colours = Map.copyOf(colours);
        Objects.requireNonNull(pages, "pages");
        Objects.requireNonNull(decor, "decor");
        bodyFont = bodyFont == null ? "" : bodyFont;
        tableFont = tableFont == null ? "" : tableFont;
        Objects.requireNonNull(folios, "folios");
        drawingPrefix = drawingPrefix == null ? "" : drawingPrefix;
        Objects.requireNonNull(cover, "cover");
        Objects.requireNonNull(holdBar, "holdBar");
    }

    /** The same style with another bar filling while the manual key is held over an item. */
    public GuideStyle withHoldBar(final HoldBar bar) {
        return new GuideStyle(this.palette, this.colours, this.pages, this.decor, this.bodyFont, this.tableFont,
                this.folios, this.drawingPrefix, this.cover, bar);
    }

    /** Whether it opens as two pages side by side. */
    public boolean spread() {
        return this.pages.spread();
    }

    public int pageWidth() {
        return this.pages.width();
    }

    public int pageHeight() {
        return this.pages.height();
    }

    public int margin() {
        return this.pages.margin();
    }

    /** Whether a binder's rings go through the pages. */
    public boolean rings() {
        return this.decor.rings();
    }

    /**
     * The pages' shape.
     *
     * @param spread  whether two pages show side by side, as in a binder, or one, as a drawing's sheet
     * @param width   a page's width, in the screen's pixels
     * @param height  a page's height
     * @param margin  the room between a page's edge and its text
     * @param columns how many columns a page's text runs in, one or more
     */
    public record Pages(boolean spread, int width, int height, int margin, int columns) {

        public Pages {
            if (width < 64 || height < 64 || margin < 0 || margin * 2 >= width || columns < 1) {
                throw new IllegalArgumentException("a page is at least 64 by 64 pixels, with room inside its margins"
                        + " and a column at least; got " + width + " by " + height + ", " + margin + ", " + columns);
            }
        }
    }

    /**
     * What is drawn on and around the pages.
     *
     * @param rings         a binder's rings through the pages
     * @param grid          a drawing's grid over the paper
     * @param frame         a drawing's frame, with its zones numbered along the top and lettered down the side
     * @param titleBlock    a drawing's title block in the corner: what it is, its number, its sheet and revision,
     *                      in place of a page's head and foot
     * @param upperHeadings headings written in capitals, as on a drawing
     * @param traced        blocks drawn as line work traced from their faces rather than as they look
     * @param plainNumbers  steps numbered without a full stop after the number
     * @param smallText     running text lettered small, as a drawing's notes are, so a sheet holds two columns
     */
    public record Decor(boolean rings, boolean grid, boolean frame, boolean titleBlock, boolean upperHeadings,
                        boolean traced, boolean plainNumbers, boolean smallText) {

        /** Nothing but a binder's rings, or not even those. */
        public static Decor binder(final boolean rings) {
            return new Decor(rings, false, false, false, false, false, false, false);
        }

        /** Everything a sheet of drawings has: the grid, the frame and its title block, its lettering. */
        public static Decor drawing() {
            return new Decor(false, true, true, true, true, true, true, true);
        }
    }

    /**
     * How the cover looks.
     *
     * @param kind  a ring binder, with its spine and rivets, or a folder, with its flap
     * @param label whether the cover's words stand on a pasted label, or on the cover itself
     * @param band  whether a stripe of the band's colour crosses the cover near its foot
     */
    public record Cover(CoverKind kind, boolean label, boolean band) {

        public Cover {
            Objects.requireNonNull(kind, "kind");
        }
    }

    /** What a manual's cover is. */
    public enum CoverKind implements IStableName {
        /** A ring binder: a spine with rivets, the chapter tabs standing out at the edge. */
        BINDER("binder"),
        /** A folder of drawings: a flap across it, the sheets' edge showing at its side. */
        FOLDER("folder");

        private final String serializedName;

        CoverKind(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }
    }

    /** The bar that fills while the manual key is held over an item, in the look of the manual it opens. */
    public enum HoldBar implements IStableName {
        /** A plain bar in the series' accent. */
        PLAIN("plain"),
        /** Blue blocks lighting one after another in a dark, rimmed track, as a system of the early 2000s loaded. */
        BLOCKS("blocks"),
        /** Black and yellow stripes running in, as the edge of a machine's guard is marked. */
        HAZARD("hazard");

        private final String serializedName;

        HoldBar(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }
    }

    /** How a manual's pages are numbered. */
    public enum Folios implements IStableName {
        /** By chapter and page within it, as technical manuals number them: 3-14 is page fourteen of chapter 3. */
        CHAPTER_PAGE("chapter_page"),
        /** One count from the first page to the last. */
        SEQUENTIAL("sequential"),
        /**
         * As drawings are numbered: each entry a drawing of one or more sheets, numbered by its section in hundreds
         * and its place in the section ({@code JI-102} is the second drawing of the second section).
         */
        DRAWING("drawing");

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
