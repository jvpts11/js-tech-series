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
 * One connected texture as a resource pack writes it for CTM: the {@code ctm} section of a texture's {@code .mcmeta}
 * file, or of a model's {@code ctm_overrides} for the faces of one tint index. Read by {@link CtmTextures}; pure, its
 * ids kept as text.
 *
 * @param type          how it is drawn
 * @param layer         the layer it is drawn in ({@code SOLID}, {@code CUTOUT}, {@code CUTOUT_MIPPED},
 *                      {@code TRANSLUCENT}); empty for its model's own
 * @param textures      its companions, the textures its type takes besides itself, as ids
 * @param proxy         a texture drawn, with what it says, in its place; empty for itself
 * @param texture       for a model's override, the texture drawn in place of the face's; empty for the face's own
 * @param light         how bright it glows at least, its block light and sky light from 0 to 15; none for not at all
 * @param grid          for a pattern or random type, its grid
 * @param ignoreStates  whether it joins every state of its block, not only its own
 * @param connectInside whether it joins a neighbour whose face is covered by another of itself; empty for its type's
 * @param connectTo     what it joins, or for the edges types what it changes for; empty for its own block
 * @param logic         for a type that is a logic written as data ({@link CtmLogic}), its id; empty for the rest
 */
public record CtmTexture(Type type, Optional<String> layer, List<String> textures, Optional<String> proxy,
                         Optional<String> texture, Optional<Light> light, Grid grid, boolean ignoreStates,
                         Optional<Boolean> connectInside, Optional<ConnectTo> connectTo, Optional<String> logic) {

    public CtmTexture {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(logic, "logic");
        Objects.requireNonNull(layer, "layer");
        textures = List.copyOf(textures);
        Objects.requireNonNull(proxy, "proxy");
        Objects.requireNonNull(texture, "texture");
        Objects.requireNonNull(light, "light");
        Objects.requireNonNull(grid, "grid");
        Objects.requireNonNull(connectInside, "connectInside");
        Objects.requireNonNull(connectTo, "connectTo");
    }

    /** Whether it joins a neighbour whose face is covered, as it says or as its type does. */
    public boolean joinsInside() {
        return this.connectInside.orElse(this.type == Type.SIMPLE);
    }

    /** The types a texture can be drawn as. */
    public enum Type {
        /** As it is. */
        NORMAL(0, "normal"),
        /** Joined to its like, a quarter of a face at a time, from itself and one companion. */
        CTM(1, "ctm"),
        /** Joined sideways only. */
        HORIZONTAL(0, "ctm_horizontal", "ctmh"),
        /** Joined up and down only. */
        VERTICAL(0, "ctm_vertical"),
        /** A pillar, standing or lying as its like join it, its ends itself and its sides from its companion. */
        PILLAR(1, "pillar", "ctmv"),
        /** A tile of a grid at random. */
        RANDOM(0, "random", "r"),
        /** A tile of a grid laid over the world as a pattern. */
        PATTERN(0, "pattern", "v"),
        /** Changed where the blocks it changes for meet it, a quarter at a time, and wholly where one covers it. */
        EDGES(2, "edges"),
        /** Changed as {@link #EDGES}, a whole face at a time from a grid of four by four. */
        EDGES_FULL(1, "edges_full"),
        /** Its quarters' meeting point moved a little at random, so its face is warped. */
        ELDRITCH(0, "eldritch"),
        /** Joined as a simple grid of two by two. */
        SIMPLE(0, "ctm_simple", "sctm"),
        /** Joined as a logic written as data says, {@link CtmLogic}, named by its id as the type. */
        LOGIC(0);

        private final int companions;
        private final List<String> words;

        Type(final int companions, final String... words) {
            this.companions = companions;
            this.words = List.of(words);
        }

        /** What files call it. */
        public List<String> words() {
            return this.words;
        }

        /** How many companions it takes. */
        public int companions() {
            return this.companions;
        }

        /** Whether it is drawn by what its faces join, so its neighbours are looked at. */
        public boolean joins() {
            return this == CTM || this == HORIZONTAL || this == VERTICAL || this == PILLAR || this == EDGES
                    || this == EDGES_FULL || this == SIMPLE || this == LOGIC;
        }
    }

    /**
     * How bright a texture glows at least.
     *
     * @param block its block light, from 0 to 15
     * @param sky   its sky light
     */
    public record Light(int block, int sky) {
    }

    /**
     * A texture laid out as a grid of tiles, for the pattern and random types.
     *
     * @param width   how many tiles across
     * @param height  how many down
     * @param offsetX how far the pattern is moved across, in tiles
     * @param offsetY how far down
     */
    public record Grid(int width, int height, int offsetX, int offsetY) {

        /** Two by two, unmoved, as a texture that says nothing is laid out. */
        public static final Grid TWO = new Grid(2, 2, 0, 0);
    }

    /**
     * What a texture joins: conditions for every face, and for some faces their own.
     *
     * @param conditions the conditions for every face; a block joins if it meets any
     * @param sides      the conditions of faces of their own, by the face's name
     */
    public record ConnectTo(List<Condition> conditions, Map<String, Side> sides) {

        public ConnectTo {
            conditions = List.copyOf(conditions);
            sides = Map.copyOf(sides);
        }
    }

    /**
     * A face's own conditions, and how they meet the conditions for every face.
     *
     * @param conditions its conditions
     * @param defer      {@code and} or {@code or} to meet the conditions for every face as well or instead; empty for
     *                   its own in their place
     */
    public record Side(List<Condition> conditions, Optional<String> defer) {

        public Side {
            conditions = List.copyOf(conditions);
            Objects.requireNonNull(defer, "defer");
        }
    }

    /**
     * A block a texture joins: by its id or a tag, and its properties' values.
     *
     * @param block      the block's id, or the tag's
     * @param tag        whether {@code block} is a tag's
     * @param predicates what its properties must be; all of them
     */
    public record Condition(String block, boolean tag, List<Predicate> predicates) {

        public Condition {
            Objects.requireNonNull(block, "block");
            predicates = List.copyOf(predicates);
        }
    }

    /**
     * What one property of a block must be.
     *
     * @param property the property's name
     * @param value    the value it is compared with
     * @param compare  how: {@code =}, {@code !=}, {@code >}, {@code <}, {@code >=} or {@code <=}
     */
    public record Predicate(String property, String value, String compare) {

        public Predicate {
            Objects.requireNonNull(property, "property");
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(compare, "compare");
        }

        /** Whether a property's value {@code actual} meets it; numbers compare as numbers, the rest as text. */
        public boolean meets(final String actual) {
            final int order = order(actual, this.value);
            return switch (this.compare) {
                case "!=" -> order != 0;
                case ">" -> order > 0;
                case "<" -> order < 0;
                case ">=" -> order >= 0;
                case "<=" -> order <= 0;
                default -> order == 0;
            };
        }

        private static int order(final String a, final String b) {
            try {
                return Double.compare(Double.parseDouble(a), Double.parseDouble(b));
            } catch (final NumberFormatException e) {
                return a.compareTo(b);
            }
        }
    }
}
