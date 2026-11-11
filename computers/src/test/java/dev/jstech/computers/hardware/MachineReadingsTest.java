/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MachineReadingsTest {

    private MachineReadings readings;

    @BeforeEach
    void setUp() {
        readings = new MachineReadings();
    }

    @Test
    void series_isTheMeanOfEachSampleWithTheNewestLast() {
        readings.read(0L, 300, 330, 60);
        readings.read(10L, 500, 550, 70);

        final int[] draw = readings.series(15L, MachineReadings.DRAW, MachineReadings.HOUR);

        assertEquals(50, draw.length);
        assertEquals(400, draw[49]);
        assertEquals(MachineReadings.NONE, draw[48]);
        assertEquals(65, readings.series(15L, MachineReadings.TEMPERATURE, MachineReadings.HOUR)[49]);
    }

    @Test
    void series_ofAMachineWithNoTemperatureHasNone() {
        readings.read(0L, 100, 110, MachineReadings.NONE);

        assertEquals(MachineReadings.NONE, readings.series(0L, MachineReadings.TEMPERATURE, MachineReadings.DAY)[47]);
        assertEquals(110, readings.series(0L, MachineReadings.LINE, MachineReadings.DAY)[47]);
    }

    @Test
    void series_forgetsWhatIsOlderThanItsPeriod() {
        readings.read(0L, 100, 110, 50);

        final int[] hour = readings.series(2_000L, MachineReadings.DRAW, MachineReadings.HOUR);
        final int[] week = readings.series(2_000L, MachineReadings.DRAW, MachineReadings.WEEK);

        for (final int sample : hour) {
            assertEquals(MachineReadings.NONE, sample);
        }
        assertEquals(100, week[165]);
    }

    @Test
    void joulesToday_addsTheLinesWattsEachSecondUntilTheDayTurns() {
        readings.read(0L, 100, 400, 50);
        readings.read(20L, 100, 600, 50);

        assertEquals(1_000L, readings.joulesToday(30L));
        assertEquals(0L, readings.joulesToday(24_000L));
    }

    @Test
    void restore_bringsBackWhatWasSaved() {
        readings.read(100L, 250, 280, 55);
        final MachineReadings back = new MachineReadings();

        back.restore(readings.saved());

        assertArrayEquals(readings.series(120L, MachineReadings.DRAW, MachineReadings.DAY),
                back.series(120L, MachineReadings.DRAW, MachineReadings.DAY));
        assertEquals(280L, back.joulesToday(120L));
    }
}
