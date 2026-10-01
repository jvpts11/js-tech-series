/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.cable.BundleShape.Box;
import dev.jstech.core.cable.BundleShape.Kind;
import dev.jstech.core.cable.BundleShape.Piece;
import dev.jstech.core.cable.BundleShape.Plug;
import dev.jstech.core.cable.BundleShape.Strand;
import java.util.List;
import org.junit.jupiter.api.Test;

class BundleShapeTest {

    private static final int DOWN = 0;
    private static final int UP = 1;
    private static final int NORTH = 2;
    private static final int SOUTH = 3;
    private static final int WEST = 4;
    private static final int EAST = 5;

    @Test
    void of_runsAWireAloneStraightThroughTheMiddle() {
        final BundleShape shape = BundleShape.of(List.of(strand(Lane.TOP, WEST, EAST)));

        assertFalse(shape.junction());
        final List<Piece> jackets = pieces(shape, Kind.JACKET);
        assertEquals(1, jackets.size());
        assertEquals(new Box(0, 6, 6, 16, 10, 10), jackets.get(0).box());
        assertEquals(0, jackets.get(0).axis());
        assertTrue(jackets.get(0).hides(WEST));
        assertTrue(jackets.get(0).hides(EAST));
        assertFalse(jackets.get(0).hides(UP));
    }

    @Test
    void of_leavesAWireThatCrossesNothingAsACoreInTheMiddle() {
        final BundleShape shape = BundleShape.of(List.of(new Strand(Lane.TOP_LEFT, 4, 0, 0, false)));

        assertEquals(List.of(new Box(6, 6, 6, 10, 10, 10)), boxes(shape, Kind.JACKET));
    }

    @Test
    void of_bendsAWireAtItsPointWithAnArmToEachFace() {
        final BundleShape shape = BundleShape.of(List.of(strand(Lane.MIDDLE, WEST, UP)));

        assertEquals(List.of(new Box(6, 6, 6, 10, 10, 10), new Box(0, 6, 6, 6, 10, 10),
                new Box(6, 10, 6, 10, 16, 10)), boxes(shape, Kind.JACKET));
    }

    @Test
    void of_runsWiresThatShareTheirFacesEachInItsLane() {
        final BundleShape shape = BundleShape.of(List.of(strand(Lane.TOP_LEFT, WEST, EAST),
                strand(Lane.TOP, WEST, EAST)));

        assertFalse(shape.junction());
        // Along x a lane's across is z and its up is y, five pixels from one lane to the next.
        assertEquals(List.of(new Box(0, 11, 1, 16, 15, 5), new Box(0, 11, 6, 16, 15, 10)),
                boxes(shape, Kind.JACKET));
    }

    @Test
    void of_makesAJunctionBoxWhereAWireChangesPlace() {
        // The backbone shares the west face in its lane and leaves by the east face alone, in the middle.
        final BundleShape shape = BundleShape.of(List.of(strand(Lane.TOP, WEST, EAST),
                strand(Lane.TOP_LEFT, WEST, NORTH)));

        assertTrue(shape.junction());
        assertEquals(List.of(new Box(1, 1, 1, 15, 15, 15)), boxes(shape, Kind.HOUSING));
        assertEquals(4, pieces(shape, Kind.SCREW).size());
        assertEquals(1, pieces(shape, Kind.HOUSING_EDGE).size());
        assertTrue(boxes(shape, Kind.JACKET).contains(new Box(0, 11, 6, 1, 15, 10)));
        assertTrue(boxes(shape, Kind.JACKET).contains(new Box(15, 6, 6, 16, 10, 10)));
    }

    @Test
    void of_makesAJunctionBoxWhereTwoWiresWouldCross() {
        // Each crosses its faces alone, so both run through the middle, one along x and one along z.
        final BundleShape shape = BundleShape.of(List.of(strand(Lane.TOP, WEST, EAST),
                strand(Lane.MIDDLE, NORTH, SOUTH)));

        assertTrue(shape.junction());
        // Half a wire from the middle, and a pixel and a half clear of it.
        assertEquals(List.of(new Box(4.5, 4.5, 4.5, 11.5, 11.5, 11.5)), boxes(shape, Kind.HOUSING));
    }

    @Test
    void of_keepsTheCoresOfWiresThatCrossNothingApartInTheirLanes() {
        final BundleShape shape = BundleShape.of(List.of(new Strand(Lane.TOP_LEFT, 4, 0, 0, false),
                new Strand(Lane.MIDDLE, 4, 0, 0, false)));

        assertFalse(shape.junction());
        assertEquals(List.of(new Box(6, 11, 1, 10, 15, 5), new Box(6, 6, 6, 10, 10, 10)),
                boxes(shape, Kind.JACKET));
    }

    @Test
    void of_ringsAStraightWireInTheMiddleOfTheBlock() {
        final BundleShape shape = BundleShape.of(List.of(new Strand(Lane.MIDDLE, 4, bits(WEST, EAST), 0, true)));

        assertEquals(List.of(new Box(6.5, 5.65, 5.65, 9.5, 10.35, 10.35)), boxes(shape, Kind.RING));
    }

    @Test
    void of_ringsABendOnItsFirstLevelArmLongEnough() {
        final BundleShape shape = BundleShape.of(List.of(new Strand(Lane.MIDDLE, 4, bits(DOWN, SOUTH), 0, true)));

        final List<Box> rings = boxes(shape, Kind.RING);
        assertEquals(1, rings.size());
        assertEquals(13.0 - 1.5, rings.get(0).minZ(), 1.0e-9);
    }

    @Test
    void of_putsAPlugWhereAWireMeetsADevice() {
        final BundleShape alone = BundleShape.of(List.of(new Strand(Lane.TOP, 4, bits(NORTH, SOUTH), bits(NORTH),
                false)));
        final BundleShape shared = BundleShape.of(List.of(new Strand(Lane.TOP, 4, bits(NORTH, SOUTH),
                bits(NORTH), false), strand(Lane.MIDDLE, NORTH, SOUTH)));

        assertEquals(List.of(new Plug(0, NORTH, 0, 0)), alone.plugs());
        assertEquals(List.of(new Plug(0, NORTH, 0, 5)), shared.plugs());
    }

    @Test
    void of_reachesThroughTheHousingToEachFaceAWireCrosses() {
        final BundleShape shape = BundleShape.of(List.of(strand(Lane.TOP, WEST, EAST),
                strand(Lane.TOP_LEFT, WEST, NORTH)));

        assertEquals(List.of(new Box(0, 11, 6, 8, 15, 10), new Box(8, 6, 6, 16, 10, 10)), shape.reach(0));
    }

    @Test
    void strand_refusesAPlugOnAFaceItDoesNotCross() {
        assertThrows(IllegalArgumentException.class, () -> new Strand(Lane.TOP, 4, bits(WEST), bits(EAST), false));
    }

    @Test
    void box_overlapsOnlyPastATouch() {
        final Box box = new Box(0, 0, 0, 4, 4, 4);

        assertTrue(box.overlaps(new Box(3, 3, 3, 5, 5, 5)));
        assertFalse(box.overlaps(new Box(4, 0, 0, 8, 4, 4)));
    }

    @Test
    void box_measuresHowFarAPointLiesOutside() {
        final Box box = new Box(0, 0, 0, 4, 4, 4);

        assertEquals(0.0, box.distanceTo(2, 2, 2), 1.0e-9);
        assertEquals(3.0, box.distanceTo(7, 2, 2), 1.0e-9);
    }

    private static Strand strand(final Lane lane, final int... faces) {
        return new Strand(lane, 4, bits(faces), 0, false);
    }

    private static int bits(final int... faces) {
        int bits = 0;
        for (final int face : faces) {
            bits |= 1 << face;
        }
        return bits;
    }

    private static List<Piece> pieces(final BundleShape shape, final Kind kind) {
        return shape.pieces().stream().filter(piece -> piece.kind() == kind).toList();
    }

    private static List<Box> boxes(final BundleShape shape, final Kind kind) {
        return pieces(shape, kind).stream().map(Piece::box).toList();
    }
}
