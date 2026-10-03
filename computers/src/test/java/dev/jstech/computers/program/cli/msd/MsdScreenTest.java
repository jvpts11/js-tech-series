/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.msd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class MsdScreenTest {

    @Test
    void render_drawsTheBarAndEveryButtonWithWhatItFound() {
        final List<String> screen = MsdScreen.render(MsdState.OPENING, data());

        assertEquals(MsdState.DEFAULT_ROWS, screen.size(), "a screen is a glass's worth of rows");
        assertTrue(screen.getFirst().contains("Midsoft Diagnostics 2.01") && screen.getFirst().contains("F3=Exit"));
        assertTrue(says(screen, "Computer...") && says(screen, "Integra 486DX2"));
        assertTrue(says(screen, "Memory...") && says(screen, "640K, 15360K Ext"));
        assertTrue(says(screen, "LPT Ports...") && says(screen, "COM Ports..."));
        assertTrue(screen.get(MsdScreen.GRID_TOP).startsWith(">Computer..."), "the first button opens picked");
        assertFalse(says(screen, "LPT and COM Ports"), "the dialog is not up");
        screen.forEach(line -> assertEquals(MsdState.DEFAULT_COLUMNS, line.length(), "every row fills the glass"));
    }

    @Test
    void render_putsThePortsDialogUnderTheButtonsWithThePickedPortMarked() {
        final List<String> screen = MsdScreen.render(MsdState.OPENING.openingPorts(1), data());

        assertTrue(screen.get(MsdScreen.DIALOG_TOP).contains("LPT and COM Ports"));
        assertTrue(screen.get(MsdScreen.PORTS_TOP).contains("COM1:")
                && screen.get(MsdScreen.PORTS_TOP).contains("Redstone Interface \"Well\"")
                && screen.get(MsdScreen.PORTS_TOP).contains("Off (disabled)"));
        assertTrue(screen.get(MsdScreen.PORTS_TOP + 1).contains("|>LPT1:"), "the picked port is marked");
        assertTrue(screen.get(MsdScreen.dialogButtonsRow(2)).contains("< OK >")
                && screen.get(MsdScreen.dialogButtonsRow(2)).contains("<Disable>"));
        assertFalse(screen.get(MsdScreen.GRID_TOP).startsWith(">"), "no button is picked under the dialog");
    }

    @Test
    void portsSaid_andPickedSaid_readTheDialogBackOffTheGlass() {
        final List<String> screen = MsdScreen.render(MsdState.OPENING.openingPorts(1), data());

        assertEquals(2, MsdScreen.portsSaid(screen));
        assertEquals(1, MsdScreen.pickedSaid(screen));
        assertEquals(0, MsdScreen.portsSaid(MsdScreen.render(MsdState.OPENING, data())),
                "no dialog, no ports");
    }

    @Test
    void buttonAt_findsTheButtonDrawnAtThatPlace() {
        final int right = MsdState.DEFAULT_COLUMNS / 2 + 3;

        assertEquals(0, MsdScreen.buttonAt(MsdScreen.GRID_TOP, 3, MsdState.DEFAULT_COLUMNS));
        assertEquals(MsdScreen.COM_BUTTON, MsdScreen.buttonAt(MsdScreen.GRID_TOP + 3 * MsdScreen.GRID_STEP, right,
                MsdState.DEFAULT_COLUMNS));
        assertEquals(-1, MsdScreen.buttonAt(MsdScreen.GRID_TOP + 1, 3, MsdState.DEFAULT_COLUMNS),
                "between two rows of buttons");
        assertEquals(-1, MsdScreen.buttonAt(0, 3, MsdState.DEFAULT_COLUMNS), "on the bar");
    }

    @Test
    void dialogButtonAt_findsEachButtonWhereItIsDrawn() {
        final String row = MsdScreen.render(MsdState.OPENING.openingPorts(0), data())
                .get(MsdScreen.dialogButtonsRow(2));

        for (int i = 0; i < MsdScreen.DIALOG_BUTTONS.length; i++) {
            final int column = row.indexOf(MsdScreen.DIALOG_BUTTONS[i]);
            assertEquals(i, MsdScreen.dialogButtonAt(column, MsdState.DEFAULT_COLUMNS));
            assertEquals(i, MsdScreen.dialogButtonAt(column + MsdScreen.DIALOG_BUTTONS[i].length() - 1,
                    MsdState.DEFAULT_COLUMNS));
        }
        assertEquals(-1, MsdScreen.dialogButtonAt(0, MsdState.DEFAULT_COLUMNS));
    }

    @Test
    void render_keepsToAGlassShorterThanTheScreen() {
        final List<String> screen = MsdScreen.render(MsdState.OPENING.on(80, 12).openingPorts(0), data());

        assertEquals(12, screen.size());
    }

    private static MsdScreen.Data data() {
        return new MsdScreen.Data(List.of("Integra 486DX2", "640K, 15360K Ext", "Pyrix 3D Blaster", "Thin coax",
                "MC-DOS", "A: C:", "1", "1"), List.of(
                new MsdScreen.PortRow("COM1:", "Redstone Interface \"Well\"", "Off (disabled)"),
                new MsdScreen.PortRow("LPT1:", "Floppy Drive", "On")));
    }

    private static boolean says(final List<String> screen, final String text) {
        return screen.stream().anyMatch(line -> line.contains(text));
    }
}
