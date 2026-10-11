/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import dev.jstech.core.id.IStableName;
import dev.jstech.core.text.Text;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * One piece of a manual's entry: a paragraph, a heading, a figure, a picture, a table, the recipes of a kind, numbered
 * steps, a warning, the problems and their fixes, a word explained, links to other entries, a command's page, a scene
 * of blocks the reader turns, or a special block another mod draws.
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
     * A scene: blocks placed in a small grid and drawn as the game draws them, which the reader turns by dragging and
     * brings nearer or farther with the wheel, with numbered balloons pinned to its parts and their legend under it;
     * numbered and captioned with the figures ("Figure 1-14."). One the reader may peel layer by layer, such as a
     * multiblock, has a control that hides the layers above the one chosen.
     *
     * @param blocks     its blocks, each where it stands, no two in one place
     * @param view       how the book first shows it, and what Reset goes back to
     * @param balloons   its balloons, each number once, in the order they are listed under it
     * @param layers     whether the reader may peel it layer by layer
     * @param height     how tall it is drawn on the page, its strip of controls under it apart
     * @param captionKey the caption's sentence
     */
    record Scene(List<SceneBlock> blocks, SceneView view, List<SceneBalloon> balloons, boolean layers, int height,
                 String captionKey) implements GuideBlock {

        /** How many blocks a scene's grid holds along each side: a scene is a small piece of a world, never a build. */
        public static final int GRID = 32;
        /** The least and the most a scene is drawn tall on a page. */
        public static final int MIN_HEIGHT = 48;
        public static final int MAX_HEIGHT = 180;

        public Scene {
            blocks = List.copyOf(blocks);
            Objects.requireNonNull(view, "view");
            balloons = List.copyOf(balloons);
            Objects.requireNonNull(captionKey, "captionKey");
            if (blocks.isEmpty()) {
                throw new IllegalArgumentException("a scene holds a block at least");
            }
            if (height < MIN_HEIGHT || height > MAX_HEIGHT) {
                throw new IllegalArgumentException("a scene is drawn " + MIN_HEIGHT + " to " + MAX_HEIGHT
                        + " pixels tall; got " + height);
            }
            final Set<Integer> places = new HashSet<>();
            for (final SceneBlock block : blocks) {
                if (!places.add(place(block.x(), block.y(), block.z()))) {
                    throw new IllegalArgumentException("two blocks of a scene stand at " + block.x() + ", "
                            + block.y() + ", " + block.z());
                }
            }
            final Set<Integer> numbers = new HashSet<>();
            for (final SceneBalloon balloon : balloons) {
                if (!numbers.add(balloon.number())) {
                    throw new IllegalArgumentException("a scene has two balloons numbered " + balloon.number());
                }
            }
        }

        /**
         * How many blocks it spans across, from its westmost block to its eastmost: a scene is framed round its blocks
         * wherever in the grid they stand.
         */
        public int width() {
            return this.blocks.stream().mapToInt(SceneBlock::x).max().orElse(0) - this.fromX() + 1;
        }

        /** The grid line its westmost block stands on, where its frame starts across. */
        public int fromX() {
            return this.blocks.stream().mapToInt(SceneBlock::x).min().orElse(0);
        }

        /** The grid line its northmost block stands on, where its frame starts back. */
        public int fromZ() {
            return this.blocks.stream().mapToInt(SceneBlock::z).min().orElse(0);
        }

        /** How many blocks it stands tall: its layers, when it is peeled. */
        public int tall() {
            return this.blocks.stream().mapToInt(SceneBlock::y).max().orElse(0) + 1;
        }

        /** How many blocks deep it goes, from its northmost block to its southmost. */
        public int depth() {
            return this.blocks.stream().mapToInt(SceneBlock::z).max().orElse(0) - this.fromZ() + 1;
        }

        /** The block standing there, if one does. */
        public Optional<SceneBlock> at(final int x, final int y, final int z) {
            return this.blocks.stream().filter(block -> block.x() == x && block.y() == y && block.z() == z)
                    .findFirst();
        }

        /** A place of the grid as one number, so two blocks in one place are told apart from two in two places. */
        private static int place(final int x, final int y, final int z) {
            return (x * GRID + y) * GRID + z;
        }
    }

    /**
     * A block of a scene, and where it stands.
     *
     * @param x     how far across the grid it stands, from 0
     * @param y     how far up
     * @param z     how far back, towards the north
     * @param state the block and its properties, as the game writes a block's state: {@code minecraft:furnace} or
     *              {@code minecraft:furnace[facing=south,lit=true]}
     * @param data  what its block entity holds, as the game writes a compound tag, or empty for what a new one holds
     */
    record SceneBlock(int x, int y, int z, String state, String data) {

        public SceneBlock {
            Objects.requireNonNull(state, "state");
            data = data == null ? "" : data;
            if (state.isBlank()) {
                throw new IllegalArgumentException("a block of a scene names its block");
            }
            inGrid(x, y, z);
        }

        /** The block's id, its state without its properties. */
        public String block() {
            final int open = this.state.indexOf('[');
            return open < 0 ? this.state : this.state.substring(0, open);
        }
    }

    /**
     * How a scene is seen: turned round and tilted, and how near. Turned by none, the scene is seen from its south
     * side, head on; tilted by a positive angle, from above.
     *
     * @param yaw   how far it is turned round, in degrees
     * @param pitch how far it is tilted towards the reader, in degrees, from looking up at it to looking down on it
     * @param zoom  how near it is, 1 for the whole scene filling its frame
     */
    record SceneView(float yaw, float pitch, float zoom) {

        /** The farthest a reader tilts a scene either way: straight down on it, or straight up under it. */
        public static final float MAX_PITCH = 90;
        /** How far and how near a reader brings a scene. */
        public static final float MIN_ZOOM = 0.5f;
        public static final float MAX_ZOOM = 4;
        /** The front corner of a scene, seen a little from above: how a book shows one it says nothing about. */
        public static final SceneView CORNER = new SceneView(-35, 25, 1);

        public SceneView {
            pitch = Math.max(-MAX_PITCH, Math.min(MAX_PITCH, pitch));
            zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));
            // A turn is the same however many times round it goes, so it is kept between -180 and 180.
            yaw = (float) Math.IEEEremainder(yaw, 360);
        }

        /** The same view turned by that many degrees more round and tilted by that many more. */
        public SceneView turned(final float byYaw, final float byPitch) {
            return new SceneView(this.yaw + byYaw, this.pitch + byPitch, this.zoom);
        }

        /** The same view, that many times nearer, within the nearest and the farthest. */
        public SceneView zoomed(final float times) {
            return new SceneView(this.yaw, this.pitch, this.zoom * times);
        }
    }

    /**
     * A numbered balloon pinned to a part of a scene, and the line of the legend that says what the part is. The
     * balloon points at the middle of its part, and the part is outlined while the pointer is on its number.
     *
     * @param number    the number in the balloon
     * @param part      what it points at
     * @param key       the legend's sentence for it
     * @param detailKey a second sentence its tooltip adds, such as what to do with it, or empty
     */
    record SceneBalloon(int number, ScenePart part, String key, String detailKey) {

        public SceneBalloon {
            Objects.requireNonNull(part, "part");
            Objects.requireNonNull(key, "key");
            detailKey = detailKey == null ? "" : detailKey;
            if (number < 1) {
                throw new IllegalArgumentException("a balloon is numbered from 1; got " + number);
            }
        }
    }

    /**
     * A part of a scene: a box inside one of its blocks, in the block's sixteen pixels each way, such as the tray of a
     * drive in the front of a case; or the whole block.
     *
     * @param x     the block, across the grid
     * @param y     up
     * @param z     back
     * @param fromX where the box starts in the block, across, from 0
     * @param fromY up
     * @param fromZ back
     * @param toX   where it ends, across, to 16
     * @param toY   up
     * @param toZ   back
     */
    record ScenePart(int x, int y, int z, int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {

        /** A block's sixteen pixels along each side. */
        public static final int PIXELS = 16;

        public ScenePart {
            inGrid(x, y, z);
            if (fromX < 0 || fromY < 0 || fromZ < 0 || toX > PIXELS || toY > PIXELS || toZ > PIXELS
                    || fromX > toX || fromY > toY || fromZ > toZ) {
                throw new IllegalArgumentException("a part is a box inside its block, from 0 to " + PIXELS
                        + " each way; got " + fromX + ", " + fromY + ", " + fromZ + " to " + toX + ", " + toY + ", "
                        + toZ);
            }
        }

        /** The whole block standing there. */
        public static ScenePart of(final int x, final int y, final int z) {
            return new ScenePart(x, y, z, 0, 0, 0, PIXELS, PIXELS, PIXELS);
        }

        /** A box inside the same block, in its sixteen pixels each way. */
        public ScenePart box(final int fromX, final int fromY, final int fromZ, final int toX, final int toY,
                             final int toZ) {
            return new ScenePart(this.x, this.y, this.z, fromX, fromY, fromZ, toX, toY, toZ);
        }

        /** Whether it is the whole block. */
        public boolean whole() {
            return this.fromX == 0 && this.fromY == 0 && this.fromZ == 0 && this.toX == PIXELS
                    && this.toY == PIXELS && this.toZ == PIXELS;
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

    /**
     * The page of a command a mod's computers run, read from the command itself as the manual is laid out
     * ({@link GuideFacts#command}): what it is for, how it is typed, what it does, a table of its options and one of
     * its examples, the systems it runs on, and the commands to read next. A mod makes the command's page at its
     * prompts from the same words, so the two never disagree.
     *
     * @param source the id its mod registered the command's page under, {@code namespace:path}
     */
    record Command(String source) implements GuideBlock {

        public Command {
            Objects.requireNonNull(source, "source");
        }
    }

    /** A cell of a table: a sentence, an amount with its unit, text that is data, or words its mod says. */
    sealed interface GuideValue {

        /** Whether it is data, an amount or a literal, which is written in the code font; words are not. */
        default boolean data() {
            return !(this instanceof Words || this instanceof Said);
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

        /**
         * Words a mod says as the game runs, such as what an option of a command does: a sentence of the mod's own,
         * put in the reader's language when it is shown. Only what a mod answers as a manual is laid out holds these
         * ({@link GuideFacts}), so they are never written to a manual's files.
         */
        record Said(Text text) implements GuideValue {

            public Said {
                Objects.requireNonNull(text, "text");
            }
        }
    }

    /** Fails for a place outside a scene's grid. */
    private static void inGrid(final int x, final int y, final int z) {
        final int last = Scene.GRID - 1;
        if (x < 0 || y < 0 || z < 0 || x > last || y > last || z > last) {
            throw new IllegalArgumentException("a scene's grid runs from 0 to " + last + " each way; got " + x + ", "
                    + y + ", " + z);
        }
    }
}
