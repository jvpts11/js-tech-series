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
import java.util.Objects;

/**
 * One piece of a manual's entry: a paragraph, a heading, a figure, a picture, a table, the recipes of a kind, numbered
 * steps, a warning, the problems and their fixes, a word explained, links to other entries, or a special block
 * another mod draws.
 *
 * <p>An entry is a list of these, laid out on the pages in the order given. Words are never held here, only the keys
 * of the sentences, so a manual reads in each player's language and a resource pack can reword it. Ids are text in
 * the {@code namespace:path} shape, so the layout of a manual can be worked out and tested with no game around it.
 *
 * <p>A sentence may lead to another page in its own words, written {@code [the words](namespace:path)}: the words
 * are drawn as a link to that entry, section or chapter, with its number after them in brackets, and
 * {@code [](namespace:path)} draws the number alone ({@link GuideLinks}).
 */
public sealed interface GuideBlock {

    /** A paragraph of running text. */
    record Paragraph(String key) implements GuideBlock {

        public Paragraph {
            Objects.requireNonNull(key, "key");
        }
    }

    /** A heading inside an entry, such as "What it is". */
    record Heading(String key) implements GuideBlock {

        public Heading {
            Objects.requireNonNull(key, "key");
        }

        /** Whether the heading leads into what can go wrong, which stands out in the style's accent. */
        public boolean leadsIntoTrouble() {
            return GuideTexts.WHAT_CAN_GO_WRONG.key().equals(this.key)
                    || GuideTexts.IF_SOMETHING_GOES_WRONG.key().equals(this.key);
        }
    }

    /**
     * A figure: an item drawn large in a frame, with its numbered caption under it ("Figure 3-9. ...").
     *
     * @param item       the item drawn, {@code namespace:path}
     * @param captionKey the caption's sentence
     */
    record Figure(String item, String captionKey) implements GuideBlock {

        public Figure {
            Objects.requireNonNull(item, "item");
            Objects.requireNonNull(captionKey, "captionKey");
        }
    }

    /**
     * A picture with its numbered caption under it ("Figure 3-9. ..."), counted with the figures: an image the mod
     * ships, or a drawing a mod's renderer makes as the page is drawn, such as one of its machines' screens, which
     * then reads in the reader's language and never falls behind the screen it shows.
     *
     * @param image      the image, a texture {@code namespace:path} under {@code textures/}, or empty for a drawing
     * @param drawing    the kind of drawing, as its mod registered the renderer, or empty for an image
     * @param data       what the drawing's renderer is handed, as JSON text
     * @param width      how wide it is drawn, in the page's pixels; 0 for the whole column
     * @param height     how tall it is drawn
     * @param captionKey the caption's sentence
     */
    record Picture(String image, String drawing, String data, int width, int height, String captionKey)
            implements GuideBlock {

        public Picture {
            image = image == null ? "" : image;
            drawing = drawing == null ? "" : drawing;
            data = data == null ? "{}" : data;
            Objects.requireNonNull(captionKey, "captionKey");
            if (image.isEmpty() == drawing.isEmpty()) {
                throw new IllegalArgumentException("a picture is an image or a drawing, one of the two");
            }
            if (width < 0 || height < 1) {
                throw new IllegalArgumentException("a picture has a size; got " + width + " by " + height);
            }
        }
    }

    /**
     * A numbered table ("Table 3-7. ..."): a head over its columns, then a row for each thing it lists. A table of a
     * thing's properties has two columns, what each property is and its value, headed "Property" and "Value" when it
     * names no heads; a table of commands, codes or models names its own, such as "Command" and "What it does".
     *
     * <p>Words are written small and wrap between words only; a column of nothing but data (amounts and literals) is
     * written in the style's code font, so its figures line up. When the columns cannot stand side by side on the
     * page, each row is laid out under the one before it instead, its further columns under their heads.
     *
     * <p>A table may hold no rows of its own and name a source instead, whose rows its mod gives as the manual is laid
     * out ({@link GuideFacts}), so a table of every part of a family or every era's figures is never out of date.
     *
     * @param captionKey the caption's sentence
     * @param headKeys   the heads of the columns, left to right, or empty for a table of properties
     * @param rows       the rows, each with a cell for every column
     * @param source     the source its rows are read from as the manual is laid out, or empty for the rows given
     */
    record Table(String captionKey, List<String> headKeys, List<TableRow> rows, String source)
            implements GuideBlock {

        /** How many columns a table of properties has: what each property is, and its value. */
        public static final int PROPERTY_COLUMNS = 2;

        public Table {
            Objects.requireNonNull(captionKey, "captionKey");
            headKeys = List.copyOf(headKeys);
            rows = List.copyOf(rows);
            source = source == null ? "" : source;
            final int columns = headKeys.isEmpty() ? PROPERTY_COLUMNS : headKeys.size();
            for (final TableRow row : rows) {
                if (row.cells().size() != columns) {
                    throw new IllegalArgumentException("every row of a table of " + columns + " columns has "
                            + columns + " cells; one has " + row.cells().size());
                }
            }
            if (!source.isEmpty() && !rows.isEmpty()) {
                throw new IllegalArgumentException("a table read from " + source + " holds no rows of its own");
            }
        }

        /** A table of properties and their values. */
        public Table(final String captionKey, final List<TableRow> rows) {
            this(captionKey, List.of(), rows, "");
        }

        /** A table with heads of its own and the rows given. */
        public Table(final String captionKey, final List<String> headKeys, final List<TableRow> rows) {
            this(captionKey, headKeys, rows, "");
        }

        /** The same table holding those rows, read from its source. */
        public Table withRows(final List<TableRow> read) {
            return new Table(this.captionKey, this.headKeys, read, "");
        }

        /** How many columns it has. */
        public int columns() {
            return this.headKeys.isEmpty() ? PROPERTY_COLUMNS : this.headKeys.size();
        }
    }

    /** One row of a table: a cell for every column, left to right. */
    record TableRow(List<GuideValue> cells) {

        public TableRow {
            cells = List.copyOf(cells);
            if (cells.isEmpty()) {
                throw new IllegalArgumentException("a table's row has a cell at least");
            }
        }

        /** A row of a table of properties: what the property is, and its value. */
        public TableRow(final String labelKey, final GuideValue value) {
            this(List.of(new GuideValue.Words(labelKey), value));
        }
    }

    /**
     * A numbered listing of code ("Listing 3-4."), headed by the name of its file: its lines in the code font with
     * their numbers beside them, coloured as its language colours them, with a button that copies it whole. A line too
     * long for the page goes on under itself, set in, never broken inside a word; a click opens the listing over the
     * whole book with no line broken.
     *
     * @param name     the file the code is, such as {@code Hello.sgs}, or empty for a fragment
     * @param language the language that colours it: a language's own name, or the extension of its files; empty takes
     *                 the extension of the file's name
     * @param lines    its lines, as they are typed
     */
    record Listing(String name, String language, List<String> lines) implements GuideBlock {

        public Listing {
            name = name == null ? "" : name;
            language = language == null || language.isEmpty() ? extensionOf(name) : language;
            lines = List.copyOf(lines);
        }

        /** The listing's code as one text, its lines joined as typed: what its button copies. */
        public String text() {
            return String.join("\n", this.lines);
        }

        private static String extensionOf(final String name) {
            final int dot = name.lastIndexOf('.');
            return dot < 0 || dot == name.length() - 1 ? "" : name.substring(dot + 1);
        }
    }

    /**
     * A session at a terminal as one system shows it: its name on a strip above, then its lines in the code font in
     * the system's own colours, each line typed after its prompt or the system's answer. A line too long for the page
     * goes on in the next row at its first character, as a terminal wraps it.
     *
     * @param look  the palette of the terminal's colours, {@code namespace:path}, of the {@code GuidePalettes.Terminal}
     *              kind
     * @param title what its strip says, such as {@code Ubuntu - bash}; the same in every language
     * @param lines its lines, in order
     */
    record Session(String look, String title, List<SessionLine> lines) implements GuideBlock {

        public Session {
            Objects.requireNonNull(look, "look");
            title = title == null ? "" : title;
            lines = List.copyOf(lines);
        }

        /** Its lines as a terminal prints them, each prompt with what was typed after it. */
        public List<String> printed() {
            return this.lines.stream().map(line -> line.prompt() + line.text()).toList();
        }
    }

    /**
     * A line of a session: what was typed after a prompt, or what the system answered.
     *
     * @param prompt the prompt, such as {@code C:\>}, when it was typed; empty for an answer
     * @param text   what was typed, or what the system said
     * @param typed  whether it was typed rather than said
     */
    record SessionLine(String prompt, String text, boolean typed) {

        public SessionLine {
            prompt = prompt == null ? "" : prompt;
            Objects.requireNonNull(text, "text");
        }

        /** A line the player typed after the prompt. */
        public static SessionLine typed(final String prompt, final String text) {
            return new SessionLine(prompt, text, true);
        }

        /** A line the system answered. */
        public static SessionLine answer(final String text) {
            return new SessionLine("", text, false);
        }
    }

    /**
     * Every recipe of a recipe type, read from the game as it runs: the inputs, an arrow with the time and the energy,
     * and the outputs.
     *
     * @param type   the recipe type, such as {@code minecraft:smelting} or a machine's kind
     * @param output only the recipes that make this item, or empty for every recipe of the type
     */
    record Recipes(String type, String output) implements GuideBlock {

        public Recipes {
            Objects.requireNonNull(type, "type");
            output = output == null ? "" : output;
        }
    }

    /** Numbered steps, one sentence each, in the order they are done. */
    record Steps(List<String> keys) implements GuideBlock {

        public Steps {
            keys = List.copyOf(keys);
        }
    }

    /** A warning drawn in a box of its own, for what costs a player something if missed. */
    record Warning(String key) implements GuideBlock {

        public Warning {
            Objects.requireNonNull(key, "key");
        }
    }

    /** What can go wrong: each problem as a player sees it, and what to do about it. */
    record Problems(List<Problem> problems) implements GuideBlock {

        public Problems {
            problems = List.copyOf(problems);
        }
    }

    /** One problem and its fix. */
    record Problem(String problemKey, String fixKey) {

        public Problem {
            Objects.requireNonNull(problemKey, "problemKey");
            Objects.requireNonNull(fixKey, "fixKey");
        }
    }

    /**
     * A word explained where the entry first uses it: the word, then what it means. The index lists every such word
     * with the page that explains it, which makes the index the manual's glossary.
     */
    record Define(String termKey, String definitionKey) implements GuideBlock {

        public Define {
            Objects.requireNonNull(termKey, "termKey");
            Objects.requireNonNull(definitionKey, "definitionKey");
        }
    }

    /** A note set apart in the style's accent: what to read next, a hint. */
    record Note(String key) implements GuideBlock {

        public Note {
            Objects.requireNonNull(key, "key");
        }
    }

    /**
     * What follows starts in the next column of the page, or on the next page: on a drawing, the second half of a
     * sheet or the next sheet. A style whose pages hold one column runs straight on past it, as a book's text does.
     */
    record Break(BreakKind kind) implements GuideBlock {

        public Break {
            Objects.requireNonNull(kind, "kind");
        }
    }

    /** Where a break sends what follows it. */
    enum BreakKind implements IStableName {
        /** The next column, or the next page when this is the last column. */
        COLUMN("column"),
        /** The next page. */
        PAGE("page");

        private final String serializedName;

        BreakKind(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }
    }

    /**
     * A block seen from three sides, as a drawing shows a machine: from above, from the front and from the side, with
     * how wide a block is under the front, and numbered balloons pointing at what the legend under it explains.
     *
     * @param item     the block's item, {@code namespace:path}
     * @param callouts the balloons, in the order their numbers go
     */
    record Views(String item, List<Callout> callouts) implements GuideBlock {

        public Views {
            Objects.requireNonNull(item, "item");
            callouts = List.copyOf(callouts);
        }
    }

    /**
     * A balloon pointing at a place on one of the views, and the line of the legend that says what it is.
     *
     * @param number the number in the balloon
     * @param view   the view it points at
     * @param u      how far across the face it points, in the face's sixteen pixels
     * @param v      how far down the face it points
     * @param key    the legend's sentence for it
     */
    record Callout(int number, View view, int u, int v, String key) {

        public Callout {
            Objects.requireNonNull(view, "view");
            Objects.requireNonNull(key, "key");
        }
    }

    /** A side a block is seen from. */
    enum View implements IStableName {
        TOP("top"),
        FRONT("front"),
        SIDE("side");

        private final String serializedName;

        View(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }
    }

    /**
     * Blocks seen from above, side by side as they are to be placed, each named under it, those that are optional
     * outlined in dots: how to set something up.
     *
     * @param captionKey the plan's title, such as "Setting it up (seen from above)"
     * @param parts      the blocks, left to right
     */
    record Plan(String captionKey, List<PlanPart> parts) implements GuideBlock {

        public Plan {
            Objects.requireNonNull(captionKey, "captionKey");
            parts = List.copyOf(parts);
        }
    }

    /**
     * One block of a plan.
     *
     * @param item     the block's item, or empty for an optional place drawn only as its outline
     * @param labelKey what it is called under it
     * @param optional whether it may be left out, drawn in dots
     */
    record PlanPart(String item, String labelKey, boolean optional) {

        public PlanPart {
            item = item == null ? "" : item;
            Objects.requireNonNull(labelKey, "labelKey");
        }
    }

    /** Links to other entries ("See 3.2.2 and 3.4.3"), each shown by the number it has in the manual. */
    record SeeAlso(List<String> entries) implements GuideBlock {

        public SeeAlso {
            entries = List.copyOf(entries);
        }
    }

    /**
     * A block of a kind another mod draws, such as a multiblock turning in three dimensions.
     *
     * @param type   the kind, as the mod registered its renderer
     * @param height how many pixels tall it is drawn
     * @param data   what the renderer is handed, as JSON text
     */
    record Custom(String type, int height, String data) implements GuideBlock {

        public Custom {
            Objects.requireNonNull(type, "type");
            data = data == null ? "{}" : data;
            if (height < 1) {
                throw new IllegalArgumentException("a special block is at least one pixel tall; got " + height);
            }
        }
    }

    /** A cell of a table: a sentence, an amount with its unit, or text that is data. */
    sealed interface GuideValue {

        /** Whether it is data, an amount or a literal, which is written in the code font; words are not. */
        default boolean data() {
            return !(this instanceof Words);
        }

        /** A sentence to translate, such as "PC, Server, Mainframe". */
        record Words(String key) implements GuideValue {

            public Words {
                Objects.requireNonNull(key, "key");
            }
        }

        /** A number, written in the reader's way of writing numbers, and the symbol of its unit ("FE/t"). */
        record Amount(long value, String unit) implements GuideValue {

            public Amount {
                unit = unit == null ? "" : unit;
            }
        }

        /** Text that reads the same in every language: a model number, a bus's name. */
        record Literal(String text) implements GuideValue {

            public Literal {
                Objects.requireNonNull(text, "text");
            }
        }
    }
}
