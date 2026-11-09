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

import dev.jstech.computers.block.CaseStyle;
import dev.jstech.computers.gui.layout.AssemblyGeometry.Box;
import dev.jstech.computers.gui.layout.AssemblyGeometry.Face;
import dev.jstech.computers.gui.layout.AssemblyGeometry.Seat;
import dev.jstech.computers.gui.layout.AssemblyGeometry.SeatKind;
import dev.jstech.computers.hardware.CaseCooling;
import dev.jstech.computers.hardware.FormFactor;
import dev.jstech.computers.hardware.TowerBoards;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
            final AssemblyGeometry geo = tower(era, TowerBoards.personalComputer(era), CaseCooling.NONE, 3);
            final boolean atTop = geo.seat(SeatKind.PSU).y() == AssemblyGeometry.TOP;
            assertEquals(era.isAtMost(HardwareEra.TRANSITION), atTop, era + " supply");
        }
    }

    @Test
    void bracketHidden_onlyOnTheSupplyRow() {
        final AssemblyGeometry geo = tower(HardwareEra.STANDARD,
                TowerBoards.personalComputer(HardwareEra.STANDARD), CaseCooling.NONE, 3);
        final List<Seat> cards = geo.seats(SeatKind.CARD);
        // The newer towers keep their supply at the foot, level with the last card.
        assertTrue(geo.bracketHidden(cards.get(cards.size() - 1)), "the last card shares the supply's row");
        assertTrue(!geo.bracketHidden(cards.get(0)), "the first card has its bracket");
    }

    @Test
    void secondRow_onlyOnATowerThatTakesAWorkstationBoard() {
        for (final Era era : HardwareEra.SERIES) {
            assertEquals(era.isAtLeast(HardwareEra.TRANSITION),
                    tower(era, TowerBoards.personalComputer(era), CaseCooling.NONE, 3).secondRow(),
                    "a Personal Computer of " + era);
            assertEquals(era.isAtLeast(HardwareEra.LEGACY),
                    tower(era, TowerBoards.clusterManagementComputer(era), CaseCooling.NONE, 4).secondRow(),
                    "a Cluster Management Computer of " + era);
            assertFalse(tower(era, TowerBoards.craftingComputer(era), CaseCooling.NONE, 4).secondRow(),
                    "a Crafting Computer of " + era);
        }
    }

    @Test
    void secondRow_seatsTheSecondProcessorAndFourModulesUnderTheFirst() {
        final AssemblyGeometry one = tower(HardwareEra.STANDARD,
                TowerBoards.craftingComputer(HardwareEra.STANDARD), CaseCooling.NONE, 4);
        final AssemblyGeometry two = tower(HardwareEra.STANDARD,
                TowerBoards.personalComputer(HardwareEra.STANDARD), CaseCooling.NONE, 3);
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

    @Test
    void coolingFace_seatsACoolerOverEveryProcessor() {
        final AssemblyGeometry geo = tower(HardwareEra.STANDARD, TowerBoards.personalComputer(HardwareEra.STANDARD),
                CaseCooling.HIGH_PERFORMANCE, 5);
        final List<Seat> cpus = geo.seats(SeatKind.CPU);
        final List<Seat> coolers = geo.seats(SeatKind.COOLER);
        assertEquals(cpus.size(), coolers.size());
        for (int i = 0; i < cpus.size(); i++) {
            assertEquals(cpus.get(i).x(), coolers.get(i).x(), "cooler " + i + " over its processor");
            assertEquals(cpus.get(i).y(), coolers.get(i).y(), "cooler " + i + " over its processor");
        }
        assertTrue(geo.inSecondRow(coolers.get(1)), "the second processor's cooler is the second row's");
    }

    @Test
    void coolingFace_seatsTheFansWhereTheCasesHaveThem() {
        // The High Performance case: one at the rear, the top pair over the memory, three down the front.
        final AssemblyGeometry geo = tower(HardwareEra.STANDARD, TowerBoards.personalComputer(HardwareEra.STANDARD),
                CaseCooling.HIGH_PERFORMANCE, 5);
        final List<Seat> fans = geo.seats(SeatKind.FAN);
        assertEquals(6, fans.size());
        assertEquals(new Seat(SeatKind.FAN, 0, AssemblyGeometry.REAR_X, AssemblyGeometry.TOP), fans.get(0));
        assertEquals(new Seat(SeatKind.FAN, 1, 98, AssemblyGeometry.TOP), fans.get(1));
        assertEquals(new Seat(SeatKind.FAN, 2, 118, AssemblyGeometry.TOP), fans.get(2));
        assertEquals(96, fans.get(3).y());
        assertEquals(136, fans.get(5).y(), "the last front fan level with the lowest seat of the board's face");
        assertEquals(geo.frontX(), fans.get(5).x());
    }

    @Test
    void coolingFace_putsTheOldTowersRearFanUnderTheirSupply() {
        final AssemblyGeometry geo = tower(HardwareEra.LEGACY, TowerBoards.personalComputer(HardwareEra.LEGACY),
                CaseCooling.LEGACY, 5);
        assertEquals(AssemblyGeometry.TOP + AssemblyGeometry.PITCH, geo.seats(SeatKind.FAN).get(0).y());
        assertEquals(CaseCooling.Side.FRONT, geo.fanSide(1));
    }

    @Test
    void coolingFace_centresALoneTopFan() {
        final AssemblyGeometry geo = tower(HardwareEra.STANDARD, TowerBoards.personalComputer(HardwareEra.STANDARD),
                CaseCooling.AESTHETIC, 5);
        assertEquals(108, geo.seats(SeatKind.FAN).get(1).x());
        assertEquals(108, geo.topCaptionX());
    }

    @Test
    void coolingFace_noneForACaseCooledFromOutside() {
        final AssemblyGeometry server = AssemblyGeometry.server(5);
        assertFalse(server.cooled());
        assertTrue(server.seats(Face.COOLING).isEmpty());
        final AssemblyGeometry mainframe = AssemblyGeometry.mainframe(6);
        assertEquals(4, mainframe.seats(SeatKind.COOLER).size(), "a cooler over each of the Mainframe's sockets");
        assertTrue(mainframe.seats(SeatKind.FAN).isEmpty(), "and no fan seats");
    }

    @Test
    void wanGateway_isTheMainframesCabinetWithNoOperationsStrip() {
        final AssemblyGeometry mainframe = AssemblyGeometry.mainframe(6);
        final AssemblyGeometry gateway = AssemblyGeometry.wanGateway(6);
        assertTrue(gateway.operations() == null, "a gateway runs no Operations of its own");
        assertEquals(mainframe.seats(SeatKind.CARD), gateway.seats(SeatKind.CARD), "the same six card slots");
        assertEquals(mainframe.seats(SeatKind.COOLER), gateway.seats(SeatKind.COOLER), "and coolers");
        assertEquals(2, gateway.switches(), "auto start, and the row of its lines' lamps");
        assertTrue(gateway.inventoryY() <= mainframe.inventoryY(), "its inventory never lower than the Mainframe's");
    }

    @Test
    void tabs_standUnderTheLampsInsideTheScreen() {
        for (final AssemblyGeometry geo : everyGeometry()) {
            if (!geo.cooled()) {
                continue;
            }
            final Box board = geo.tab(Face.BOARD);
            final Box cooling = geo.tab(Face.COOLING);
            assertTrue(board.y() >= geo.lamps().y() + geo.lamps().h(), "the tabs under the lamps");
            assertTrue(cooling.x() >= board.x() + board.w(), "the cooling tab right of the board's");
            assertTrue(cooling.y() + cooling.h() <= geo.height() && cooling.x() + cooling.w() <= geo.width(),
                    "the tabs inside the screen");
        }
    }

    private static AssemblyGeometry tower(final Era era, final Set<FormFactor> boards, final CaseCooling cooling,
                                          final int readouts) {
        return AssemblyGeometry.tower(era, boards, cooling, readouts);
    }

    private static List<AssemblyGeometry> everyGeometry() {
        final List<AssemblyGeometry> out = new ArrayList<>();
        for (final Era era : HardwareEra.SERIES) {
            for (final CaseStyle style : CaseStyle.values()) {
                out.add(AssemblyGeometry.tower(era, TowerBoards.personalComputer(era), style.cooling(era), 5));
                out.add(AssemblyGeometry.tower(era, TowerBoards.craftingComputer(era), style.cooling(era), 6));
                out.add(AssemblyGeometry.tower(era, TowerBoards.clusterManagementComputer(era), style.cooling(era),
                        6));
            }
        }
        out.add(AssemblyGeometry.mainframe(6));
        out.add(AssemblyGeometry.wanGateway(6));
        out.add(AssemblyGeometry.server(5));
        return out;
    }
}
