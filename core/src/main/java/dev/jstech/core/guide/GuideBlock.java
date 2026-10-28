/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import dev.jstech.core.id.IStableName;
import dev.jstech.core.multiblock.ShapePreview;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * One piece of a manual's entry: a paragraph, a heading, a figure, a picture, a table, the recipes of a kind, numbered
 * steps, a warning, the problems and their fixes, a word explained, links to other entries, a command's page, a scene
 * of blocks the reader turns, a multiblock or a saved structure drawn as one, or a special block another mod draws.
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
     * @param frames     how many pictures the image holds one under the other, shown in turn as the game shows an
     *                   animated texture; 1 for a still picture
     * @param frameTicks how many ticks each of them is shown
     */
    record Picture(String image, String drawing, String data, int width, int height, String captionKey, int frames,
                   int frameTicks) implements GuideBlock {

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
            if (frames < 1 || frameTicks < 1) {
                throw new IllegalArgumentException("a picture shows a frame at least, each a tick at least; got "
                        + frames + " frames of " + frameTicks + " ticks");
            }
            if (frames > 1 && image.isEmpty()) {
                throw new IllegalArgumentException("only an image is a strip of frames, never a drawing");
            }
        }

        /** A still picture. */
        public Picture(final String image, final String drawing, final String data, final int width,
                       final int height, final String captionKey) {
            this(image, drawing, data, width, height, captionKey, 1, 1);
        }

        /** The frame shown at the game time {@code ticks}: the frames in turn, from the first again after the last. */
        public int frameAt(final long ticks) {
            return (int) Math.floorMod(ticks / this.frameTicks, (long) this.frames);
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

    /**
     * A caution drawn in a box of its own, in a colour of its own: what stops a thing from working or holds it back,
     * where a warning is for what does harm.
     */
    record Caution(String key) implements GuideBlock {

        public Caution {
            Objects.requireNonNull(key, "key");
        }
    }

    /**
     * A checklist, as a crew reads one: each line a box to tick, its number, what to look at, a row of dots and what
     * it should be ({@code ROCKET ..... ON PAD}).
     */
    record Checklist(List<Check> lines) implements GuideBlock {

        public Checklist {
            lines = List.copyOf(lines);
        }
    }

    /**
     * One line of a checklist.
     *
     * @param itemKey  what to look at
     * @param stateKey what it should be, or empty for a line with no state
     * @param changed  whether the manual's last revision changed the line, which a bar in the margin marks
     */
    record Check(String itemKey, String stateKey, boolean changed) {

        public Check {
            Objects.requireNonNull(itemKey, "itemKey");
            stateKey = stateKey == null ? "" : stateKey;
        }
    }

    /**
     * A card bound on rings, as the checklists a crew wore on their cuff were: a title bar, then numbered steps, each a
     * box to tick, its name and a line of what it is, ruled one from the next.
     *
     * @param titleKey the card's title
     * @param first    the number of its first step, so a run of cards counts on from the one before
     * @param steps    its steps
     */
    record Card(String titleKey, int first, List<CardStep> steps) implements GuideBlock {

        public Card {
            Objects.requireNonNull(titleKey, "titleKey");
            first = Math.max(1, first);
            steps = List.copyOf(steps);
        }
    }

    /**
     * One step of a card.
     *
     * @param nameKey its name
     * @param textKey what it is, in a line or two
     * @param changed whether the manual's last revision changed the step, which a clip on the card's edge marks
     */
    record CardStep(String nameKey, String textKey, boolean changed) {

        public CardStep {
            Objects.requireNonNull(nameKey, "nameKey");
            textKey = textKey == null ? "" : textKey;
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
     * @param steps      the steps it is shown in, each putting blocks in and taking some out, under a sentence of
     *                   its own, played in turn with a timeline under it; empty for a scene shown whole
     */
    record Scene(List<SceneBlock> blocks, SceneView view, List<SceneBalloon> balloons, boolean layers, int height,
                 String captionKey, List<SceneStep> steps) implements GuideBlock {

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
            steps = List.copyOf(steps);
            if (blocks.isEmpty() && steps.stream().allMatch(step -> step.put().isEmpty())) {
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

        /** A scene shown whole, in no steps. */
        public Scene(final List<SceneBlock> blocks, final SceneView view, final List<SceneBalloon> balloons,
                     final boolean layers, final int height, final String captionKey) {
            this(blocks, view, balloons, layers, height, captionKey, List.of());
        }

        /**
         * How many blocks it spans across, from its westmost block to its eastmost: a scene is framed round every
         * block it shows in any of its steps, wherever in the grid they stand, so its frame holds still as it plays.
         */
        public int width() {
            return this.everyBlock().stream().mapToInt(SceneBlock::x).max().orElse(0) - this.fromX() + 1;
        }

        /** The grid line its westmost block stands on, where its frame starts across. */
        public int fromX() {
            return this.everyBlock().stream().mapToInt(SceneBlock::x).min().orElse(0);
        }

        /** The grid line its northmost block stands on, where its frame starts back. */
        public int fromZ() {
            return this.everyBlock().stream().mapToInt(SceneBlock::z).min().orElse(0);
        }

        /** How many blocks it stands tall: its layers, when it is peeled. */
        public int tall() {
            return this.everyBlock().stream().mapToInt(SceneBlock::y).max().orElse(0) + 1;
        }

        /** How many blocks deep it goes, from its northmost block to its southmost. */
        public int depth() {
            return this.everyBlock().stream().mapToInt(SceneBlock::z).max().orElse(0) - this.fromZ() + 1;
        }

        /** The block standing there before any step, if one does. */
        public Optional<SceneBlock> at(final int x, final int y, final int z) {
            return this.blocks.stream().filter(block -> block.x() == x && block.y() == y && block.z() == z)
                    .findFirst();
        }

        /** Every block it shows in any of its steps, those it starts with first. */
        public List<SceneBlock> everyBlock() {
            if (this.steps.isEmpty()) {
                return this.blocks;
            }
            final List<SceneBlock> every = new ArrayList<>(this.blocks);
            this.steps.forEach(step -> every.addAll(step.put()));
            return every;
        }

        /**
         * The blocks standing once the step {@code step} (from 0) is done: those it starts with, then each step's in
         * turn, a block put where one stands taking its place and a place taken emptied. Past its last step, the last
         * step's; a scene with no steps always stands whole.
         */
        public List<SceneBlock> blocksAt(final int step) {
            if (this.steps.isEmpty()) {
                return this.blocks;
            }
            final Map<Integer, SceneBlock> standing = new LinkedHashMap<>();
            this.blocks.forEach(block -> standing.put(place(block.x(), block.y(), block.z()), block));
            for (int i = 0; i <= Math.min(step, this.steps.size() - 1); i++) {
                final SceneStep done = this.steps.get(i);
                done.take().forEach(part -> standing.remove(place(part.x(), part.y(), part.z())));
                done.put().forEach(block -> standing.put(place(block.x(), block.y(), block.z()), block));
            }
            return List.copyOf(standing.values());
        }

        /**
         * The blocks standing once the step {@code step} is done laid out as places of the world, for the scene to be
         * shown there as a multiblock is shown before it is built: the scene's front to the south, as the book first
         * shows a scene, its west to the west.
         */
        public ShapePreview inWorld(final int step) {
            final List<ShapePreview.Ghost> places = new ArrayList<>();
            for (final SceneBlock block : this.blocksAt(step)) {
                places.add(new ShapePreview.Ghost(block.x(), block.y(), -block.z(), 0, List.of(block.block()),
                        List.of(), false));
            }
            return ShapePreview.of(places);
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
     * A step of a scene shown step by step: what it puts in and takes out of the scene, under a sentence of its own
     * shown over the picture ("2 of 3: Put the chest beside the furnace"), and how long it is shown before the next.
     *
     * @param captionKey its sentence
     * @param put        the blocks it puts in, each in its place, one put where a block stands taking its place
     * @param take       the places it empties, each a whole block
     * @param ticks      how long it is shown while the scene plays, in ticks
     */
    record SceneStep(String captionKey, List<SceneBlock> put, List<ScenePart> take, int ticks) {

        /** How long a step is shown when its scene says nothing: two seconds. */
        public static final int TICKS = 40;

        public SceneStep {
            Objects.requireNonNull(captionKey, "captionKey");
            put = List.copyOf(put);
            take = List.copyOf(take);
            if (ticks < 1) {
                throw new IllegalArgumentException("a step is shown a tick at least; got " + ticks);
            }
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
     * A multiblock drawn as a scene the reader turns, brings nearer and peels layer by layer, numbered with the
     * figures. Its blocks are laid out from the multiblock in force when the manual is read, its terminal in its
     * place, so a data pack's change to the multiblock shows in it; where the multiblock is not installed, or is too
     * big for a scene, it is left out.
     *
     * @param multiblock the multiblock's id, as the views that show multiblocks before they are built name it
     * @param view       how the book first shows it, and what Reset goes back to
     * @param height     how tall it is drawn on the page, its strip of controls under it apart
     * @param captionKey the caption's sentence
     */
    record Multiblock(String multiblock, SceneView view, int height, String captionKey) implements GuideBlock {

        public Multiblock {
            Objects.requireNonNull(multiblock, "multiblock");
            Objects.requireNonNull(view, "view");
            Objects.requireNonNull(captionKey, "captionKey");
            if (multiblock.isBlank()) {
                throw new IllegalArgumentException("a multiblock's scene names the multiblock");
            }
            if (height < Scene.MIN_HEIGHT || height > Scene.MAX_HEIGHT) {
                throw new IllegalArgumentException("a scene is drawn " + Scene.MIN_HEIGHT + " to " + Scene.MAX_HEIGHT
                        + " pixels tall; got " + height);
            }
        }

        /**
         * The blocks of {@code preview} as a scene holds them, its least corner at the grid's: at each place the block
         * {@code blockOf} gives for what is shown there (a block id, or a tag as {@code #namespace:path}), a place it
         * gives none for (an empty id) left empty; none at all for a multiblock too big for a scene.
         */
        public static List<SceneBlock> blocksOf(final ShapePreview preview, final UnaryOperator<String> blockOf) {
            if (preview.sizeX() > Scene.GRID || preview.sizeY() > Scene.GRID || preview.sizeZ() > Scene.GRID) {
                return List.of();
            }
            final List<SceneBlock> blocks = new ArrayList<>();
            for (final ShapePreview.Ghost ghost : preview.ghosts()) {
                final String block = ghost.first() == null ? "" : blockOf.apply(ghost.first());
                if (block != null && !block.isBlank()) {
                    blocks.add(new SceneBlock(ghost.x() - preview.minX(), ghost.y() - preview.minY(),
                            ghost.z() - preview.minZ(), block, ""));
                }
            }
            return List.copyOf(blocks);
        }

        /** The scene it is drawn as, of {@code blocks}: peeled layer by layer, with no balloons. */
        public Scene scene(final List<SceneBlock> blocks) {
            return new Scene(blocks, this.view, List.of(), true, this.height, this.captionKey);
        }
    }

    /**
     * A structure saved with the game's structure block, drawn as a scene the reader turns, brings nearer and peels
     * layer by layer, numbered with the figures: its blocks are read from the structure's file in the mod's assets,
     * {@code assets/<namespace>/jstech/structures/<path>.nbt}, as the manual is read, so building it in the game and
     * saving it is how its scene is made. Where the file is missing, or holds a structure too big for a scene, it is
     * left out.
     *
     * @param structure  the structure's id, {@code namespace:path}
     * @param view       how the book first shows it, and what Reset goes back to
     * @param height     how tall it is drawn on the page, its strip of controls under it apart
     * @param captionKey the caption's sentence
     */
    record Structure(String structure, SceneView view, int height, String captionKey) implements GuideBlock {

        /** The blocks a structure's file lists that a scene leaves empty: air, and the structure's own void. */
        public static final Set<String> EMPTY = Set.of("minecraft:air", "minecraft:cave_air", "minecraft:void_air",
                "minecraft:structure_void");

        public Structure {
            Objects.requireNonNull(structure, "structure");
            Objects.requireNonNull(view, "view");
            Objects.requireNonNull(captionKey, "captionKey");
            if (structure.isBlank()) {
                throw new IllegalArgumentException("a structure's scene names the structure");
            }
            if (height < Scene.MIN_HEIGHT || height > Scene.MAX_HEIGHT) {
                throw new IllegalArgumentException("a scene is drawn " + Scene.MIN_HEIGHT + " to " + Scene.MAX_HEIGHT
                        + " pixels tall; got " + height);
            }
        }

        /**
         * The blocks of a saved structure {@code sizeX} by {@code sizeY} by {@code sizeZ} as a scene holds them: each
         * where it stood, the structure's south made the scene's front (a structure counts back to the south, a
         * scene to the north), air and the structure's void left empty; none at all for a structure too big for a
         * scene.
         */
        public static List<SceneBlock> blocksOf(final int sizeX, final int sizeY, final int sizeZ,
                                                final List<Saved> saved) {
            if (sizeX > Scene.GRID || sizeY > Scene.GRID || sizeZ > Scene.GRID || sizeX < 1 || sizeY < 1
                    || sizeZ < 1) {
                return List.of();
            }
            final List<SceneBlock> blocks = new ArrayList<>();
            for (final Saved block : saved) {
                final String id = block.state().indexOf('[') < 0 ? block.state()
                        : block.state().substring(0, block.state().indexOf('['));
                if (!EMPTY.contains(id) && block.x() >= 0 && block.x() < sizeX && block.y() >= 0
                        && block.y() < sizeY && block.z() >= 0 && block.z() < sizeZ) {
                    blocks.add(new SceneBlock(block.x(), block.y(), sizeZ - 1 - block.z(), block.state(),
                            block.data()));
                }
            }
            return List.copyOf(blocks);
        }

        /** The scene it is drawn as, of {@code blocks}: peeled layer by layer, with no balloons. */
        public Scene scene(final List<SceneBlock> blocks) {
            return new Scene(blocks, this.view, List.of(), true, this.height, this.captionKey);
        }

        /**
         * A block as a structure's file holds it, where it stands in the structure.
         *
         * @param x     across, from the structure's west side
         * @param y     up
         * @param z     back to the south, as the game counts
         * @param state the block and its properties, as the game writes a block's state
         * @param data  what its block entity holds, as the game writes a compound tag, or empty
         */
        public record Saved(int x, int y, int z, String state, String data) {

            public Saved {
                Objects.requireNonNull(state, "state");
                data = data == null ? "" : data;
            }
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
     * One figure drawn more than one way, with a switch over it choosing which: a diagram and the scene of its blocks,
     * a section of the ground in lines and in colours. Each way is a figure of its own (a figure, a picture or a
     * drawing, a scene, a multiblock or a structure), named on the switch by its word; the first is shown first, and
     * the figure keeps one number whichever is shown.
     *
     * @param labelKeys the switch's word for each way, a sentence's key
     * @param ways      the figures, one for each word, in the switch's order
     */
    record Modes(List<String> labelKeys, List<GuideBlock> ways) implements GuideBlock {

        public Modes {
            labelKeys = List.copyOf(labelKeys);
            ways = List.copyOf(ways);
            if (ways.size() < 2 || labelKeys.size() != ways.size()) {
                throw new IllegalArgumentException("a figure drawn more than one way has two ways at least and a word"
                        + " for each; got " + ways.size() + " ways and " + labelKeys.size() + " words");
            }
            for (final GuideBlock way : ways) {
                if (!(way instanceof Figure || way instanceof Picture || way instanceof Scene
                        || way instanceof Multiblock || way instanceof Structure)) {
                    throw new IllegalArgumentException("a way of drawing a figure is a figure, a picture, a scene, a"
                            + " multiblock or a structure, not " + way.getClass().getSimpleName());
                }
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
