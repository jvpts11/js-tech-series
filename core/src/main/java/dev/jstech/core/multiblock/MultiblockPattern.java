/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * A multiblock's shape drawn in letters: a 3D grid of places, one letter each, layer by layer from the bottom, and
 * what each letter stands for. {@code #} is the controller and a space a place left free; every other letter is a
 * {@link Place}: what fits there, and what a block there counts and gives.
 *
 * <p>Besides the drawing, a pattern may declare the least and the most of each count ("from one to four energy
 * ports"); that it is also found mirrored, or lying on its side, besides its four horizontal turns; one slice repeated
 * a number of times, for a structure that grows (a tower's middle layer, a battery's oven); and one letter that is its
 * inside, read by an {@link InsideRule}.
 */
public final class MultiblockPattern implements IStructureShape {

    private final String name;
    private final char[][][] layers; // layers[y][z][x]
    private final Map<Character, Place> places;
    private final Map<Character, IBlockMatcher> mapping;
    /** The chars whose slots are ports, with what each opens onto. */
    private final Map<Character, PortKind> ports;
    private final Map<String, Limit> limits;
    private final boolean mirrors;
    private final boolean liesDown;
    @Nullable
    private final Repeat repeat;
    @Nullable
    private final Inside inside;
    private final int controllerX;
    private final int controllerY;
    private final int controllerZ;
    @Nullable
    private final String controllerBlock;

    public static final char CONTROLLER_CHAR = '#';

    public static final char IGNORE_CHAR = ' ';

    /** The pattern's axis along its columns, west to east. */
    public static final int AXIS_X = 0;
    /** Along its layers, bottom to top. */
    public static final int AXIS_Y = 1;
    /** Along its rows, north to south. */
    public static final int AXIS_Z = 2;

    private MultiblockPattern(final Builder from, final char[][][] layers, final int controllerX,
                              final int controllerY, final int controllerZ) {
        this.name = from.name;
        this.layers = layers;
        this.places = Map.copyOf(from.places);
        final Map<Character, IBlockMatcher> fits = new HashMap<>();
        this.places.forEach((letter, place) -> fits.put(letter, place.fits()));
        this.mapping = Map.copyOf(fits);
        this.ports = Map.copyOf(from.ports);
        this.limits = Map.copyOf(from.limits);
        this.mirrors = from.mirrors;
        this.liesDown = from.liesDown;
        this.repeat = from.repeat;
        this.inside = from.inside;
        this.controllerX = controllerX;
        this.controllerY = controllerY;
        this.controllerZ = controllerZ;
        this.controllerBlock = from.controllerBlock;
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public String name() { return name; }
    public int sizeY() { return layers.length; }
    public int sizeZ() { return layers[0].length; }
    public int sizeX() { return layers[0][0].length; }

    public char charAt(int x, int y, int z) {
        return layers[y][z][x];
    }

    /** What fits each letter's places. */
    public Map<Character, IBlockMatcher> mapping() {
        return Collections.unmodifiableMap(mapping);
    }

    /** Each letter's place: what fits, and what it counts and gives. */
    public Map<Character, Place> places() {
        return this.places;
    }

    /** The chars whose slots are ports, with what each opens onto. */
    public Map<Character, PortKind> ports() {
        return ports;
    }

    /** The least and the most of each count, by name. */
    public Map<String, Limit> limits() {
        return this.limits;
    }

    /** Whether it is also found mirrored. */
    @Override
    public boolean mirrors() {
        return this.mirrors;
    }

    /** Whether it is also found lying on its side. */
    @Override
    public boolean liesDown() {
        return this.liesDown;
    }

    /** Every way it is found standing, the four horizontal turns first. */
    public List<Orientation> orientations() {
        if (this.liesDown) {
            return this.mirrors ? Orientation.lyingAndMirrors() : Orientation.lying();
        }
        return this.mirrors ? Orientation.turnsAndMirrors() : Orientation.turns();
    }

    /** Its repeated slice, when it has one. */
    public Optional<Repeat> repeat() {
        return Optional.ofNullable(this.repeat);
    }

    /** Its inside letter and the rule that reads it, when it has one. */
    public Optional<Inside> inside() {
        return Optional.ofNullable(this.inside);
    }

    /** The rows of layer {@code y}, north to south, each west to east: the pattern as it was written. */
    public String[] rows(int y) {
        final String[] rows = new String[sizeZ()];
        for (int z = 0; z < sizeZ(); z++) {
            rows[z] = new String(layers[y][z]);
        }
        return rows;
    }

    public int controllerX() { return controllerX; }

    public int controllerY() { return controllerY; }

    public int controllerZ() { return controllerZ; }

    /**
     * The id of the block that stands at {@code #}, when the pattern names it: the matcher never needs it, since the
     * controller is the one matching, but a projector, a recipe viewer and a manual show it there and list it.
     */
    public Optional<String> controllerBlock() {
        return Optional.ofNullable(this.controllerBlock);
    }

    @Override
    public ShapeMatch check(final IBlockProvider provider, final long controller) {
        return ShapeMatcher.match(this, provider, controller);
    }

    /** Its longest side, grown to its repeated slice's most: however it stands, no place is farther than that. */
    @Override
    public int reach() {
        final int[] size = {sizeX(), sizeY(), sizeZ()};
        if (this.repeat != null) {
            size[this.repeat.axis()] += this.repeat.times().most() - 1;
        }
        return Math.max(size[0], Math.max(size[1], size[2]));
    }

    /** The pattern as drawn, its repeated slice there its least times and {@code grow} more. */
    @Override
    public Optional<ShapePreview> preview(final int grow) {
        return Optional.of(ShapePreviews.pattern(this, Orientation.NORTH, grow));
    }

    /** What fits a letter's places: its place's blocks, or what the inside allows for the inside letter. */
    @Override
    public @Nullable IBlockMatcher wanted(final String wanted) {
        if (wanted.length() != 1) {
            return null;
        }
        final char letter = wanted.charAt(0);
        if (this.inside != null && this.inside.letter() == letter) {
            return this.inside.rule().allows();
        }
        final Place place = this.places.get(letter);
        return place == null ? null : place.fits();
    }

    /**
     * The pattern with its repeated slice there {@code times} times, and where its controller then stands: the
     * drawing itself for a pattern with no repeated slice.
     */
    Grid grown(final int times) {
        if (this.repeat == null) {
            return new Grid(this.layers, this.controllerX, this.controllerY, this.controllerZ);
        }
        final int[] size = {sizeX(), sizeY(), sizeZ()};
        final int extra = times - 1;
        size[this.repeat.axis()] += extra;
        final char[][][] grown = new char[size[1]][size[2]][size[0]];
        for (int y = 0; y < size[1]; y++) {
            for (int z = 0; z < size[2]; z++) {
                for (int x = 0; x < size[0]; x++) {
                    final int[] from = {x, y, z};
                    final int axis = this.repeat.axis();
                    if (from[axis] > this.repeat.index()) {
                        from[axis] = Math.max(this.repeat.index(), from[axis] - extra);
                    }
                    grown[y][z][x] = this.layers[from[1]][from[2]][from[0]];
                }
            }
        }
        final int[] controller = {this.controllerX, this.controllerY, this.controllerZ};
        if (controller[this.repeat.axis()] > this.repeat.index()) {
            controller[this.repeat.axis()] += extra;
        }
        return new Grid(grown, controller[0], controller[1], controller[2]);
    }

    /**
     * A slice of the pattern repeated a number of times.
     *
     * @param axis  the pattern's axis it lies across ({@link #AXIS_X}, {@link #AXIS_Y}, {@link #AXIS_Z})
     * @param index the slice's place along that axis
     * @param times the least and the most times it is there
     */
    public record Repeat(int axis, int index, Limit times) {

        /** The most times a slice may be repeated: more than the world is high. */
        public static final int MOST_TIMES = 512;

        public Repeat {
            Objects.requireNonNull(times, "times");
            if (axis < AXIS_X || axis > AXIS_Z || index < 0 || times.least() < 1 || times.most() > MOST_TIMES) {
                throw new IllegalArgumentException("a repeated slice lies across x, y or z, at zero or further, and is"
                        + " there from once to " + MOST_TIMES + " times");
            }
        }
    }

    /**
     * A pattern's inside.
     *
     * @param letter the letter of its places
     * @param rule   what may stand there and what it gives
     */
    public record Inside(char letter, InsideRule rule) {

        public Inside {
            Objects.requireNonNull(rule, "rule");
        }
    }

    /** A pattern's places, grown to a number of repeats, with its controller's place. */
    record Grid(char[][][] cells, int controllerX, int controllerY, int controllerZ) {
    }

    /**
     * DSL for declarative pattern construction.
     */
    public static final class Builder {
        private final String name;
        private final List<String[]> layers = new ArrayList<>();
        private final Map<Character, Place> places = new HashMap<>();
        private final Map<Character, PortKind> ports = new HashMap<>();
        private final Map<String, Limit> limits = new LinkedHashMap<>();
        private boolean mirrors;
        private boolean liesDown;
        @Nullable
        private Repeat repeat;
        @Nullable
        private Inside inside;
        @Nullable
        private String controllerBlock;

        private Builder(String name) {
            this.name = Objects.requireNonNull(name, "name must not be null");
        }

        public Builder layer(String... rows) {
            Objects.requireNonNull(rows, "rows must not be null");
            if (rows.length == 0) {
                throw new IllegalArgumentException("Layer must have at least one row");
            }
            int width = rows[0].length();
            if (width == 0) {
                throw new IllegalArgumentException("Rows must have at least one column");
            }
            for (String row : rows) {
                if (row.length() != width) {
                    throw new IllegalArgumentException(
                            "Inconsistent row width: expected " + width + ", got " + row.length()
                                    + " in row '" + row + "'");
                }
            }
            // First layer fixes the (sizeZ, sizeX) shape.
            if (!layers.isEmpty()) {
                String[] first = layers.get(0);
                if (rows.length != first.length || width != first[0].length()) {
                    throw new IllegalArgumentException(
                            "All layers must have the same dimensions; first layer is "
                                    + first[0].length() + "x" + first.length
                                    + " but new layer is " + width + "x" + rows.length);
                }
            }
            layers.add(rows);
            return this;
        }

        public Builder where(char c, IBlockMatcher matcher) {
            Objects.requireNonNull(matcher, "matcher must not be null");
            return where(c, Place.of(matcher));
        }

        /** Makes {@code c} the place {@code place}: what fits there, and what it counts and gives. */
        public Builder where(final char c, final Place place) {
            Objects.requireNonNull(place, "place must not be null");
            requireFree(c);
            if (isInside(c)) {
                throw new IllegalArgumentException("Char '" + c + "' is the inside and cannot be a place too");
            }
            places.put(c, place);
            return this;
        }

        /**
         * Makes every slot of {@code c} a port of that kind: the block standing there still has to fit the slot, and
         * the formed machine is reached through it.
         */
        public Builder port(char c, PortKind kind) {
            Objects.requireNonNull(kind, "kind must not be null");
            if (c == CONTROLLER_CHAR || c == IGNORE_CHAR) {
                throw new IllegalArgumentException("Char '" + c + "' is reserved and cannot be a port");
            }
            ports.put(c, kind);
            return this;
        }

        /** Keeps the count {@code count} between {@code least} and {@code most}, both counted in. */
        public Builder limit(final String count, final int least, final int most) {
            limits.put(Objects.requireNonNull(count, "count must not be null"), new Limit(least, most));
            return this;
        }

        /** Names the block that stands at {@code #}, by id, for the views that show it before it is built. */
        public Builder controller(final String block) {
            this.controllerBlock = Objects.requireNonNull(block, "block must not be null");
            return this;
        }

        /** Finds it mirrored too, for a shape that is not symmetric. */
        public Builder mirrors() {
            this.mirrors = true;
            return this;
        }

        /** Finds it lying on its side too. */
        public Builder liesDown() {
            this.liesDown = true;
            return this;
        }

        /** Repeats the column {@code x}, west to east from zero, from {@code least} to {@code most} times. */
        public Builder repeatColumn(final int x, final int least, final int most) {
            return repeat(new Repeat(AXIS_X, x, new Limit(least, most)));
        }

        /** Repeats the layer {@code y}, from the bottom, from {@code least} to {@code most} times. */
        public Builder repeatLayer(final int y, final int least, final int most) {
            return repeat(new Repeat(AXIS_Y, y, new Limit(least, most)));
        }

        /** Repeats the row {@code z}, north to south, from {@code least} to {@code most} times. */
        public Builder repeatRow(final int z, final int least, final int most) {
            return repeat(new Repeat(AXIS_Z, z, new Limit(least, most)));
        }

        /** Repeats a slice; a pattern repeats one at most. */
        public Builder repeat(final Repeat slice) {
            if (this.repeat != null) {
                throw new IllegalStateException("a pattern repeats one slice at most");
            }
            this.repeat = Objects.requireNonNull(slice, "slice must not be null");
            return this;
        }

        /** Makes {@code c}'s places the inside, read by {@code rule}; a pattern has one inside letter at most. */
        public Builder inside(final char c, final InsideRule rule) {
            Objects.requireNonNull(rule, "rule must not be null");
            requireFree(c);
            if (places.containsKey(c)) {
                throw new IllegalArgumentException("Char '" + c + "' is a place and cannot be the inside too");
            }
            rule.requireKnownParts();
            if (this.inside != null) {
                throw new IllegalStateException("a pattern has one inside letter at most");
            }
            this.inside = new Inside(c, rule);
            return this;
        }

        public MultiblockPattern build() {
            if (layers.isEmpty()) {
                throw new IllegalStateException("Pattern must have at least one layer");
            }
            int sizeY = layers.size();
            int sizeZ = layers.get(0).length;
            int sizeX = layers.get(0)[0].length();

            // Convert to 3D char array.
            char[][][] grid = new char[sizeY][sizeZ][sizeX];
            int controllerX = -1, controllerY = -1, controllerZ = -1;
            int controllerCount = 0;
            var unmappedChars = new HashSet<Character>();

            for (int y = 0; y < sizeY; y++) {
                String[] layer = layers.get(y);
                for (int z = 0; z < sizeZ; z++) {
                    String row = layer[z];
                    for (int x = 0; x < sizeX; x++) {
                        char c = row.charAt(x);
                        grid[y][z][x] = c;
                        if (c == CONTROLLER_CHAR) {
                            controllerCount++;
                            controllerX = x;
                            controllerY = y;
                            controllerZ = z;
                        } else if (c != IGNORE_CHAR && !places.containsKey(c) && !isInside(c)) {
                            unmappedChars.add(c);
                        }
                    }
                }
            }

            if (controllerCount == 0) {
                throw new IllegalStateException(
                        "Pattern has no controller (no '" + CONTROLLER_CHAR + "' char)");
            }
            if (controllerCount > 1) {
                throw new IllegalStateException(
                        "Pattern has " + controllerCount + " controllers; exactly one '"
                                + CONTROLLER_CHAR + "' is required");
            }
            if (!unmappedChars.isEmpty()) {
                throw new IllegalStateException(
                        "Pattern contains unmapped chars: " + unmappedChars
                                + ". Call .where(c, matcher) for each");
            }
            for (final char port : ports.keySet()) {
                if (!places.containsKey(port)) {
                    throw new IllegalStateException("Port char '" + port + "' has no matcher; call .where for it");
                }
            }
            requireCounted();
            requireOneGatherPerKey();
            if (repeat != null) {
                final int[] size = {sizeX, sizeY, sizeZ};
                final int[] controller = {controllerX, controllerY, controllerZ};
                if (repeat.index() >= size[repeat.axis()]) {
                    throw new IllegalStateException("The repeated slice " + repeat.index() + " is outside the pattern,"
                            + " which is " + size[repeat.axis()] + " across");
                }
                if (controller[repeat.axis()] == repeat.index()) {
                    throw new IllegalStateException("The controller stands in the repeated slice; a structure has one"
                            + " controller");
                }
            }
            return new MultiblockPattern(this, grid, controllerX, controllerY, controllerZ);
        }

        private boolean isInside(final char c) {
            return inside != null && inside.letter() == c;
        }

        /* Refuses the two letters a pattern keeps for itself. */
        private static void requireFree(final char c) {
            if (c == CONTROLLER_CHAR || c == IGNORE_CHAR) {
                throw new IllegalArgumentException(
                        "Char '" + c + "' is reserved (controller or ignore) "
                                + "and must not be mapped");
            }
        }

        /* Every limit names a count some place or inside part keeps, so a misspelt name does not go unnoticed. */
        private void requireCounted() {
            final Set<String> counted = new HashSet<>();
            for (final Place place : places.values()) {
                if (place.counted() != null) {
                    counted.add(place.counted().name());
                }
            }
            if (inside != null) {
                for (final InsidePart part : inside.rule().parts()) {
                    counted.add(part.name());
                }
            }
            for (final String count : limits.keySet()) {
                if (!counted.contains(count)) {
                    throw new IllegalStateException("The limit " + count + " names a count no place keeps");
                }
            }
        }

        /* Every number of one key is gathered the same way, the least of them or their sum. */
        private void requireOneGatherPerKey() {
            final Map<String, Place.Gather> gathers = new HashMap<>();
            for (final Place place : places.values()) {
                final Place.Valued valued = place.valued();
                if (valued == null) {
                    continue;
                }
                final Place.Gather before = gathers.putIfAbsent(valued.key(), valued.how());
                if (before != null && before != valued.how()) {
                    throw new IllegalStateException("The number " + valued.key() + " is gathered two ways");
                }
            }
        }
    }
}
