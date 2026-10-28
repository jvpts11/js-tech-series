/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide.markdown;

import dev.jstech.core.guide.ChecklistLines;
import dev.jstech.core.guide.GuideBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;

/**
 * The blocks of a manual's entry read from the body of its Markdown file, CommonMark with tables, and the title, its
 * first heading of one {@code #}. Each Markdown form is a block of the manual's own: a paragraph, a heading, numbered
 * steps, a table, a picture or an item's figure; a quote is a note, and one that GitHub marks as an alert is a note, a
 * warning, a problem and its fix, a word explained or a "See" line; a fence is a listing of code, or, named for one,
 * a block the manual has ({@link FencedBlocks}), or any block written as its file's JSON. Pure.
 */
public final class MarkdownBlocks {

    private final List<String> lines;
    private final int firstLine;
    private final IJsonBlocks json;
    private final List<GuideBlock> blocks = new ArrayList<>();
    private final List<ManualFileException> problems = new ArrayList<>();
    private @Nullable String title;
    private @Nullable String caption;
    private boolean lastWasProblem;
    private int at;
    /** How many steps the entry's cards have so far. */
    private int cardSteps;

    private static final Pattern HEADING = Pattern.compile("^ {0,3}(#{1,6})\\s+(.*?)\\s*#*\\s*$");
    private static final Pattern ORDERED = Pattern.compile("^ {0,3}\\d+[.)]\\s+(.*)$");
    private static final Pattern BULLET = Pattern.compile("^ {0,3}[-*+]\\s+(.*)$");
    private static final Pattern THEMATIC = Pattern.compile("^ {0,3}([-*_])(\\s*\\1){2,}\\s*$");
    private static final Pattern DELIMITER = Pattern.compile("^\\s*\\|?\\s*:?-+:?\\s*(\\|\\s*:?-+:?\\s*)*\\|?\\s*$");
    private static final Pattern IMAGE = Pattern.compile(
            "^\\s*!\\[([^\\]]*)]\\(\\s*(\\S+?)(?:\\s+\"([^\"]*)\")?\\s*\\)\\s*$");
    private static final Pattern ALERT = Pattern.compile("^\\[!(\\w+)]\\s*(.*)$");
    private static final Pattern SIZE = Pattern.compile("^(\\d+)x(\\d+)$");
    private static final Pattern NUMBER_OF = Pattern.compile("^(\\w+)=(\\d+)$");
    private static final String FENCE = "```";
    private static final String TILDES = "~~~";
    private static final String COMMENT = "<!--";
    private static final String COMMENT_END = "-->";
    private static final String QUOTE = ">";
    private static final String ITEM = "item:";
    private static final String CAPTION = "Table:";
    private static final String BULLET_MARK = "• ";
    private static final String CELLS = "|";
    private static final String FRAMES = "frames";
    private static final String TICKS = "ticks";
    private static final int TITLE_LEVEL = 1;

    private MarkdownBlocks(final String body, final int firstLine, final IJsonBlocks json) {
        this.lines = body.lines().toList();
        this.firstLine = firstLine;
        this.json = json;
    }

    /**
     * The title and the blocks of {@code body}, its first line being line {@code firstLine} of its file, with what it
     * does not say right; {@code json} reads the blocks written as JSON.
     */
    public static Parsed parse(final String body, final int firstLine, final IJsonBlocks json) {
        final MarkdownBlocks reading = new MarkdownBlocks(body, firstLine, json);
        reading.read();
        return new Parsed(Optional.ofNullable(reading.title), List.copyOf(reading.blocks),
                List.copyOf(reading.problems));
    }

    private void read() {
        while (this.at < this.lines.size()) {
            final String line = this.lines.get(this.at);
            if (line.isBlank()) {
                this.at++;
            } else if (fence(line)) {
                this.fenced();
            } else if (line.strip().startsWith(COMMENT)) {
                this.comment();
            } else if (HEADING.matcher(line).matches()) {
                this.heading(line);
            } else if (line.strip().startsWith(QUOTE)) {
                this.quote();
            } else if (THEMATIC.matcher(line).matches()) {
                this.at++;
            } else if (this.tableAt(this.at)) {
                this.table();
            } else if (ORDERED.matcher(line).matches()) {
                this.list(ORDERED, true);
            } else if (BULLET.matcher(line).matches()) {
                this.list(BULLET, false);
            } else if (IMAGE.matcher(line).matches()) {
                this.image(line);
            } else {
                this.paragraph();
            }
        }
    }

    private void add(final GuideBlock block) {
        // A card counts its steps on from the cards before it in the entry, as a run of cuff cards was numbered.
        if (block instanceof GuideBlock.Card card) {
            this.blocks.add(new GuideBlock.Card(card.titleKey(), this.cardSteps + 1, card.steps()));
            this.cardSteps += card.steps().size();
        } else {
            this.blocks.add(block);
        }
        this.lastWasProblem = false;
    }

    /* A list: a checklist when its first item opens with a box, otherwise numbered steps or lines with bullets. */
    private void list(final Pattern marker, final boolean ordered) {
        final List<String> items = this.items(marker);
        if (!items.isEmpty() && ChecklistLines.boxed(items.get(0))) {
            final List<GuideBlock.Check> lines = new ArrayList<>();
            for (final String item : items) {
                final ChecklistLines.Line line = ChecklistLines.check(item);
                lines.add(new GuideBlock.Check(line.first(), line.second(), line.changed()));
            }
            this.add(new GuideBlock.Checklist(lines));
        } else if (ordered) {
            this.add(new GuideBlock.Steps(items));
        } else {
            for (final String item : items) {
                this.add(new GuideBlock.Paragraph(BULLET_MARK + item));
            }
        }
    }

    /* The line of the file the line at {@code index} of the body is. */
    private int lineOf(final int index) {
        return this.firstLine + index;
    }

    private void heading(final String line) {
        final Matcher heading = HEADING.matcher(line);
        heading.matches();
        final String text = MarkdownText.of(heading.group(2));
        if (heading.group(1).length() == TITLE_LEVEL && this.title == null) {
            this.title = text;
        } else {
            this.add(new GuideBlock.Heading(text));
        }
        this.at++;
    }

    private void paragraph() {
        final List<String> taken = new ArrayList<>();
        while (this.at < this.lines.size() && !this.lines.get(this.at).isBlank() && !this.startsBlock(this.at)) {
            taken.add(this.lines.get(this.at));
            this.at++;
        }
        final String text = MarkdownText.of(taken);
        if (taken.size() == 1 && text.startsWith(CAPTION) && this.tableAt(this.nextFilled(this.at))) {
            this.caption = text.substring(CAPTION.length()).strip();
            return;
        }
        this.add(new GuideBlock.Paragraph(text));
    }

    /* Whether the line at {@code index} begins a block of its own, which ends the paragraph before it. */
    private boolean startsBlock(final int index) {
        final String line = this.lines.get(index);
        return fence(line) || line.strip().startsWith(COMMENT) || HEADING.matcher(line).matches()
                || line.strip().startsWith(QUOTE) || THEMATIC.matcher(line).matches() || this.tableAt(index)
                || ORDERED.matcher(line).matches() || BULLET.matcher(line).matches()
                || IMAGE.matcher(line).matches();
    }

    private int nextFilled(final int from) {
        int index = from;
        while (index < this.lines.size() && this.lines.get(index).isBlank()) {
            index++;
        }
        return index;
    }

    /* The items of a list, each with the lines set in under it. */
    private List<String> items(final Pattern marker) {
        final List<String> items = new ArrayList<>();
        List<String> item = new ArrayList<>();
        while (this.at < this.lines.size()) {
            final String line = this.lines.get(this.at);
            final Matcher starts = marker.matcher(line);
            if (starts.matches()) {
                if (!item.isEmpty()) {
                    items.add(MarkdownText.of(item));
                }
                item = new ArrayList<>(List.of(starts.group(1)));
            } else if (!line.isBlank() && Character.isWhitespace(line.charAt(0)) && !item.isEmpty()) {
                item.add(line.strip());
            } else {
                break;
            }
            this.at++;
        }
        if (!item.isEmpty()) {
            items.add(MarkdownText.of(item));
        }
        return items;
    }

    /* A comment is left out, but for the two that start the next column or the next page. */
    private void comment() {
        final StringBuilder said = new StringBuilder();
        while (this.at < this.lines.size()) {
            final String line = this.lines.get(this.at);
            said.append(line).append(' ');
            this.at++;
            if (line.contains(COMMENT_END)) {
                break;
            }
        }
        final String inside = said.toString().replace(COMMENT, "").replace(COMMENT_END, "").strip()
                .toLowerCase(Locale.ROOT);
        if (inside.equals("column")) {
            this.add(new GuideBlock.Break(GuideBlock.BreakKind.COLUMN));
        } else if (inside.equals("page")) {
            this.add(new GuideBlock.Break(GuideBlock.BreakKind.PAGE));
        }
    }

    private void quote() {
        final int start = this.at;
        final List<String> inside = new ArrayList<>();
        while (this.at < this.lines.size() && this.lines.get(this.at).strip().startsWith(QUOTE)) {
            final String line = this.lines.get(this.at).strip().substring(1);
            inside.add(line.startsWith(" ") ? line.substring(1) : line);
            this.at++;
        }
        final Matcher alert = ALERT.matcher(inside.get(0).strip());
        if (!alert.matches()) {
            this.add(new GuideBlock.Note(MarkdownText.of(inside)));
            return;
        }
        final List<String> said = new ArrayList<>();
        if (!alert.group(2).isBlank()) {
            said.add(alert.group(2));
        }
        said.addAll(inside.subList(1, inside.size()).stream().filter(line -> !line.isBlank()).toList());
        final String kind = alert.group(1).toUpperCase(Locale.ROOT);
        switch (kind) {
            case "NOTE", "TIP" -> this.add(new GuideBlock.Note(MarkdownText.of(said)));
            case "WARNING", "IMPORTANT" -> this.add(new GuideBlock.Warning(MarkdownText.of(said)));
            case "CAUTION" -> this.add(new GuideBlock.Caution(MarkdownText.of(said)));
            case "PROBLEM" -> this.troubled(said, start);
            case "DEFINE" -> {
                if (said.size() < 2) {
                    this.problems.add(new ManualFileException(this.lineOf(start), "a word explained is the word on "
                            + "its first line and what it means under it"));
                } else {
                    this.add(new GuideBlock.Define(MarkdownText.of(said.get(0)), MarkdownText.of(said.subList(1,
                            said.size()))));
                }
            }
            case "SEE" -> this.add(new GuideBlock.SeeAlso(FrontMatter.listOf(String.join(",", said)
                    .replace(' ', ','))));
            default -> this.problems.add(new ManualFileException(this.lineOf(start), "an alert is a note, a tip, a "
                    + "warning, a caution, an important line, a problem, a word explained or a See line, not "
                    + kind));
        }
    }

    /* A problem and its fix; problems one after another make one list, as the manual prints them. */
    private void troubled(final List<String> said, final int start) {
        if (said.size() < 2) {
            this.problems.add(new ManualFileException(this.lineOf(start), "a problem is what goes wrong on its first "
                    + "line and how to fix it under it"));
            return;
        }
        final GuideBlock.Problem problem = new GuideBlock.Problem(MarkdownText.of(said.get(0)),
                MarkdownText.of(said.subList(1, said.size())));
        if (this.lastWasProblem && this.blocks.get(this.blocks.size() - 1) instanceof GuideBlock.Problems before) {
            final List<GuideBlock.Problem> all = new ArrayList<>(before.problems());
            all.add(problem);
            this.blocks.set(this.blocks.size() - 1, new GuideBlock.Problems(all));
        } else {
            this.blocks.add(new GuideBlock.Problems(List.of(problem)));
        }
        this.lastWasProblem = true;
    }

    /* An image alone on its line: an item's figure, or a picture the size its title says. */
    private void image(final String line) {
        final Matcher image = IMAGE.matcher(line);
        image.matches();
        final int index = this.at++;
        final String said = MarkdownText.of(image.group(1));
        final String target = image.group(2);
        if (target.startsWith(ITEM)) {
            this.add(new GuideBlock.Figure(target.substring(ITEM.length()), said));
            return;
        }
        int width = -1;
        int height = -1;
        int frames = 1;
        int ticks = 1;
        for (final String token : (image.group(3) == null ? "" : image.group(3)).split("\\s+")) {
            final Matcher size = SIZE.matcher(token);
            final Matcher count = NUMBER_OF.matcher(token);
            if (size.matches()) {
                width = Integer.parseInt(size.group(1));
                height = Integer.parseInt(size.group(2));
            } else if (count.matches() && count.group(1).equals(FRAMES)) {
                frames = Integer.parseInt(count.group(2));
            } else if (count.matches() && count.group(1).equals(TICKS)) {
                ticks = Integer.parseInt(count.group(2));
            }
        }
        if (height < 1) {
            this.problems.add(new ManualFileException(this.lineOf(index), "a picture says how big it is drawn after "
                    + "its image, as \"width x height\" in pixels (a width of 0 takes the column's): "
                    + line.strip()));
            return;
        }
        this.add(new GuideBlock.Picture(target, "", "{}", width, height, said, Math.max(1, frames),
                Math.max(1, ticks)));
    }

    private boolean tableAt(final int index) {
        return index + 1 < this.lines.size() && this.lines.get(index).contains(CELLS)
                && DELIMITER.matcher(this.lines.get(index + 1)).matches() && this.lines.get(index + 1).contains("-");
    }

    private void table() {
        final List<String> heads = cells(this.lines.get(this.at));
        this.at += 2;
        final List<GuideBlock.TableRow> rows = new ArrayList<>();
        while (this.at < this.lines.size() && this.lines.get(this.at).contains(CELLS)) {
            final List<GuideBlock.GuideValue> row = new ArrayList<>();
            for (final String cell : cells(this.lines.get(this.at))) {
                row.add(value(cell));
            }
            while (row.size() < heads.size()) {
                row.add(new GuideBlock.GuideValue.Literal(""));
            }
            rows.add(new GuideBlock.TableRow(row.subList(0, heads.size())));
            this.at++;
        }
        this.add(new GuideBlock.Table(this.caption == null ? "" : this.caption, heads.stream()
                .map(MarkdownText::of).toList(), rows, ""));
        this.caption = null;
    }

    private void fenced() {
        final int start = this.at;
        final String opening = this.lines.get(this.at).strip();
        final String marks = opening.startsWith(FENCE) ? FENCE : TILDES;
        final String info = opening.substring(marks.length()).strip();
        final List<String> body = new ArrayList<>();
        this.at++;
        boolean closed = false;
        while (this.at < this.lines.size()) {
            if (this.lines.get(this.at).strip().startsWith(marks)) {
                closed = true;
                this.at++;
                break;
            }
            body.add(this.lines.get(this.at));
            this.at++;
        }
        if (!closed) {
            this.problems.add(new ManualFileException(this.lineOf(start), "a fence is left open to the end of the "
                    + "file"));
        }
        final String[] words = info.isEmpty() ? new String[0] : info.split("\\s+");
        final String kind = words.length == 0 ? "" : words[0].toLowerCase(Locale.ROOT);
        final String joined = String.join("\n", body).strip();
        if (joined.startsWith("{") && this.json.kinds().contains(kind)) {
            this.json.read(kind, joined).ifPresentOrElse(this::add, () -> this.problems.add(new ManualFileException(
                    this.lineOf(start), "the JSON of a " + kind + " does not read as one")));
            return;
        }
        if (FencedBlocks.KINDS.contains(kind)) {
            FencedBlocks.read(kind, words, body, this.lineOf(start), this.problems).ifPresent(this::add);
        } else {
            this.add(new GuideBlock.Listing(words.length > 1 ? words[1] : "", kind, List.copyOf(body)));
        }
    }

    /* A cell: words as written, or a literal left as it is when written as code. */
    private static GuideBlock.GuideValue value(final String cell) {
        final String bare = cell.strip();
        if (bare.length() > 1 && bare.startsWith("`") && bare.endsWith("`")) {
            return new GuideBlock.GuideValue.Literal(bare.substring(1, bare.length() - 1));
        }
        return new GuideBlock.GuideValue.Words(MarkdownText.of(bare));
    }

    private static List<String> cells(final String row) {
        String inside = row.strip();
        if (inside.startsWith(CELLS)) {
            inside = inside.substring(1);
        }
        if (inside.endsWith(CELLS) && !inside.endsWith("\\" + CELLS)) {
            inside = inside.substring(0, inside.length() - 1);
        }
        final List<String> cells = new ArrayList<>();
        final StringBuilder cell = new StringBuilder();
        for (int i = 0; i < inside.length(); i++) {
            final char c = inside.charAt(i);
            if (c == '\\' && i + 1 < inside.length() && inside.charAt(i + 1) == '|') {
                cell.append('|');
                i++;
            } else if (c == '|') {
                cells.add(cell.toString().strip());
                cell.setLength(0);
            } else {
                cell.append(c);
            }
        }
        cells.add(cell.toString().strip());
        return cells;
    }

    private static boolean fence(final String line) {
        final String bare = line.strip();
        return bare.startsWith(FENCE) || bare.startsWith(TILDES);
    }

    /**
     * What a body of Markdown says.
     *
     * @param title    its first heading of one {@code #}, when it has one
     * @param blocks   its blocks, in order
     * @param problems what it does not say right
     */
    public record Parsed(Optional<String> title, List<GuideBlock> blocks, List<ManualFileException> problems) {
    }
}
