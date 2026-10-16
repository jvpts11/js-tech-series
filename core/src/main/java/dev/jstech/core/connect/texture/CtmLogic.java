/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.connect.texture;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.jstech.core.connect.ConnectedQuadrants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * A logic of connected textures written as data, as CTM reads its {@code ctm_logic} files: named neighbours, the
 * cells of its textures, the cells a face is drawn in, and rules, each the cell of a texture a cell of the face takes
 * when the neighbours it names are joined and the others it names are not; the first rule that holds wins. A texture
 * names one by its type ({@code "type": "ctm:optifine_full"}). The Core brings OptiFine's full layout as one, made from
 * {@link OptifineLayouts}; packs may write their own. Pure.
 *
 * @param cells     the cells of its textures by name, each a share of its texture
 * @param faceCells the cells a face is drawn in by name; none for the whole face, named {@code ""}
 * @param rules     its rules, in order
 */
public record CtmLogic(Map<String, Region> cells, Map<String, Region> faceCells, List<Rule> rules) {

    /** OptiFine's full layout in one texture: a sheet of twelve by four tiles, its 47 cases in order. */
    public static final String OPTIFINE_FULL = "ctm:optifine_full";

    private static final String WHOLE = "";
    private static final int SHEET_WIDTH = 12;
    private static final int SHEET_HEIGHT = 4;
    private static final float PIXELS = 16.0F;
    private static final int EDGES = ConnectedQuadrants.UP | ConnectedQuadrants.RIGHT | ConnectedQuadrants.DOWN
            | ConnectedQuadrants.LEFT;
    private static final Map<String, Integer> DIRECTIONS = Map.of("up", ConnectedQuadrants.UP, "down",
            ConnectedQuadrants.DOWN, "east", ConnectedQuadrants.RIGHT, "west", ConnectedQuadrants.LEFT, "right",
            ConnectedQuadrants.RIGHT, "left", ConnectedQuadrants.LEFT);

    public CtmLogic {
        cells = Map.copyOf(cells);
        // The face's cells keep the order they were written in, so a face is drawn cell by cell in that order.
        faceCells = Collections.unmodifiableMap(new LinkedHashMap<>(faceCells));
        rules = List.copyOf(rules);
    }

    /** The cells of a face joined as {@code mask} take: for each cell of the face, the first rule that holds. */
    public List<Pick> pick(final int mask) {
        final List<Pick> out = new ArrayList<>();
        final Map<String, Region> faces = this.faceCells.isEmpty() ? Map.of(WHOLE, Region.WHOLE) : this.faceCells;
        for (final Map.Entry<String, Region> face : faces.entrySet()) {
            for (final Rule rule : this.rules) {
                if (rule.at().equals(face.getKey()) && (mask & rule.connected()) == rule.connected()
                        && (mask & rule.unconnected()) == 0) {
                    final Region cell = this.cells.get(rule.output());
                    if (cell != null) {
                        out.add(new Pick(face.getValue(), rule.from(), cell));
                    }
                    break;
                }
            }
        }
        return out;
    }

    /** OptiFine's full layout as a logic: a face takes the tile of its case from a sheet of twelve by four. */
    public static CtmLogic optifineFull() {
        final Map<String, Region> cells = grid(WHOLE, SHEET_WIDTH, SHEET_HEIGHT);
        final List<Rule> rules = new ArrayList<>();
        for (int tile = 0; tile < OptifineLayouts.FULL_TILES; tile++) {
            final int joined = OptifineLayouts.maskOfFull(tile);
            int apart = EDGES & ~joined;
            // A corner counts only where both edges beside it are joined; there, one not joined is apart.
            apart |= cornerApart(joined, ConnectedQuadrants.UP, ConnectedQuadrants.RIGHT, ConnectedQuadrants.UP_RIGHT);
            apart |= cornerApart(joined, ConnectedQuadrants.DOWN, ConnectedQuadrants.RIGHT,
                    ConnectedQuadrants.DOWN_RIGHT);
            apart |= cornerApart(joined, ConnectedQuadrants.DOWN, ConnectedQuadrants.LEFT,
                    ConnectedQuadrants.DOWN_LEFT);
            apart |= cornerApart(joined, ConnectedQuadrants.UP, ConnectedQuadrants.LEFT, ConnectedQuadrants.UP_LEFT);
            rules.add(new Rule((tile % SHEET_WIDTH) + "," + (tile / SHEET_WIDTH), 0, WHOLE, joined, apart));
        }
        return new CtmLogic(cells, Map.of(), rules);
    }

    /**
     * The logic a {@code ctm_logic} file {@code json} says.
     *
     * @throws IllegalArgumentException when it names a neighbour, a cell or a kind of cells it does not declare
     */
    public static CtmLogic parse(final JsonObject json) {
        final Map<String, Integer> positions = new HashMap<>();
        for (final JsonElement element : json.getAsJsonArray("positions")) {
            final JsonObject position = element.getAsJsonObject();
            int bits = 0;
            for (final JsonElement direction : position.getAsJsonArray("directions")) {
                bits |= direction(direction.getAsString());
            }
            positions.put(position.get("id").getAsString(), combined(bits));
        }
        final Map<String, Region> cells = regions(json.getAsJsonObject("submaps"));
        final Map<String, Region> faces = json.has("faces") ? regions(json.getAsJsonObject("faces")) : Map.of();
        final List<Rule> rules = new ArrayList<>();
        for (final JsonElement element : json.getAsJsonArray("rules")) {
            final JsonObject rule = element.getAsJsonObject();
            rules.add(new Rule(rule.get("output").getAsString(), rule.has("from") ? rule.get("from").getAsInt() : 0,
                    rule.has("at") ? rule.get("at").getAsString() : WHOLE, bits(rule, "connected", positions),
                    bits(rule, "unconnected", positions)));
        }
        return new CtmLogic(cells, faces, rules);
    }

    private static int cornerApart(final int joined, final int edge, final int other, final int corner) {
        return (joined & edge) != 0 && (joined & other) != 0 && (joined & corner) == 0 ? corner : 0;
    }

    private static int bits(final JsonObject rule, final String key, final Map<String, Integer> positions) {
        int bits = 0;
        if (rule.has(key)) {
            for (final JsonElement name : rule.getAsJsonArray(key)) {
                final Integer position = positions.get(name.getAsString());
                if (position == null) {
                    throw new IllegalArgumentException("a rule names a neighbour its logic does not: " + name);
                }
                bits |= position;
            }
        }
        return bits;
    }

    private static int direction(final String word) {
        final Integer bit = DIRECTIONS.get(word.toLowerCase(Locale.ROOT));
        if (bit == null) {
            throw new IllegalArgumentException("a neighbour lies a way no face has: " + word);
        }
        return bit;
    }

    /* The neighbour two ways name together: up and right is the corner between them. */
    private static int combined(final int bits) {
        final boolean up = (bits & ConnectedQuadrants.UP) != 0;
        final boolean down = (bits & ConnectedQuadrants.DOWN) != 0;
        final boolean right = (bits & ConnectedQuadrants.RIGHT) != 0;
        final boolean left = (bits & ConnectedQuadrants.LEFT) != 0;
        if (up && right) {
            return ConnectedQuadrants.UP_RIGHT;
        }
        if (down && right) {
            return ConnectedQuadrants.DOWN_RIGHT;
        }
        if (down && left) {
            return ConnectedQuadrants.DOWN_LEFT;
        }
        if (up && left) {
            return ConnectedQuadrants.UP_LEFT;
        }
        return bits;
    }

    private static Map<String, Region> regions(final JsonObject submaps) {
        final Map<String, Region> out = new LinkedHashMap<>();
        for (final Map.Entry<String, JsonElement> entry : submaps.entrySet()) {
            final JsonObject submap = entry.getValue().getAsJsonObject();
            final String type = submap.has("type") ? submap.get("type").getAsString() : "unit";
            switch (type) {
                case "grid" -> out.putAll(grid(entry.getKey(), submap.get("width").getAsInt(),
                        submap.get("height").getAsInt()));
                case "unit", "pixel" -> {
                    final float scale = "pixel".equals(type) ? 1.0F / PIXELS : 1.0F;
                    out.put(entry.getKey(), new Region(submap.get("x_offset").getAsFloat() * scale,
                            submap.get("y_offset").getAsFloat() * scale, submap.get("width").getAsFloat() * scale,
                            submap.get("height").getAsFloat() * scale));
                }
                default -> throw new IllegalArgumentException("cells of a kind no logic knows: " + type);
            }
        }
        return out;
    }

    /* A grid's cells, named as CTM names them: the grid's name, then the column and the row. */
    private static Map<String, Region> grid(final String name, final int width, final int height) {
        final Map<String, Region> out = new LinkedHashMap<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                out.put(name + x + "," + y, new Region((float) x / width, (float) y / height, 1.0F / width,
                        1.0F / height));
            }
        }
        return out;
    }

    /**
     * A share of a texture or of a face, from its left and top.
     *
     * @param x      where it starts across, from 0 to 1
     * @param y      where it starts down
     * @param width  how wide it is
     * @param height how tall
     */
    public record Region(float x, float y, float width, float height) {

        /** The whole of it. */
        public static final Region WHOLE = new Region(0, 0, 1, 1);
    }

    /**
     * A rule: the cell {@code output} of texture {@code from} for the face's cell {@code at}, while the neighbours of
     * {@code connected} are joined and those of {@code unconnected} are not.
     *
     * @param output      the texture's cell, by name
     * @param from        which texture: 0 the texture itself, 1 its first companion, and so on
     * @param at          the face's cell, by name
     * @param connected   the neighbours that must be joined, as a mask
     * @param unconnected those that must not
     */
    public record Rule(String output, int from, String at, int connected, int unconnected) {

        public Rule {
            Objects.requireNonNull(output, "output");
            Objects.requireNonNull(at, "at");
        }
    }

    /**
     * What a cell of a face takes.
     *
     * @param face    the cell of the face
     * @param texture which texture: 0 the texture itself, 1 its first companion, and so on
     * @param cell    the cell of that texture
     */
    public record Pick(Region face, int texture, Region cell) {
    }
}
