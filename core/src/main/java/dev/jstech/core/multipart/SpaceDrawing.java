/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import java.util.ArrayList;
import java.util.List;

/**
 * How the pieces of a block space are drawn, worked out each time they change. Two pieces on faces side by side meet
 * along their edge and would both be drawn there, two blocks fighting to be seen in one place: the thinner gives way
 * where they meet, or, as thick as each other, the one on the later face. And a face of a piece that lies flat
 * against a piece of an opaque block, or against another box of the same piece, is hidden, as the game hides the
 * faces between two blocks; a face on the space's side is left to the game, which hides it against its neighbour.
 * Pure.
 */
public final class SpaceDrawing {

    private static final int FACES = 6;

    private SpaceDrawing() {
    }

    /** How each of {@code pieces} is drawn, in their order. */
    public static List<List<DrawnBox>> of(final List<Shown> pieces) {
        final List<List<PartBox>> boxes = new ArrayList<>(pieces.size());
        for (int i = 0; i < pieces.size(); i++) {
            List<PartBox> mine = pieces.get(i).boxes();
            for (int j = 0; j < pieces.size(); j++) {
                if (j != i && givesWay(pieces.get(i), pieces.get(j))) {
                    mine = minus(mine, pieces.get(j).boxes());
                }
            }
            boxes.add(mine);
        }
        final List<List<DrawnBox>> out = new ArrayList<>(pieces.size());
        for (int i = 0; i < pieces.size(); i++) {
            final List<DrawnBox> drawn = new ArrayList<>(boxes.get(i).size());
            for (int k = 0; k < boxes.get(i).size(); k++) {
                int hidden = 0;
                for (int face = 0; face < FACES; face++) {
                    if (covered(i, k, face, pieces, boxes)) {
                        hidden |= 1 << face;
                    }
                }
                drawn.add(new DrawnBox(boxes.get(i).get(k), hidden));
            }
            out.add(List.copyOf(drawn));
        }
        return List.copyOf(out);
    }

    /* Whether {@code mine} gives way to {@code other} where they meet: both on faces side by side, it the thinner. */
    private static boolean givesWay(final Shown mine, final Shown other) {
        if (!SpaceSlots.isFace(mine.slot()) || !SpaceSlots.isFace(other.slot())) {
            return false;
        }
        final int face = SpaceSlots.faceOf(mine.slot());
        final int theirFace = SpaceSlots.faceOf(other.slot());
        if (SpaceSlots.faceAxis(face) == SpaceSlots.faceAxis(theirFace)) {
            return false;
        }
        final int depth = depth(mine);
        final int theirs = depth(other);
        return theirs > depth || theirs == depth && theirFace < face;
    }

    /* How far a piece on a face reaches in from it, in pixels. */
    private static int depth(final Shown onFace) {
        final int face = SpaceSlots.faceOf(onFace.slot());
        final int axis = SpaceSlots.faceAxis(face);
        final boolean far = SpaceSlots.faceFar(face);
        int depth = 0;
        for (final PartBox box : onFace.boxes()) {
            depth = Math.max(depth, far ? PartBox.SIDE - box.min(axis) : box.max(axis));
        }
        return depth;
    }

    private static List<PartBox> minus(final List<PartBox> boxes, final List<PartBox> taken) {
        List<PartBox> left = boxes;
        for (final PartBox away : taken) {
            final List<PartBox> next = new ArrayList<>();
            for (final PartBox box : left) {
                next.addAll(box.minus(away));
            }
            left = next;
        }
        return left;
    }

    /*
     * Whether the face numbered {@code face} of box {@code k} of piece {@code i} lies flat against a box that covers it
     * whole: one of the same piece, or one of a piece of an opaque block. A face on the space's side is never.
     */
    private static boolean covered(final int i, final int k, final int face, final List<Shown> pieces,
                                   final List<List<PartBox>> boxes) {
        final PartBox box = boxes.get(i).get(k);
        final int axis = SpaceSlots.faceAxis(face);
        final boolean far = SpaceSlots.faceFar(face);
        final int plane = far ? box.max(axis) : box.min(axis);
        if (plane == (far ? PartBox.SIDE : 0)) {
            return false;
        }
        final int[] across = SpaceSlots.across(axis);
        for (int j = 0; j < pieces.size(); j++) {
            if (j != i && !pieces.get(j).opaque()) {
                continue;
            }
            for (int m = 0; m < boxes.get(j).size(); m++) {
                final PartBox other = boxes.get(j).get(m);
                if (j == i && m == k || (far ? other.min(axis) : other.max(axis)) != plane) {
                    continue;
                }
                if (other.min(across[0]) <= box.min(across[0]) && other.max(across[0]) >= box.max(across[0])
                        && other.min(across[1]) <= box.min(across[1]) && other.max(across[1]) >= box.max(across[1])) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * A piece as the drawing sees it.
     *
     * @param slot   its place in the space ({@link SpaceSlots})
     * @param boxes  the room it takes
     * @param opaque whether its block hides what lies behind it
     */
    public record Shown(int slot, List<PartBox> boxes, boolean opaque) {

        public Shown {
            boxes = List.copyOf(boxes);
        }
    }

    /**
     * A box of a piece as it is drawn, and the faces of it that are hidden.
     *
     * @param box    the box
     * @param hidden a bit for each hidden face, in the order of the game's directions
     */
    public record DrawnBox(PartBox box, int hidden) {

        /** Whether its face numbered {@code face}, in the order of the game's directions, is hidden. */
        public boolean hides(final int face) {
            return (this.hidden & 1 << face) != 0;
        }
    }
}
