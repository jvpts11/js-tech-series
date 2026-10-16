/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SpaceFitTest {

    private static final int DOWN = 0;
    private static final int UP = 1;
    private static final int NORTH = 2;
    private static final int EAST = 5;
    /* A cable running north to south through the middle. */
    private static final List<PartBox> CABLE = List.of(new PartBox(6, 6, 0, 10, 10, 16));

    @Test
    void fits_letsPiecesOnFacesMeetAtAnEdge() {
        final SpaceFit.Piece floor = face(DOWN, MicroShape.SLAB);
        assertTrue(SpaceFit.fits(face(NORTH, MicroShape.SLAB), List.of(floor), List.of()));
    }

    @Test
    void fits_refusesATakenPlaceAndAPieceInAnother() {
        final SpaceFit.Piece floor = face(DOWN, MicroShape.SLAB);
        assertFalse(SpaceFit.fits(face(DOWN, MicroShape.COVER), List.of(floor), List.of()), "the face is taken");
        final SpaceFit.Piece corner = piece(MicroShape.CORNER, SpaceSlots.corner(false, false, false),
                MicroShape.PANEL);
        assertFalse(SpaceFit.fits(corner, List.of(floor), List.of()), "the corner lies inside the slab");
        assertTrue(SpaceFit.fits(corner, List.of(face(1, MicroShape.COVER)), List.of()),
                "a corner below a cover on the ceiling fits");
    }

    @Test
    void fits_refusesPiecesOnOppositeFacesThatReachIntoEachOther() {
        final SpaceFit.Piece floor = face(DOWN, MicroShape.SLAB);
        assertTrue(SpaceFit.fits(face(UP, MicroShape.SLAB), List.of(floor), List.of()), "two slabs make a block");
        assertFalse(SpaceFit.fits(face(UP, MicroShape.SLAB + 1), List.of(floor), List.of()),
                "five eighths above a slab reach into it");
    }

    @Test
    void fits_keepsPostsAndCornersOutOfTheMiddle() {
        assertFalse(SpaceFit.fits(piece(MicroShape.POST, SpaceSlots.CENTRE, MicroShape.PANEL), List.of(), CABLE),
                "no post through a cable");
        assertTrue(SpaceFit.fits(piece(MicroShape.POST, SpaceSlots.edge(SpaceSlots.Y, false, false),
                MicroShape.PANEL), List.of(), CABLE), "a post on an edge stands beside it");
    }

    @Test
    void fits_letsAFacePieceCloseACablesEndButNotReachIntoIt() {
        assertTrue(SpaceFit.fits(face(NORTH, MicroShape.COVER), List.of(), CABLE), "a cover closes the cable's end");
        assertTrue(SpaceFit.fits(face(NORTH, MicroShape.PANEL), List.of(), CABLE), "and a panel");
        assertTrue(SpaceFit.fits(device(NORTH), List.of(), CABLE), "and a device");
        assertFalse(SpaceFit.fits(face(NORTH, MicroShape.SLAB), List.of(), CABLE), "a slab reaches into the cable");
        assertFalse(SpaceFit.fits(face(EAST, MicroShape.SLAB), List.of(), CABLE), "and beside it");
        assertTrue(SpaceFit.fits(piece(MicroShape.HOLLOW, SpaceSlots.face(NORTH), MicroShape.SLAB), List.of(),
                CABLE), "a hollow slab lets it through");
    }

    @Test
    void fits_letsDevicesMeetEachOtherButNotReachIntoAMicroblock() {
        assertTrue(SpaceFit.fits(device(EAST), List.of(device(NORTH)), List.of()), "two devices side by side");
        assertFalse(SpaceFit.fits(device(NORTH), List.of(face(DOWN, MicroShape.SLAB)), List.of()),
                "a device would reach into a slab on the floor");
        assertTrue(SpaceFit.fits(device(NORTH), List.of(face(DOWN, MicroShape.COVER)), List.of()),
                "but stands above a cover");
    }

    @Test
    void deviceBox_isAPlateTwelvePixelsAcross() {
        final PartBox plate = SpaceFit.deviceBox(NORTH);
        assertTrue(plate.equals(new PartBox(2, 2, 0, 14, 14, 5)));
    }

    private static SpaceFit.Piece face(final int face, final int size) {
        return piece(MicroShape.FACE, SpaceSlots.face(face), size);
    }

    private static SpaceFit.Piece piece(final MicroShape shape, final int slot, final int size) {
        return new SpaceFit.Piece(slot, shape.boxes(slot, size, SpaceSlots.Y), shape.onFace(), shape.onFace());
    }

    private static SpaceFit.Piece device(final int face) {
        return SpaceFit.Piece.device(SpaceSlots.face(face), List.of(SpaceFit.deviceBox(face)));
    }
}
