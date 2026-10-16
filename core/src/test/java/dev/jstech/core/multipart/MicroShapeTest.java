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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class MicroShapeTest {

    private static final int DOWN = 0;
    private static final int EAST = 5;

    @Test
    void boxes_cutThePiecesAsTheApprovedDesignDrawsThem() {
        assertEquals(List.of(new PartBox(0, 0, 0, 16, 2, 16)),
                MicroShape.FACE.boxes(SpaceSlots.face(DOWN), MicroShape.COVER, 0), "a cover is 2 pixels");
        assertEquals(List.of(new PartBox(8, 0, 0, 16, 16, 16)),
                MicroShape.FACE.boxes(SpaceSlots.face(EAST), MicroShape.SLAB, 0), "a slab against the east face");
        assertEquals(List.of(new PartBox(0, 0, 0, 4, 4, 4)),
                MicroShape.CORNER.boxes(SpaceSlots.corner(false, false, false), MicroShape.PANEL, 0));
        assertEquals(List.of(new PartBox(6, 0, 6, 10, 16, 10)),
                MicroShape.POST.boxes(SpaceSlots.CENTRE, MicroShape.PANEL, SpaceSlots.Y), "a post through the middle");
        assertEquals(List.of(new PartBox(12, 0, 0, 16, 16, 4)),
                MicroShape.POST.boxes(SpaceSlots.edge(SpaceSlots.Y, true, false), MicroShape.PANEL, 0),
                "a thin post on an edge");
    }

    @Test
    void hollow_leavesAnEightPixelHoleInItsMiddle() {
        final List<PartBox> bars = MicroShape.HOLLOW.boxes(SpaceSlots.face(DOWN), MicroShape.COVER, 0);
        assertEquals(4, bars.size());
        assertTrue(bars.stream().noneMatch(bar -> bar.overlaps(new PartBox(4, 0, 4, 12, 2, 12))));
    }

    @Test
    void shapes_standOnlyWhereTheyBelongAndCutOnlyWhenSolid() {
        assertTrue(MicroShape.FACE.fits(SpaceSlots.face(DOWN)));
        assertFalse(MicroShape.FACE.fits(SpaceSlots.CENTRE));
        assertTrue(MicroShape.POST.fits(SpaceSlots.CENTRE));
        assertFalse(MicroShape.CORNER.fits(SpaceSlots.face(DOWN)));
        assertTrue(MicroShape.FACE.cutsLink());
        assertFalse(MicroShape.HOLLOW.cutsLink());
        assertFalse(MicroShape.FACADE.cutsLink());
        assertFalse(MicroShape.FACADE.takes(MicroShape.PANEL), "a facade is a cover's thickness only");
        assertThrows(IllegalArgumentException.class,
                () -> MicroShape.FACE.boxes(SpaceSlots.face(DOWN), 8, 0), "a whole block is no piece");
    }
}
