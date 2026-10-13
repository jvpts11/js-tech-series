/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.gui.layout.AssemblyGeometry.Box;
import dev.jstech.computers.gui.layout.AssemblyGeometry.Seat;
import dev.jstech.computers.gui.layout.AssemblyGeometry.SeatKind;
import dev.jstech.core.tier.HardwareEra;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AssemblyGeometryTest {

    @Test
    void seats_standInsideTheirCase() {
        for (final AssemblyGeometry geo : everyGeometry()) {
            final Box box = geo.caseBox();
            for (final Seat seat : geo.seats()) {
                assertTrue(seat.x() - 1 >= box.x() && seat.y() - 1 >= box.y()
                                && seat.x() + 17 <= box.x() + box.w() && seat.y() + 17 <= box.y() + box.h(),
                        geo.kind() + " seat " + seat + " leaves the case " + box);
            }
        }
    }

    @Test
    void frontPanel_standsRightOfTheCase() {
        for (final AssemblyGeometry geo : everyGeometry()) {
            assertTrue(geo.power().x() >= geo.caseBox().x() + geo.caseBox().w(),
                    geo.kind() + " power button over the case");
        }
    }

    @Test
    void screen_fitsTheHeightEveryPanelIsHeldTo() {
        for (final AssemblyGeometry geo : everyGeometry()) {
            assertTrue(geo.height() <= 256, geo.kind() + " is " + geo.height() + " tall");
        }
    }

    @Test
    void tower_keepsItsSupplyAtTheTopUpToTheTransitionAndAtTheFootAfter() {
        for (final HardwareEra era : HardwareEra.values()) {
            final AssemblyGeometry geo = AssemblyGeometry.tower(era, 3);
            final boolean atTop = geo.seat(SeatKind.PSU).y() == AssemblyGeometry.TOP;
            assertEquals(era.isAtMost(HardwareEra.TRANSITION), atTop, era + " supply");
        }
    }

    @Test
    void bracketHidden_onlyOnTheSupplyRow() {
        final AssemblyGeometry geo = AssemblyGeometry.tower(HardwareEra.STANDARD, 3);
        final List<Seat> cards = geo.seats(SeatKind.CARD);
        // The newer towers keep their supply at the foot, level with the last card.
        assertTrue(geo.bracketHidden(cards.get(cards.size() - 1)), "the last card shares the supply's row");
        assertTrue(!geo.bracketHidden(cards.get(0)), "the first card has its bracket");
    }

    private static List<AssemblyGeometry> everyGeometry() {
        final List<AssemblyGeometry> out = new ArrayList<>();
        for (final HardwareEra era : HardwareEra.values()) {
            out.add(AssemblyGeometry.tower(era, 4));
        }
        out.add(AssemblyGeometry.mainframe(4));
        out.add(AssemblyGeometry.server(5));
        return out;
    }
}
