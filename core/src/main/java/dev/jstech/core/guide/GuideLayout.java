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
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;

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
 * text going on in the next column when one is full. A paragraph, a table, a list of steps or recipes runs on where it
 * does not fit; a figure, a warning, the views of a block and a special block move on whole. A heading is never left
 * alone at the foot of a column.
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
    private List<GuidePiece> pieces = new ArrayList<>();
    private int y;
    private int column;
    private int front;
    private int chapterNumber;
    private int pageInChapter;
    private int figures;
    private int tables;
    private String chapterNamespace = "";
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
    private static final int TABLE_PAD = 3;
    private static final int TABLE_LABEL_SHARE = 45;
    private static final int TITLE_GAP = 2;
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
            line = new StringBuilder(word);
            while (text.width(line.toString(), size) > room && line.length() > 1) {
                int cut = line.length() - 1;
                while (cut > 1 && text.width(line.substring(0, cut), size) > room) {
                    cut--;
                }
                lines.add(line.substring(0, cut));
                line = new StringBuilder(line.substring(cut));
            }
        }
        if (!line.isEmpty() || lines.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    private GuideBook book(final GuideContents contents) {
        this.number(contents);
        final List<Leader> contentsLines = this.drawings ? List.of() : this.contentsLines(contents);
        final int contentsPages = this.drawings ? 0
                : Math.max(1, this.pagesFor(contentsLines.size(), TextSize.BODY, TextSize.TITLE));
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
        final int indexAt = pages.size();
        if (!this.drawings) {
            this.layIndex(pages, index);
        }
        return new GuideBook(pages, targets, this.numbers, index, 0, indexAt);
    }

    /** Numbers every section and entry first, so a link may lead to one printed after it. */
    private void number(final GuideContents contents) {
        int chapterNo = 0;
        int sectionsBefore = 0;
        for (final GuideContents.Chapter chapter : contents.chapters()) {
            chapterNo++;
            this.chapterNumbers.put(chapter.chapter().namespace(), String.valueOf(chapterNo));
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
        this.chapterNamespace = part.chapter().namespace();
        this.chapterTitle = this.text.text(part.chapter().titleKey());
        this.sectionLabel = this.chapterTitle;
        if (this.drawings) {
            // A set of drawings has no opening pages: its list says what each drawing is.
            this.bodyTargets.put(this.chapterNamespace, this.body.size());
        } else {
            this.startPage();
            this.bodyTargets.put(this.chapterNamespace, this.body.size());
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
            this.ensure(OPENER_ROW);
            final int rowX = this.x();
            final String id = section.section().id();
            if (!section.section().icon().isEmpty()) {
                this.place(new GuidePiece.Item(rowX, this.y, section.section().icon(), 1));
            }
            this.place(new GuidePiece.Leader(rowX + ICON + 2, this.y + 4, this.width - ICON - 2,
                    this.numbers.get(id), " " + this.text.text(section.section().titleKey()), "", TextSize.BODY,
                    GuideStyle.INK, id, ""));
            this.y += OPENER_ROW;
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
            case GuideBlock.Paragraph paragraph -> {
                this.richLines(this.text.text(paragraph.key()), 0, this.width, this.sized(TextSize.BODY),
                        GuideStyle.INK);
                this.y += PARAGRAPH_GAP;
            }
            case GuideBlock.Heading heading -> this.heading(heading);
            case GuideBlock.Figure figure -> this.figure(figure);
            case GuideBlock.Picture picture -> this.picture(picture);
            case GuideBlock.Table table -> this.table(table);
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
            case GuideBlock.Custom custom -> {
                this.ensure(custom.height());
                this.place(new GuidePiece.Custom(this.x(), this.y, this.width, custom.height(), custom.type(),
                        custom.data()));
                this.y += custom.height() + BLOCK_GAP;
            }
        }
    }

    private void heading(final GuideBlock.Heading heading) {
        final TextSize size = this.sized(TextSize.HEADING);
        this.ensure(size.lineHeight() + 2 * this.sized(TextSize.BODY).lineHeight());
        // A drawing's notes stand close, as lettered notes do: the gap after a paragraph is room enough.
        if (this.y > this.top && !this.style.decor().smallText()) {
            this.y += TITLE_GAP;
        }
        // What can go wrong stands out in the style's accent, so a reader in trouble finds it at a glance.
        final String colour = troubles(heading.key()) ? GuideStyle.ACCENT : GuideStyle.HEADING;
        this.lines(this.upper(this.text.text(heading.key())), 0, this.width, size, colour, "");
        if (this.style.decor().smallText()) {
            this.y += 1;
        }
    }

    private void figure(final GuideBlock.Figure figure) {
        this.figures++;
        final String label = this.text.text(GuideTexts.FIGURE.key(), this.chapterNumber + "-" + this.figures);
        final List<String> caption = this.captionLines(label, figure.captionKey());
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
        final List<String> caption = this.captionLines(label, picture.captionKey());
        final int pictureWidth = picture.width() == 0 ? this.width : Math.min(this.width, picture.width());
        final int pictureHeight = picture.width() == 0 || picture.width() <= this.width ? picture.height()
                : picture.height() * this.width / picture.width();
        this.ensure(pictureHeight + CAPTION_GAP + this.captionHeight(label, caption));
        this.place(new GuidePiece.Picture(this.x() + (this.width - pictureWidth) / 2, this.y, pictureWidth,
                pictureHeight, picture.image(), picture.drawing(), picture.data()));
        this.y += pictureHeight + CAPTION_GAP;
        this.caption(label, caption);
        this.y += BLOCK_GAP;
    }

    private void table(final GuideBlock.Table table) {
        this.tables++;
        final String label = this.text.text(GuideTexts.TABLE.key(), this.chapterNumber + "-" + this.tables);
        final List<String> caption = this.captionLines(label, table.captionKey());
        final int row = TextSize.TABLE.lineHeight();
        this.ensure(this.captionHeight(label, caption) + CAPTION_GAP + 2 * row);
        this.caption(label, caption);
        this.y += CAPTION_GAP;
        final int valueX = this.x() + this.width * TABLE_LABEL_SHARE / 100;
        final int valueRoom = this.x() + this.width - valueX - TABLE_PAD;
        this.place(new GuidePiece.Box(this.x(), this.y, this.width, row + 1, GuideStyle.SHADE, GuideStyle.SHADE));
        this.place(new GuidePiece.Text(this.x() + TABLE_PAD, this.y + 2, this.text.text(GuideTexts.PROPERTY.key()),
                TextSize.TABLE, GuideStyle.INK, ""));
        this.place(new GuidePiece.Text(valueX, this.y + 2, this.text.text(GuideTexts.VALUE.key()), TextSize.TABLE,
                GuideStyle.INK, ""));
        this.y += row + 1;
        this.place(new GuidePiece.Rule(this.x(), this.y, this.width, GuideStyle.RULE));
        for (final GuideBlock.TableRow cells : table.rows()) {
            final int rowX = this.x();
            final int rowValueX = rowX + this.width * TABLE_LABEL_SHARE / 100;
            final List<String> labels = wrap(this.text.text(cells.labelKey()), rowValueX - rowX - 2 * TABLE_PAD,
                    TextSize.TABLE, this.text);
            final List<String> values = wrap(this.value(cells.value()), valueRoom, TextSize.TABLE, this.text);
            final int lines = Math.max(labels.size(), values.size());
            if (this.y + lines * row + 2 > this.bottom()) {
                this.nextColumn();
                this.place(new GuidePiece.Rule(this.x(), this.y, this.width, GuideStyle.RULE));
            }
            final int at = this.x();
            final int valueAt = at + this.width * TABLE_LABEL_SHARE / 100;
            for (int i = 0; i < lines; i++) {
                if (i < labels.size()) {
                    this.place(new GuidePiece.Text(at + TABLE_PAD, this.y + 2 + i * row, labels.get(i),
                            TextSize.TABLE, GuideStyle.INK, ""));
                }
                if (i < values.size()) {
                    this.place(new GuidePiece.Text(valueAt, this.y + 2 + i * row, values.get(i), TextSize.TABLE,
                            GuideStyle.INK, ""));
                }
            }
            this.y += lines * row + 2;
            this.place(new GuidePiece.Rule(at, this.y, this.width, GuideStyle.RULE));
        }
        this.y += BLOCK_GAP;
    }

    private String value(final GuideBlock.GuideValue value) {
        return switch (value) {
            case GuideBlock.GuideValue.Words words -> this.text.text(words.key());
            case GuideBlock.GuideValue.Amount amount -> this.text.amount(amount.value(), amount.unit());
            case GuideBlock.GuideValue.Literal literal -> literal.text();
        };
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
            this.lines(this.text.text(problem.problemKey()), 0, this.width, this.sized(TextSize.BOLD),
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
            final String number = this.numbers.getOrDefault(target, GuideIds.path(target));
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

    /** Blocks seen from above as they are placed, under the plan's title. */
    private void plan(final GuideBlock.Plan plan) {
        final TextSize bold = this.sized(TextSize.BOLD);
        this.ensure(bold.lineHeight() + LABEL_GAP + PLAN_HEIGHT);
        this.lines(this.text.text(plan.captionKey()), 0, this.width, bold, GuideStyle.HEADING, "");
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
    private List<String> captionLines(final String label, final String captionKey) {
        final int labelRoom = this.labelRoom(label);
        return wrap(this.text.text(captionKey), this.width - labelRoom, TextSize.SMALL, this.text);
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
        for (final Run run : GuideLinks.runs(sentence, this::numberOf)) {
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
        final List<List<Run>> lines = new ArrayList<>();
        List<Run> line = new ArrayList<>();
        for (final List<Run> next : words) {
            final List<Run> tried = new ArrayList<>(line);
            if (!tried.isEmpty()) {
                // A space between two words of one link belongs to the link; any other to the plain words.
                final String between = tried.getLast().target().equals(next.getFirst().target())
                        ? next.getFirst().target() : "";
                tried.add(new Run(" ", between));
            }
            tried.addAll(next);
            if (line.isEmpty() || this.text.width(plain(tried), size) <= room) {
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

    /** Places lines of runs from the cursor down, each {@code indent} into its column, running on where it must. */
    private void placeRich(final List<List<Run>> lines, final int indent, final TextSize size, final String colour) {
        for (final List<Run> line : lines) {
            this.ensure(size.lineHeight());
            this.placeRun(line, indent, size, colour);
            this.y += size.lineHeight();
        }
    }

    /** One line of runs at the cursor's height, plain words in {@code colour} and links in the link's. */
    private void placeRun(final List<Run> line, final int indent, final TextSize size, final String colour) {
        final StringBuilder before = new StringBuilder();
        for (final Run run : line) {
            final int at = this.x() + indent + this.text.width(before.toString(), size);
            this.place(new GuidePiece.Text(at, this.y, run.text(), size,
                    run.target().isEmpty() ? colour : GuideStyle.LINK, run.target()));
            before.append(run.text());
        }
    }

    /** The number a link's target has in this manual: an entry's or a section's, or a chapter's; null when not held. */
    private String numberOf(final String target) {
        final String number = this.numbers.get(target);
        return number != null ? number : this.chapterNumbers.get(target);
    }

    /** Runs side by side that lead to the same place made one, so a link is one piece to point at. */
    private static List<Run> join(final List<Run> runs) {
        final List<Run> joined = new ArrayList<>();
        for (final Run run : runs) {
            if (!joined.isEmpty() && joined.getLast().target().equals(run.target())) {
                final Run last = joined.removeLast();
                joined.add(new Run(last.text() + run.text(), run.target()));
            } else {
                joined.add(run);
            }
        }
        return joined;
    }

    private static String plain(final List<Run> runs) {
        final StringBuilder out = new StringBuilder();
        for (final Run run : runs) {
            out.append(run.text());
        }
        return out.toString();
    }

    /** Whether a heading leads into what can go wrong, which stands out in the style's accent. */
    private static boolean troubles(final String headingKey) {
        return GuideTexts.WHAT_CAN_GO_WRONG.key().equals(headingKey)
                || GuideTexts.IF_SOMETHING_GOES_WRONG.key().equals(headingKey);
    }

    /** Moves on to the next column when what comes next is taller than the room left in this one. */
    private void ensure(final int height) {
        if (this.y + height > this.bottom() && this.y > this.top) {
            this.nextColumn();
        }
    }

    /** Goes on at the top of the next column, or of the next page after the last column. */
    private void nextColumn() {
        if (this.column + 1 < this.style.pages().columns()) {
            this.column++;
            this.y = this.top;
        } else {
            this.startPage();
        }
    }

    /** Closes the open page, if any, and opens the next: the page it opens will be {@code body.size()}. */
    private void startPage() {
        this.finishPage();
        this.pieces = new ArrayList<>();
        this.y = this.top;
        this.column = 0;
        this.pageInChapter++;
        this.pageLabel = this.sectionLabel;
        this.open = true;
    }

    private void finishPage() {
        if (this.open) {
            this.body.add(new BodyPage(this.pieces, this.chapterNamespace, this.chapterTitle, this.pageLabel,
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
        this.pieces.add(piece);
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
            lines.add(new Leader(chapterNo + "  " + this.text.text(chapter.chapter().titleKey()),
                    chapter.chapter().namespace(), TextSize.BOLD, 0));
            for (final GuideContents.Section section : chapter.sections()) {
                lines.add(new Leader(this.numbers.get(section.section().id()) + "  "
                        + this.text.text(section.section().titleKey()), section.section().id(), TextSize.BODY,
                        SECTION_INDENT));
            }
        }
        lines.add(new Leader(this.text.text(GuideTexts.INDEX.key()), "", TextSize.BOLD, 0));
        return lines;
    }

    /** How many pages that many lines take, the first page carrying a title above them. */
    private int pagesFor(final int lines, final TextSize line, final TextSize title) {
        final int room = bottom(this.style, 0) - this.top;
        final int first = Math.max(1, (room - title.lineHeight() - BLOCK_GAP) / (line.lineHeight() + 2));
        final int rest = Math.max(1, room / (line.lineHeight() + 2));
        return lines <= first ? 1 : 1 + (lines - first + rest - 1) / rest;
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
            final int height = line.size().lineHeight() + 2;
            if (lineY + height > bottom) {
                pages.add(new GuideBook.Page(roman(pages.size() + 1), contentsTitle, "", page));
                page = new ArrayList<>();
                lineY = this.top;
            }
            final String folio = line.target().isEmpty() ? "X-1" : this.folioOf(line.target(), bodyFolios);
            page.add(new GuidePiece.Leader(x + line.indent(), lineY, this.fullWidth - line.indent(), "", line.text(),
                    folio, line.size(), GuideStyle.INK, line.target(), ""));
            lineY += height;
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
            page.add(new GuidePiece.Text(x, lineY, row.number(), TextSize.TABLE, GuideStyle.INK, row.target()));
            page.add(new GuidePiece.Text(titleX, lineY + 1, this.fit(row.title(), sheetsX - titleX - 6,
                    TextSize.BODY), TextSize.BODY, GuideStyle.INK, row.target()));
            page.add(new GuidePiece.Text(sheetsX + 8, lineY, String.valueOf(count), TextSize.TABLE, GuideStyle.INK,
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
            final int height = TextSize.BODY.lineHeight() + 2 + (first.equals(letter) ? 0
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
            final int indent = line.term() ? SECTION_INDENT : 0;
            page.add(new GuidePiece.Leader(x + indent, lineY, this.fullWidth - indent, "", line.text(),
                    line.folio(), line.term() ? TextSize.BODY : TextSize.BOLD, GuideStyle.INK, line.target(),
                    line.icon()));
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

    /** A line of the contents, before its page is known. */
    private record Leader(String text, String target, TextSize size, int indent) {
    }

    /** A line of the index, before its folio is known. */
    private record PendingLine(String text, String target, String icon, boolean term, int page) {
    }

    /** A row of the drawing list: a drawing's number, its title, and the entry it is. */
    private record DrawingRow(String number, String title, String target) {
    }
}
