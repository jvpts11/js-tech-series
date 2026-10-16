/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import dev.jstech.core.id.IStableName;
import java.util.List;

/**
 * The shapes a microblock is cut in, each of a thickness in eighths of a block: a piece against a face (a cover is
 * an eighth, a panel a quarter, a slab a half, and the thicknesses between), the same with a hole in its middle for a
 * cable, a facade (a cover on a cable's face that hides the cable and keeps its link), a thin post along an edge or
 * through the middle, and a corner. Pure: the room each takes is worked out in pixels.
 */
public enum MicroShape implements IStableName {
    /** Against a face, solid. */
    FACE("face"),
    /** Against a face, an eight-pixel hole in its middle for a cable to pass. */
    HOLLOW("hollow"),
    /** A cover on a cable's face, hiding the cable without cutting its link. */
    FACADE("facade"),
    /** Along an edge, or through the middle along an axis. */
    POST("post"),
    /** In a corner. */
    CORNER("corner");

    private final String serializedName;

    /** The thinnest a piece is, in eighths of a block: a cover. */
    public static final int THINNEST = 1;
    /** The thickest a piece is: a whole block is no piece. */
    public static final int THICKEST = 7;
    /** A slab's thickness, a panel's and a cover's. */
    public static final int SLAB = 4;
    public static final int PANEL = 2;
    public static final int COVER = 1;
    private static final int PIXELS_PER_EIGHTH = 2;
    private static final int HOLE_FROM = 4;
    private static final int HOLE_TO = 12;
    private static final int MIDDLE = 8;

    MicroShape(final String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }

    /** Whether it stands against a face, as a cover does. */
    public boolean onFace() {
        return this == FACE || this == HOLLOW || this == FACADE;
    }

    /** Whether, against a cable's face, it cuts the cable's link on that face: a solid piece does. */
    public boolean cutsLink() {
        return this == FACE;
    }

    /** Whether {@code size}, in eighths, is a thickness it comes in: a facade is a cover's only. */
    public boolean takes(final int size) {
        return this == FACADE ? size == COVER : size >= THINNEST && size <= THICKEST;
    }

    /** Whether it may stand in {@code slot}: a piece on a face on a face, a post on an edge or the centre. */
    public boolean fits(final int slot) {
        return switch (this) {
            case FACE, HOLLOW, FACADE -> SpaceSlots.isFace(slot);
            case POST -> SpaceSlots.isEdge(slot) || slot == SpaceSlots.CENTRE;
            case CORNER -> SpaceSlots.isCorner(slot);
        };
    }

    /**
     * The room it takes in {@code slot}, {@code size} eighths thick; a post in the centre runs along {@code axis}.
     *
     * @throws IllegalArgumentException when it does not stand there or come that thick
     */
    public List<PartBox> boxes(final int slot, final int size, final int axis) {
        if (!fits(slot) || !takes(size)) {
            throw new IllegalArgumentException(this.serializedName + " of " + size + " eighths does not stand in place "
                    + slot);
        }
        final int t = size * PIXELS_PER_EIGHTH;
        return switch (this) {
            case FACE, FACADE -> List.of(onFace(SpaceSlots.faceOf(slot), t, 0, PartBox.SIDE, 0, PartBox.SIDE));
            case HOLLOW -> hollow(SpaceSlots.faceOf(slot), t);
            case POST -> List.of(slot == SpaceSlots.CENTRE ? centrePost(axis, t) : edgePost(slot, t));
            case CORNER -> List.of(corner(slot, t));
        };
    }

    /* A slab {@code t} pixels deep against {@code face}, from {@code a0} to {@code a1} and {@code b0} to {@code b1}. */
    private static PartBox onFace(final int face, final int t, final int a0, final int a1, final int b0,
                                  final int b1) {
        final int axis = SpaceSlots.faceAxis(face);
        final int[] across = SpaceSlots.across(axis);
        final int[] least = new int[3];
        final int[] most = new int[3];
        least[axis] = SpaceSlots.faceFar(face) ? PartBox.SIDE - t : 0;
        most[axis] = SpaceSlots.faceFar(face) ? PartBox.SIDE : t;
        least[across[0]] = a0;
        most[across[0]] = a1;
        least[across[1]] = b0;
        most[across[1]] = b1;
        return PartBox.of(least, most);
    }

    /* The four bars round an eight-pixel hole. */
    private static List<PartBox> hollow(final int face, final int t) {
        return List.of(onFace(face, t, 0, PartBox.SIDE, 0, HOLE_FROM),
                onFace(face, t, 0, PartBox.SIDE, HOLE_TO, PartBox.SIDE),
                onFace(face, t, 0, HOLE_FROM, HOLE_FROM, HOLE_TO),
                onFace(face, t, HOLE_TO, PartBox.SIDE, HOLE_FROM, HOLE_TO));
    }

    private static PartBox edgePost(final int slot, final int t) {
        final int axis = SpaceSlots.edgeAxis(slot);
        final int[] across = SpaceSlots.across(axis);
        final int[] least = new int[3];
        final int[] most = new int[3];
        least[axis] = 0;
        most[axis] = PartBox.SIDE;
        final boolean[] far = {SpaceSlots.edgeFirstFar(slot), SpaceSlots.edgeSecondFar(slot)};
        for (int k = 0; k < 2; k++) {
            least[across[k]] = far[k] ? PartBox.SIDE - t : 0;
            most[across[k]] = far[k] ? PartBox.SIDE : t;
        }
        return PartBox.of(least, most);
    }

    private static PartBox centrePost(final int axis, final int t) {
        final int[] across = SpaceSlots.across(axis);
        final int[] least = new int[3];
        final int[] most = new int[3];
        least[axis] = 0;
        most[axis] = PartBox.SIDE;
        for (final int other : across) {
            least[other] = MIDDLE - t / 2;
            most[other] = MIDDLE + (t + 1) / 2;
        }
        return PartBox.of(least, most);
    }

    private static PartBox corner(final int slot, final int t) {
        final int[] least = new int[3];
        final int[] most = new int[3];
        for (int axis = SpaceSlots.X; axis <= SpaceSlots.Z; axis++) {
            final boolean far = SpaceSlots.cornerFar(slot, axis);
            least[axis] = far ? PartBox.SIDE - t : 0;
            most[axis] = far ? PartBox.SIDE : t;
        }
        return PartBox.of(least, most);
    }
}
