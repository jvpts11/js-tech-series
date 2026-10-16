/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Whether a part fits a block space beside the parts already there: its place has to be free, and it may not share
 * room with another part, nor with what runs through the space's middle. Two microblocks on faces side by side may
 * meet at an edge, as two covers on a block's sides do, and two devices at the corner between their plates, but a
 * device may not reach into a microblock, and two parts on opposite faces may not reach into each other; a part on
 * a face may close the end of a cable running out of that face, as deep as a device, which is how a cover cuts the
 * cable's link, but not reach into the rest of the cable. Pure.
 */
public final class SpaceFit {

    /** How far across a device mounted on a face reaches, and how deep, in pixels. */
    public static final int DEVICE_FROM = 2;
    public static final int DEVICE_TO = 14;
    public static final int DEVICE_DEPTH = 5;

    private SpaceFit() {
    }

    /** Whether {@code candidate} fits beside {@code placed}, with {@code middle} running through the space. */
    public static boolean fits(final Piece candidate, final Collection<Piece> placed, final List<PartBox> middle) {
        for (final Piece piece : placed) {
            if (piece.slot() == candidate.slot()) {
                return false;
            }
            if (meetAtAnEdge(candidate, piece)) {
                continue;
            }
            if (overlap(candidate.boxes(), piece.boxes())) {
                return false;
            }
        }
        return !overlap(candidate.boxes(), candidate.onFace() ? awayFrom(candidate, middle) : middle);
    }

    /** The room a device mounted on the face numbered {@code face} takes: a plate twelve pixels across, five deep. */
    public static PartBox deviceBox(final int face) {
        final int axis = SpaceSlots.faceAxis(face);
        final int[] across = SpaceSlots.across(axis);
        final int[] least = new int[3];
        final int[] most = new int[3];
        least[axis] = SpaceSlots.faceFar(face) ? PartBox.SIDE - DEVICE_DEPTH : 0;
        most[axis] = SpaceSlots.faceFar(face) ? PartBox.SIDE : DEVICE_DEPTH;
        for (final int other : across) {
            least[other] = DEVICE_FROM;
            most[other] = DEVICE_TO;
        }
        return PartBox.of(least, most);
    }

    /*
     * Whether two parts on faces side by side may share room where they meet: two microblocks spanning their faces
     * meet along the edge, two devices at the corner between their plates; a device and a microblock never.
     */
    private static boolean meetAtAnEdge(final Piece a, final Piece b) {
        return a.onFace() && b.onFace() && a.wholeFace() == b.wholeFace() && axisOf(a) != axisOf(b);
    }

    private static int axisOf(final Piece onFace) {
        return SpaceSlots.faceAxis(SpaceSlots.faceOf(onFace.slot()));
    }

    /*
     * What runs through the middle but its end at the face {@code onFace} stands against: the end is as deep as a
     * device, so a cover, a panel or a device closes a cable's end while a slab would reach into the cable itself.
     */
    private static List<PartBox> awayFrom(final Piece onFace, final List<PartBox> middle) {
        final int face = SpaceSlots.faceOf(onFace.slot());
        final int axis = SpaceSlots.faceAxis(face);
        final boolean far = SpaceSlots.faceFar(face);
        final List<PartBox> away = new ArrayList<>(middle.size());
        for (final PartBox box : middle) {
            final int[] least = {box.x0(), box.y0(), box.z0()};
            final int[] most = {box.x1(), box.y1(), box.z1()};
            if (far) {
                most[axis] = Math.min(most[axis], PartBox.SIDE - DEVICE_DEPTH);
            } else {
                least[axis] = Math.max(least[axis], DEVICE_DEPTH);
            }
            if (least[axis] < most[axis]) {
                away.add(PartBox.of(least, most));
            }
        }
        return away;
    }

    private static boolean overlap(final List<PartBox> a, final List<PartBox> b) {
        for (final PartBox one : a) {
            for (final PartBox other : b) {
                if (one.overlaps(other)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * A part as the fit sees it: its place, the room it takes, whether it stands against a face, and whether it spans
     * that face to its edges, as a microblock on a face does, and so may meet another such along an edge.
     *
     * @param slot      its place
     * @param boxes     the room it takes
     * @param onFace    whether it stands against a face
     * @param wholeFace whether it spans its face to the edges
     */
    public record Piece(int slot, List<PartBox> boxes, boolean onFace, boolean wholeFace) {

        public Piece {
            boxes = List.copyOf(boxes);
        }

        /** A device mounted on a face: against it, but not to its edges. */
        public static Piece device(final int slot, final List<PartBox> boxes) {
            return new Piece(slot, boxes, true, false);
        }
    }
}
