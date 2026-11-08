/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.gui.layout.AssemblyGeometry.Box;
import dev.jstech.computers.gui.layout.AssemblyGeometry.Seat;
import dev.jstech.computers.gui.layout.AssemblyGeometry.SeatKind;
import dev.jstech.computers.hardware.TowerBoards;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
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
        for (final Era era : HardwareEra.SERIES) {
            final AssemblyGeometry geo = AssemblyGeometry.tower(era, TowerBoards.personalComputer(era), 3);
            final boolean atTop = geo.seat(SeatKind.PSU).y() == AssemblyGeometry.TOP;
            assertEquals(era.isAtMost(HardwareEra.TRANSITION), atTop, era + " supply");
        }
    }

    @Test
    void bracketHidden_onlyOnTheSupplyRow() {
        final AssemblyGeometry geo = AssemblyGeometry.tower(HardwareEra.STANDARD,
                TowerBoards.personalComputer(HardwareEra.STANDARD), 3);
        final List<Seat> cards = geo.seats(SeatKind.CARD);
        // The newer towers keep their supply at the foot, level with the last card.
        assertTrue(geo.bracketHidden(cards.get(cards.size() - 1)), "the last card shares the supply's row");
        assertTrue(!geo.bracketHidden(cards.get(0)), "the first card has its bracket");
    }

    @Test
    void secondRow_onlyOnATowerThatTakesAWorkstationBoard() {
        for (final Era era : HardwareEra.SERIES) {
            assertEquals(era.isAtLeast(HardwareEra.TRANSITION),
                    AssemblyGeometry.tower(era, TowerBoards.personalComputer(era), 3).secondRow(),
                    "a Personal Computer of " + era);
            assertEquals(era.isAtLeast(HardwareEra.LEGACY),
                    AssemblyGeometry.tower(era, TowerBoards.clusterManagementComputer(era), 4).secondRow(),
                    "a Cluster Management Computer of " + era);
            assertFalse(AssemblyGeometry.tower(era, TowerBoards.craftingComputer(era), 4).secondRow(),
                    "a Crafting Computer of " + era);
        }
    }

    @Test
    void secondRow_seatsTheSecondProcessorAndFourModulesUnderTheFirst() {
        final AssemblyGeometry one = AssemblyGeometry.tower(HardwareEra.STANDARD,
                TowerBoards.craftingComputer(HardwareEra.STANDARD), 4);
        final AssemblyGeometry two = AssemblyGeometry.tower(HardwareEra.STANDARD,
                TowerBoards.personalComputer(HardwareEra.STANDARD), 3);
        final List<Seat> cpus = two.seats(SeatKind.CPU);
        final List<Seat> ram = two.seats(SeatKind.RAM);
        assertEquals(2, cpus.size());
        assertEquals(8, ram.size());
        assertEquals(cpus.get(0).y() + AssemblyGeometry.PITCH, cpus.get(1).y(), "the second processor under the first");
        for (int i = 0; i < 4; i++) {
            assertEquals(ram.get(i).x(), ram.get(i + 4).x(), "module " + (i + 5) + " under module " + (i + 1));
            assertEquals(ram.get(i).y() + AssemblyGeometry.PITCH, ram.get(i + 4).y());
        }
        assertTrue(two.inSecondRow(cpus.get(1)) && two.inSecondRow(ram.get(4)) && !two.inSecondRow(ram.get(3)),
                "the second row's seats are told apart");
        assertEquals(one.seats(SeatKind.CARD).get(0).y() + AssemblyGeometry.PITCH, two.seats(SeatKind.CARD).get(0).y(),
                "the cards stand a row lower");
    }

    private static List<AssemblyGeometry> everyGeometry() {
        final List<AssemblyGeometry> out = new ArrayList<>();
        for (final Era era : HardwareEra.SERIES) {
            out.add(AssemblyGeometry.tower(era, TowerBoards.personalComputer(era), 4));
            out.add(AssemblyGeometry.tower(era, TowerBoards.craftingComputer(era), 4));
        }
        out.add(AssemblyGeometry.mainframe(4));
        out.add(AssemblyGeometry.server(5));
        return out;
    }
}
