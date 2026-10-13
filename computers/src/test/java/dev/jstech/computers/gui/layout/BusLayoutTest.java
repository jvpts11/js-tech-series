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

import dev.jstech.computers.bus.BusAbilities;
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.tier.HardwareEra;
import java.util.List;
import org.junit.jupiter.api.Test;

class BusLayoutTest {

    @Test
    void frame_isCleanForEveryEraAndEveryKindOfBus() {
        for (final BusLayout.Window window : BusLayout.Window.values()) {
            for (final HardwareEra era : HardwareEra.values()) {
                final GuiLayout l = BusLayout.frame(BusLayout.abilities(era, window), window);
                assertTrue(l.isClean(), window + " " + era + ": " + l.overlaps() + " " + l.outOfBounds());
            }
        }
    }

    @Test
    void content_isCleanWithEverythingAtItsMost() {
        for (final BusLayout.Window window : BusLayout.Window.values()) {
            for (final HardwareEra era : HardwareEra.values()) {
                final GuiLayout l = BusLayout.content(BusLayout.Shape.most(BusLayout.abilities(era, window),
                        window));
                assertTrue(l.isClean(), window + " " + era + ": " + l.overlaps() + " " + l.outOfBounds());
            }
        }
    }

    @Test
    void configureHeight_neverPassesWhatAScreenHolds() {
        for (final BusLayout.Window window : BusLayout.Window.values()) {
            for (final HardwareEra era : HardwareEra.values()) {
                assertTrue(BusLayout.configureHeight(BusLayout.abilities(era, window), window)
                        <= BusLayout.MAX_HEIGHT, window + " " + era);
            }
        }
        assertTrue(BusLayout.ACTIVITY_HEIGHT <= BusLayout.MAX_HEIGHT);
        assertTrue(BusLayout.softwareHeight(10_000) <= BusLayout.MAX_HEIGHT);
    }

    @Test
    void configureView_showsTheShortEraWholeAndScrollsTheLongOnes() {
        final BusAbilities legacy = BusAbilities.of(HardwareEra.LEGACY);
        final BusAbilities advanced = BusAbilities.of(HardwareEra.ADVANCED);

        assertEquals(BusLayout.contentHeight(BusLayout.Shape.most(legacy, BusLayout.Window.MOVER)),
                BusLayout.configureView(legacy, BusLayout.Window.MOVER));
        assertEquals(BusLayout.CONFIGURE_VIEW_MOST, BusLayout.configureView(advanced, BusLayout.Window.MOVER));
    }

    @Test
    void rows_giveEachEraTheRowsItsWindowShows() {
        assertEquals(List.of(BusLayout.Kind.INTRO, BusLayout.Kind.NOW, BusLayout.Kind.MODE, BusLayout.Kind.POWER,
                BusLayout.Kind.SPEED), kinds(HardwareEra.VINTAGE, BusLayout.Window.MOVER, 0, 0));
        assertEquals(List.of(BusLayout.Kind.FILTER, BusLayout.Kind.FILTER_MODE, BusLayout.Kind.KEEP,
                BusLayout.Kind.MAX, BusLayout.Kind.MODE, BusLayout.Kind.POWER, BusLayout.Kind.SPEED),
                kinds(HardwareEra.LEGACY, BusLayout.Window.MOVER, 0, 0));
        assertTrue(kinds(HardwareEra.TRANSITION, BusLayout.Window.MOVER, 2, 0).containsAll(List.of(
                BusLayout.Kind.ITEM, BusLayout.Kind.ADD_ITEM)));
        assertFalse(kinds(HardwareEra.TRANSITION, BusLayout.Window.MOVER, 2, 0).contains(BusLayout.Kind.KEEP));
        assertTrue(kinds(HardwareEra.STANDARD, BusLayout.Window.MOVER, 0, 2).containsAll(List.of(
                BusLayout.Kind.PRIORITY, BusLayout.Kind.CONDITION, BusLayout.Kind.ADD_CONDITION)));
        assertTrue(kinds(HardwareEra.ADVANCED, BusLayout.Window.MOVER, 0, 0).containsAll(List.of(
                BusLayout.Kind.TAGS, BusLayout.Kind.MATCH)));
    }

    @Test
    void rows_giveTheExternalStorageBusWhatItsInventoryHoldsAndHowItIsUsed() {
        assertEquals(List.of(BusLayout.Kind.HOLDS, BusLayout.Kind.INTRO),
                kinds(HardwareEra.VINTAGE, BusLayout.Window.EXTERNAL, 0, 0));
        assertEquals(List.of(BusLayout.Kind.HOLDS, BusLayout.Kind.FILTER, BusLayout.Kind.FILTER_MODE,
                BusLayout.Kind.ACCESS, BusLayout.Kind.NOTE),
                kinds(HardwareEra.LEGACY, BusLayout.Window.EXTERNAL, 0, 0));
        assertTrue(kinds(HardwareEra.STANDARD, BusLayout.Window.EXTERNAL, 0, 0).contains(BusLayout.Kind.PRIORITY));
        assertFalse(kinds(HardwareEra.STANDARD, BusLayout.Window.EXTERNAL, 0, 0).contains(BusLayout.Kind.MODE));
    }

    @Test
    void rows_giveTheCraftingPartsTheirFilterThenTheirCraftingLines() {
        for (final BusLayout.Window window : List.of(BusLayout.Window.ROUTER, BusLayout.Window.RECEIVING)) {
            final BusLayout.Shape shape = new BusLayout.Shape(BusLayout.abilities(HardwareEra.STANDARD, window),
                    window, 2, 0, 1, false, 2, 0, false, 3,
                    List.of(BusLayout.craftLine(false), BusLayout.craftLine(true), BusLayout.craftBox(2)));
            final List<BusLayout.Row> rows = BusLayout.rows(shape);

            assertEquals(BusLayout.Kind.FILTER, rows.get(0).kind(), window.toString());
            assertEquals(3, rows.stream().filter(r -> r.kind() == BusLayout.Kind.CRAFT_LINE).count(),
                    window.toString());
            assertEquals(BusLayout.craftLine(true), rows.get(2).height(), window.toString());
            assertFalse(rows.stream().anyMatch(r -> r.kind() == BusLayout.Kind.SPEED), window.toString());
        }
        assertTrue(BusLayout.craftLine(true) > BusLayout.craftLine(false));
        assertEquals(2 * BusLayout.craftBox(1) - 6, BusLayout.craftBox(2));
    }

    @Test
    void rows_offerNoMoreConditionsPastTheMost() {
        assertFalse(kinds(HardwareEra.STANDARD, BusLayout.Window.MOVER, 0, BusLayout.MOST_CONDITIONS)
                .contains(BusLayout.Kind.ADD_CONDITION));
    }

    @Test
    void rows_stackWithNoGapsOrOverlaps() {
        final List<BusLayout.Row> rows = BusLayout.rows(BusLayout.Shape.most(BusAbilities.of(HardwareEra.ADVANCED),
                BusLayout.Window.MOVER));
        for (int i = 1; i < rows.size(); i++) {
            assertEquals(rows.get(i - 1).y() + rows.get(i - 1).height(), rows.get(i).y());
        }
    }

    @Test
    void chips_wrapOntoANewLineWhenTheyRunPastTheRight() {
        final List<int[]> at = BusLayout.chips(List.of(50, 50, 60));

        assertEquals(BusLayout.CONTROL_X, at.get(0)[0]);
        assertEquals(0, at.get(1)[1]);
        assertEquals(1, at.get(2)[1]);
    }

    @Test
    void frame_endsTheLongestTitleBeforeTheLamp() {
        final GuiLayout l = BusLayout.frame(BusAbilities.of(HardwareEra.STANDARD), BusLayout.Window.MOVER);
        final GuiLayout.Box title = l.boxAt("title");

        assertTrue(title.x() + title.width() < BusLayout.LAMP_X);
    }

    private static List<BusLayout.Kind> kinds(final HardwareEra era, final BusLayout.Window window, final int items,
                                              final int conditions) {
        final BusLayout.Shape shape = new BusLayout.Shape(BusLayout.abilities(era, window), window, 2, items, 1,
                false, 2, conditions, false, 3);
        return BusLayout.rows(shape).stream().map(BusLayout.Row::kind).distinct().toList();
    }
}
