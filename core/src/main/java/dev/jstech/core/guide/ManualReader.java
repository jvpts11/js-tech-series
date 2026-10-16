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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A manual read as text rather than as pages: what a help program on a computer, or a viewer at a terminal, shows of
 * it.
 *
 * <p>The chapters, sections and entries are numbered as the printed manual numbers them, so "3.2.6" is the same entry
 * in the binder and in a help window. Each entry becomes an article of headings, paragraphs, numbered steps, words
 * explained, tables and links, every sentence already in the reader's language, and every figure and table numbered
 * by its chapter as on paper. What only pages can hold (a break to the next column, a block another mod draws) is
 * left out, and a block seen from three sides becomes its picture and the legend of its balloons.
 *
 * <p>Pure: the words come through {@link IGuideText}, so a test reads a manual with no game at all.
 */
public final class ManualReader {

    private final GuideManual manual;
    private final IGuideText text;
    private final Map<String, Node> nodes = new LinkedHashMap<>();
    private final Map<String, List<String>> children = new HashMap<>();
    private final Map<String, String> parents = new HashMap<>();
    private final List<String> chapters = new ArrayList<>();
    private final List<String> entries = new ArrayList<>();
    private final Map<String, Article> articles = new HashMap<>();
    private final Map<String, String> searchable = new HashMap<>();
    private final List<IndexLine> index = new ArrayList<>();
    /** The entry that prints each command's page, by the command's name. */
    private final Map<String, String> commandEntries;
    /** The entry that prints each command's page, by the source the page is read from. */
    private final Map<String, String> commandSources = new HashMap<>();

    /** The characters a typed name may spell a gap with, all read alike: "graphics-cards" is "graphics cards". */
    private static final String GAPS = "[-_\\s]+";
    /** How many of the items an entry shows its line names before saying how many more there are. */
    private static final int NAMED_ITEMS = 8;

    public ManualReader(final GuideManual manual, final GuideContents contents, final IGuideText text) {
        this.manual = Objects.requireNonNull(manual, "manual");
        this.text = Objects.requireNonNull(text, "text");
        this.commandEntries = GuideCommandPage.entries(contents, text);
        this.number(contents);
        for (final GuideContents.Chapter chapter : contents.chapters()) {
            Counted counted = new Counted(0, 0, 0);
            for (final GuideContents.Section section : chapter.sections()) {
                for (final GuideEntry entry : section.entries()) {
                    counted = this.read(entry, this.nodes.get(chapter.chapter().id()).number(), counted);
                }
            }
        }
        this.index.sort(Comparator.comparing((IndexLine line) -> line.text().toLowerCase(Locale.ROOT))
                .thenComparing(IndexLine::target));
    }

    /** The manual's id. */
    public String manualId() {
        return this.manual.id();
    }

    /** The manual's title, in the reader's language. */
    public String title() {
        return this.text.text(this.manual.titleKey());
    }

    /** Every chapter, section and entry, in print order, each after the one it belongs to. */
    public List<Node> tree() {
        return List.copyOf(this.nodes.values());
    }

    /** The chapters' ids, in print order. */
    public List<String> chapters() {
        return List.copyOf(this.chapters);
    }

    /** The chapter, section or entry of that id, when this manual holds it. */
    public Optional<Node> node(final String id) {
        return Optional.ofNullable(this.nodes.get(id));
    }

    /** What a chapter or a section holds, in print order; nothing for an entry. */
    public List<Node> children(final String id) {
        final List<Node> out = new ArrayList<>();
        for (final String child : this.children.getOrDefault(id, List.of())) {
            out.add(this.nodes.get(child));
        }
        return out;
    }

    /** What a section or an entry belongs to: an entry's section, a section's chapter; nothing for a chapter. */
    public Optional<String> parent(final String id) {
        return Optional.ofNullable(this.parents.get(id));
    }

    /** The entries, in print order. */
    public List<String> entries() {
        return List.copyOf(this.entries);
    }

    /** An entry as text, when this manual holds it. */
    public Optional<Article> article(final String entry) {
        return Optional.ofNullable(this.articles.get(entry));
    }

    /** The entry printed after this one, across sections and chapters. */
    public Optional<String> next(final String entry) {
        final int at = this.entries.indexOf(entry);
        return at < 0 || at + 1 >= this.entries.size() ? Optional.empty() : Optional.of(this.entries.get(at + 1));
    }

    /** The entry printed before this one. */
    public Optional<String> previous(final String entry) {
        final int at = this.entries.indexOf(entry);
        return at <= 0 ? Optional.empty() : Optional.of(this.entries.get(at - 1));
    }

    /** The index: every entry's title and every word an entry explains, alphabetically, each with where it is. */
    public List<IndexLine> index() {
        return List.copyOf(this.index);
    }

    /**
     * The entries that answer to what was typed: every word of it found in the entry's title or in its text, without
     * regard to capitals. Nothing typed answers nothing.
     */
    public List<String> search(final String typed) {
        final List<String> words = new ArrayList<>();
        for (final String word : typed.toLowerCase(Locale.ROOT).split("\\s+")) {
            if (!word.isEmpty()) {
                words.add(word);
            }
        }
        final List<String> found = new ArrayList<>();
        if (words.isEmpty()) {
            return found;
        }
        for (final String entry : this.entries) {
            final String haystack = this.searchable.getOrDefault(entry, "");
            if (words.stream().allMatch(haystack::contains)) {
                found.add(entry);
            }
        }
        return found;
    }

    /**
     * The chapter, section or entry a reader named: by its id, by the last part of its id, by its number or by its
     * title, capitals and the way gaps are written not counting. What is typed at a terminal ("man graphics-cards",
     * "info 'Graphics cards'") comes here.
     */
    public Optional<String> find(final String asked) {
        final String wanted = loose(asked);
        if (wanted.isEmpty()) {
            return Optional.empty();
        }
        for (final Node node : this.nodes.values()) {
            if (node.id().equalsIgnoreCase(asked.strip()) || loose(GuideIds.path(node.id())).equals(wanted)
                    || node.number().equals(asked.strip()) || loose(node.title()).equals(wanted)) {
                return Optional.of(node.id());
            }
        }
        return Optional.empty();
    }

    /**
     * The entry that prints the page of a command read from that source, when this manual holds one: the page a
     * system's help shows for the command, in place of the one made of the command's words alone.
     */
    public Optional<String> commandPage(final String source) {
        return Optional.ofNullable(this.commandSources.get(source));
    }

    /** The number and title of a chapter, section or entry as a link names it: "3.4.2 Monitors". */
    public String label(final String id) {
        final Node node = this.nodes.get(id);
        if (node != null) {
            return node.number() + " " + node.title();
        }
        // A page another book holds is named by its title there.
        final String title = this.text.titleOf(id);
        return title.isEmpty() ? GuideIds.path(id) : title;
    }

    /** A name the way two spellings of it compare: lower case, every run of gaps one underscore. */
    private static String loose(final String name) {
        return name.strip().toLowerCase(Locale.ROOT).replaceAll(GAPS, "_");
    }

    /** Numbers the chapters, sections and entries first, so a link may name one printed after it. */
    private void number(final GuideContents contents) {
        int chapterNo = 0;
        for (final GuideContents.Chapter chapter : contents.chapters()) {
            chapterNo++;
            final String chapterId = chapter.chapter().id();
            this.put(new Node(chapterId, Kind.CHAPTER, String.valueOf(chapterNo),
                    this.text.text(chapter.chapter().titleKey()), 0, ""), "");
            this.chapters.add(chapterId);
            int sectionNo = 0;
            for (final GuideContents.Section section : chapter.sections()) {
                sectionNo++;
                final String sectionId = section.section().id();
                this.put(new Node(sectionId, Kind.SECTION, chapterNo + "." + sectionNo,
                        this.text.text(section.section().titleKey()), 1, section.section().icon()), chapterId);
                int entryNo = 0;
                for (final GuideEntry entry : section.entries()) {
                    entryNo++;
                    this.put(new Node(entry.id(), Kind.ENTRY, chapterNo + "." + sectionNo + "." + entryNo,
                            this.text.text(entry.titleKey()), 2, entry.icon()), sectionId);
                    this.entries.add(entry.id());
                }
            }
        }
    }

    private void put(final Node node, final String parent) {
        this.nodes.put(node.id(), node);
        if (!parent.isEmpty()) {
            this.parents.put(node.id(), parent);
            this.children.computeIfAbsent(parent, key -> new ArrayList<>()).add(node.id());
        }
    }

    /** Reads one entry into its article, numbering its figures and tables after those before it in its chapter. */
    private Counted read(final GuideEntry entry, final String chapterNo, final Counted before) {
        int figures = before.figures();
        int tables = before.tables();
        int listings = before.listings();
        final Node node = this.nodes.get(entry.id());
        final List<Piece> pieces = new ArrayList<>();
        final StringBuilder words = new StringBuilder(node.title()).append('\n');
        // What its sentences link to in their own words, listed after it, since running text has no links in it.
        final Set<String> linked = new LinkedHashSet<>();
        this.index.add(new IndexLine(node.title(), entry.id(), false));
        if (!entry.shown().isEmpty()) {
            final List<Part> parts = new ArrayList<>();
            final List<String> names = new ArrayList<>();
            for (final String item : entry.shown()) {
                final String name = this.text.itemName(item);
                parts.add(new Part(item, name));
                if (names.size() < NAMED_ITEMS) {
                    names.add(name);
                }
            }
            // A family of a hundred parts is named by its first few, so the line stays a line.
            final int more = parts.size() - names.size();
            final String line = more == 0 ? this.text.text(GuideTexts.ITEMS_LINE.key(), String.join(", ", names))
                    : this.text.text(GuideTexts.ITEMS_MORE.key(), String.join(", ", names), more);
            pieces.add(new Piece.Parts(line, parts));
        }
        for (final GuideBlock block : entry.blocks()) {
            switch (block) {
                case GuideBlock.Paragraph paragraph -> pieces.add(new Piece.Paragraph(
                        this.linked(paragraph.key(), linked), Tone.PLAIN));
                case GuideBlock.Heading heading -> pieces.add(new Piece.Heading(this.text.text(heading.key()),
                        heading.leadsIntoTrouble()));
                case GuideBlock.Figure figure -> {
                    figures++;
                    pieces.add(new Piece.Picture(figure.item(), this.text.text(GuideTexts.FIGURE.key(),
                            chapterNo + "-" + figures) + " " + this.text.text(figure.captionKey())));
                }
                case GuideBlock.Picture picture -> {
                    figures++;
                    pieces.add(new Piece.Picture("", this.text.text(GuideTexts.FIGURE.key(),
                            chapterNo + "-" + figures) + " " + this.text.text(picture.captionKey())));
                }
                case GuideBlock.Table declared -> {
                    tables++;
                    final GuideBlock.Table table = declared.source().isEmpty() ? declared
                            : declared.withRows(this.text.rows(declared.source(), declared.columns()));
                    pieces.add(this.table(this.text.text(GuideTexts.TABLE.key(), chapterNo + "-" + tables) + " "
                            + this.text.text(table.captionKey()), table));
                }
                case GuideBlock.Listing listing -> {
                    listings++;
                    final String label = this.text.text(GuideTexts.LISTING.key(), chapterNo + "-" + listings);
                    pieces.add(new Piece.Code(listing.name().isEmpty() ? label : label + " " + listing.name(),
                            listing.language(), listing.lines()));
                }
                // Read as text, a session is the lines its terminal printed, under the terminal's name.
                case GuideBlock.Session session -> pieces.add(new Piece.Code(session.title(), "", session.printed()));
                case GuideBlock.Recipes recipes -> pieces.add(new Piece.Recipes(recipes.type(), recipes.output()));
                case GuideBlock.Steps steps -> {
                    int number = 0;
                    for (final String key : steps.keys()) {
                        number++;
                        pieces.add(new Piece.Item(number + ".", this.linked(key, linked)));
                    }
                }
                case GuideBlock.Warning warning -> pieces.add(new Piece.Paragraph(this.text.text(
                        GuideTexts.WARNING_LINE.key(), this.linked(warning.key(), linked)), Tone.WARNING));
                case GuideBlock.Problems problems -> {
                    for (final GuideBlock.Problem problem : problems.problems()) {
                        pieces.add(new Piece.Term(this.text.text(problem.problemKey()),
                                this.linked(problem.fixKey(), linked)));
                    }
                }
                case GuideBlock.Define define -> {
                    final String term = this.text.text(define.termKey());
                    pieces.add(new Piece.Term(term, this.linked(define.definitionKey(), linked)));
                    this.index.add(new IndexLine(term, entry.id(), true));
                }
                case GuideBlock.SeeAlso see -> {
                    final List<Link> links = new ArrayList<>();
                    for (final String target : see.entries()) {
                        links.add(new Link(target, this.label(target)));
                    }
                    pieces.add(new Piece.Links(this.text.text(GuideTexts.SEE_ALSO.key()), links));
                }
                case GuideBlock.Note note -> pieces.add(new Piece.Paragraph(this.linked(note.key(), linked),
                        Tone.NOTE));
                case GuideBlock.Views views -> {
                    pieces.add(new Piece.Picture(views.item(), ""));
                    for (final GuideBlock.Callout callout : views.callouts()) {
                        pieces.add(new Piece.Item(String.valueOf(callout.number()), this.text.text(callout.key())));
                    }
                }
                case GuideBlock.Plan plan -> {
                    pieces.add(new Piece.Paragraph(this.text.text(plan.captionKey()), Tone.PLAIN));
                    for (final GuideBlock.PlanPart part : plan.parts()) {
                        final String label = this.text.text(part.labelKey());
                        pieces.add(new Piece.Item("-", part.optional()
                                ? this.text.text(GuideTexts.OPTIONAL_PART.key(), label) : label));
                    }
                }
                case GuideBlock.Command command -> {
                    this.commandSources.putIfAbsent(command.source(), entry.id());
                    this.command(command, pieces, linked);
                }
                // Read as text, a scene is its caption, numbered with the figures, and the list of its balloons.
                case GuideBlock.Scene scene -> {
                    figures++;
                    pieces.add(new Piece.Picture("", this.text.text(GuideTexts.FIGURE.key(), chapterNo + "-"
                            + figures) + " " + this.text.text(scene.captionKey())));
                    scene.balloons().stream().sorted(Comparator.comparingInt(GuideBlock.SceneBalloon::number))
                            .forEach(balloon -> pieces.add(new Piece.Item(String.valueOf(balloon.number()),
                                    balloon.detailKey().isEmpty() ? this.text.text(balloon.key())
                                            : this.text.text(balloon.key()) + ". " + this.text.text(
                                            balloon.detailKey()))));
                }
                // Read as text, a multiblock is its caption, numbered with the figures, where it is installed.
                case GuideBlock.Multiblock multiblock -> {
                    if (!this.text.multiblock(multiblock.multiblock()).isEmpty()) {
                        figures++;
                        pieces.add(new Piece.Picture("", this.text.text(GuideTexts.FIGURE.key(), chapterNo + "-"
                                + figures) + " " + this.text.text(multiblock.captionKey())));
                    }
                }
                // Where a page breaks, and what another mod draws on one, have no place in running text.
                case GuideBlock.Break cut -> {
                }
                case GuideBlock.Custom custom -> {
                }
            }
        }
        for (final GuideBlock block : entry.blocks()) {
            if (block instanceof GuideBlock.SeeAlso see) {
                see.entries().forEach(linked::remove);
            }
        }
        linked.remove(entry.id());
        if (!linked.isEmpty()) {
            final List<Link> links = new ArrayList<>();
            for (final String target : linked) {
                links.add(new Link(target, this.label(target)));
            }
            pieces.add(new Piece.Links(this.text.text(GuideTexts.SEE_ALSO.key()), links));
        }
        for (final Piece piece : pieces) {
            words.append(piece.words()).append('\n');
        }
        this.articles.put(entry.id(), new Article(entry.id(), node.number(), node.title(), entry.icon(),
                this.parents.getOrDefault(entry.id(), ""), pieces));
        this.searchable.put(entry.id(), words.toString().toLowerCase(Locale.ROOT));
        return new Counted(figures, tables, listings);
    }

    /**
     * A command's page as text ({@link GuideCommandPage}): what it is for, how it is typed as a line of code, what it
     * does, its options and examples as tables, the systems it runs on, and the commands to read next, the pages of
     * those this manual holds listed with the entry's other links.
     */
    private void command(final GuideBlock.Command block, final List<Piece> pieces, final Set<String> linked) {
        final Optional<GuideCommand> found = this.text.command(block.source());
        if (found.isEmpty()) {
            pieces.add(new Piece.Paragraph(this.text.text(GuideTexts.NO_COMMAND.key(), block.source()),
                    Tone.WARNING));
            return;
        }
        final GuideCommand command = found.get();
        this.paragraph(pieces, GuideCommandPage.lead(command, this.text), linked);
        pieces.add(new Piece.Heading(this.text.text(GuideTexts.HOW_TO_TYPE_IT.key()), false));
        pieces.add(new Piece.Code("", "", List.of(GuideCommandPage.synopsis(command, this.text))));
        for (final String paragraph : GuideCommandPage.description(command, this.text)) {
            this.paragraph(pieces, paragraph, linked);
        }
        if (!command.options().isEmpty()) {
            pieces.add(new Piece.Heading(this.text.text(GuideTexts.OPTIONS.key()), false));
            pieces.add(this.table("", GuideCommandPage.options(command)));
        }
        if (!command.examples().isEmpty()) {
            pieces.add(new Piece.Heading(this.text.text(GuideTexts.EXAMPLES.key()), false));
            pieces.add(this.table("", GuideCommandPage.examples(command)));
        }
        this.paragraph(pieces, GuideCommandPage.runsOn(command, this.text), linked);
        this.paragraph(pieces, GuideCommandPage.readNext(command, this.text, name -> name), linked);
        for (final String name : command.seeAlso()) {
            final String entry = this.commandEntries.get(name);
            if (entry != null && this.nodes.containsKey(entry)) {
                linked.add(entry);
            }
        }
    }

    /** A sentence as a paragraph, its links written out; nothing for no words. */
    private void paragraph(final List<Piece> pieces, final String sentence, final Set<String> linked) {
        if (!sentence.isEmpty()) {
            pieces.add(new Piece.Paragraph(this.written(sentence, linked), Tone.PLAIN));
        }
    }

    /** A table as text under that caption, its heads and cells in the reader's language. */
    private Piece.Table table(final String caption, final GuideBlock.Table table) {
        final List<String> heads = new ArrayList<>();
        for (final String head : table.headKeys()) {
            heads.add(this.text.text(head));
        }
        final List<Row> rows = new ArrayList<>();
        for (final GuideBlock.TableRow row : table.rows()) {
            final List<String> cells = new ArrayList<>();
            for (final GuideBlock.GuideValue cell : row.cells()) {
                // A link in a cell is written as a link in a sentence is: its words and the number it leads to.
                cells.add(cell instanceof GuideBlock.GuideValue.Words
                        ? this.written(this.text.valueText(cell), new HashSet<>()) : this.text.valueText(cell));
            }
            rows.add(new Row(cells));
        }
        return new Piece.Table(caption, heads, rows);
    }

    /**
     * A sentence in the reader's language with its links written as their words and numbers, the entries they lead
     * to that this manual holds noted in {@code linked}.
     */
    private String linked(final String key, final Set<String> linked) {
        return this.written(this.text.text(key), linked);
    }

    /** A sentence already in the reader's language, with its links written as {@link #linked} writes them. */
    private String written(final String sentence, final Set<String> linked) {
        for (final String target : GuideLinks.targets(sentence)) {
            if (this.nodes.containsKey(target)) {
                linked.add(target);
            }
        }
        return GuideLinks.plain(sentence, target -> {
            final Node node = this.nodes.get(target);
            return node == null ? null : node.number();
        }, target -> {
            final String book = this.text.elsewhere(target);
            return book.isEmpty() ? null : this.text.text(GuideTexts.IN_BOOK.key(), book);
        }, target -> this.text.titleOf(target).isEmpty() ? null : this.text.titleOf(target));
    }

    /** What a node of the manual is. */
    public enum Kind {
        CHAPTER, SECTION, ENTRY
    }

    /** How a paragraph reads: as the text runs, as a note set apart, or as a warning. */
    public enum Tone {
        PLAIN, NOTE, WARNING
    }

    /**
     * A chapter, section or entry of the manual.
     *
     * @param id     the chapter's namespace, or the section's or entry's id
     * @param kind   which of the three it is
     * @param number its number in the manual: "3", "3.2", "3.2.6"
     * @param title  its title, in the reader's language
     * @param depth  how deep it sits: 0 for a chapter, 1 for a section, 2 for an entry
     * @param icon   the item drawn beside it, or empty
     */
    public record Node(String id, Kind kind, String number, String title, int depth, String icon) {
    }

    /**
     * An entry as text.
     *
     * @param id      the entry's id
     * @param number  its number in the manual
     * @param title   its title
     * @param icon    the item it is about, drawn beside its title, or empty
     * @param section the section it belongs to
     * @param pieces  what it says, in order
     */
    public record Article(String id, String number, String title, String icon, String section, List<Piece> pieces) {

        public Article {
            pieces = List.copyOf(pieces);
        }
    }

    /** One piece of an article. */
    public sealed interface Piece {

        /** Its words, all of them, as one line of plain text: what a search reads. */
        String words();

        /** A heading such as "What it is"; one that leads to trouble ("What can go wrong") says so. */
        record Heading(String text, boolean warning) implements Piece {

            @Override
            public String words() {
                return this.text;
            }
        }

        /** A paragraph, read the way its tone says. */
        record Paragraph(String text, Tone tone) implements Piece {

            @Override
            public String words() {
                return this.text;
            }
        }

        /** A line with a marker before it: a numbered step, a balloon's number with its legend, a part of a plan. */
        record Item(String marker, String text) implements Piece {

            @Override
            public String words() {
                return this.marker + " " + this.text;
            }
        }

        /** A word or a problem in bold, and what it means or how it is fixed set in under it. */
        record Term(String term, String text) implements Piece {

            @Override
            public String words() {
                return this.term + " " + this.text;
            }
        }

        /** A numbered table, its caption with its number, the heads of its columns, and its rows. */
        record Table(String caption, List<String> heads, List<Row> rows) implements Piece {

            public Table {
                heads = List.copyOf(heads);
                rows = List.copyOf(rows);
            }

            @Override
            public String words() {
                final StringBuilder out = new StringBuilder(this.caption);
                for (final String head : this.heads) {
                    out.append(' ').append(head);
                }
                for (final Row row : this.rows) {
                    for (final String cell : row.cells()) {
                        out.append(' ').append(cell);
                    }
                }
                return out.toString();
            }
        }

        /**
         * A listing of code: its caption with its number and its file's name, the language that colours it, and its
         * lines as typed, which a reader shows as they are, never wrapped inside a word.
         */
        record Code(String caption, String language, List<String> lines) implements Piece {

            public Code {
                lines = List.copyOf(lines);
            }

            @Override
            public String words() {
                return this.caption + " " + String.join(" ", this.lines);
            }
        }

        /**
         * The items an entry shows under its title.
         *
         * @param line  them named in a line, for a reader that draws no items: "Items: ..."
         * @param parts each item with its name
         */
        record Parts(String line, List<Part> parts) implements Piece {

            public Parts {
                parts = List.copyOf(parts);
            }

            @Override
            public String words() {
                final StringBuilder out = new StringBuilder(this.line);
                for (final Part part : this.parts) {
                    out.append(' ').append(part.name());
                }
                return out.toString();
            }
        }

        /**
         * An item drawn large, with its numbered caption, or none for a block shown before its legend; a picture that
         * is no item (an image, a drawing) has an empty item and only its caption.
         */
        record Picture(String item, String caption) implements Piece {

            @Override
            public String words() {
                return this.caption;
            }
        }

        /** Every recipe of a type, read from the game by whoever shows the article. */
        record Recipes(String type, String output) implements Piece {

            @Override
            public String words() {
                return "";
            }
        }

        /** Links to other entries or sections, after the words that lead them in. */
        record Links(String lead, List<Link> links) implements Piece {

            public Links {
                links = List.copyOf(links);
            }

            @Override
            public String words() {
                final StringBuilder out = new StringBuilder(this.lead);
                for (final Link link : this.links) {
                    out.append(' ').append(link.text());
                }
                return out.toString();
            }
        }
    }

    /** A row of a table: its cells, left to right, in the reader's language. */
    public record Row(List<String> cells) {

        public Row {
            cells = List.copyOf(cells);
        }

        /** A row of a table of properties: what the property is, and its value. */
        public Row(final String label, final String value) {
            this(List.of(label, value));
        }

        /** Its first cell: what it names. */
        public String label() {
            return this.cells.getFirst();
        }

        /** The cells after its first, as one piece of text. */
        public String value() {
            return String.join("  ", this.cells.subList(1, this.cells.size()));
        }
    }

    /** An item an entry shows, {@code namespace:path}, and its name in the reader's language. */
    public record Part(String item, String name) {
    }

    /** A link: where it goes, and its words, the number and title of what it goes to. */
    public record Link(String target, String text) {
    }

    /** A line of the index: a title or an explained word, where it is, and which of the two it is. */
    public record IndexLine(String text, String target, boolean term) {
    }

    /** How many figures, tables and listings a chapter has numbered so far. */
    private record Counted(int figures, int tables, int listings) {
    }
}
