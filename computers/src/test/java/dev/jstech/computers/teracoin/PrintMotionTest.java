/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.teracoin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class PrintMotionTest {

    private static final List<PrintMotion.Row> ROWS = List.of(new PrintMotion.Row(0, 2, 10),
            new PrintMotion.Row(1, 3, 9), new PrintMotion.Row(2, 4, 8));
    private static final double CLOSE = 1e-9;

    @Test
    void printing_startsTravellingToTheFirstRowsLeftEnd() {
        final PrintMotion.Nozzle nozzle = PrintMotion.printing(ROWS, 0.0);

        assertEquals(0, nozzle.row());
        assertEquals(-1, nozzle.laid());
        assertEquals(1, nozzle.sweep());
        assertEquals(PrintMotion.itemU0() + 2 * PrintMotion.TEXEL, nozzle.u(), CLOSE);
        assertEquals(PrintMotion.TEXEL, nozzle.top(), CLOSE);
    }

    @Test
    void printing_turnsItsWayEveryRow() {
        assertEquals(1, PrintMotion.printing(ROWS, 0.2).sweep());
        assertEquals(-1, PrintMotion.printing(ROWS, 0.5).sweep());
        assertEquals(1, PrintMotion.printing(ROWS, 0.9).sweep());
    }

    @Test
    void printing_layTheWholeLastRowAtTheEnd() {
        final PrintMotion.Nozzle nozzle = PrintMotion.printing(ROWS, 1.0);

        assertEquals(2, nozzle.row());
        assertEquals(4, nozzle.laid());
        assertEquals(3 * PrintMotion.TEXEL, nozzle.top(), CLOSE);
    }

    @Test
    void laid_countsTheTexelsBehindTheNozzleEitherWay() {
        final PrintMotion.Row row = ROWS.get(0);
        final double u0 = PrintMotion.itemU0();

        assertEquals(0, PrintMotion.laid(row, u0 + 2 * PrintMotion.TEXEL, 1));
        assertEquals(8, PrintMotion.laid(row, u0 + 10 * PrintMotion.TEXEL, 1));
        assertEquals(0, PrintMotion.laid(row, u0 + 10 * PrintMotion.TEXEL, -1));
        assertEquals(8, PrintMotion.laid(row, u0 + 2 * PrintMotion.TEXEL, -1));
    }

    @Test
    void prosa_standsReadyWithNothingToPrint() {
        final PrintMotion.Prosa pose = PrintMotion.prosa(List.of(), 0.0, -1.0);

        assertTrue(pose.gone());
        assertNull(pose.nozzle());
        assertEquals(PrintMotion.itemU0() + 8 * PrintMotion.TEXEL, pose.head(), CLOSE);
    }

    @Test
    void prosa_slidesItsBedOutAndBackThenDropsTheItem() {
        final PrintMotion.Prosa printing = PrintMotion.prosa(ROWS, 0.5, -1.0);
        final PrintMotion.Prosa sliding = PrintMotion.prosa(ROWS, 1.0, 1.1);
        final PrintMotion.Prosa done = PrintMotion.prosa(ROWS, 1.0, 5.0);

        assertFalse(printing.whole());
        assertEquals(0.0, printing.bed(), CLOSE);
        assertTrue(sliding.bed() < 0.0);
        assertTrue(done.gone());
        assertEquals(0.0, done.bed(), CLOSE);
    }

    @Test
    void rattan_sinksItsBedUnderTheNozzleAsItPrints() {
        final PrintMotion.Rattan first = PrintMotion.rattan(ROWS, 0.0, -1.0);
        final PrintMotion.Rattan last = PrintMotion.rattan(ROWS, 1.0, -1.0);

        assertEquals(PrintMotion.RATTAN_TIP - PrintMotion.TEXEL, first.bed(), CLOSE);
        assertEquals(PrintMotion.RATTAN_TIP - 3 * PrintMotion.TEXEL, last.bed(), CLOSE);
    }

    @Test
    void rattan_runsItsBeltToCarryTheItemOff() {
        final PrintMotion.Rattan start = PrintMotion.rattan(ROWS, 1.0, 0.0);
        final PrintMotion.Rattan done = PrintMotion.rattan(ROWS, 1.0, 5.0);

        assertEquals(0.0, start.belt(), CLOSE);
        assertEquals(PrintMotion.BELT_RUN, done.belt(), CLOSE);
        assertTrue(done.gone());
    }

    @Test
    void keyed_holdsTheEndsAndEasesBetweenKeys() {
        final double[][] keys = {{1.0, 10.0}, {3.0, 20.0}};

        assertEquals(10.0, PrintMotion.keyed(0.0, keys), CLOSE);
        assertEquals(15.0, PrintMotion.keyed(2.0, keys), CLOSE);
        assertEquals(20.0, PrintMotion.keyed(9.0, keys), CLOSE);
    }

    @Test
    void smooth_clampsAndEases() {
        assertEquals(0.0, PrintMotion.smooth(-1.0), CLOSE);
        assertEquals(0.5, PrintMotion.smooth(0.5), CLOSE);
        assertEquals(1.0, PrintMotion.smooth(2.0), CLOSE);
    }
}
