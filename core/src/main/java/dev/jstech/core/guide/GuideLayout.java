/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import dev.jstech.core.guide.GuideLinks.Run;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Lays a manual out on pages: the contents and the "About this manual" page first, then each chapter with its opening
 * pages and its entries, then the index. A set of drawings is laid out as drawings are: its drawing list first, then
 * each entry a drawing of one sheet or more.
 *
 * <p>Everything is numbered as technical manuals number it: chapters from 1, sections within a chapter (3.2), entries
 * within a section (3.2.6), figures and tables within a chapter (Figure 3-9), and pages within a chapter (3-14), the
 * front matter in small roman numerals and the index as X-1, X-2. A style may number every page in one count instead,
 * or number its entries as drawings (JI-102, the second drawing of the second section).
 *
 * <p>A chapter opens on two facing pages when the style shows two: its number, title and what it is about on the left,
 * its sections and the pages they start on on the right. So the opening always falls on a left page, a page is left
 * blank before it when it would not, as printed manuals do.
 *
 * <p>Every entry starts on a page of its own, so the page an item opens to is its own. A page may run in columns, the
 * text going on in the next column when one is full. A paragraph, a list of steps or recipes runs on where it does not
 * fit; a figure, a warning, the views of a block and a special block move on whole; a table moves on whole unless it is
 * taller than a page, and then runs on with its caption and head said again. A heading is never left alone at the foot
 * of a column.
 *
 * <p>Nothing here knows the game: text is measured and translated through {@link IGuideText}, so the layout is the same
 * piece of logic in the game and in a test.
 */
public final class GuideLayout {

    private final GuideManual manual;
    private final GuideStyle style;
    private final IGuideText text;
    private final boolean drawings;
    private final int width;
    private final int fullWidth;
    private final int top;
    private final List<BodyPage> body = new ArrayList<>();
    private final Map<String, Integer> bodyTargets = new HashMap<>();
    private final Map<String, String> numbers = new HashMap<>();
    private final Map<String, String> chapterNumbers = new HashMap<>();
    private final List<PendingLine> indexLines = new ArrayList<>();
    private final List<DrawingRow> drawingRows = new ArrayList<>();
    /** The words of the glossary already led from in the entry being laid, so only a word's first use leads. */
    private final Set<String> usedTerms = new HashSet<>();
    private GuideGlossary glossary;
    /** The entry that prints each command's page, by the command's name, so a page's "Read next" leads there. */
    private Map<String, String> commandEntries = Map.of();
    private String currentEntry = "";
    /** How many scenes the entry being laid has shown so far, which tells each from the others. */
    private int entryScenes;
    /** The words the tables laid so far had to cut, too wide for any room they had. */
    private final List<String> cut = new ArrayList<>();
    private List<GuidePiece> pieces = new ArrayList<>();
    /**
     * Where the last heading's lines start among the open page's pieces while nothing stands under it yet, or -1, and
     * how tall it is: what it heads moving on carries it along.
     */
    private int headingFrom = -1;
    private int headingHeight;
    private int y;
    private int column;
    private int front;
    private int chapterNumber;
    private int pageInChapter;
    private int figures;
    private int tables;
    private int listings;
    private String chapterId = "";
    private String chapterTitle = "";
    private String sectionLabel = "";
    /** The head of the page being laid, as it was when the page was opened. */
    private String pageLabel = "";
    private String drawing = "";
    private String drawingTitle = "";
    private boolean open;

    /** Where the head of a page is written, and the rule under it; the client draws both. */
    public static final int HEADER_Y = 4;
    public static final int HEADER_RULE_Y = 14;
    /** Where the text of a page starts. */
    public static final int BODY_TOP = 20;
    /** The room the foot of a page keeps for its rule and its folio. */
    public static final int FOOTER_ROOM = 21;
    /** How tall a figure's frame is, an item drawn twice its size in the middle. */
    public static final int FIGURE_HEIGHT = 46;
    /** How tall the plate under an entry's title is: a row of items at their own size, and its edge. */
    public static final int PLATE_HEIGHT = 20;
    /** How tall a row of recipes is: the slots, and the time and energy over the arrow. */
    public static final int RECIPE_ROW = 26;
    /** The same on a drawing, whose notes are lettered small: the slots and a little room. */
    public static final int SMALL_RECIPE_ROW = 22;
    /** The room an item's icon takes beside a line of the contents or the index. */
    public static final int ICON = 18;
    /** How far a drawing's frame stands in from the edge of its sheet, the zone marks outside it. */
    public static final int FRAME = 7;
    /** The room between a drawing's frame and its text. */
    public static final int FRAME_ROOM = 8;
    /** A drawing's title block, in the bottom right corner inside its frame. */
    public static final int TITLE_BLOCK_WIDTH = 150;
    public static final int TITLE_BLOCK_HEIGHT = 32;
    /** The room between two columns of a page. */
    public static final int COLUMN_GAP = 12;
    /** How many times its size a face of a block is drawn in its views and plans. */
    public static final int VIEW_SCALE = 3;
    /** A face drawn at that scale, with its outline. */
    public static final int VIEW = 16 * VIEW_SCALE + 1;
    /** How far apart the views stand, across and down. */
    public static final int VIEW_PITCH = 64;
    /** The room the views of a block take: two rows of views with their names, and the size of a block under them. */
    public static final int VIEWS_HEIGHT = 132;
    /** The room a plan takes: its blocks and their names under them. */
    public static final int PLAN_HEIGHT = 61;
    /** A listing: the strip over it, how far apart its rows are, the room over and under them, its numbers' column. */
    public static final int LISTING_HEAD = 10;
    public static final int CODE_PITCH = 9;
    public static final int CODE_PAD = 2;
    public static final int LISTING_GUTTER = 13;
    /** The width of the Copy button at the right end of a listing's strip, and the room kept clear around it. */
    public static final int LISTING_COPY = 24;
    private static final int LISTING_COPY_ROOM = LISTING_COPY + 13;
    /** A session at a terminal: the strip with its name, and the room between its frame and its text. */
    public static final int SESSION_HEAD = 9;
    public static final int SESSION_PAD = 3;
    /** How much wider a key is drawn than its name: the room either side of it and its edges. */
    public static final int KEY_ROOM = 5;
    /** What a link to a manual's glossary names. */
    public static final String GLOSSARY = "glossary";
    /** The strip along the foot of a scene's frame, holding what the pointer does to it and its buttons. */
    public static final int SCENE_STRIP = 11;
    /** A line of a scene's legend: how tall it is, and how far its words are set in past its balloon. */
    public static final int SCENE_KEY_ROW = 12;
    public static final int SCENE_KEY_ROOM = 15;

    /** How far below a legend line's top its small words start, level with the middle of its balloon. */
    private static final int KEY_TEXT_DROP = 3;
    /** What a path or a piece of code may be broken after when it is too wide for its line. */
    private static final String BREAKS_AFTER = "/\\.(,=:_-@|";
    private static final int PARAGRAPH_GAP = 4;
    private static final int BLOCK_GAP = 6;
    private static final int CAPTION_GAP = 3;
    private static final int LABEL_GAP = 4;
    private static final int STEP_INDENT = 12;
    private static final int SMALL_STEP_INDENT = 9;
    private static final int FIX_INDENT = 8;
    private static final int SMALL_FIX_INDENT = 4;
    private static final int SECTION_INDENT = 10;
    private static final int BOX_PAD = 4;
    /** The fewest rows a part of a parted table keeps, at the foot of one page and at the top of the next. */
    private static final int MIN_PART_ROWS = 3;
    private static final int TITLE_GAP = 2;
    /** How many lines of what a heading heads must fit under it on its page, or it starts the next. */
    private static final int HEADING_KEEPS = 4;
    private static final int LEGEND_GAP = 8;
    /** The chapter's opening page: the room beside its number, under it, and the height of a line of its sections. */
    private static final int OPENER_GAP = 12;
    private static final int OPENER_RULE_GAP = 8;
    private static final int OPENER_RULE = 2;
    private static final int OPENER_ROW = 19;
    /** The drawing list: where its title column and its sheets column start, and the height of a row. */
    private static final int LIST_TITLE_X = 44;
    private static final int LIST_SHEETS_FROM_RIGHT = 90;
    private static final int LIST_ROW = 14;
    private static final int DRAWING_SECTION = 100;
    private static final String TAB = GuideStyle.TAB;
    private static final String ELLIPSIS = "...";
    /** How a manual's glossary pages are numbered: GL-1, GL-2. */
    private static final String GLOSSARY_FOLIO = "GL-";
    /** The least room a glossary's word leaves for the dots before its page, before it runs on to a second line. */
    private static final int LEAST_DOTS = 12;
    /**
     * A line of the contents, laid before the page it leads to is known: the widest page of a chapter it keeps clear
     * after the chapter's number, and the least room it leaves for dots.
     */
    private static final String FOLIO_PAGES = "-000";
    private static final int LEADER_DOTS = 6;
    /** The fewest lines of what a glossary's word means at the foot of one page and at the top of the next. */
    private static final int LEAST_LINES = 2;

    private GuideLayout(final GuideManual manual, final GuideStyle style, final IGuideText text) {
        this.manual = manual;
        this.style = style;
        this.text = text;
        this.drawings = style.folios() == GuideStyle.Folios.DRAWING;
        this.width = columnWidth(style);
        this.fullWidth = style.pageWidth() - 2 * style.margin();
        this.top = top(style);
    }

    /** Lays out a manual holding those contents, in that style, measuring and translating through {@code text}. */
    public static GuideBook lay(final GuideManual manual, final GuideStyle style, final GuideContents contents,
                                final IGuideText text) {
        return new GuideLayout(manual, style, text).book(contents);
    }

    /** Where the text of a page starts, down from its top: under its head, or inside a drawing's frame. */
    public static int top(final GuideStyle style) {
        return style.decor().frame() ? FRAME + FRAME_ROOM : BODY_TOP;
    }

    /**
     * Where the text of a column must end: above the page's foot, inside a drawing's frame, or above its title block
     * when the column runs over the block's corner.
     */
    public static int bottom(final GuideStyle style, final int column) {
        if (!style.decor().frame() && !style.decor().titleBlock()) {
            return style.pageHeight() - FOOTER_ROOM;
        }
        final int frameBottom = style.pageHeight() - FRAME;
        final boolean overBlock = style.decor().titleBlock()
                && columnX(style, column) + columnWidth(style) > style.pageWidth() - FRAME - TITLE_BLOCK_WIDTH;
        return (overBlock ? frameBottom - TITLE_BLOCK_HEIGHT : frameBottom) - LABEL_GAP;
    }

    /** How tall a row of recipes is in the style. */
    public static int recipeRow(final GuideStyle style) {
        return style.decor().smallText() ? SMALL_RECIPE_ROW : RECIPE_ROW;
    }

    /** How wide a column of the style's pages is. */
    public static int columnWidth(final GuideStyle style) {
        final int columns = style.pages().columns();
        return (style.pageWidth() - 2 * style.margin() - (columns - 1) * COLUMN_GAP) / columns;
    }

    /** Where a column of the style's pages starts, across the page. */
    public static int columnX(final GuideStyle style, final int column) {
        return style.margin() + column * (columnWidth(style) + COLUMN_GAP);
    }

    /** A number in small roman numerals, as front matter is numbered: 1 is i, 4 is iv, 14 is xiv. */
    public static String roman(final int number) {
        final int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        final String[] marks = {"m", "cm", "d", "cd", "c", "xc", "l", "xl", "x", "ix", "v", "iv", "i"};
        final StringBuilder out = new StringBuilder();
        int left = Math.max(1, number);
        for (int i = 0; i < values.length; i++) {
            while (left >= values[i]) {
                out.append(marks[i]);
                left -= values[i];
            }
        }
        return out.toString();
    }

    /**
     * Breaks text into lines no wider than {@code room} at that size, between words; a word wider than the room on its
     * own is broken where it has to be.
     */
    public static List<String> wrap(final String words, final int room, final TextSize size, final IGuideText text) {
        final List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (final String word : words.split(" ")) {
            if (word.isEmpty()) {
                continue;
            }
            final String tried = line.isEmpty() ? word : line + " " + word;
            if (text.width(tried, size) <= room) {
                line = new StringBuilder(tried);
                continue;
            }
            if (!line.isEmpty()) {
                lines.add(line.toString());
            }
            final List<String> pieces = pieces(word, room, size, text);
            lines.addAll(pieces.subList(0, pieces.size() - 1));
            line = new StringBuilder(pieces.getLast());
        }
        if (!line.isEmpty() || lines.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    /**
     * A word too wide for the room in pieces that fit: broken where code and paths break, after a slash, a dot, a
     * bracket, a comma, an equals sign, a colon, an underscore, a hyphen, an at sign or a bar, or between a small
     * letter and a capital; cut between two letters only where it has none of those.
     */
    static List<String> pieces(final String word, final int room, final TextSize size, final IGuideText text) {
        final List<String> pieces = new ArrayList<>();
        String rest = word;
        while (text.width(rest, size) > room && rest.length() > 1) {
            int fits = rest.length() - 1;
            while (fits > 1 && text.width(rest.substring(0, fits), size) > room) {
                fits--;
            }
            int cut = fits;
            for (int at = fits; at > 1; at--) {
                if (breaksBefore(rest, at)) {
                    cut = at;
                    break;
                }
            }
            pieces.add(rest.substring(0, cut));
            rest = rest.substring(cut);
        }
        pieces.add(rest);
        return pieces;
    }

    /** Whether a word may be broken between its characters at {@code at - 1} and {@code at} without a cut. */
    static boolean breaksBefore(final String word, final int at) {
        final char before = word.charAt(at - 1);
        final char after = word.charAt(at);
        return BREAKS_AFTER.indexOf(before) >= 0 || Character.isLowerCase(before) && Character.isUpperCase(after);
    }

    private GuideBook book(final GuideContents contents) {
        this.number(contents);
        this.glossary = GuideGlossary.of(contents, this.text);
        this.commandEntries = GuideCommandPage.entries(contents, this.text);
        final List<Leader> contentsLines = this.drawings ? List.of() : this.contentsLines(contents);
        final int contentsPages = this.drawings ? 0 : this.contentsPages(contentsLines);
        // Two pages side by side start a chapter on a left page; an even front matter keeps that count simple.
        final boolean pad = this.style.spread() && !this.drawings && (contentsPages + 1) % 2 == 1;
        this.front = contentsPages + 1 + (pad ? 1 : 0);
        for (final GuideContents.Chapter chapter : contents.chapters()) {
            this.chapter(chapter);
        }
        this.finishPage();
        final List<GuideBook.Page> pages = new ArrayList<>();
        final int[] sheets = this.sheets();
        if (this.drawings) {
            this.layDrawingList(pages, sheets);
            this.front = pages.size();
        }
        final List<String> bodyFolios = this.bodyFolios();
        this.fillLeaders(bodyFolios);
        if (!this.drawings) {
            this.layContents(pages, contentsLines, bodyFolios);
            this.layAbout(pages, contentsPages + 1);
            if (pad) {
                pages.add(new GuideBook.Page(roman(pages.size() + 1), "", "", this.blankPieces()));
            }
        }
        for (int i = 0; i < this.body.size(); i++) {
            final BodyPage page = this.body.get(i);
            if (this.drawings) {
                pages.add(new GuideBook.Page(bodyFolios.get(i), "", page.chapter(), page.pieces(), page.title(),
                        this.sheetOf(i), sheets[i]));
            } else {
                final boolean left = (this.front + i) % 2 == 0;
                pages.add(new GuideBook.Page(bodyFolios.get(i), left ? page.chapterTitle() : page.sectionLabel(),
                        page.chapter(), page.pieces()));
            }
        }
        final Map<String, Integer> targets = new HashMap<>();
        this.bodyTargets.forEach((target, page) -> targets.put(target, page + this.front));
        final List<GuideBook.IndexLine> index = this.indexLines(bodyFolios);
        if (!this.drawings && !this.glossary.isEmpty()) {
            targets.put(GLOSSARY, pages.size());
            this.layGlossary(pages, index);
        }
        final int indexAt = pages.size();
        if (!this.drawings) {
            this.layIndex(pages, index);
        }
        return new GuideBook(pages, targets, this.numbers, index, 0, indexAt, this.cut);
    }

    /** Numbers every section and entry first, so a link may lead to one printed after it. */
    private void number(final GuideContents contents) {
        int chapterNo = 0;
        int sectionsBefore = 0;
        for (final GuideContents.Chapter chapter : contents.chapters()) {
            chapterNo++;
            this.chapterNumbers.put(chapter.chapter().id(), String.valueOf(chapterNo));
            int sectionNo = 0;
            for (final GuideContents.Section section : chapter.sections()) {
                sectionNo++;
                sectionsBefore++;
                this.numbers.put(section.section().id(), this.drawings
                        ? this.drawingNumber((sectionsBefore - 1) * DRAWING_SECTION) : chapterNo + "." + sectionNo);
                int entryNo = 0;
                for (final GuideEntry entry : section.entries()) {
                    entryNo++;
                    this.numbers.put(entry.id(), this.drawings
                            ? this.drawingNumber((sectionsBefore - 1) * DRAWING_SECTION + entryNo)
                            : chapterNo + "." + sectionNo + "." + entryNo);
                }
            }
        }
    }

    /** A drawing's number: the style's letters and three figures, JI-102. */
    private String drawingNumber(final int number) {
        final String figures = String.format(Locale.ROOT, "%03d", number);
        return this.style.drawingPrefix().isEmpty() ? figures : this.style.drawingPrefix() + "-" + figures;
    }

    private void chapter(final GuideContents.Chapter part) {
        this.finishPage();
        if (this.style.spread() && !this.drawings && this.body.size() % 2 == 1) {
            this.blank();
        }
        this.chapterNumber++;
        this.pageInChapter = 0;
        this.figures = 0;
        this.tables = 0;
        this.listings = 0;
        this.chapterId = part.chapter().id();
        this.chapterTitle = this.text.text(part.chapter().titleKey());
        this.sectionLabel = this.chapterTitle;
        if (this.drawings) {
            // A set of drawings has no opening pages: its list says what each drawing is.
            this.bodyTargets.put(this.chapterId, this.body.size());
        } else {
            this.startPage();
            this.bodyTargets.put(this.chapterId, this.body.size());
            this.opener(part);
        }
        for (final GuideContents.Section section : part.sections()) {
            this.section(section);
        }
    }

    /**
     * A chapter's opening: "Chapter", its number large in its tab's colour with its title beside it, a rule in the
     * same colour and what the chapter is about; then its sections with their icons and pages, on the facing page.
     */
    private void opener(final GuideContents.Chapter part) {
        final int x = this.x();
        this.place(new GuidePiece.Text(x, this.y, this.text.text(GuideTexts.CHAPTER.key()), TextSize.SMALL,
                GuideStyle.FAINT, ""));
        this.y += TextSize.SMALL.lineHeight() + TITLE_GAP;
        final String number = String.valueOf(this.chapterNumber);
        this.place(new GuidePiece.Text(x, this.y, number, TextSize.CHAPTER, TAB, ""));
        final int numberHeight = TextSize.CHAPTER.lineHeight() - TITLE_GAP;
        final int titleX = x + this.text.width(number, TextSize.CHAPTER) + OPENER_GAP;
        final List<String> title = wrap(this.chapterTitle, x + this.width - titleX, TextSize.HEADING, this.text);
        int titleY = this.y + Math.max(0, (numberHeight - title.size() * TextSize.HEADING.lineHeight()) / 2);
        for (final String line : title) {
            this.place(new GuidePiece.Text(titleX, titleY, line, TextSize.HEADING, GuideStyle.HEADING, ""));
            titleY += TextSize.HEADING.lineHeight();
        }
        this.y = Math.max(this.y + numberHeight, titleY) + OPENER_RULE_GAP;
        this.place(new GuidePiece.Box(x, this.y, this.width, OPENER_RULE, TAB, TAB));
        this.y += OPENER_RULE + 2 * BLOCK_GAP;
        if (!part.chapter().aboutKey().isEmpty()) {
            this.lines(this.text.text(part.chapter().aboutKey()), 0, this.width, TextSize.BODY, GuideStyle.INK, "");
        }
        if (part.sections().isEmpty()) {
            return;
        }
        if (this.style.spread()) {
            this.sectionLabel = this.text.text(GuideTexts.IN_THIS_CHAPTER.key());
            this.startPage();
            this.y += TITLE_GAP;
        } else {
            this.y += BLOCK_GAP;
        }
        for (final GuideContents.Section section : part.sections()) {
            final String id = section.section().id();
            final String sectionNumber = this.numbers.get(id);
            final int hang = this.text.width(sectionNumber, TextSize.BOLD) + this.text.width(" ", TextSize.BODY);
            final int room = this.width - ICON - 2;
            // A title too long for its row runs on under itself, the dots and its page after its last line.
            final List<String> words = wrap(this.text.text(section.section().titleKey()), room - hang
                    - this.text.width(this.chapterNumber + FOLIO_PAGES, TextSize.BODY) - LEADER_DOTS, TextSize.BODY,
                    this.text);
            this.ensure(OPENER_ROW + (words.size() - 1) * TextSize.BODY.lineHeight());
            final int rowX = this.x();
            if (!section.section().icon().isEmpty()) {
                this.place(new GuidePiece.Item(rowX, this.y, section.section().icon(), 1));
            }
            int lineY = this.y + 4;
            if (words.size() > 1) {
                this.place(new GuidePiece.Text(rowX + ICON + 2, lineY, sectionNumber, TextSize.BOLD,
                        GuideStyle.HEADING, ""));
                for (final String line : words.subList(0, words.size() - 1)) {
                    this.place(new GuidePiece.Text(rowX + ICON + 2 + hang, lineY, line, TextSize.BODY,
                            GuideStyle.INK, ""));
                    lineY += TextSize.BODY.lineHeight();
                }
                this.place(new GuidePiece.Leader(rowX + ICON + 2 + hang, lineY, room - hang, "", words.getLast(),
                        "", TextSize.BODY, GuideStyle.INK, id, ""));
            } else {
                this.place(new GuidePiece.Leader(rowX + ICON + 2, lineY, room, sectionNumber, " " + words.getFirst(),
                        "", TextSize.BODY, GuideStyle.INK, id, ""));
            }
            this.y = lineY - 4 + OPENER_ROW;
        }
    }

    private void section(final GuideContents.Section part) {
        this.sectionLabel = this.numbers.get(part.section().id()) + " "
                + this.text.text(part.section().titleKey());
        boolean first = true;
        for (final GuideEntry entry : part.entries()) {
            this.entry(entry);
            if (first) {
                this.bodyTargets.put(part.section().id(), this.bodyTargets.get(entry.id()));
                first = false;
            }
        }
    }

    private void entry(final GuideEntry entry) {
        this.startPage();
        this.currentEntry = entry.id();
        this.usedTerms.clear();
        this.entryScenes = 0;
        final String number = this.numbers.get(entry.id());
        this.bodyTargets.put(entry.id(), this.body.size());
        final String title = this.text.text(entry.titleKey());
        this.indexLines.add(new PendingLine(title, entry.id(), entry.icon(), false, this.body.size()));
        if (this.drawings) {
            // A drawing's number and title stand in its title block, not over its views.
            this.drawing = number;
            this.drawingTitle = title;
            this.drawingRows.add(new DrawingRow(number, title, entry.id()));
        } else {
            this.lines(number + "  " + title, 0, this.width, TextSize.HEADING, GuideStyle.HEADING, "");
            this.y += TITLE_GAP;
            // A drawing shows its machine in its views; a page of a binder shows its items on a plate under the title.
            if (!entry.shown().isEmpty()) {
                this.ensure(PLATE_HEIGHT);
                this.place(new GuidePiece.Plate(this.x(), this.y, this.width, entry.id(), entry.shown()));
                this.y += PLATE_HEIGHT + BLOCK_GAP;
            }
        }
        for (final GuideBlock block : entry.blocks()) {
            this.block(block, entry);
        }
    }

    private void block(final GuideBlock block, final GuideEntry entry) {
        switch (block) {
            case GuideBlock.Paragraph paragraph -> this.prose(this.text.text(paragraph.key()));
            case GuideBlock.Heading heading -> this.heading(heading);
            case GuideBlock.Figure figure -> this.figure(figure);
            case GuideBlock.Picture picture -> this.picture(picture);
            case GuideBlock.Table table -> this.table(table);
            case GuideBlock.Listing listing -> this.listing(listing);
            case GuideBlock.Session session -> this.session(session);
            case GuideBlock.Recipes recipes -> this.recipes(recipes);
            case GuideBlock.Steps steps -> this.steps(steps);
            case GuideBlock.Warning warning -> this.warning(warning);
            case GuideBlock.Problems problems -> this.problems(problems);
            case GuideBlock.Define define -> this.define(define, entry);
            case GuideBlock.SeeAlso see -> this.seeAlso(see);
            case GuideBlock.Note note -> {
                this.richLines(this.text.text(note.key()), 0, this.width, this.sized(TextSize.BODY),
                        GuideStyle.ACCENT);
                this.y += PARAGRAPH_GAP;
            }
            case GuideBlock.Break cut -> this.cut(cut);
            case GuideBlock.Views views -> this.views(views);
            case GuideBlock.Plan plan -> this.plan(plan);
            case GuideBlock.Command command -> this.command(command);
            case GuideBlock.Scene scene -> this.scene(scene);
            case GuideBlock.Custom custom -> {
                // A block taller than the text area would run past the page foot, so it is cut to the area.
                final int height = Math.min(custom.height(), this.bottom() - this.top);
                this.ensure(height);
                this.place(new GuidePiece.Custom(this.x(), this.y, this.width, height, custom.type(),
                        custom.data()));
                this.y += height + BLOCK_GAP;
            }
        }
    }

    /** A paragraph of running text, its links and keys drawn as such; nothing at all for no words. */
    private void prose(final String sentence) {
        if (sentence.isEmpty()) {
            return;
        }
        this.richLines(sentence, 0, this.width, this.sized(TextSize.BODY), GuideStyle.INK);
        this.y += PARAGRAPH_GAP;
    }

    /**
     * A command's page, as its mod answers it from the command itself ({@link GuideCommandPage}): what it is for, how
     * it is typed in the code font, what it does, a table of its options and one of its examples, the systems it runs
     * on, and the commands to read next, each leading to its page where this manual has one. A command no mod answers
     * for prints a line saying so, so the gap is seen and mended.
     */
    private void command(final GuideBlock.Command block) {
        final Optional<GuideCommand> found = this.text.command(block.source());
        if (found.isEmpty()) {
            this.richLines(this.text.text(GuideTexts.NO_COMMAND.key(), block.source()), 0, this.width,
                    this.sized(TextSize.BODY), GuideStyle.ACCENT);
            this.y += PARAGRAPH_GAP;
            return;
        }
        final GuideCommand command = found.get();
        this.prose(GuideCommandPage.lead(command, this.text));
        this.heading(new GuideBlock.Heading(GuideTexts.HOW_TO_TYPE_IT.key()));
        this.synopsis(GuideCommandPage.synopsis(command, this.text));
        GuideCommandPage.description(command, this.text).forEach(this::prose);
        if (!command.options().isEmpty()) {
            this.heading(new GuideBlock.Heading(GuideTexts.OPTIONS.key()));
            this.layTable(GuideCommandPage.options(command), "", "");
        }
        if (!command.examples().isEmpty()) {
            this.heading(new GuideBlock.Heading(GuideTexts.EXAMPLES.key()));
            this.layTable(GuideCommandPage.examples(command), "", "");
        }
        this.prose(GuideCommandPage.runsOn(command, this.text));
        this.prose(GuideCommandPage.readNext(command, this.text, name -> {
            final String entry = this.commandEntries.get(name);
            return entry == null || entry.equals(this.currentEntry) ? name : "[" + name + "](" + entry + ")";
        }));
    }

    /** How a command is typed, in the code font; a line too long for the column goes on under itself, set in. */
    private void synopsis(final String synopsis) {
        final List<String> lines = wrap(synopsis, this.width - SECTION_INDENT, TextSize.CODE, this.text);
        for (int i = 0; i < lines.size(); i++) {
            this.ensure(CODE_PITCH);
            this.place(new GuidePiece.Text(this.x() + (i == 0 ? 0 : SECTION_INDENT), this.y, lines.get(i),
                    TextSize.CODE, GuideStyle.INK, ""));
            this.y += CODE_PITCH;
        }
        this.y += PARAGRAPH_GAP;
    }

    private void heading(final GuideBlock.Heading heading) {
        final TextSize size = this.sized(TextSize.HEADING);
        final String words = this.upper(this.plainOf(this.text.text(heading.key())));
        // A heading keeps a few lines of what it heads under it, so it is never left alone at a page's foot.
        this.ensure(wrap(words, this.width, size, this.text).size() * size.lineHeight()
                + HEADING_KEEPS * this.sized(TextSize.BODY).lineHeight());
        // A drawing's notes stand close, as lettered notes do: the gap after a paragraph is room enough.
        if (this.y > this.top && !this.style.decor().smallText()) {
            this.y += TITLE_GAP;
        }
        // What can go wrong stands out in the style's accent, so a reader in trouble finds it at a glance.
        final String colour = heading.leadsIntoTrouble() ? GuideStyle.ACCENT : GuideStyle.HEADING;
        final List<GuidePiece> page = this.pieces;
        final int from = page.size();
        final int startY = this.y;
        this.lines(words, 0, this.width, size, colour, "");
        if (this.style.decor().smallText()) {
            this.y += 1;
        }
        if (this.pieces == page && page.subList(from, page.size()).stream()
                .allMatch(piece -> piece instanceof GuidePiece.Text)) {
            this.headingFrom = from;
            this.headingHeight = this.y - startY;
        }
    }

    private void figure(final GuideBlock.Figure figure) {
        this.figures++;
        final String label = this.text.text(GuideTexts.FIGURE.key(), this.chapterNumber + "-" + this.figures);
        final List<String> caption = this.captionLines(label, this.text.text(figure.captionKey()));
        this.ensure(FIGURE_HEIGHT + CAPTION_GAP + this.captionHeight(label, caption));
        this.place(new GuidePiece.Box(this.x(), this.y, this.width, FIGURE_HEIGHT, GuideStyle.SHADE,
                GuideStyle.RULE));
        this.place(new GuidePiece.Item(this.x() + this.width / 2 - 16, this.y + (FIGURE_HEIGHT - 32) / 2,
                figure.item(), 2));
        this.y += FIGURE_HEIGHT + CAPTION_GAP;
        this.caption(label, caption);
        this.y += BLOCK_GAP;
    }

    /* A picture is counted with the figures, drawn as wide as asked (the column at most) and centred in it. */
    private void picture(final GuideBlock.Picture picture) {
        this.figures++;
        final String label = this.text.text(GuideTexts.FIGURE.key(), this.chapterNumber + "-" + this.figures);
        final List<String> caption = this.captionLines(label, this.text.text(picture.captionKey()));
        final int askedWidth = picture.width() == 0 ? this.width : Math.min(this.width, picture.width());
        final int askedHeight = picture.width() == 0 || picture.width() <= this.width ? picture.height()
                : picture.height() * this.width / picture.width();
        // A picture taller than a column with its caption would run past the page foot, so it is scaled down to fit.
        final int room = Math.max(1, this.bottom() - this.top - CAPTION_GAP - this.captionHeight(label, caption));
        final int pictureHeight = Math.min(askedHeight, room);
        final int pictureWidth = pictureHeight == askedHeight ? askedWidth
                : Math.max(1, askedWidth * pictureHeight / askedHeight);
        this.ensure(pictureHeight + CAPTION_GAP + this.captionHeight(label, caption));
        this.place(new GuidePiece.Picture(this.x() + (this.width - pictureWidth) / 2, this.y, pictureWidth,
                pictureHeight, picture.image(), picture.drawing(), picture.data()));
        this.y += pictureHeight + CAPTION_GAP;
        this.caption(label, caption);
        this.y += BLOCK_GAP;
    }

    /**
     * A table, shaped to the column ({@link GuideTable}). One that fits on a page moves on whole when the room left
     * here is too little, unless that would leave more than half of the page empty: then it parts here, as a longer
     * one does. One taller than a page runs on, its caption said again with "(continued)" and its head again over each
     * part, and leaves at least three rows on either side of a part's end; a row taller than a page is cut between two
     * of its lines rather than run past the page's foot.
     */
    private void table(final GuideBlock.Table declared) {
        this.tables++;
        final GuideBlock.Table block = declared.source().isEmpty() ? declared
                : declared.withRows(this.text.rows(declared.source(), declared.columns()));
        this.layTable(block, this.text.text(GuideTexts.TABLE.key(), this.chapterNumber + "-" + this.tables),
                this.text.text(block.captionKey()));
    }

    /**
     * A table laid as {@link #table} lays it, under a caption of that label and title; with no label it has no
     * caption and is not numbered, as a part of a command's page is, its head alone said again over each part.
     */
    private void layTable(final GuideBlock.Table block, final String label, final String title) {
        final boolean captioned = !label.isEmpty();
        final List<String> caption = captioned ? this.captionLines(label, title) : List.of();
        final List<String> continued = captioned ? this.captionLines(label,
                this.text.text(GuideTexts.TABLE_CONTINUED.key(), title)) : List.of();
        final GuideTable table = GuideTable.shape(block, this.width, this.text, this::numberOf, this::elsewhere);
        this.cut.addAll(table.cut());
        final int lead = (captioned ? this.captionHeight(label, caption) + CAPTION_GAP : 0) + table.headHeight();
        final int whole = lead + table.height();
        final int fitsHere = whole(table, 0, this.bottom() - this.y - lead);
        // Moving on whole would leave more than half of this page empty: the table parts here instead.
        final boolean partsHere = this.bottom() - this.y >= (this.bottom() - this.top) / 2
                && fitsHere >= MIN_PART_ROWS && table.rows() - fitsHere >= MIN_PART_ROWS;
        if (whole <= this.bottom() - this.top) {
            if (!partsHere) {
                this.ensure(whole);
            }
        } else if (this.y > this.top
                && whole(table, 0, this.bottom() - this.y - lead) < Math.min(MIN_PART_ROWS, table.rows())) {
            // A table that must part starts on the next page rather than leave a row or two at this one's foot.
            this.nextColumn();
        }
        if (captioned) {
            this.caption(label, caption);
            this.y += CAPTION_GAP;
        }
        this.placeAll(table.head(this.x(), this.y));
        this.y += table.headHeight();
        int row = 0;
        int line = 0;
        while (row < table.rows()) {
            final int[] end = partEnd(table, row, line, this.bottom() - this.y);
            while (row < end[0] || (row == end[0] && line < end[1])) {
                final int to = row < end[0] ? table.lines(row) : end[1];
                this.placeAll(table.slice(this.x(), this.y, row, line, to));
                this.y += table.height(row, line, to);
                if (to == table.lines(row)) {
                    row++;
                    line = 0;
                } else {
                    line = to;
                }
            }
            if (row < table.rows()) {
                this.nextColumn();
                if (captioned) {
                    this.caption(label, continued);
                    this.y += CAPTION_GAP;
                }
                this.placeAll(table.head(this.x(), this.y));
                this.y += table.headHeight();
            }
        }
        this.y += BLOCK_GAP;
    }

    /**
     * A listing of code ({@link GuideListing}), framed under its strip, its rows numbered and coloured. One that fits
     * on a page moves on whole; a longer one runs on, its strip said again with "(continued)", keeping at least three
     * rows on either side of where it parts.
     */
    private void listing(final GuideBlock.Listing block) {
        this.listings++;
        final String label = this.text.text(GuideTexts.LISTING.key(), this.chapterNumber + "-" + this.listings);
        final int columns = (this.width - LISTING_GUTTER - 3) / Math.max(1, this.text.width("0", TextSize.CODE));
        final List<GuideListing.Row> rows = GuideListing.rows(block.lines(),
                this.text.highlight(block.language(), block.lines()), columns);
        final int frame = LISTING_HEAD + 2 * CODE_PAD;
        final int whole = frame + rows.size() * CODE_PITCH;
        if (whole <= this.bottom() - this.top) {
            this.ensure(whole);
        } else if (this.y > this.top
                && (this.bottom() - this.y - frame) / CODE_PITCH < Math.min(MIN_PART_ROWS, rows.size())) {
            this.nextColumn();
        }
        int at = 0;
        while (at < rows.size() || at == 0) {
            int count = Math.max(1, Math.min(rows.size() - at, (this.bottom() - this.y - frame) / CODE_PITCH));
            final int left = rows.size() - at - count;
            if (left > 0 && left < MIN_PART_ROWS && count - (MIN_PART_ROWS - left) >= MIN_PART_ROWS) {
                count -= MIN_PART_ROWS - left;
            }
            count = Math.min(count, rows.size() - at);
            final String continued = this.text.text(GuideTexts.TABLE_CONTINUED.key(), block.name()).strip();
            // A strip too narrow for the name and "(continued)" beside the Copy button keeps the second only.
            final boolean fits = this.text.width(label, TextSize.SMALL_BOLD) + this.text.width(continued,
                    TextSize.SMALL) <= this.width - LISTING_COPY_ROOM;
            final String name = at == 0 ? block.name() : fits ? continued
                    : this.text.text(GuideTexts.TABLE_CONTINUED.key(), "").strip();
            this.place(new GuidePiece.Listing(this.x(), this.y, this.width, frame + count * CODE_PITCH, label, name,
                    LISTING_GUTTER, block.language(), block.lines()));
            for (int i = 0; i < count; i++) {
                this.codeRow(rows.get(at + i), this.y + LISTING_HEAD + CODE_PAD + i * CODE_PITCH);
            }
            this.y += frame + count * CODE_PITCH;
            at += Math.max(1, count);
            if (at < rows.size()) {
                this.nextColumn();
            }
        }
        this.y += BLOCK_GAP;
    }

    /**
     * A session at a terminal, its strip over it and its rows in the code font, each line cut where the page ends and
     * going on at the next row's first character, as a terminal wraps it. One that fits on a page moves on whole; a
     * longer one runs on, its strip said again with "(continued)".
     */
    private void session(final GuideBlock.Session block) {
        final int columns = Math.max(1, (this.width - 2 * SESSION_PAD) / Math.max(1,
                this.text.width("0", TextSize.CODE)));
        final List<List<GuidePiece.SessionRun>> rows = new ArrayList<>();
        for (final GuideBlock.SessionLine line : block.lines()) {
            rows.addAll(sessionRows(line, columns));
        }
        final int frame = SESSION_HEAD + 2 * CODE_PAD;
        if (frame + rows.size() * CODE_PITCH <= this.bottom() - this.top) {
            this.ensure(frame + rows.size() * CODE_PITCH);
        }
        int at = 0;
        while (at < rows.size() || at == 0) {
            final int count = Math.min(rows.size() - at, Math.max(1, (this.bottom() - this.y - frame) / CODE_PITCH));
            final String title = at == 0 ? block.title()
                    : this.text.text(GuideTexts.TABLE_CONTINUED.key(), block.title()).strip();
            this.place(new GuidePiece.Session(this.x(), this.y, this.width, frame + count * CODE_PITCH,
                    block.look(), title, rows.subList(at, at + count)));
            this.y += frame + count * CODE_PITCH;
            at += Math.max(1, count);
            if (at < rows.size()) {
                this.nextColumn();
            }
        }
        this.y += BLOCK_GAP;
    }

    /** The rows a line of a session takes, cut every {@code columns} characters as a terminal cuts it. */
    private static List<List<GuidePiece.SessionRun>> sessionRows(final GuideBlock.SessionLine line,
                                                                 final int columns) {
        final String printed = line.prompt() + line.text();
        final int typedFrom = line.typed() ? line.prompt().length() : Integer.MAX_VALUE;
        final List<List<GuidePiece.SessionRun>> rows = new ArrayList<>();
        int start = 0;
        do {
            final int end = Math.min(printed.length(), start + columns);
            final List<GuidePiece.SessionRun> row = new ArrayList<>();
            final int split = Math.max(start, Math.min(end, typedFrom));
            if (split > start) {
                row.add(new GuidePiece.SessionRun(printed.substring(start, split), line.typed()
                        ? GuidePiece.SessionInk.PROMPT : GuidePiece.SessionInk.ANSWER));
            }
            if (end > split) {
                row.add(new GuidePiece.SessionRun(printed.substring(split, end), GuidePiece.SessionInk.TYPED));
            }
            rows.add(row);
            start = end;
        } while (start < printed.length());
        return rows;
    }

    /** One row of a listing: the number of the line it starts in the column beside it, then its coloured code. */
    private void codeRow(final GuideListing.Row row, final int rowY) {
        if (row.number() > 0) {
            final String number = String.valueOf(row.number());
            this.place(new GuidePiece.Text(this.x() + LISTING_GUTTER - 3 - this.text.width(number, TextSize.CODE),
                    rowY, number, TextSize.CODE, GuideStyle.FAINT, ""));
        }
        int at = this.x() + LISTING_GUTTER + 1;
        for (final GuideListing.Segment segment : row.segments()) {
            if (!segment.text().isBlank()) {
                this.place(new GuidePiece.Text(at, rowY, segment.text(), TextSize.CODE,
                        GuideCode.role(segment.ink()), ""));
            }
            at += this.text.width(segment.text(), TextSize.CODE);
        }
    }

    /**
     * Where the part of a table starting at line {@code line} of row {@code row} ends, in {@code room}: the row, and
     * the line of it, it stops before. A row whose rest does not fit even alone is cut after the lines that do, one at
     * least; a part that would leave fewer than three rows for the next ends earlier, so three go on, when it keeps
     * three of its own.
     */
    private static int[] partEnd(final GuideTable table, final int row, final int line, final int room) {
        int end = row;
        int used = 0;
        int from = line;
        while (end < table.rows() && used + table.height(end, from, table.lines(end)) <= room) {
            used += table.height(end, from, table.lines(end));
            end++;
            from = 0;
        }
        if (end == row) {
            return new int[] {row, line + Math.max(1, table.fitting(row, line, room))};
        }
        final int left = table.rows() - end;
        if (left > 0 && left < MIN_PART_ROWS && table.rows() - MIN_PART_ROWS - row >= MIN_PART_ROWS) {
            end = table.rows() - MIN_PART_ROWS;
        }
        return new int[] {end, 0};
    }

    /** How many whole rows of a table, from {@code row} on, fit in {@code room}. */
    private static int whole(final GuideTable table, final int row, final int room) {
        int end = row;
        int used = 0;
        while (end < table.rows() && used + table.height(end, 0, table.lines(end)) <= room) {
            used += table.height(end, 0, table.lines(end));
            end++;
        }
        return end - row;
    }

    private void recipes(final GuideBlock.Recipes recipes) {
        final int count = this.text.recipeCount(recipes.type(), recipes.output());
        if (count <= 0) {
            this.lines(this.text.text(GuideTexts.NO_RECIPES.key()), 0, this.width, this.sized(TextSize.BODY),
                    GuideStyle.FAINT, "");
            this.y += PARAGRAPH_GAP;
            return;
        }
        final int row = recipeRow(this.style);
        int done = 0;
        while (done < count) {
            this.ensure(row);
            final int fit = Math.max(1, Math.min(count - done, (this.bottom() - this.y) / row));
            this.place(new GuidePiece.Recipes(this.x(), this.y, this.width, recipes.type(), recipes.output(), done,
                    fit));
            this.y += fit * row;
            done += fit;
        }
        this.y += BLOCK_GAP;
    }

    private void steps(final GuideBlock.Steps steps) {
        final TextSize body = this.sized(TextSize.BODY);
        final TextSize bold = this.sized(TextSize.BOLD);
        // Every step's words start where the widest number ends, so steps 9 and 10 line up.
        final int least = this.style.decor().smallText() ? SMALL_STEP_INDENT : STEP_INDENT;
        final int indent = Math.max(least, this.text.width(this.stepNumber(steps.keys().size()), bold) + 4);
        int number = 0;
        for (final String key : steps.keys()) {
            number++;
            final List<List<Run>> lines = this.richWrap(this.text.text(key), this.width - indent, body);
            this.ensure(body.lineHeight());
            this.place(new GuidePiece.Text(this.x(), this.y, this.stepNumber(number), bold, GuideStyle.NUMBER, ""));
            this.placeRich(lines, indent, body, GuideStyle.INK);
            this.y += 2;
        }
        this.y += PARAGRAPH_GAP;
    }

    private String stepNumber(final int number) {
        return this.style.decor().plainNumbers() ? String.valueOf(number) : number + ".";
    }

    private void warning(final GuideBlock.Warning warning) {
        final TextSize body = this.sized(TextSize.BODY);
        final TextSize bold = this.sized(TextSize.BOLD);
        final List<List<Run>> lines = this.richWrap(this.text.text(warning.key()), this.width - 2 * BOX_PAD, body);
        final int height = 2 * BOX_PAD + bold.lineHeight() + lines.size() * body.lineHeight();
        if (height > this.bottom() - this.top) {
            this.richLines(this.text.text(warning.key()), 0, this.width, body, GuideStyle.WARNING);
            this.y += PARAGRAPH_GAP;
            return;
        }
        this.ensure(height);
        this.place(new GuidePiece.Box(this.x(), this.y, this.width, height, GuideStyle.HIGHLIGHT,
                GuideStyle.WARNING));
        this.place(new GuidePiece.Text(this.x() + BOX_PAD, this.y + BOX_PAD,
                this.upper(this.text.text(GuideTexts.WARNING.key())), bold, GuideStyle.WARNING, ""));
        final int boxBottom = this.y + height;
        this.y += BOX_PAD + bold.lineHeight();
        for (final List<Run> line : lines) {
            this.placeRun(line, BOX_PAD, body, GuideStyle.INK);
            this.y += body.lineHeight();
        }
        this.y = boxBottom + BLOCK_GAP;
    }

    private void problems(final GuideBlock.Problems problems) {
        final TextSize body = this.sized(TextSize.BODY);
        final boolean small = this.style.decor().smallText();
        final int indent = small ? SMALL_FIX_INDENT : FIX_INDENT;
        for (final GuideBlock.Problem problem : problems.problems()) {
            this.ensure(2 * body.lineHeight());
            this.lines(this.plainOf(this.text.text(problem.problemKey())), 0, this.width, this.sized(TextSize.BOLD),
                    GuideStyle.INK, "");
            this.richLines(this.text.text(problem.fixKey()), indent, this.width - indent, body, GuideStyle.INK);
            this.y += small ? 3 : PARAGRAPH_GAP;
        }
    }

    private void define(final GuideBlock.Define define, final GuideEntry entry) {
        final TextSize body = this.sized(TextSize.BODY);
        final String term = this.text.text(define.termKey());
        this.ensure(2 * body.lineHeight());
        this.indexLines.add(new PendingLine(term, entry.id(), "", true, this.body.size()));
        this.lines(term, 0, this.width, this.sized(TextSize.BOLD), GuideStyle.HEADING, "");
        this.richLines(this.text.text(define.definitionKey()), FIX_INDENT, this.width - FIX_INDENT, body,
                GuideStyle.INK);
        this.y += PARAGRAPH_GAP;
    }

    private void seeAlso(final GuideBlock.SeeAlso see) {
        final TextSize body = this.sized(TextSize.BODY);
        this.ensure(body.lineHeight());
        int at = this.x();
        final String lead = this.text.text(GuideTexts.SEE.key()) + " ";
        this.place(new GuidePiece.Text(at, this.y, lead, body, GuideStyle.INK, ""));
        at += this.text.width(lead, body);
        for (int i = 0; i < see.entries().size(); i++) {
            final String target = see.entries().get(i);
            // A page this book does not hold is named by its title in the book that does.
            final String held = this.numbers.get(target);
            final String title = held == null ? this.titleElsewhere(target) : null;
            final String number = held != null ? held : title != null ? title : GuideIds.path(target);
            final String joint = i == 0 ? "" : i == see.entries().size() - 1
                    ? " " + this.text.text(GuideTexts.AND.key()) + " " : ", ";
            final int needed = this.text.width(joint + number, body);
            if (at + needed > this.x() + this.width) {
                this.y += body.lineHeight();
                this.ensure(body.lineHeight());
                at = this.x();
            }
            if (!joint.isEmpty()) {
                this.place(new GuidePiece.Text(at, this.y, joint, body, GuideStyle.INK, ""));
                at += this.text.width(joint, body);
            }
            this.place(new GuidePiece.Text(at, this.y, number, body, GuideStyle.LINK, target));
            at += this.text.width(number, body);
        }
        this.y += body.lineHeight() + PARAGRAPH_GAP;
    }

    /** What follows goes on in the next column or on the next page; a page of one column runs straight on. */
    private void cut(final GuideBlock.Break cut) {
        if (this.style.pages().columns() < 2) {
            return;
        }
        if (cut.kind() == GuideBlock.BreakKind.COLUMN) {
            if (this.y > this.top) {
                this.nextColumn();
            }
        } else if (this.column > 0 || this.y > this.top) {
            this.startPage();
        }
    }

    /** The block from above, the front and the side, then the legend of its balloons under them. */
    private void views(final GuideBlock.Views views) {
        final TextSize body = this.sized(TextSize.BODY);
        final TextSize bold = this.sized(TextSize.BOLD);
        this.ensure(VIEWS_HEIGHT);
        final List<String> labels = List.of(this.upper(this.text.text(GuideTexts.VIEW_TOP.key())),
                this.upper(this.text.text(GuideTexts.VIEW_FRONT.key())),
                this.upper(this.text.text(GuideTexts.VIEW_SIDE.key())),
                this.upper(this.text.text(GuideTexts.ONE_BLOCK.key())));
        this.place(new GuidePiece.Views(this.x(), this.y, this.width, views.item(), views.callouts(), labels));
        this.y += VIEWS_HEIGHT + LEGEND_GAP;
        int widest = 0;
        for (final GuideBlock.Callout callout : views.callouts()) {
            widest = Math.max(widest, this.text.width(String.valueOf(callout.number()), bold));
        }
        final int indent = Math.max(LEGEND_GAP, widest + 3);
        for (final GuideBlock.Callout callout : views.callouts()) {
            final List<String> lines = wrap(this.text.text(callout.key()), this.width - indent, body, this.text);
            this.ensure(body.lineHeight());
            this.place(new GuidePiece.Text(this.x(), this.y, String.valueOf(callout.number()), bold,
                    GuideStyle.NUMBER, ""));
            this.placeLines(lines, indent, body, GuideStyle.INK, "");
            this.y += 1;
        }
        this.y += PARAGRAPH_GAP;
    }

    /**
     * A scene in its frame with the strip of its controls under it, then its caption, numbered with the figures, and
     * the legend of its balloons: in two columns when every line of it fits half the column, as a short list does.
     * The frame moves on whole with its caption.
     */
    private void scene(final GuideBlock.Scene scene) {
        this.figures++;
        final String label = this.text.text(GuideTexts.FIGURE.key(), this.chapterNumber + "-" + this.figures);
        final List<String> caption = this.captionLines(label, this.text.text(scene.captionKey()));
        final int captionRoom = CAPTION_GAP + this.captionHeight(label, caption);
        // A scene taller than a fresh page holds with its caption is drawn shorter, so nothing runs past the foot.
        final int frame = Math.min(scene.height() + SCENE_STRIP, this.bottom() - this.top - captionRoom);
        this.ensure(frame + captionRoom);
        this.entryScenes++;
        final String id = this.currentEntry + "#" + this.entryScenes;
        this.place(new GuidePiece.Scene(this.x(), this.y, this.width, frame, id, scene));
        this.y += frame + CAPTION_GAP;
        this.caption(label, caption);
        this.y += CAPTION_GAP;
        if (!scene.balloons().isEmpty()) {
            this.legend(scene.balloons(), id);
        }
        this.y += BLOCK_GAP;
    }

    /** The lines of a scene's legend in small letters, each its balloon's number and what it points at. */
    private void legend(final List<GuideBlock.SceneBalloon> balloons, final String scene) {
        final TextSize body = TextSize.SMALL;
        final List<GuideBlock.SceneBalloon> sorted = balloons.stream()
                .sorted(Comparator.comparingInt(GuideBlock.SceneBalloon::number)).toList();
        final int half = (this.width - COLUMN_GAP) / 2;
        final boolean twoColumns = sorted.size() > 1 && sorted.stream().allMatch(balloon ->
                this.text.width(this.text.text(balloon.key()), body) <= half - SCENE_KEY_ROOM);
        if (twoColumns) {
            final int rows = (sorted.size() + 1) / 2;
            this.ensure(rows * SCENE_KEY_ROW);
            for (int i = 0; i < sorted.size(); i++) {
                final int keyX = this.x() + i / rows * (half + COLUMN_GAP);
                final int keyY = this.y + i % rows * SCENE_KEY_ROW;
                this.place(new GuidePiece.SceneKey(keyX, keyY, half, SCENE_KEY_ROW, scene, sorted.get(i).number()));
                this.place(new GuidePiece.Text(keyX + SCENE_KEY_ROOM, keyY + KEY_TEXT_DROP,
                        this.text.text(sorted.get(i).key()), body, GuideStyle.INK, ""));
            }
            this.y += rows * SCENE_KEY_ROW;
            return;
        }
        for (final GuideBlock.SceneBalloon balloon : sorted) {
            final List<String> lines = wrap(this.text.text(balloon.key()), this.width - SCENE_KEY_ROOM, body,
                    this.text);
            final int height = Math.max(SCENE_KEY_ROW, lines.size() * body.lineHeight() + 2 * KEY_TEXT_DROP);
            this.ensure(height);
            this.place(new GuidePiece.SceneKey(this.x(), this.y, this.width, height, scene, balloon.number()));
            for (int i = 0; i < lines.size(); i++) {
                this.place(new GuidePiece.Text(this.x() + SCENE_KEY_ROOM,
                        this.y + KEY_TEXT_DROP + i * body.lineHeight(), lines.get(i), body, GuideStyle.INK, ""));
            }
            this.y += height;
        }
    }

    /** Blocks seen from above as they are placed, under the plan's title. */
    private void plan(final GuideBlock.Plan plan) {
        final TextSize bold = this.sized(TextSize.BOLD);
        final String caption = this.text.text(plan.captionKey());
        this.ensure(wrap(caption, this.width, bold, this.text).size() * bold.lineHeight() + LABEL_GAP + PLAN_HEIGHT);
        this.lines(caption, 0, this.width, bold, GuideStyle.HEADING, "");
        this.y += LABEL_GAP;
        final List<GuidePiece.PlanPart> parts = new ArrayList<>();
        for (final GuideBlock.PlanPart part : plan.parts()) {
            final String label = this.text.text(part.labelKey());
            parts.add(new GuidePiece.PlanPart(part.item(), part.optional() ? label : this.upper(label),
                    part.optional()));
        }
        this.place(new GuidePiece.Plan(this.x(), this.y, this.width, parts));
        this.y += PLAN_HEIGHT + 1;
    }

    /** The size text of a kind is written in: a drawing letters its notes small, headings small and bold. */
    private TextSize sized(final TextSize size) {
        if (!this.style.decor().smallText()) {
            return size;
        }
        return switch (size) {
            case BODY -> TextSize.SMALL;
            case BOLD, HEADING -> TextSize.SMALL_BOLD;
            default -> size;
        };
    }

    /** Words in capitals where the style writes its headings so, as a drawing does. */
    private String upper(final String words) {
        return this.style.decor().upperHeadings() ? words.toUpperCase(Locale.ROOT) : words;
    }

    /**
     * A caption's lines beside its label, or under it when the label takes more than half the line, which a long
     * translation of "Figure" can.
     */
    private List<String> captionLines(final String label, final String caption) {
        final int labelRoom = this.labelRoom(label);
        return wrap(caption, this.width - labelRoom, TextSize.SMALL, this.text);
    }

    /** How far the caption stands from the label's start: past the label, or nothing when it goes under it. */
    private int labelRoom(final String label) {
        final int room = this.text.width(label, TextSize.SMALL_BOLD) + LABEL_GAP;
        return room <= this.width / 2 ? room : 0;
    }

    /** The caption's height: its lines, and the label's own line when the caption goes under it. */
    private int captionHeight(final String label, final List<String> lines) {
        return (lines.size() + (this.labelRoom(label) == 0 ? 1 : 0)) * TextSize.SMALL.lineHeight();
    }

    private void caption(final String label, final List<String> lines) {
        final int labelRoom = this.labelRoom(label);
        this.place(new GuidePiece.Text(this.x(), this.y, label, TextSize.SMALL_BOLD, GuideStyle.INK, ""));
        if (labelRoom == 0) {
            this.y += TextSize.SMALL.lineHeight();
        }
        for (final String line : lines) {
            this.place(new GuidePiece.Text(this.x() + labelRoom, this.y, line, TextSize.SMALL, GuideStyle.INK, ""));
            this.y += TextSize.SMALL.lineHeight();
        }
    }

    /** Wraps text and places its lines from the cursor down, {@code indent} into the column, running on as it must. */
    private void lines(final String words, final int indent, final int room, final TextSize size, final String colour,
                       final String link) {
        this.placeLines(wrap(words, room, size, this.text), indent, size, colour, link);
    }

    /** Places lines from the cursor down, each {@code indent} into its column, running on where it must. */
    private void placeLines(final List<String> lines, final int indent, final TextSize size, final String colour,
                            final String link) {
        for (final String line : lines) {
            this.ensure(size.lineHeight());
            this.place(new GuidePiece.Text(this.x() + indent, this.y, line, size, colour, link));
            this.y += size.lineHeight();
        }
    }

    /**
     * Wraps a sentence that may hold links and places its lines from the cursor down, each link's words in the link's
     * colour and leading where it does.
     */
    private void richLines(final String sentence, final int indent, final int room, final TextSize size,
                           final String colour) {
        this.placeRich(this.richWrap(sentence, room, size), indent, size, colour);
    }

    /**
     * A sentence that may hold links broken into lines no wider than {@code room}, between words only: a link's
     * number stays with its words, and a comma after a link stays with it.
     */
    private List<List<Run>> richWrap(final String sentence, final int room, final TextSize size) {
        final List<List<Run>> words = new ArrayList<>();
        List<Run> word = new ArrayList<>();
        // A word another entry explains leads there the first time this entry uses it.
        final List<Run> runs = this.glossary.linked(GuideLinks.runs(sentence, this::numberOf, this::elsewhere,
                this::titleElsewhere),
                this.currentEntry, this.usedTerms);
        for (final Run run : runs) {
            if (run.key()) {
                // A key is drawn as one key, its name never broken even where it has a space, as Page Up does.
                word.add(run);
                continue;
            }
            int at = 0;
            final String runText = run.text();
            while (at < runText.length()) {
                if (runText.charAt(at) == ' ') {
                    if (!word.isEmpty()) {
                        words.add(word);
                        word = new ArrayList<>();
                    }
                    at++;
                    continue;
                }
                int end = at;
                while (end < runText.length() && runText.charAt(end) != ' ') {
                    end++;
                }
                word.add(new Run(runText.substring(at, end), run.target()));
                at = end;
            }
        }
        if (!word.isEmpty()) {
            words.add(word);
        }
        final List<List<Run>> fitted = new ArrayList<>();
        for (final List<Run> each : words) {
            fitted.addAll(this.brokenWord(each, room, size));
        }
        final List<List<Run>> lines = new ArrayList<>();
        List<Run> line = new ArrayList<>();
        for (final List<Run> next : fitted) {
            final List<Run> tried = new ArrayList<>(line);
            if (!tried.isEmpty()) {
                // A space between two words of one link belongs to the link; any other to the plain words.
                final String between = tried.getLast().target().equals(next.getFirst().target())
                        ? next.getFirst().target() : "";
                tried.add(new Run(" ", between));
            }
            tried.addAll(next);
            if (line.isEmpty() || this.runsWidth(tried, size) <= room) {
                line = tried;
                continue;
            }
            lines.add(line);
            line = new ArrayList<>(next);
        }
        if (!line.isEmpty() || lines.isEmpty()) {
            lines.add(line);
        }
        final List<List<Run>> joined = new ArrayList<>();
        for (final List<Run> each : lines) {
            joined.add(join(each));
        }
        return joined;
    }

    /**
     * A word too wide for the room on its own, as a name in code or a setting is, in pieces that fit, broken where
     * {@link #pieces} breaks a word, each character still leading where it did. A word with a key in it is left whole.
     */
    private List<List<Run>> brokenWord(final List<Run> word, final int room, final TextSize size) {
        if (word.stream().anyMatch(Run::key) || this.runsWidth(word, size) <= room) {
            return List.of(word);
        }
        final StringBuilder whole = new StringBuilder();
        final List<String> targets = new ArrayList<>();
        for (final Run run : word) {
            whole.append(run.text());
            for (int i = 0; i < run.text().length(); i++) {
                targets.add(run.target());
            }
        }
        final List<List<Run>> broken = new ArrayList<>();
        int at = 0;
        for (final String piece : pieces(whole.toString(), room, size, this.text)) {
            final int end = at + piece.length();
            final List<Run> runs = new ArrayList<>();
            int start = at;
            for (int i = at + 1; i <= end; i++) {
                if (i == end || !targets.get(i).equals(targets.get(start))) {
                    runs.add(new Run(whole.substring(start, i), targets.get(start)));
                    start = i;
                }
            }
            broken.add(runs);
            at = end;
        }
        return broken;
    }

    /** Places lines of runs from the cursor down, each {@code indent} into its column, running on where it must. */
    private void placeRich(final List<List<Run>> lines, final int indent, final TextSize size, final String colour) {
        for (final List<Run> line : lines) {
            this.ensure(size.lineHeight());
            this.placeRun(line, indent, size, colour);
            this.y += size.lineHeight();
        }
    }

    /**
     * One line of runs at the cursor's height, plain words in {@code colour}, links in the link's, and keys drawn as
     * keys.
     */
    private void placeRun(final List<Run> line, final int indent, final TextSize size, final String colour) {
        final List<Run> before = new ArrayList<>();
        for (final Run run : line) {
            final int at = this.x() + indent + this.runsWidth(before, size);
            if (run.key()) {
                this.place(new GuidePiece.Key(at, this.y, run.text()));
            } else {
                this.place(new GuidePiece.Text(at, this.y, run.text(), size,
                        run.target().isEmpty() ? colour : GuideStyle.LINK, run.target()));
            }
            before.add(run);
        }
    }

    /** How wide runs are drawn side by side: words at their size, a key at the width of the key it is drawn as. */
    private int runsWidth(final List<Run> runs, final TextSize size) {
        int width = 0;
        final StringBuilder words = new StringBuilder();
        for (final Run run : runs) {
            if (run.key()) {
                width += this.text.width(words.toString(), size) + this.keyWidth(run.text());
                words.setLength(0);
            } else {
                words.append(run.text());
            }
        }
        return width + this.text.width(words.toString(), size);
    }

    /** How wide a key is drawn: its name in small letters, the key round it, and a pixel after it. */
    private int keyWidth(final String name) {
        return this.text.width(name, TextSize.SMALL) + KEY_ROOM + 1;
    }

    /** The number a link's target has in this manual: an entry's or a section's, or a chapter's; null when not held. */
    private String numberOf(final String target) {
        final String number = this.numbers.get(target);
        return number != null ? number : this.chapterNumbers.get(target);
    }

    /** For a link's target this manual does not hold, the words naming the book that does, or null. */
    private String elsewhere(final String target) {
        final String book = this.text.elsewhere(target);
        return book.isEmpty() ? null : this.text.text(GuideTexts.IN_BOOK.key(), book);
    }

    /** A sentence as plain words: its links as their words and numbers, its keys as their names in brackets. */
    private String plainOf(final String sentence) {
        return GuideLinks.plain(sentence, this::numberOf, this::elsewhere, this::titleElsewhere);
    }

    /** For a link's target this manual does not hold, its title in the book that does, or null. */
    private String titleElsewhere(final String target) {
        final String title = this.text.titleOf(target);
        return title.isEmpty() ? null : title;
    }

    /** Runs side by side that lead to the same place made one, so a link is one piece to point at; a key stays one. */
    private static List<Run> join(final List<Run> runs) {
        final List<Run> joined = new ArrayList<>();
        for (final Run run : runs) {
            if (!joined.isEmpty() && !run.key() && !joined.getLast().key()
                    && joined.getLast().target().equals(run.target())) {
                final Run last = joined.removeLast();
                joined.add(new Run(last.text() + run.text(), run.target()));
            } else {
                joined.add(run);
            }
        }
        return joined;
    }

    /** Moves on to the next column when what comes next is taller than the room left in this one. */
    private void ensure(final int height) {
        if (this.y + height > this.bottom() && this.y > this.top) {
            this.nextColumn();
        }
    }

    /**
     * Goes on at the top of the next column, or of the next page after the last column. A heading with nothing yet
     * under it goes on too, at the top, so it is never left alone at the foot of the column before what it heads.
     */
    private void nextColumn() {
        final List<GuidePiece> heading = new ArrayList<>();
        final int fromX = this.x();
        final int fromY = this.y - this.headingHeight;
        // A heading already at the top of its column stays: carried on, it would leave the column empty.
        if (this.headingFrom >= 0 && this.headingFrom < this.pieces.size() && fromY > this.top) {
            heading.addAll(this.pieces.subList(this.headingFrom, this.pieces.size()));
            this.pieces.subList(this.headingFrom, this.pieces.size()).clear();
        }
        if (this.column + 1 < this.style.pages().columns()) {
            this.column++;
            this.y = this.top;
        } else {
            this.startPage();
        }
        this.headingFrom = -1;
        for (final GuidePiece piece : heading) {
            final GuidePiece.Text line = (GuidePiece.Text) piece;
            this.pieces.add(new GuidePiece.Text(line.x() - fromX + this.x(), line.y() - fromY + this.y, line.text(),
                    line.size(), line.colour(), line.link()));
        }
        if (!heading.isEmpty()) {
            this.y += this.headingHeight;
        }
    }

    /** Closes the open page, if any, and opens the next: the page it opens will be {@code body.size()}. */
    private void startPage() {
        this.finishPage();
        this.pieces = new ArrayList<>();
        this.headingFrom = -1;
        this.y = this.top;
        this.column = 0;
        this.pageInChapter++;
        this.pageLabel = this.sectionLabel;
        this.open = true;
    }

    private void finishPage() {
        if (this.open) {
            this.body.add(new BodyPage(this.pieces, this.chapterId, this.chapterTitle, this.pageLabel,
                    this.chapterNumber, this.pageInChapter, this.drawing, this.drawingTitle));
            this.open = false;
        }
    }

    /** A page left blank, so the next chapter opens on a left page. */
    private void blank() {
        this.startPage();
        this.pieces.addAll(this.blankPieces());
        this.finishPage();
    }

    /** The words a blank page carries, in its middle, saying it was left blank on purpose. */
    private List<GuidePiece> blankPieces() {
        final List<GuidePiece> words = new ArrayList<>();
        final List<String> lines = wrap(this.text.text(GuideTexts.BLANK.key()), this.fullWidth, TextSize.SMALL,
                this.text);
        int lineY = (this.top + bottom(this.style, 0)) / 2 - lines.size() * TextSize.SMALL.lineHeight() / 2;
        for (final String line : lines) {
            final int lineWidth = this.text.width(line, TextSize.SMALL);
            words.add(new GuidePiece.Text(this.style.margin() + Math.max(0, (this.fullWidth - lineWidth) / 2),
                    lineY, line, TextSize.SMALL, GuideStyle.FAINT, ""));
            lineY += TextSize.SMALL.lineHeight();
        }
        return words;
    }

    private void place(final GuidePiece piece) {
        this.headingFrom = -1;
        this.pieces.add(piece);
    }

    private void placeAll(final List<GuidePiece> placed) {
        this.headingFrom = -1;
        this.pieces.addAll(placed);
    }

    private int x() {
        return columnX(this.style, this.column);
    }

    private int bottom() {
        return bottom(this.style, this.column);
    }

    /** The folio of every page of the chapters, the front matter being {@link #front} pages long. */
    private List<String> bodyFolios() {
        final List<String> folios = new ArrayList<>();
        for (int i = 0; i < this.body.size(); i++) {
            final BodyPage page = this.body.get(i);
            folios.add(switch (this.style.folios()) {
                case CHAPTER_PAGE -> page.chapterNumber() + "-" + page.pageInChapter();
                case SEQUENTIAL -> String.valueOf(this.front + i + 1);
                case DRAWING -> page.drawing().isEmpty() ? String.valueOf(this.front + i + 1) : page.drawing();
            });
        }
        return folios;
    }

    /** How many sheets the drawing each page belongs to has: the pages in a row that carry its number. */
    private int[] sheets() {
        final int[] sheets = new int[this.body.size()];
        int start = 0;
        for (int i = 1; i <= this.body.size(); i++) {
            if (i == this.body.size() || !this.body.get(i).drawing().equals(this.body.get(start).drawing())) {
                for (int page = start; page < i; page++) {
                    sheets[page] = i - start;
                }
                start = i;
            }
        }
        return sheets;
    }

    /** Which sheet of its drawing a page is, from one. */
    private int sheetOf(final int page) {
        int sheet = 1;
        while (page - sheet >= 0 && this.body.get(page - sheet).drawing().equals(this.body.get(page).drawing())) {
            sheet++;
        }
        return sheet;
    }

    /** Gives every line laid before its page was known, a chapter's sections, the page it leads to. */
    private void fillLeaders(final List<String> bodyFolios) {
        for (final BodyPage page : this.body) {
            final ListIterator<GuidePiece> pieces = page.pieces().listIterator();
            while (pieces.hasNext()) {
                if (pieces.next() instanceof GuidePiece.Leader leader && leader.right().isEmpty()
                        && !leader.link().isEmpty()) {
                    pieces.set(leader.withRight(this.folioOf(leader.link(), bodyFolios)));
                }
            }
        }
    }

    private List<Leader> contentsLines(final GuideContents contents) {
        final List<Leader> lines = new ArrayList<>();
        int chapterNo = 0;
        for (final GuideContents.Chapter chapter : contents.chapters()) {
            chapterNo++;
            final String widest = chapterNo + FOLIO_PAGES;
            lines.add(this.contentsLine(chapterNo + "  ", this.text.text(chapter.chapter().titleKey()),
                    chapter.chapter().id(), TextSize.BOLD, 0, widest));
            for (final GuideContents.Section section : chapter.sections()) {
                lines.add(this.contentsLine(this.numbers.get(section.section().id()) + "  ",
                        this.text.text(section.section().titleKey()), section.section().id(), TextSize.BODY,
                        SECTION_INDENT, widest));
            }
        }
        if (!this.glossary.isEmpty()) {
            lines.add(this.contentsLine("", this.text.text(GuideTexts.GLOSSARY.key()), GLOSSARY, TextSize.BOLD, 0,
                    GLOSSARY_FOLIO + "1"));
        }
        lines.add(this.contentsLine("", this.text.text(GuideTexts.INDEX.key()), "", TextSize.BOLD, 0, "X-1"));
        return lines;
    }

    /**
     * A line of the contents, its title broken into as many lines as it needs beside the widest page it may lead to,
     * the lines after the first set in under the title rather than under its number.
     */
    private Leader contentsLine(final String number, final String title, final String target, final TextSize size,
                                final int indent, final String widest) {
        final int hang = this.text.width(number, size);
        final int room = this.fullWidth - indent - hang - this.text.width(widest, size) - LEADER_DOTS;
        final List<String> lines = new ArrayList<>(wrap(title, room, size, this.text));
        lines.set(0, number + lines.getFirst());
        return new Leader(lines, hang, target, size, indent);
    }

    /** How many pages the contents take, the first carrying their title above them. */
    private int contentsPages(final List<Leader> lines) {
        final int bottom = bottom(this.style, 0);
        int pages = 1;
        int lineY = this.top + TextSize.TITLE.lineHeight() + BLOCK_GAP;
        for (final Leader line : lines) {
            if (lineY + line.height() > bottom) {
                pages++;
                lineY = this.top;
            }
            lineY += line.height();
        }
        return pages;
    }

    private void layContents(final List<GuideBook.Page> pages, final List<Leader> lines,
                             final List<String> bodyFolios) {
        final String contentsTitle = this.text.text(GuideTexts.CONTENTS.key());
        final int x = this.style.margin();
        final int bottom = bottom(this.style, 0);
        List<GuidePiece> page = new ArrayList<>();
        int lineY = this.top;
        page.add(new GuidePiece.Text(x, lineY, contentsTitle, TextSize.TITLE, GuideStyle.HEADING, ""));
        lineY += TextSize.TITLE.lineHeight() + BLOCK_GAP;
        for (final Leader line : lines) {
            // A line that runs on keeps its lines together, on the next page when they do not fit this one.
            if (lineY + line.height() > bottom) {
                pages.add(new GuideBook.Page(roman(pages.size() + 1), contentsTitle, "", page));
                page = new ArrayList<>();
                lineY = this.top;
            }
            final String folio = line.target().isEmpty() ? "X-1" : GLOSSARY.equals(line.target())
                    ? GLOSSARY_FOLIO + "1" : this.folioOf(line.target(), bodyFolios);
            final int lastX = x + line.indent() + (line.lines().size() > 1 ? line.hang() : 0);
            for (int i = 0; i < line.lines().size() - 1; i++) {
                page.add(new GuidePiece.Text(i == 0 ? x + line.indent() : lastX, lineY, line.lines().get(i),
                        line.size(), GuideStyle.INK, ""));
                lineY += line.size().lineHeight();
            }
            page.add(new GuidePiece.Leader(lastX, lineY, x + this.fullWidth - lastX, "", line.lines().getLast(),
                    folio, line.size(), GuideStyle.INK, line.target(), ""));
            lineY += line.size().lineHeight() + 2;
        }
        pages.add(new GuideBook.Page(roman(pages.size() + 1), contentsTitle, "", page));
    }

    private String folioOf(final String target, final List<String> bodyFolios) {
        final Integer page = this.bodyTargets.get(target);
        return page == null || page >= bodyFolios.size() ? "" : bodyFolios.get(page);
    }

    /** The "About this manual" page; what does not fit on it is left off, an about page being one page. */
    private void layAbout(final List<GuideBook.Page> pages, final int number) {
        final String about = this.text.text(GuideTexts.ABOUT.key());
        final int x = this.style.margin();
        final int bottom = bottom(this.style, 0);
        final List<GuidePiece> page = new ArrayList<>();
        int lineY = this.top;
        for (final String line : wrap(about, this.fullWidth, TextSize.HEADING, this.text)) {
            page.add(new GuidePiece.Text(x, lineY, line, TextSize.HEADING, GuideStyle.HEADING, ""));
            lineY += TextSize.HEADING.lineHeight();
        }
        lineY += TITLE_GAP;
        for (final String key : this.manual.aboutKeys()) {
            for (final String line : wrap(this.text.text(key), this.fullWidth, TextSize.BODY, this.text)) {
                if (lineY + TextSize.BODY.lineHeight() > bottom) {
                    break;
                }
                page.add(new GuidePiece.Text(x, lineY, line, TextSize.BODY, GuideStyle.INK, ""));
                lineY += TextSize.BODY.lineHeight();
            }
            lineY += PARAGRAPH_GAP;
        }
        pages.add(new GuideBook.Page(roman(number), about, "", page));
    }

    /**
     * The drawing list a set of drawings opens with: each drawing's number, title and how many sheets it has, then
     * the manual's notes on where to start; as many sheets of it as its rows need.
     */
    private void layDrawingList(final List<GuideBook.Page> pages, final int[] sheets) {
        final String title = this.text.text(GuideTexts.DRAWING_LIST.key());
        final int x = this.style.margin();
        final int titleX = x + LIST_TITLE_X;
        final int sheetsX = Math.max(titleX + LIST_TITLE_X, x + this.fullWidth - LIST_SHEETS_FROM_RIGHT);
        final int bottom = this.style.decor().titleBlock()
                ? this.style.pageHeight() - FRAME - TITLE_BLOCK_HEIGHT - LABEL_GAP : bottom(this.style, 0);
        final List<List<GuidePiece>> list = new ArrayList<>();
        List<GuidePiece> page = this.listSheet(title, x, titleX, sheetsX);
        int lineY = this.top + TextSize.TITLE.lineHeight() + 10;
        for (final DrawingRow row : this.drawingRows) {
            if (lineY + LIST_ROW > bottom) {
                list.add(page);
                page = this.listSheet(title, x, titleX, sheetsX);
                lineY = this.top + TextSize.TITLE.lineHeight() + 10;
            }
            lineY += 3;
            final Integer at = this.bodyTargets.get(row.target());
            final int count = at == null || at >= sheets.length ? 1 : sheets[at];
            page.add(new GuidePiece.Text(x, lineY, row.number(), TextSize.CODE, GuideStyle.INK, row.target()));
            page.add(new GuidePiece.Text(titleX, lineY + 1, this.fit(row.title(), sheetsX - titleX - 6,
                    TextSize.BODY), TextSize.BODY, GuideStyle.INK, row.target()));
            page.add(new GuidePiece.Text(sheetsX + 8, lineY, String.valueOf(count), TextSize.CODE, GuideStyle.INK,
                    ""));
            lineY += LIST_ROW - 3;
            page.add(new GuidePiece.Dots(x, lineY - 1, this.fullWidth, GuideStyle.FAINT));
        }
        lineY += BLOCK_GAP;
        for (final String key : this.manual.aboutKeys()) {
            for (final String line : wrap(this.text.text(key), this.fullWidth, TextSize.SMALL, this.text)) {
                if (lineY + TextSize.SMALL.lineHeight() > bottom) {
                    list.add(page);
                    page = this.listSheet(title, x, titleX, sheetsX);
                    lineY = this.top + TextSize.TITLE.lineHeight() + 10;
                }
                page.add(new GuidePiece.Text(x, lineY, line, TextSize.SMALL, GuideStyle.ACCENT, ""));
                lineY += TextSize.SMALL.lineHeight();
            }
            lineY += PARAGRAPH_GAP;
        }
        list.add(page);
        final String number = this.drawingNumber(0);
        for (int i = 0; i < list.size(); i++) {
            pages.add(new GuideBook.Page(number, "", "", list.get(i), title, i + 1, list.size()));
        }
    }

    /** A new sheet of the drawing list, with its title and the heads of its columns between two rules. */
    private List<GuidePiece> listSheet(final String title, final int x, final int titleX, final int sheetsX) {
        final List<GuidePiece> page = new ArrayList<>();
        int lineY = this.top;
        page.add(new GuidePiece.Text(x, lineY, title, TextSize.TITLE, GuideStyle.HEADING, ""));
        lineY += TextSize.TITLE.lineHeight();
        page.add(new GuidePiece.Rule(x, lineY - 2, this.fullWidth, GuideStyle.RULE));
        page.add(new GuidePiece.Text(x, lineY + 1, this.text.text(GuideTexts.DRAWING.key()).toUpperCase(Locale.ROOT),
                TextSize.SMALL_BOLD, GuideStyle.FAINT, ""));
        page.add(new GuidePiece.Text(titleX, lineY + 1, this.text.text(GuideTexts.TITLE.key())
                .toUpperCase(Locale.ROOT), TextSize.SMALL_BOLD, GuideStyle.FAINT, ""));
        page.add(new GuidePiece.Text(sheetsX, lineY + 1, this.text.text(GuideTexts.SHEETS.key())
                .toUpperCase(Locale.ROOT), TextSize.SMALL_BOLD, GuideStyle.FAINT, ""));
        page.add(new GuidePiece.Rule(x, lineY + 9, this.fullWidth, GuideStyle.RULE));
        return page;
    }

    /** The words as they fit the room, cut short with an ellipsis when they do not. */
    private String fit(final String words, final int room, final TextSize size) {
        if (this.text.width(words, size) <= room) {
            return words;
        }
        String cut = words;
        while (!cut.isEmpty() && this.text.width(cut + ELLIPSIS, size) > room) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut.stripTrailing() + ELLIPSIS;
    }

    private List<GuideBook.IndexLine> indexLines(final List<String> bodyFolios) {
        final List<GuideBook.IndexLine> lines = new ArrayList<>();
        for (final PendingLine line : this.indexLines) {
            final String folio = line.page() < bodyFolios.size() ? bodyFolios.get(line.page()) : "";
            lines.add(new GuideBook.IndexLine(line.text(), folio, line.target(), line.icon(), line.term()));
        }
        lines.sort(Comparator.comparing((GuideBook.IndexLine line) -> line.text().toLowerCase(Locale.ROOT))
                .thenComparing(GuideBook.IndexLine::folio));
        return lines;
    }

    /**
     * The glossary, between the last chapter and the index: every word an entry explains, alphabetical under its
     * letter, with the page that explains it and what it means set in under it.
     */
    private void layGlossary(final List<GuideBook.Page> pages, final List<GuideBook.IndexLine> index) {
        final String header = this.text.text(GuideTexts.GLOSSARY.key());
        final int x = this.style.margin();
        final int bottom = bottom(this.style, 0);
        int number = 1;
        List<GuidePiece> page = new ArrayList<>();
        int lineY = this.top;
        String letter = "";
        for (final GuideGlossary.Item item : this.glossary.items()) {
            final String first = item.term().substring(0, 1).toUpperCase(Locale.ROOT);
            final List<String> meaning = wrap(GuideLinks.plain(this.text.text(item.definitionKey()), this::numberOf,
                    this::elsewhere, this::titleElsewhere), this.fullWidth - SECTION_INDENT, TextSize.BODY, this.text);
            // The page that explains the word, as the index lists it.
            final String folio = index.stream().filter(line -> line.term() && line.text().equals(item.term())
                    && line.target().equals(item.entry())).map(GuideBook.IndexLine::folio).findFirst().orElse("");
            // A word too long for its line runs on under itself, whole, the page on its last line.
            final List<String> words = wrap(item.term(), this.fullWidth - this.text.width(folio, TextSize.BOLD)
                    - LEAST_DOTS, TextSize.BOLD, this.text);
            final int head = words.size() * TextSize.BODY.lineHeight() + 2 + (first.equals(letter) ? 0
                    : TextSize.LETTER.lineHeight());
            // What a word means runs on to the next page only with two lines of it at least on either side, so no
            // line is ever left alone at the foot of a page or the top of the next; otherwise the word starts there.
            int room = (bottom - lineY - head) / TextSize.BODY.lineHeight();
            if (room < meaning.size() && Math.min(room, meaning.size() - LEAST_LINES) < LEAST_LINES
                    && !page.isEmpty()) {
                pages.add(new GuideBook.Page(GLOSSARY_FOLIO + number++, header, "", page));
                page = new ArrayList<>();
                lineY = this.top;
                room = (bottom - lineY - head) / TextSize.BODY.lineHeight();
            }
            final int runsOn = room < meaning.size() ? Math.min(room, meaning.size() - LEAST_LINES)
                    : meaning.size();
            if (!first.equals(letter)) {
                letter = first;
                page.add(new GuidePiece.Text(x, lineY, letter, TextSize.LETTER, GuideStyle.HEADING, ""));
                lineY += TextSize.LETTER.lineHeight();
            }
            for (final String line : words.subList(0, words.size() - 1)) {
                page.add(new GuidePiece.Text(x, lineY, line, TextSize.BOLD, GuideStyle.INK, ""));
                lineY += TextSize.BODY.lineHeight();
            }
            page.add(new GuidePiece.Leader(x, lineY, this.fullWidth, "", words.getLast(), folio, TextSize.BOLD,
                    GuideStyle.INK, item.entry(), ""));
            lineY += TextSize.BODY.lineHeight() + 2;
            for (int i = 0; i < meaning.size(); i++) {
                if (i == runsOn || lineY + TextSize.BODY.lineHeight() > bottom) {
                    pages.add(new GuideBook.Page(GLOSSARY_FOLIO + number++, header, "", page));
                    page = new ArrayList<>();
                    lineY = this.top;
                }
                page.add(new GuidePiece.Text(x + SECTION_INDENT, lineY, meaning.get(i), TextSize.BODY,
                        GuideStyle.INK, ""));
                lineY += TextSize.BODY.lineHeight();
            }
            lineY += PARAGRAPH_GAP;
        }
        pages.add(new GuideBook.Page(GLOSSARY_FOLIO + number, header, "", page));
    }

    private void layIndex(final List<GuideBook.Page> pages, final List<GuideBook.IndexLine> lines) {
        final String header = this.text.text(GuideTexts.INDEX.key());
        final int x = this.style.margin();
        final int bottom = bottom(this.style, 0);
        int number = 1;
        List<GuidePiece> page = new ArrayList<>();
        int lineY = this.top;
        String letter = "";
        for (final GuideBook.IndexLine line : lines) {
            final String first = line.text().isEmpty() ? "" : line.text().substring(0, 1).toUpperCase(Locale.ROOT);
            final int indent = line.term() ? SECTION_INDENT : 0;
            final TextSize size = line.term() ? TextSize.BODY : TextSize.BOLD;
            // A line too long for the page runs on under itself, its item and its page beside its last words.
            final int hang = line.icon().isEmpty() ? 0 : ICON - 4;
            final List<String> words = wrap(line.text(), this.fullWidth - indent - hang
                    - this.text.width(line.folio(), size) - LEADER_DOTS, size, this.text);
            final int height = words.size() * TextSize.BODY.lineHeight() + 2 + (first.equals(letter) ? 0
                    : TextSize.LETTER.lineHeight());
            if (lineY + height > bottom) {
                pages.add(new GuideBook.Page("X-" + number++, header, "", page));
                page = new ArrayList<>();
                lineY = this.top;
            }
            if (!first.equals(letter)) {
                letter = first;
                page.add(new GuidePiece.Text(x, lineY, letter, TextSize.LETTER, GuideStyle.HEADING, ""));
                lineY += TextSize.LETTER.lineHeight();
            }
            for (final String part : words.subList(0, words.size() - 1)) {
                page.add(new GuidePiece.Text(x + indent + hang, lineY, part, size, GuideStyle.INK, ""));
                lineY += TextSize.BODY.lineHeight();
            }
            page.add(new GuidePiece.Leader(x + indent, lineY, this.fullWidth - indent, "", words.getLast(),
                    line.folio(), size, GuideStyle.INK, line.target(), line.icon()));
            lineY += TextSize.BODY.lineHeight() + 2;
        }
        pages.add(new GuideBook.Page("X-" + number, header, "", page));
    }

    /**
     * A page of a chapter as it was laid, before the front matter's length fixes its place and its head.
     *
     * @param drawing the number of the drawing it is a sheet of, in a set of drawings, or empty
     * @param title   that drawing's title
     */
    private record BodyPage(List<GuidePiece> pieces, String chapter, String chapterTitle, String sectionLabel,
                            int chapterNumber, int pageInChapter, String drawing, String title) {
    }

    /**
     * A line of the contents, before its page is known.
     *
     * @param lines its title as it is broken, the first line opening with its number
     * @param hang  how far in from the number the lines after the first are set
     */
    private record Leader(List<String> lines, int hang, String target, TextSize size, int indent) {

        /** The height it takes on its page. */
        int height() {
            return this.lines.size() * this.size.lineHeight() + 2;
        }
    }

    /** A line of the index, before its folio is known. */
    private record PendingLine(String text, String target, String icon, boolean term, int page) {
    }

    /** A row of the drawing list: a drawing's number, its title, and the entry it is. */
    private record DrawingRow(String number, String title, String target) {
    }
}
