/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import dev.jstech.core.id.IStableName;
import java.util.List;
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
 * @param codeFont      the font its code and figures are written in, at the font's own size: a command or a code in
 *                      a table, a drawing's number in a drawing list; or empty for the game's own
 * @param folios        how its pages are numbered
 * @param drawingPrefix the letters a drawing's number starts with ({@code JI} for JI-102), for drawing numbers
 * @param cover         how its cover looks
 * @param holdBar       the little bar that fills while the manual key is held over an item to open its page here
 * @param make          how the book is made: its binding, its boards, its paper, the marks of its chapters and its
 *                      ribbon
 */
public record GuideStyle(String palette, Map<String, String> colours, Pages pages, Decor decor, String bodyFont,
                         String codeFont, Folios folios, String drawingPrefix, Cover cover, HoldBar holdBar,
                         Make make) {

    /**
     * The colour roles a style fills, each named as a palette's record names its components and as style files name
     * them: the cover and its edge, the paper, the gutter between two pages, the ink, faint ink, headings, links,
     * rules, shaded boxes, a warning's box, the rings, the cover's label and its ink, a chapter's tab and the number on
     * it, a warning's ink, the accent that marks notes and what can go wrong, the numbers of steps, a cover's band,
     * a drawing's grid, a binder's spine, and the first line of the cover, which names the series or the mod; and a
     * listing's paper, the strip over it, the colours of its code by what each piece is, and its Copy button once it
     * has copied.
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
    public static final String CODE_PAPER = "codePaper";
    public static final String CODE_HEAD = "codeHead";
    public static final String CODE_KEYWORD = "codeKeyword";
    public static final String CODE_NAME = "codeName";
    public static final String CODE_TEXT = "codeText";
    public static final String CODE_NUMBER = "codeNumber";
    public static final String CODE_COMMENT = "codeComment";
    public static final String CODE_COPIED = "codeCopied";
    /** The line down a page of graph paper's outer margin, and a hardcover's ribbon. */
    public static final String MARGIN = "margin";
    public static final String RIBBON = "ribbon";

    public GuideStyle {
        palette = palette == null ? "" : palette;
        colours = Map.copyOf(colours);
        Objects.requireNonNull(pages, "pages");
        Objects.requireNonNull(decor, "decor");
        bodyFont = bodyFont == null ? "" : bodyFont;
        codeFont = codeFont == null ? "" : codeFont;
        Objects.requireNonNull(folios, "folios");
        drawingPrefix = drawingPrefix == null ? "" : drawingPrefix;
        Objects.requireNonNull(cover, "cover");
        Objects.requireNonNull(holdBar, "holdBar");
        Objects.requireNonNull(make, "make");
    }

    /** A style made as its decor says: a binder with rings when it has them, otherwise glued. */
    public GuideStyle(final String palette, final Map<String, String> colours, final Pages pages, final Decor decor,
                      final String bodyFont, final String codeFont, final Folios folios, final String drawingPrefix,
                      final Cover cover, final HoldBar holdBar) {
        this(palette, colours, pages, decor, bodyFont, codeFont, folios, drawingPrefix, cover, holdBar,
                Make.of(decor));
    }

    /** The same style with another bar filling while the manual key is held over an item. */
    public GuideStyle withHoldBar(final HoldBar bar) {
        return new GuideStyle(this.palette, this.colours, this.pages, this.decor, this.bodyFont, this.codeFont,
                this.folios, this.drawingPrefix, this.cover, bar, this.make);
    }

    /** The same style made another way: bound, papered and marked so. */
    public GuideStyle withMake(final Make made) {
        return new GuideStyle(this.palette, this.colours, this.pages, this.decor, this.bodyFont, this.codeFont,
                this.folios, this.drawingPrefix, this.cover, this.holdBar, made);
    }

    /** The same style with another cover. */
    public GuideStyle withCover(final Cover covered) {
        return new GuideStyle(this.palette, this.colours, this.pages, this.decor, this.bodyFont, this.codeFont,
                this.folios, this.drawingPrefix, covered, this.holdBar, this.make);
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
        return this.make.binding() == Binding.RINGS;
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
     * @param rings         a binder's rings through the pages, which is how a style is made when it says nothing
     *                      of that ({@link Make#of})
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
     * How a book is made, past its pages.
     *
     * @param binding how its leaves are held together
     * @param board   what its boards are covered in, seen round the pages of the open book
     * @param paper   the paper its pages are
     * @param marks   how its chapters are marked at the edge
     * @param colours the colours of its thumb index or its flags, one chapter after another and round again, each a
     *                role of the style's palette or a colour written {@code #AARRGGBB}; none for each chapter's own
     *                colour. A chapter bound in several books takes each book's colours.
     * @param ribbon  whether a ribbon bookmark keeps the page the reader stopped at
     */
    public record Make(Binding binding, Board board, Paper paper, Marks marks, List<String> colours,
                       boolean ribbon) {

        public Make {
            Objects.requireNonNull(binding, "binding");
            Objects.requireNonNull(board, "board");
            Objects.requireNonNull(paper, "paper");
            Objects.requireNonNull(marks, "marks");
            colours = List.copyOf(colours);
        }

        /** How a style with that decor is made: a ring binder's with plastic tabs, or glued with them. */
        public static Make of(final Decor decor) {
            return new Make(decor.rings() ? Binding.RINGS : Binding.GLUED, Board.PLAIN, Paper.PLAIN, Marks.TABS,
                    List.of(), false);
        }
    }

    /** How a book's leaves are held together, seen in the middle of an open book. */
    public enum Binding implements IStableName {
        /** A ring binder's rings through punched holes. */
        RINGS("rings"),
        /** Glued at the spine, the pages meeting in a soft shadow. */
        GLUED("glued"),
        /** Sewn in signatures, the pages curving deep into the spine. */
        SEWN("sewn"),
        /** A wire coil through a row of holes, the pages lying flat. */
        WIRE("wire");

        private final String serializedName;

        Binding(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }
    }

    /** What a book's boards are covered in. */
    public enum Board implements IStableName {
        /** Vinyl, paper or plain board, in its colour alone. */
        PLAIN("plain"),
        /** A hardback's cloth, its fine weave showing. */
        CLOTH("cloth"),
        /** A notebook's kraft card, flecked. */
        KRAFT("kraft");

        private final String serializedName;

        Board(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }
    }

    /** The paper a book's pages are. */
    public enum Paper implements IStableName {
        /** Plain paper. */
        PLAIN("plain"),
        /** A notebook's graph paper, a red line down each page's outer margin. */
        GRAPH("graph");

        private final String serializedName;

        Paper(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }
    }

    /** How a book's chapters are marked at the edge of its pages, each mark leading to its chapter. */
    public enum Marks implements IStableName {
        /** A binder's plastic tabs standing out past the pages. */
        TABS("tabs"),
        /** A thumb index printed on the edge of the right page, the open chapter's block the widest. */
        THUMB("thumb"),
        /** Paper flags stuck on the pages' edge by the book's owner, each its chapter's colour. */
        FLAGS("flags");

        private final String serializedName;

        Marks(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }
    }

    /**
     * How the cover looks.
     *
     * @param kind  a ring binder, with its spine and rivets; a folder, with its flap; or a book, drawn from its parts
     * @param label whether the cover's words stand on a pasted label, or on the cover itself
     * @param band  whether a stripe of the band's colour crosses the cover near its foot
     * @param parts what a book's cover is drawn from, in order, each over the ones before it
     */
    public record Cover(CoverKind kind, boolean label, boolean band, List<CoverPart> parts) {

        public Cover {
            Objects.requireNonNull(kind, "kind");
            parts = List.copyOf(parts);
        }

        /** A binder's or a folder's cover, drawn as its kind is. */
        public Cover(final CoverKind kind, final boolean label, final boolean band) {
            this(kind, label, band, List.of());
        }

        /** A book's cover, drawn from those parts. */
        public static Cover book(final List<CoverPart> parts) {
            return new Cover(CoverKind.BOOK, false, false, parts);
        }
    }

    /**
     * A part of a book's cover, placed in the cover's pixels from its top left corner; the cover is the closed book
     * seen from the front, its spine at its left edge.
     *
     * @param kind   what is drawn
     * @param x      where it starts across
     * @param y      where it starts down
     * @param width  how wide it is; for words, how wide they may run before they wrap
     * @param height how tall it is
     * @param colour a role of the style's palette, or a colour written {@code #AARRGGBB}
     * @param words  for words and a stamp, which of the manual's cover lines they are, from 1; or 0
     * @param image  for an image, a texture {@code namespace:path} under {@code textures/}
     * @param scale  for words, how large they are lettered, 1 for running text
     * @param turn   for a stamp, how far its frame is turned, in degrees
     */
    public record CoverPart(PartKind kind, int x, int y, int width, int height, String colour, int words,
                            String image, float scale, int turn) {

        public CoverPart {
            Objects.requireNonNull(kind, "kind");
            colour = colour == null ? "" : colour;
            image = image == null ? "" : image;
            if (width < 0 || height < 0 || words < 0 || scale <= 0) {
                throw new IllegalArgumentException("a cover's part has a size, a line from 1 and a scale; got "
                        + width + " by " + height + ", line " + words + ", " + scale);
            }
        }

        /** A rectangle of a colour. */
        public static CoverPart fill(final int x, final int y, final int width, final int height,
                                     final String colour) {
            return new CoverPart(PartKind.FILL, x, y, width, height, colour, 0, "", 1, 0);
        }

        /** A rectangle's outline in a colour. */
        public static CoverPart frame(final int x, final int y, final int width, final int height,
                                      final String colour) {
            return new CoverPart(PartKind.FRAME, x, y, width, height, colour, 0, "", 1, 0);
        }

        /** An image the mod ships, drawn as a square of that size. */
        public static CoverPart image(final int x, final int y, final int size, final String texture) {
            return image(x, y, size, size, texture);
        }

        /** An image the mod ships, drawn at that width and height. */
        public static CoverPart image(final int x, final int y, final int width, final int height,
                                      final String texture) {
            return new CoverPart(PartKind.IMAGE, x, y, width, height, "", 0, texture, 1, 0);
        }

        /** One of the manual's cover lines, from 1, lettered at that scale in a colour, wrapping in that width. */
        public static CoverPart words(final PartKind kind, final int x, final int y, final int width,
                                      final int line, final float scale, final String colour) {
            return new CoverPart(kind, x, y, width, 0, colour, line, "", scale, 0);
        }

        /** A rubber stamp: its frame turned by that many degrees, one of the cover lines inside it. */
        public static CoverPart stamp(final int x, final int y, final int width, final int height, final int line,
                                      final int turn, final String colour) {
            return new CoverPart(PartKind.STAMP, x, y, width, height, colour, line, "", 1, turn);
        }

        /**
         * A grain over a rectangle: a cloth's weave, kraft paper's flecks, a wire coil down a spine or the holes
         * punched for it.
         */
        public static CoverPart grain(final PartKind kind, final int x, final int y, final int width,
                                      final int height, final String colour) {
            return new CoverPart(kind, x, y, width, height, colour, 0, "", 1, 0);
        }
    }

    /** What a part of a book's cover is. */
    public enum PartKind implements IStableName {
        /** A rectangle of its colour. */
        FILL("fill"),
        /** The outline of a rectangle. */
        FRAME("frame"),
        /** A bevelled plate, lit along its top and left and shaded along the rest. */
        PLATE("plate"),
        /** An image. */
        IMAGE("image"),
        /** Words, small. */
        SMALL("small"),
        /** Words, small and in bold, as a publisher's name stands at the foot of a cover. */
        SMALL_BOLD("small_bold"),
        /** Words. */
        WORDS("words"),
        /** Words in bold. */
        BOLD("bold"),
        /** Words in bold, centred on the part's middle. */
        CENTRED("centred"),
        /** Words, small, centred on the part's middle. */
        CENTRED_SMALL("centred_small"),
        /** Words in the code font, each of its pixels as large as the part's scale says. */
        CODE("code"),
        /** A rubber stamp: a turned frame round words. */
        STAMP("stamp"),
        /** A cloth's fine weave: a darker line every other row. */
        CLOTH("cloth"),
        /** Kraft paper's flecks, scattered the same way each time. */
        KRAFT("kraft"),
        /** A wire coil down the spine. */
        WIRE("wire"),
        /** The holes punched down the board for a wire coil, one to each of its loops. */
        HOLES("holes");

        private final String serializedName;

        PartKind(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }
    }

    /** What a manual's cover is. */
    public enum CoverKind implements IStableName {
        /** A ring binder: a spine with rivets, the chapter tabs standing out at the edge. */
        BINDER("binder"),
        /** A folder of drawings: a flap across it, the sheets' edge showing at its side. */
        FOLDER("folder"),
        /** A bound book, its cover drawn from the parts its style lists. */
        BOOK("book");

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
