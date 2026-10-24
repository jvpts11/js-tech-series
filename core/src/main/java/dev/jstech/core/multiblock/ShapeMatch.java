/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * What checking a structure in the world found: formed, with all it is made of and what that gives, or not, with the
 * first place that is wrong. Places are packed into one number each ({@link PatternMatcher#encodePosition}). Pure.
 */
public sealed interface ShapeMatch permits ShapeMatch.Formed, ShapeMatch.Failure {

    /** What a shape that is a box, a floor and walls or a cuboid calls the places it checks, in a failure. */
    String FRAME = "frame";
    /** The faces of a box, between its edges. */
    String FACE = "face";
    /** The inside of a shape. */
    String INSIDE = "inside";
    /** The floor under a floor and walls. */
    String FLOOR = "floor";
    /** The walls round a floor. */
    String WALL = "wall";
    /** A unit of a cuboid. */
    String UNIT = "unit";
    /** A size along the world's x, in a failure of the size. */
    String SIZE_X = "x";
    /** The height. */
    String SIZE_Y = "y";
    /** A size along z. */
    String SIZE_Z = "z";
    /** The area of a floor. */
    String AREA = "area";

    /**
     * A formed structure.
     *
     * @param orientation how it stands, for a drawn pattern; north for a shape by rule
     * @param terminal  its terminal's place
     * @param parts       the places of its shell but the terminal's: where ports may stand as blocks of their own
     * @param ports       the places a pattern makes ports, and those holding entries, with what each opens onto
     * @param inside      the places inside it, empty ones too
     * @param sizeX       its size along the world's x, in blocks
     * @param sizeY       its height
     * @param sizeZ       its size along z
     * @param repeats     the times a pattern's repeated slice is there; one for a shape with none
     * @param counts      how many blocks of each count it has
     * @param values      the numbers its blocks give, each gathered into one, by key
     */
    record Formed(Orientation orientation, long terminal, List<Long> parts, Map<Long, PortKind> ports,
                  List<Long> inside, int sizeX, int sizeY, int sizeZ, int repeats, Map<String, Integer> counts,
                  Map<String, Double> values) implements ShapeMatch {

        public Formed {
            Objects.requireNonNull(orientation, "orientation");
            parts = List.copyOf(parts);
            ports = Map.copyOf(ports);
            inside = List.copyOf(inside);
            counts = Map.copyOf(counts);
            values = Map.copyOf(values);
        }

        /** How many places its shell has, its terminal's with them. */
        public int blocks() {
            return this.parts.size() + 1;
        }

        /** The count of {@code name}, or none. */
        public int count(final String name) {
            return this.counts.getOrDefault(name, 0);
        }

        /** The number of {@code key}, or {@code otherwise} when it has none. */
        public double value(final String key, final double otherwise) {
            return this.values.getOrDefault(key, otherwise);
        }
    }

    /**
     * Not formed.
     *
     * @param wrong  what is wrong
     * @param at     the first place that is wrong, or the terminal's for a count or a size
     * @param relX   that place's offset from the terminal along x
     * @param relY   along y
     * @param relZ   along z
     * @param wanted for a block, what goes there: a pattern's letter, or the part of a shape ({@link #FRAME},
     *               {@link #WALL}...); for a count, its name; for a size, which ({@link #SIZE_X}, {@link #AREA}...)
     * @param found  for a block, the one found there, by id; for a count or a size, the number found
     */
    record Failure(Wrong wrong, long at, int relX, int relY, int relZ, String wanted, String found)
            implements ShapeMatch {

        public Failure {
            Objects.requireNonNull(wrong, "wrong");
            Objects.requireNonNull(wanted, "wanted");
            Objects.requireNonNull(found, "found");
        }
    }

    /** What is wrong with a structure that does not form. */
    enum Wrong {
        /** A place holds a block that does not go there. */
        BLOCK,
        /** A count is below its least or above its most. */
        COUNT,
        /** The shape is smaller or larger than it may be. */
        SIZE,
        /**
         * Two counts that go together, as a coolant's inlet and its outlet, differ: {@code wanted} names the first and
         * {@code found} the second.
         */
        APART
    }
}
