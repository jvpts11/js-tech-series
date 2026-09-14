/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ProgramWindowsTest {

    private static Values.Obj window(final String title) {
        return UiWidgets.create(UiWidgets.WINDOW, List.of(title, 200, 100), 0);
    }

    @Test
    void open_numbersTheWindowsInTheOrderTheyOpen() {
        final ProgramWindows windows = new ProgramWindows();
        final Values.Obj first = window("first");
        final Values.Obj second = window("second");

        windows.open(first, 1);
        windows.open(second, 1);

        assertEquals(1L, first.get(UiWidgets.ID));
        assertEquals(2L, second.get(UiWidgets.ID));
        assertEquals(List.of(first, second), windows.all());
        assertSame(second, windows.of(2));
    }

    @Test
    void open_leavesAWindowAlreadyOpenAsItIs() {
        final ProgramWindows windows = new ProgramWindows();
        final Values.Obj first = window("first");
        windows.open(first, 1);

        windows.open(first, 1);

        assertEquals(1L, first.get(UiWidgets.ID));
        assertEquals(1, windows.all().size());
    }

    @Test
    void open_haltsPastTheMostWindows() {
        final ProgramWindows windows = new ProgramWindows();
        for (int i = 0; i < ProgramWindows.MOST_WINDOWS; i++) {
            windows.open(window("window " + i), 1);
        }

        assertThrows(Halt.class, () -> windows.open(window("one too many"), 7));
    }

    @Test
    void close_takesTheWindowOffTheDesktop() {
        final ProgramWindows windows = new ProgramWindows();
        final Values.Obj first = window("first");
        windows.open(first, 1);

        windows.close(first);

        assertTrue(windows.isEmpty());
        assertNull(windows.of(1));
        assertEquals(Boolean.FALSE, first.get(UiWidgets.OPEN));
    }

    @Test
    void endsNow_onceAPersonShutTheLastWindow() {
        final ProgramWindows windows = new ProgramWindows();
        final Values.Obj first = window("first");
        windows.open(first, 1);
        windows.close(first);

        windows.closedByPerson();

        assertTrue(windows.endsNow());
        assertFalse(windows.endsNow(), "saying yes is the end of it");
    }

    @Test
    void endsNow_notWhenTheProgramOpenedAnotherWindowOnHearingIt() {
        final ProgramWindows windows = new ProgramWindows();
        final Values.Obj first = window("first");
        windows.open(first, 1);
        windows.close(first);
        windows.closedByPerson();

        windows.open(window("second"), 1);

        assertFalse(windows.endsNow());
    }

    @Test
    void startFrom_carriesOnNumberingFromASave() {
        final ProgramWindows windows = new ProgramWindows();
        windows.startFrom(5, 9);
        final Values.Obj first = window("first");

        windows.open(first, 1);

        assertEquals(5L, first.get(UiWidgets.ID));
        assertEquals(9L, windows.nextWidgetId());
        windows.startFrom(0, -3);
        assertEquals(1L, windows.nextWindow());
        assertEquals(1L, windows.nextWidget());
    }
}
