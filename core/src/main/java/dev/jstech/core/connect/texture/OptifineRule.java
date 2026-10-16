/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.connect.texture;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * One connected texture as a resource pack writes it for OptiFine and Continuity, a {@code .properties} file under
 * {@code optifine/ctm}: what it is drawn on, by which method, from which tiles, and when. Read by
 * {@link OptifineRules}; pure, its ids and paths kept as text.
 *
 * @param file        the file it was read from, {@code namespace:path}, for messages and to order the rules
 * @param method      how its tiles are picked
 * @param tiles       its tiles, in order
 * @param matchTiles  the textures it is drawn on, as {@code namespace:path} ids; none for a rule by blocks
 * @param matchBlocks the blocks it is drawn on; none for a rule by textures
 * @param connect     what a neighbour must share to join
 * @param connectTo   the blocks a face joins, or an overlay is laid for, besides its own; none for the method's own
 * @param connectTiles the textures a face joins, or an overlay is laid for, on the neighbour's face
 * @param faces       the faces it is drawn on, a bit for each face by its index (down, up, north, south, west, east)
 * @param where       the biomes and heights it is drawn in
 * @param weight      which of two rules for one face wins, the higher
 * @param innerSeams  whether a face shows a seam where it meets a neighbour whose face is covered
 * @param random      how a random or repeating method picks
 * @param overlay     how an overlay is drawn
 * @param overrides   for {@code ctm_compact}, the tile drawn whole for a case of the 47 of {@code ctm}, by that case
 * @param name        for a block that can be named, the name it must have; empty for any
 */
public record OptifineRule(String file, Method method, List<Tile> tiles, List<String> matchTiles,
                           List<BlockFilter> matchBlocks, Connect connect, List<BlockFilter> connectTo,
                           List<String> connectTiles, int faces, Where where, int weight, boolean innerSeams,
                           Random random, Overlay overlay, Map<Integer, Integer> overrides, String name) {

    /** Every face. */
    public static final int ALL_FACES = 0b11_1111;
    /** The four faces around a block. */
    public static final int SIDE_FACES = 0b11_1100;

    public OptifineRule {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(method, "method");
        tiles = List.copyOf(tiles);
        matchTiles = List.copyOf(matchTiles);
        matchBlocks = List.copyOf(matchBlocks);
        connectTo = List.copyOf(connectTo);
        connectTiles = List.copyOf(connectTiles);
        overrides = Map.copyOf(overrides);
        Objects.requireNonNull(where, "where");
        Objects.requireNonNull(random, "random");
        Objects.requireNonNull(overlay, "overlay");
        Objects.requireNonNull(name, "name");
    }

    /** Whether it is drawn on textures, else on blocks. */
    public boolean byTiles() {
        return !this.matchTiles.isEmpty();
    }

    /** Whether it is drawn on the face of index {@code face}. */
    public boolean onFace(final int face) {
        return (this.faces & (1 << face)) != 0;
    }

    /** The methods of picking a tile. */
    public enum Method {
        CTM("ctm", OptifineLayouts.FULL_TILES),
        COMPACT("ctm_compact", OptifineLayouts.COMPACT_TILES),
        HORIZONTAL("horizontal", OptifineLayouts.LINE_TILES),
        VERTICAL("vertical", OptifineLayouts.LINE_TILES),
        HORIZONTAL_VERTICAL("horizontal+vertical", OptifineLayouts.LINES_TILES),
        VERTICAL_HORIZONTAL("vertical+horizontal", OptifineLayouts.LINES_TILES),
        TOP("top", 1),
        RANDOM("random", 1),
        REPEAT("repeat", 1),
        FIXED("fixed", 1),
        OVERLAY("overlay", OptifineLayouts.OVERLAY_TILES),
        OVERLAY_CTM("overlay_ctm", OptifineLayouts.FULL_TILES),
        OVERLAY_RANDOM("overlay_random", 1),
        OVERLAY_REPEAT("overlay_repeat", 1),
        OVERLAY_FIXED("overlay_fixed", 1);

        private final String word;
        private final int tiles;

        Method(final String word, final int tiles) {
            this.word = word;
            this.tiles = tiles;
        }

        /** What a file calls it. */
        public String word() {
            return this.word;
        }

        /** The fewest tiles it takes. */
        public int tiles() {
            return this.tiles;
        }

        /** Whether it lays tiles over a face rather than in place of its texture. */
        public boolean overlays() {
            return this == OVERLAY || this == OVERLAY_CTM || this == OVERLAY_RANDOM || this == OVERLAY_REPEAT
                    || this == OVERLAY_FIXED;
        }

        /** Whether it picks by what a face joins, so its neighbours are looked at. */
        public boolean joins() {
            return !(this == RANDOM || this == REPEAT || this == FIXED || this == OVERLAY_RANDOM
                    || this == OVERLAY_REPEAT || this == OVERLAY_FIXED);
        }
    }

    /** What a neighbour must share with a block to join it. */
    public enum Connect {
        /** Its block. */
        BLOCK("block"),
        /** The texture on its face. */
        TILE("tile"),
        /** Its whole state. */
        STATE("state");

        private final String word;

        Connect(final String word) {
            this.word = word;
        }

        /** What a file calls it. */
        public String word() {
            return this.word;
        }
    }

    /**
     * A tile of a rule: a file by its index or name in the rule's folder, a file by its whole path, nothing, or the
     * texture the face had.
     *
     * @param kind  which of those it is
     * @param value the index, the name or the path; empty for nothing or the texture the face had
     */
    public record Tile(TileKind kind, String value) {

        public Tile {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(value, "value");
        }
    }

    /** What a tile is. */
    public enum TileKind {
        /** A file in the rule's folder, by its index or name: {@code 3}, {@code glass}, {@code glass.png}. */
        IN_FOLDER,
        /** A file by its whole path, with or without its namespace. */
        PATH,
        /** Nothing: the face is drawn without this tile. */
        SKIP,
        /** The texture the face had. */
        DEFAULT
    }

    /**
     * A block a rule is drawn on or joins: its id, or a tag of blocks, and the values its properties must have.
     *
     * @param id         the block's id, {@code namespace:path}, or the tag's
     * @param tag        whether {@code id} is a tag's
     * @param properties for each property named, the values it may have
     */
    public record BlockFilter(String id, boolean tag, Map<String, List<String>> properties) {

        public BlockFilter {
            Objects.requireNonNull(id, "id");
            properties = Map.copyOf(properties);
        }
    }

    /**
     * Where a rule is drawn: in which biomes, and at which heights.
     *
     * @param biomes   the biomes, as ids; none for all
     * @param inverted whether it is drawn everywhere but in those biomes
     * @param heights  the ranges of heights; none for every height
     */
    public record Where(List<String> biomes, boolean inverted, List<Heights> heights) {

        /** Anywhere. */
        public static final Where ANYWHERE = new Where(List.of(), false, List.of());

        public Where {
            biomes = List.copyOf(biomes);
            heights = List.copyOf(heights);
        }

        /** Whether a block at height {@code y} is within its heights. */
        public boolean atHeight(final int y) {
            if (this.heights.isEmpty()) {
                return true;
            }
            for (final Heights range : this.heights) {
                if (y >= range.lowest() && y <= range.highest()) {
                    return true;
                }
            }
            return false;
        }

        /** Whether it is drawn in the biome {@code biome}. */
        public boolean inBiome(final String biome) {
            return this.biomes.isEmpty() || this.biomes.contains(biome) != this.inverted;
        }
    }

    /**
     * A range of heights, both ends in it.
     *
     * @param lowest  its lowest height
     * @param highest its highest
     */
    public record Heights(int lowest, int highest) {
    }

    /**
     * How a random or repeating method picks a tile.
     *
     * @param weights  how likely each tile is, for {@code random}; none for all alike
     * @param loops    how many more times the place is mixed before picking, from 0 to 9
     * @param symmetry which faces pick alike ({@link TilePicks#EACH_FACE} and the rest)
     * @param linked   whether the halves of a two-block-high block pick alike, by its lower half
     * @param width    the pattern's width in tiles, for {@code repeat}
     * @param height   its height
     */
    public record Random(List<Integer> weights, int loops, int symmetry, boolean linked, int width, int height) {

        /** Each face for itself, with no weights. */
        public static final Random PLAIN = new Random(List.of(), 0, TilePicks.EACH_FACE, false, 1, 1);

        public Random {
            weights = List.copyOf(weights);
        }

        /** The weights as an array, one for each of {@code tiles}, alike when none were given. */
        public int[] weightsFor(final int tiles) {
            final int[] out = new int[tiles];
            for (int i = 0; i < tiles; i++) {
                out[i] = i < this.weights.size() ? this.weights.get(i) : (this.weights.isEmpty() ? 1 : 0);
            }
            return out;
        }
    }

    /**
     * How an overlay is drawn.
     *
     * @param tintIndex the tint it takes, as the block's colours give it; -1 for none
     * @param tintBlock the block whose colours tint it, by id; empty for the block it lies on
     * @param layer     the layer it is drawn in: {@code cutout} or {@code translucent}
     */
    public record Overlay(int tintIndex, Optional<String> tintBlock, String layer) {

        /** Untinted, cut out. */
        public static final Overlay PLAIN = new Overlay(-1, Optional.empty(), "cutout");

        public Overlay {
            Objects.requireNonNull(tintBlock, "tintBlock");
            Objects.requireNonNull(layer, "layer");
        }
    }
}
