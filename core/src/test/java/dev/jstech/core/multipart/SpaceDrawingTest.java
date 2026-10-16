/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SpaceDrawingTest {

    private static final int DOWN = 0;
    private static final int UP = 1;
    private static final int NORTH = 2;

    @Test
    void of_letsTheThinnerOfTwoPiecesSideBySideGiveWay() {
        final SpaceDrawing.Shown slab = face(DOWN, MicroShape.SLAB, true);
        final SpaceDrawing.Shown cover = face(NORTH, MicroShape.COVER, true);
        final List<List<SpaceDrawing.DrawnBox>> drawn = SpaceDrawing.of(List.of(slab, cover));
        assertEquals(slab.boxes(), boxes(drawn.get(0)), "the slab is drawn whole");
        for (final PartBox box : boxes(drawn.get(1))) {
            assertFalse(box.overlaps(slab.boxes().getFirst()), "the cover gives way where it meets the slab: " + box);
        }
        assertEquals(List.of(new PartBox(0, 8, 0, 16, 16, 2)), boxes(drawn.get(1)));
    }

    @Test
    void of_letsThePieceOnTheLaterFaceGiveWayBetweenEqualOnes() {
        final SpaceDrawing.Shown floor = face(DOWN, MicroShape.COVER, true);
        final SpaceDrawing.Shown wall = face(NORTH, MicroShape.COVER, true);
        final List<List<SpaceDrawing.DrawnBox>> drawn = SpaceDrawing.of(List.of(wall, floor));
        assertEquals(floor.boxes(), boxes(drawn.get(1)), "the cover on the floor is drawn whole");
        assertEquals(List.of(new PartBox(0, 2, 0, 16, 16, 2)), boxes(drawn.get(0)), "the wall's gives way");
    }

    @Test
    void of_hidesAFaceFlatAgainstAnOpaquePiece() {
        final SpaceDrawing.Shown stone = face(DOWN, MicroShape.SLAB, true);
        final SpaceDrawing.Shown glass = face(UP, MicroShape.SLAB, false);
        final List<List<SpaceDrawing.DrawnBox>> drawn = SpaceDrawing.of(List.of(stone, glass));
        assertFalse(drawn.get(0).getFirst().hides(UP), "the stone shows its top through the glass");
        assertTrue(drawn.get(1).getFirst().hides(DOWN), "the glass's bottom lies flat on the stone");
    }

    @Test
    void of_leavesTheFacesOnTheSpacesSideToTheGame() {
        final List<List<SpaceDrawing.DrawnBox>> drawn = SpaceDrawing.of(List.of(face(DOWN, MicroShape.COVER, true)));
        assertEquals(0, drawn.getFirst().getFirst().hidden(), "a cover alone hides nothing");
    }

    @Test
    void of_hidesTheFacesBetweenTheBarsOfAHollowPiece() {
        final int slot = SpaceSlots.face(NORTH);
        final SpaceDrawing.Shown hollow = new SpaceDrawing.Shown(slot,
                MicroShape.HOLLOW.boxes(slot, MicroShape.PANEL, SpaceSlots.Y), false);
        final List<SpaceDrawing.DrawnBox> bars = SpaceDrawing.of(List.of(hollow)).getFirst();
        assertEquals(4, bars.size());
        assertTrue(bars.stream().anyMatch(bar -> bar.hidden() != 0), "a short bar's ends lie flat on the long bars");
    }

    private static SpaceDrawing.Shown face(final int face, final int size, final boolean opaque) {
        final int slot = SpaceSlots.face(face);
        return new SpaceDrawing.Shown(slot, MicroShape.FACE.boxes(slot, size, SpaceSlots.Y), opaque);
    }

    private static List<PartBox> boxes(final List<SpaceDrawing.DrawnBox> drawn) {
        return drawn.stream().map(SpaceDrawing.DrawnBox::box).toList();
    }
}
