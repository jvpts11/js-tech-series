/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class CutsTest {

    @Test
    void cut_halvesABlockIntoSlabsIntoPanelsIntoCovers() {
        assertEquals(Optional.of(new Cuts.Made(new Cuts.Piece(MicroShape.FACE, MicroShape.SLAB), 2)),
                Cuts.cut(Cuts.Piece.WHOLE, Cuts.Way.HALVES));
        assertEquals(Optional.of(new Cuts.Made(new Cuts.Piece(MicroShape.FACE, MicroShape.PANEL), 2)),
                Cuts.cut(new Cuts.Piece(MicroShape.FACE, MicroShape.SLAB), Cuts.Way.HALVES));
        assertEquals(Optional.of(new Cuts.Made(new Cuts.Piece(MicroShape.FACE, MicroShape.COVER), 2)),
                Cuts.cut(new Cuts.Piece(MicroShape.FACE, MicroShape.PANEL), Cuts.Way.HALVES));
        assertTrue(Cuts.cut(new Cuts.Piece(MicroShape.FACE, MicroShape.COVER), Cuts.Way.HALVES).isEmpty(),
                "a cover is the thinnest");
        assertTrue(Cuts.cut(new Cuts.Piece(MicroShape.FACE, 3), Cuts.Way.HALVES).isEmpty(), "three has no half");
    }

    @Test
    void cut_acrossMakesPostsThenCornersAndAtACornerHollows() {
        assertEquals(Optional.of(new Cuts.Made(new Cuts.Piece(MicroShape.POST, MicroShape.PANEL), 2)),
                Cuts.cut(new Cuts.Piece(MicroShape.FACE, MicroShape.PANEL), Cuts.Way.ACROSS));
        assertEquals(Optional.of(new Cuts.Made(new Cuts.Piece(MicroShape.CORNER, MicroShape.PANEL), 2)),
                Cuts.cut(new Cuts.Piece(MicroShape.POST, MicroShape.PANEL), Cuts.Way.ACROSS));
        assertEquals(Optional.of(new Cuts.Made(new Cuts.Piece(MicroShape.HOLLOW, MicroShape.COVER), 1)),
                Cuts.cut(new Cuts.Piece(MicroShape.FACE, MicroShape.COVER), Cuts.Way.HOLLOWED));
        assertTrue(Cuts.cut(Cuts.Piece.WHOLE, Cuts.Way.ACROSS).isEmpty(), "a block is halved first");
    }

    @Test
    void join_makesTheThicknessesBetweenAndTheBlockAgain() {
        assertEquals(Optional.of(new Cuts.Piece(MicroShape.FACE, 3)), Cuts.join(new Cuts.Piece(MicroShape.FACE,
                MicroShape.COVER), new Cuts.Piece(MicroShape.FACE, MicroShape.PANEL)));
        assertEquals(Optional.of(Cuts.Piece.WHOLE), Cuts.join(new Cuts.Piece(MicroShape.FACE, MicroShape.SLAB),
                new Cuts.Piece(MicroShape.FACE, MicroShape.SLAB)));
        assertTrue(Cuts.join(new Cuts.Piece(MicroShape.FACE, MicroShape.SLAB), new Cuts.Piece(MicroShape.POST,
                MicroShape.SLAB)).isEmpty(), "two shapes do not join");
        assertTrue(Cuts.join(new Cuts.Piece(MicroShape.POST, MicroShape.SLAB), new Cuts.Piece(MicroShape.POST,
                MicroShape.SLAB)).isEmpty(), "two half posts make no block");
        assertEquals(Optional.of(new Cuts.Piece(MicroShape.FACADE, MicroShape.COVER)),
                Cuts.facade(new Cuts.Piece(MicroShape.FACE, MicroShape.COVER)));
    }
}
