/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

/**
 * The places of a block space, each numbered once and for all: the centre, where a cable's bundle runs; the six faces,
 * for covers, panels and the devices mounted on a cable; the twelve edges, for thin posts; and the eight corners. A
 * place holds one part at most. Faces are numbered as the game numbers its directions: down, up, north, south, west,
 * east. Pure: positions are given in the axes' own terms, never in the game's types.
 */
public final class SpaceSlots {

    /** Where a cable's bundle runs, or a post along an axis. */
    public static final int CENTRE = 0;
    /** How many places a space has. */
    public static final int COUNT = 27;
    /** The axes, in the order of a point's coordinates. */
    public static final int X = 0;
    public static final int Y = 1;
    public static final int Z = 2;
    private static final int FIRST_FACE = 1;
    private static final int FACES = 6;
    private static final int FIRST_EDGE = FIRST_FACE + FACES;
    private static final int EDGES = 12;
    private static final int FIRST_CORNER = FIRST_EDGE + EDGES;
    private static final int CORNERS = 8;
    /* The axis each face looks along, and whether toward its far end: down, up, north, south, west, east. */
    private static final int[] FACE_AXIS = {Y, Y, Z, Z, X, X};
    private static final boolean[] FACE_FAR = {false, true, false, true, false, true};

    private SpaceSlots() {
    }

    /** The place of the face numbered {@code face}, from 0 for down to 5 for east. */
    public static int face(final int face) {
        if (face < 0 || face >= FACES) {
            throw new IllegalArgumentException("a block has six faces, numbered 0 to 5; got " + face);
        }
        return FIRST_FACE + face;
    }

    /**
     * The place of the edge that runs along {@code axis}, at the near or far end of each of the other two axes, in
     * their order: along y, {@code first} is x's end and {@code second} z's.
     */
    public static int edge(final int axis, final boolean first, final boolean second) {
        requireAxis(axis);
        return FIRST_EDGE + axis * 4 + (first ? 1 : 0) + (second ? 2 : 0);
    }

    /** The place of the corner at the near or far end of each axis. */
    public static int corner(final boolean farX, final boolean farY, final boolean farZ) {
        return FIRST_CORNER + (farX ? 1 : 0) + (farY ? 2 : 0) + (farZ ? 4 : 0);
    }

    public static boolean isFace(final int slot) {
        return slot >= FIRST_FACE && slot < FIRST_EDGE;
    }

    public static boolean isEdge(final int slot) {
        return slot >= FIRST_EDGE && slot < FIRST_CORNER;
    }

    public static boolean isCorner(final int slot) {
        return slot >= FIRST_CORNER && slot < COUNT;
    }

    /** The face a face's place is, from 0 for down to 5 for east. */
    public static int faceOf(final int slot) {
        if (!isFace(slot)) {
            throw new IllegalArgumentException("place " + slot + " is not a face");
        }
        return slot - FIRST_FACE;
    }

    /** The axis a face looks along. */
    public static int faceAxis(final int face) {
        return FACE_AXIS[face];
    }

    /** Whether a face looks toward the far end of its axis: up, south, east. */
    public static boolean faceFar(final int face) {
        return FACE_FAR[face];
    }

    /** The axis an edge runs along. */
    public static int edgeAxis(final int slot) {
        if (!isEdge(slot)) {
            throw new IllegalArgumentException("place " + slot + " is not an edge");
        }
        return (slot - FIRST_EDGE) / 4;
    }

    /** Whether an edge stands at the far end of the first of the two axes across it. */
    public static boolean edgeFirstFar(final int slot) {
        return ((slot - FIRST_EDGE) & 1) != 0;
    }

    /** Whether an edge stands at the far end of the second of the two axes across it. */
    public static boolean edgeSecondFar(final int slot) {
        return ((slot - FIRST_EDGE) & 2) != 0;
    }

    /** Whether a corner stands at the far end of {@code axis}. */
    public static boolean cornerFar(final int slot, final int axis) {
        if (!isCorner(slot)) {
            throw new IllegalArgumentException("place " + slot + " is not a corner");
        }
        requireAxis(axis);
        return ((slot - FIRST_CORNER) >> axis & 1) != 0;
    }

    /** Whether {@code slot} is a place of a space. */
    public static boolean valid(final int slot) {
        return slot >= 0 && slot < COUNT;
    }

    /** The two axes across {@code axis}, in their order. */
    public static int[] across(final int axis) {
        requireAxis(axis);
        return switch (axis) {
            case X -> new int[] {Y, Z};
            case Y -> new int[] {X, Z};
            default -> new int[] {X, Y};
        };
    }

    private static void requireAxis(final int axis) {
        if (axis < X || axis > Z) {
            throw new IllegalArgumentException("an axis is 0, 1 or 2; got " + axis);
        }
    }
}
