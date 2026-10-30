/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.space.api.ShipKit;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ShipLayoutTest {

    @Test
    void lay_setsTheSpineEndToEndFromTheNose() {
        final ShipLayout.Laid laid = ShipLayout.lay(List.of(section(2, 'C'), section(3, 'A'), section(1, 'R')),
                List.of());
        assertEquals(6, laid.length());
        assertEquals(new ShipLayout.Spot(0, 'C'), laid.cells().get(new ShipLayout.Cell(-2, 0, 1)));
        assertEquals(new ShipLayout.Spot(1, 'A'), laid.cells().get(new ShipLayout.Cell(1, 0, 2)));
        assertEquals(new ShipLayout.Spot(2, 'R'), laid.cells().get(new ShipLayout.Cell(0, 0, 5)));
        assertTrue(laid.clashes().isEmpty());
    }

    @Test
    void lay_hangsPiecesFromTheirAnchors() {
        final List<ShipLayout.Section> spine = List.of(section(2, 'C'), section(3, 'A'), section(4, 'B'),
                section(1, 'R'));
        final ShipLayout.Laid laid = ShipLayout.lay(spine, List.of(
                List.of(piece(ShipKit.Anchor.COCKPIT_JOINT, 2, 0, "I", false)),
                List.of(piece(ShipKit.Anchor.WING_ROOT, 2, 0, "W", false)),
                List.of(piece(ShipKit.Anchor.SPINE_END, 0, 0, "E", false))));
        assertEquals(new ShipLayout.Spot(4, 'I'), laid.cells().get(new ShipLayout.Cell(2, 0, 2)));
        assertEquals(new ShipLayout.Spot(5, 'W'), laid.cells().get(new ShipLayout.Cell(2, 0, 5)), "the wings root "
                + "at the second section behind the cockpit");
        assertEquals(new ShipLayout.Spot(6, 'E'), laid.cells().get(new ShipLayout.Cell(0, 0, 10)));
        final ShipLayout.Laid alone = ShipLayout.lay(List.of(section(2, 'C'), section(3, 'A')),
                List.of(List.of(piece(ShipKit.Anchor.WING_ROOT, 2, 0, "W", false))));
        assertEquals(new ShipLayout.Spot(2, 'W'), alone.cells().get(new ShipLayout.Cell(2, 0, 2)), "or at the "
                + "first if it is alone");
    }

    @Test
    void lay_setsAMirroredPieceOnBothSides() {
        final ShipLayout.Laid laid = ShipLayout.lay(List.of(section(2, 'C')),
                List.of(List.of(piece(ShipKit.Anchor.COCKPIT_JOINT, 2, 1, "WT", true))));
        assertEquals(Set.of(new ShipLayout.Cell(2, 1, 2), new ShipLayout.Cell(3, 1, 2), new ShipLayout.Cell(-3, 1, 2),
                        new ShipLayout.Cell(-4, 1, 2)),
                Set.copyOf(laid.cells().keySet().stream().filter(cell -> cell.y() == 1).toList()));
        assertEquals(new ShipLayout.Spot(1, 'T'), laid.cells().get(new ShipLayout.Cell(-4, 1, 2)));
    }

    @Test
    void lay_findsTwoPartsWantingOnePlace() {
        final ShipLayout.Laid laid = ShipLayout.lay(List.of(section(2, 'C')), List.of(
                List.of(piece(ShipKit.Anchor.SPINE_END, -2, 0, "X", false)),
                List.of(piece(ShipKit.Anchor.SPINE_END, -2, 0, "Y", false))));
        assertEquals(Set.of(new ShipLayout.Cell(-2, 0, 2)), laid.clashes());
    }

    /* A section of {@code length} slices, its one layer the hull's four blocks of {@code letter}. */
    private static ShipLayout.Section section(final int length, final char letter) {
        final String row = String.valueOf(letter).repeat(4);
        return new ShipLayout.Section(length, List.of(new ShipKit.Piece(ShipKit.Anchor.SECTION, -2, 0, 0,
                List.of(Collections.nCopies(length, row)), false)));
    }

    private static ShipKit.Piece piece(final ShipKit.Anchor anchor, final int x, final int y, final String row,
                                       final boolean mirrored) {
        return new ShipKit.Piece(anchor, x, y, 0, List.of(List.of(row)), mirrored);
    }
}
