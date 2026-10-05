/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lays a manual out on pages: the contents and the "About this manual" page first, then each chapter with its opening
 * page and its entries, then the index.
 *
 * <p>Everything is numbered as technical manuals number it: chapters from 1, sections within a chapter (3.2), entries
 * within a section (3.2.6), figures and tables within a chapter (Figure 3-9), and pages within a chapter (3-14), the
 * front matter in small roman numerals and the index as X-1, X-2. A style may number every page in one count instead.
 *
 * <p>Every entry starts on a page of its own, so the page an item opens to is its own. A paragraph, a table, a list of
 * steps or recipes runs on to the next page where it does not fit; a figure, a warning and a special block move to the
 * next page whole. A heading is never left alone at the foot of a page.
 *
 * <p>Nothing here knows the game: text is measured and translated through {@link IGuideText}, so the layout is the same
 * piece of logic in the game and in a test.
 */
public final class GuideLayout {

    private final GuideManual manual;
    private final GuideStyle style;
    private final IGuideText text;
    private final int width;
    private final int bottom;
    private final List<BodyPage> body = new ArrayList<>();
    private final Map<String, Integer> bodyTargets = new HashMap<>();
    private final Map<String, String> numbers = new HashMap<>();
    private final List<PendingLine> indexLines = new ArrayList<>();
    private List<GuidePiece> pieces = new ArrayList<>();
    private int y;
    private int chapterNumber;
    private int pageInChapter;
    private int figures;
    private int tables;
    private String chapterNamespace = "";
    private String chapterTitle = "";
    private String sectionLabel = "";
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
    /** How tall a row of recipes is: the slots, and the time and energy over the arrow. */
    public static final int RECIPE_ROW = 26;
    /** The room an item's icon takes beside a line of the contents or the index. */
    public static final int ICON = 18;

    private static final int PARAGRAPH_GAP = 4;
    private static final int BLOCK_GAP = 6;
    private static final int CAPTION_GAP = 3;
    private static final int LABEL_GAP = 4;
    private static final int STEP_INDENT = 12;
    private static final int FIX_INDENT = 8;
    private static final int SECTION_INDENT = 10;
    private static final int BOX_PAD = 4;
    private static final int TABLE_PAD = 3;
    private static final int TABLE_LABEL_SHARE = 45;
    private static final int TITLE_GAP = 2;
    private static final String TAB = GuideStyle.TAB;

    private GuideLayout(final GuideManual manual, final GuideStyle style, final IGuideText text) {
        this.manual = manual;
        this.style = style;
        this.text = text;
        this.width = style.pageWidth() - 2 * style.margin();
        this.bottom = style.pageHeight() - FOOTER_ROOM;
    }

    /** Lays out a manual holding those contents, in that style, measuring and translating through {@code text}. */
    public static GuideBook lay(final GuideManual manual, final GuideStyle style, final GuideContents contents,
                                final IGuideText text) {
        return new GuideLayout(manual, style, text).book(contents);
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
        for (final GuideContents.Chapter chapter : contents.chapters()) {
            this.chapter(chapter);
        }
        this.finishPage();
        final List<Leader> contentsLines = this.contentsLines(contents);
        final int contentsPages = Math.max(1, this.pagesFor(contentsLines.size(), TextSize.BODY, TextSize.TITLE));
        final int front = contentsPages + 1;
        final List<String> bodyFolios = this.bodyFolios(front);
        final List<GuideBook.Page> pages = new ArrayList<>();
        this.layContents(pages, contentsLines, bodyFolios);
        this.layAbout(pages, contentsPages + 1);
        for (int i = 0; i < this.body.size(); i++) {
            final BodyPage page = this.body.get(i);
            final boolean left = (front + i) % 2 == 0;
            pages.add(new GuideBook.Page(bodyFolios.get(i), left ? page.chapterTitle() : page.sectionLabel(),
                    page.chapter(), page.pieces()));
        }
        final Map<String, Integer> targets = new HashMap<>();
        this.bodyTargets.forEach((target, page) -> targets.put(target, page + front));
        final List<GuideBook.IndexLine> index = this.indexLines(bodyFolios);
        final int indexAt = pages.size();
        this.layIndex(pages, index);
        return new GuideBook(pages, targets, this.numbers, index, 0, indexAt);
    }

    /** Numbers every section and entry first, so a link may lead to one printed after it. */
    private void number(final GuideContents contents) {
        int chapterNo = 0;
        for (final GuideContents.Chapter chapter : contents.chapters()) {
            chapterNo++;
            int sectionNo = 0;
            for (final GuideContents.Section section : chapter.sections()) {
                sectionNo++;
                this.numbers.put(section.section().id(), chapterNo + "." + sectionNo);
                int entryNo = 0;
                for (final GuideEntry entry : section.entries()) {
                    entryNo++;
                    this.numbers.put(entry.id(), chapterNo + "." + sectionNo + "." + entryNo);
                }
            }
        }
    }

    private void chapter(final GuideContents.Chapter part) {
        this.finishPage();
        this.chapterNumber++;
        this.pageInChapter = 0;
        this.figures = 0;
        this.tables = 0;
        this.chapterNamespace = part.chapter().namespace();
        this.chapterTitle = this.text.text(part.chapter().titleKey());
        this.sectionLabel = this.chapterTitle;
        this.startPage();
        this.bodyTargets.put(this.chapterNamespace, this.body.size());
        this.place(new GuidePiece.Text(this.x(), this.y, String.valueOf(this.chapterNumber), TextSize.LETTER, TAB, ""));
        this.y += TextSize.LETTER.lineHeight() + TITLE_GAP;
        this.lines(this.chapterTitle, this.x(), this.width, TextSize.TITLE, GuideStyle.HEADING, "");
        this.y += TITLE_GAP;
        this.place(new GuidePiece.Rule(this.x(), this.y, this.width, GuideStyle.RULE));
        this.y += BLOCK_GAP;
        int sectionNumber = 0;
        for (final GuideContents.Section section : part.sections()) {
            sectionNumber++;
            final String number = this.chapterNumber + "." + sectionNumber;
            this.numbers.put(section.section().id(), number);
            final String title = number + "  " + this.text.text(section.section().titleKey());
            this.ensure(TextSize.BODY.lineHeight() + 2);
            final int indent = section.section().icon().isEmpty() ? 0 : ICON;
            if (indent > 0) {
                this.place(new GuidePiece.Item(this.x(), this.y - 4, section.section().icon(), 1));
            }
            this.place(new GuidePiece.Text(this.x() + indent, this.y, title, TextSize.BODY, GuideStyle.LINK,
                    section.section().id()));
            this.y += TextSize.BODY.lineHeight() + 4;
        }
        sectionNumber = 0;
        for (final GuideContents.Section section : part.sections()) {
            sectionNumber++;
            this.section(section, sectionNumber);
        }
    }

    private void section(final GuideContents.Section part, final int sectionNumber) {
        this.sectionLabel = this.numbers.get(part.section().id()) + " "
                + this.text.text(part.section().titleKey());
        int entryNumber = 0;
        for (final GuideEntry entry : part.entries()) {
            entryNumber++;
            this.entry(entry, this.chapterNumber + "." + sectionNumber + "." + entryNumber);
            if (entryNumber == 1) {
                this.bodyTargets.put(part.section().id(), this.bodyTargets.get(entry.id()));
            }
        }
    }

    private void entry(final GuideEntry entry, final String number) {
        this.startPage();
        this.bodyTargets.put(entry.id(), this.body.size());
        this.numbers.put(entry.id(), number);
        final String title = this.text.text(entry.titleKey());
        this.indexLines.add(new PendingLine(title, entry.id(), entry.icon(), false, this.body.size()));
        this.lines(number + "  " + title, this.x(), this.width, TextSize.HEADING, GuideStyle.HEADING, "");
        this.y += TITLE_GAP;
        for (final GuideBlock block : entry.blocks()) {
            this.block(block, entry);
        }
    }

    private void block(final GuideBlock block, final GuideEntry entry) {
        switch (block) {
            case GuideBlock.Paragraph paragraph -> {
                this.lines(this.text.text(paragraph.key()), this.x(), this.width, TextSize.BODY, GuideStyle.INK, "");
                this.y += PARAGRAPH_GAP;
            }
            case GuideBlock.Heading heading -> {
                this.ensure(TextSize.HEADING.lineHeight() + 2 * TextSize.BODY.lineHeight());
                this.y += 2;
                this.lines(this.text.text(heading.key()), this.x(), this.width, TextSize.HEADING, GuideStyle.HEADING,
                        "");
            }
            case GuideBlock.Figure figure -> this.figure(figure);
            case GuideBlock.Table table -> this.table(table);
            case GuideBlock.Recipes recipes -> this.recipes(recipes);
            case GuideBlock.Steps steps -> this.steps(steps);
            case GuideBlock.Warning warning -> this.warning(warning);
            case GuideBlock.Problems problems -> this.problems(problems);
            case GuideBlock.Define define -> this.define(define, entry);
            case GuideBlock.SeeAlso see -> this.seeAlso(see);
            case GuideBlock.Custom custom -> {
                this.ensure(custom.height());
                this.place(new GuidePiece.Custom(this.x(), this.y, this.width, custom.height(), custom.type(),
                        custom.data()));
                this.y += custom.height() + BLOCK_GAP;
            }
        }
    }

    private void figure(final GuideBlock.Figure figure) {
        this.figures++;
        final String label = this.text.text(GuideTexts.FIGURE.key(), this.chapterNumber + "-" + this.figures);
        final List<String> caption = this.captionLines(label, figure.captionKey());
        this.ensure(FIGURE_HEIGHT + CAPTION_GAP + this.captionHeight(label, caption));
        this.place(new GuidePiece.Box(this.x(), this.y, this.width, FIGURE_HEIGHT, GuideStyle.SHADE, GuideStyle.RULE));
        this.place(new GuidePiece.Item(this.x() + this.width / 2 - 16, this.y + (FIGURE_HEIGHT - 32) / 2,
                figure.item(), 2));
        this.y += FIGURE_HEIGHT + CAPTION_GAP;
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
            final List<String> labels = wrap(this.text.text(cells.labelKey()), valueX - this.x() - 2 * TABLE_PAD,
                    TextSize.TABLE, this.text);
            final List<String> values = wrap(this.value(cells.value()), valueRoom, TextSize.TABLE, this.text);
            final int lines = Math.max(labels.size(), values.size());
            if (this.y + lines * row + 2 > this.bottom) {
                this.newPage();
                this.place(new GuidePiece.Rule(this.x(), this.y, this.width, GuideStyle.RULE));
            }
            for (int i = 0; i < lines; i++) {
                if (i < labels.size()) {
                    this.place(new GuidePiece.Text(this.x() + TABLE_PAD, this.y + 2 + i * row, labels.get(i),
                            TextSize.TABLE, GuideStyle.INK, ""));
                }
                if (i < values.size()) {
                    this.place(new GuidePiece.Text(valueX, this.y + 2 + i * row, values.get(i), TextSize.TABLE,
                            GuideStyle.INK, ""));
                }
            }
            this.y += lines * row + 2;
            this.place(new GuidePiece.Rule(this.x(), this.y, this.width, GuideStyle.RULE));
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
            this.lines(this.text.text(GuideTexts.NO_RECIPES.key()), this.x(), this.width, TextSize.BODY,
                    GuideStyle.FAINT, "");
            this.y += PARAGRAPH_GAP;
            return;
        }
        int done = 0;
        while (done < count) {
            this.ensure(RECIPE_ROW);
            final int fit = Math.max(1, Math.min(count - done, (this.bottom - this.y) / RECIPE_ROW));
            this.place(new GuidePiece.Recipes(this.x(), this.y, this.width, recipes.type(), recipes.output(), done,
                    fit));
            this.y += fit * RECIPE_ROW;
            done += fit;
        }
        this.y += BLOCK_GAP;
    }

    private void steps(final GuideBlock.Steps steps) {
        // Every step's words start where the widest number ends, so steps 9 and 10 line up.
        final int indent = Math.max(STEP_INDENT, this.text.width(steps.keys().size() + ".", TextSize.BOLD) + 4);
        int number = 0;
        for (final String key : steps.keys()) {
            number++;
            final List<String> lines = wrap(this.text.text(key), this.width - indent, TextSize.BODY, this.text);
            this.ensure(TextSize.BODY.lineHeight());
            this.place(new GuidePiece.Text(this.x(), this.y, number + ".", TextSize.BOLD, GuideStyle.INK, ""));
            this.placeLines(lines, this.x() + indent, TextSize.BODY, GuideStyle.INK, "");
            this.y += 2;
        }
        this.y += PARAGRAPH_GAP;
    }

    private void warning(final GuideBlock.Warning warning) {
        final List<String> lines = wrap(this.text.text(warning.key()), this.width - 2 * BOX_PAD, TextSize.BODY,
                this.text);
        final int height = 2 * BOX_PAD + TextSize.BOLD.lineHeight() + lines.size() * TextSize.BODY.lineHeight();
        if (height > this.bottom - BODY_TOP) {
            this.lines(this.text.text(warning.key()), this.x(), this.width, TextSize.BODY, GuideStyle.WARNING, "");
            this.y += PARAGRAPH_GAP;
            return;
        }
        this.ensure(height);
        this.place(new GuidePiece.Box(this.x(), this.y, this.width, height, GuideStyle.HIGHLIGHT, GuideStyle.WARNING));
        this.place(new GuidePiece.Text(this.x() + BOX_PAD, this.y + BOX_PAD, this.text.text(GuideTexts.WARNING.key()),
                TextSize.BOLD, GuideStyle.WARNING, ""));
        int lineY = this.y + BOX_PAD + TextSize.BOLD.lineHeight();
        for (final String line : lines) {
            this.place(new GuidePiece.Text(this.x() + BOX_PAD, lineY, line, TextSize.BODY, GuideStyle.INK, ""));
            lineY += TextSize.BODY.lineHeight();
        }
        this.y += height + BLOCK_GAP;
    }

    private void problems(final GuideBlock.Problems problems) {
        for (final GuideBlock.Problem problem : problems.problems()) {
            this.ensure(2 * TextSize.BODY.lineHeight());
            this.lines(this.text.text(problem.problemKey()), this.x(), this.width, TextSize.BOLD, GuideStyle.INK, "");
            this.lines(this.text.text(problem.fixKey()), this.x() + FIX_INDENT, this.width - FIX_INDENT,
                    TextSize.BODY, GuideStyle.INK, "");
            this.y += PARAGRAPH_GAP;
        }
    }

    private void define(final GuideBlock.Define define, final GuideEntry entry) {
        final String term = this.text.text(define.termKey());
        this.ensure(2 * TextSize.BODY.lineHeight());
        this.indexLines.add(new PendingLine(term, entry.id(), "", true, this.body.size()));
        this.lines(term, this.x(), this.width, TextSize.BOLD, GuideStyle.HEADING, "");
        this.lines(this.text.text(define.definitionKey()), this.x() + FIX_INDENT, this.width - FIX_INDENT,
                TextSize.BODY, GuideStyle.INK, "");
        this.y += PARAGRAPH_GAP;
    }

    private void seeAlso(final GuideBlock.SeeAlso see) {
        this.ensure(TextSize.BODY.lineHeight());
        int at = this.x();
        final String lead = this.text.text(GuideTexts.SEE.key()) + " ";
        this.place(new GuidePiece.Text(at, this.y, lead, TextSize.BODY, GuideStyle.INK, ""));
        at += this.text.width(lead, TextSize.BODY);
        for (int i = 0; i < see.entries().size(); i++) {
            final String target = see.entries().get(i);
            final String number = this.numbers.getOrDefault(target, GuideIds.path(target));
            final String joint = i == 0 ? "" : i == see.entries().size() - 1
                    ? " " + this.text.text(GuideTexts.AND.key()) + " " : ", ";
            final int needed = this.text.width(joint + number, TextSize.BODY);
            if (at + needed > this.x() + this.width) {
                this.y += TextSize.BODY.lineHeight();
                this.ensure(TextSize.BODY.lineHeight());
                at = this.x();
            }
            if (!joint.isEmpty()) {
                this.place(new GuidePiece.Text(at, this.y, joint, TextSize.BODY, GuideStyle.INK, ""));
                at += this.text.width(joint, TextSize.BODY);
            }
            this.place(new GuidePiece.Text(at, this.y, number, TextSize.BODY, GuideStyle.LINK, target));
            at += this.text.width(number, TextSize.BODY);
        }
        this.y += TextSize.BODY.lineHeight() + PARAGRAPH_GAP;
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

    /** Wraps text and places its lines from the cursor down, running on to the next page where it must. */
    private void lines(final String words, final int x, final int room, final TextSize size, final String colour,
                       final String link) {
        this.placeLines(wrap(words, room, size, this.text), x, size, colour, link);
    }

    private void placeLines(final List<String> lines, final int x, final TextSize size, final String colour,
                            final String link) {
        for (final String line : lines) {
            this.ensure(size.lineHeight());
            this.place(new GuidePiece.Text(x, this.y, line, size, colour, link));
            this.y += size.lineHeight();
        }
    }

    /** Starts a new page when what comes next is taller than the room left on this one. */
    private void ensure(final int height) {
        if (this.y + height > this.bottom && this.y > BODY_TOP) {
            this.newPage();
        }
    }

    private void newPage() {
        this.startPage();
    }

    /** Closes the open page, if any, and opens the next: the page it opens will be {@code body.size()}. */
    private void startPage() {
        this.finishPage();
        this.pieces = new ArrayList<>();
        this.y = BODY_TOP;
        this.pageInChapter++;
        this.open = true;
    }

    private void finishPage() {
        if (this.open) {
            this.body.add(new BodyPage(this.pieces, this.chapterNamespace, this.chapterTitle, this.sectionLabel,
                    this.chapterNumber, this.pageInChapter));
            this.open = false;
        }
    }

    private void place(final GuidePiece piece) {
        this.pieces.add(piece);
    }

    private int x() {
        return this.style.margin();
    }

    /** The folio of every page of the chapters, the front matter being {@code front} pages long. */
    private List<String> bodyFolios(final int front) {
        final List<String> folios = new ArrayList<>();
        for (int i = 0; i < this.body.size(); i++) {
            final BodyPage page = this.body.get(i);
            folios.add(this.style.folios() == GuideStyle.Folios.CHAPTER_PAGE
                    ? page.chapterNumber() + "-" + page.pageInChapter() : String.valueOf(front + i + 1));
        }
        return folios;
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
        final int room = this.bottom - BODY_TOP;
        final int first = Math.max(1, (room - title.lineHeight() - BLOCK_GAP) / (line.lineHeight() + 2));
        final int rest = Math.max(1, room / (line.lineHeight() + 2));
        return lines <= first ? 1 : 1 + (lines - first + rest - 1) / rest;
    }

    private void layContents(final List<GuideBook.Page> pages, final List<Leader> lines,
                             final List<String> bodyFolios) {
        final String contentsTitle = this.text.text(GuideTexts.CONTENTS.key());
        List<GuidePiece> page = new ArrayList<>();
        int lineY = BODY_TOP;
        page.add(new GuidePiece.Text(this.x(), lineY, contentsTitle, TextSize.TITLE, GuideStyle.HEADING, ""));
        lineY += TextSize.TITLE.lineHeight() + BLOCK_GAP;
        for (final Leader line : lines) {
            final int height = line.size().lineHeight() + 2;
            if (lineY + height > this.bottom) {
                pages.add(new GuideBook.Page(roman(pages.size() + 1), contentsTitle, "", page));
                page = new ArrayList<>();
                lineY = BODY_TOP;
            }
            final String folio = line.target().isEmpty() ? "X-1" : this.folioOf(line.target(), bodyFolios);
            page.add(new GuidePiece.Leader(this.x() + line.indent(), lineY, this.width - line.indent(), line.text(),
                    folio, line.size(), GuideStyle.INK, line.target().isEmpty() ? "" : line.target(), ""));
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
        final List<GuidePiece> page = new ArrayList<>();
        int lineY = BODY_TOP;
        for (final String line : wrap(about, this.width, TextSize.HEADING, this.text)) {
            page.add(new GuidePiece.Text(this.x(), lineY, line, TextSize.HEADING, GuideStyle.HEADING, ""));
            lineY += TextSize.HEADING.lineHeight();
        }
        lineY += TITLE_GAP;
        for (final String key : this.manual.aboutKeys()) {
            for (final String line : wrap(this.text.text(key), this.width, TextSize.BODY, this.text)) {
                if (lineY + TextSize.BODY.lineHeight() > this.bottom) {
                    break;
                }
                page.add(new GuidePiece.Text(this.x(), lineY, line, TextSize.BODY, GuideStyle.INK, ""));
                lineY += TextSize.BODY.lineHeight();
            }
            lineY += PARAGRAPH_GAP;
        }
        pages.add(new GuideBook.Page(roman(number), about, "", page));
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
        int number = 1;
        List<GuidePiece> page = new ArrayList<>();
        int lineY = BODY_TOP;
        String letter = "";
        for (final GuideBook.IndexLine line : lines) {
            final String first = line.text().isEmpty() ? "" : line.text().substring(0, 1).toUpperCase(Locale.ROOT);
            final int height = TextSize.BODY.lineHeight() + 2 + (first.equals(letter) ? 0
                    : TextSize.LETTER.lineHeight());
            if (lineY + height > this.bottom) {
                pages.add(new GuideBook.Page("X-" + number++, header, "", page));
                page = new ArrayList<>();
                lineY = BODY_TOP;
            }
            if (!first.equals(letter)) {
                letter = first;
                page.add(new GuidePiece.Text(this.x(), lineY, letter, TextSize.LETTER, GuideStyle.HEADING, ""));
                lineY += TextSize.LETTER.lineHeight();
            }
            final int indent = line.term() ? SECTION_INDENT : 0;
            page.add(new GuidePiece.Leader(this.x() + indent, lineY, this.width - indent, line.text(), line.folio(),
                    line.term() ? TextSize.BODY : TextSize.BOLD, GuideStyle.INK, line.target(), line.icon()));
            lineY += TextSize.BODY.lineHeight() + 2;
        }
        pages.add(new GuideBook.Page("X-" + number, header, "", page));
    }

    /** A page of a chapter as it was laid, before the front matter's length fixes its place and its head. */
    private record BodyPage(List<GuidePiece> pieces, String chapter, String chapterTitle, String sectionLabel,
                            int chapterNumber, int pageInChapter) {
    }

    /** A line of the contents, before its page is known. */
    private record Leader(String text, String target, TextSize size, int indent) {
    }

    /** A line of the index, before its folio is known. */
    private record PendingLine(String text, String target, String icon, boolean term, int page) {
    }
}
