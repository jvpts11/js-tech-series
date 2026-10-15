/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.heat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class HeatSourceTest {

    @Test
    void joules_ofAHeaterAreItsPowerTimesTheTime() {
        assertEquals(1_000.0, HeatSource.heater(1_000.0, 400.0).joules(300.0, 100.0, 1.0), 1e-9);
    }

    @Test
    void joules_ofAHeaterStopAtItsLimit() {
        final HeatSource heater = HeatSource.heater(1_000.0, 400.0);
        assertEquals(50.0, heater.joules(399.5, 100.0, 1.0), 1e-9);
        assertEquals(0.0, heater.joules(400.0, 100.0, 1.0), 1e-9);
        assertEquals(0.0, heater.joules(450.0, 100.0, 1.0), 1e-9, "a heater never cools");
    }

    @Test
    void joules_ofACoolerAreTakenOutDownToItsLimit() {
        final HeatSource cooler = HeatSource.cooler(1_000.0, 200.0);
        assertEquals(-1_000.0, cooler.joules(300.0, 100.0, 1.0), 1e-9);
        assertEquals(-50.0, cooler.joules(200.5, 100.0, 1.0), 1e-9);
        assertEquals(0.0, cooler.joules(150.0, 100.0, 1.0), 1e-9, "a cooler never warms");
    }

    @Test
    void joules_areNothingForABodyNotYetWarmedOrHoldingNothing() {
        assertEquals(0.0, HeatSource.heater(1_000.0, 400.0).joules(Double.NaN, 100.0, 1.0), 1e-9);
        assertEquals(0.0, HeatSource.heater(1_000.0, 400.0).joules(300.0, 0.0, 1.0), 1e-9);
    }

    @Test
    void apply_putsItsHeatInThePortAndSaysHowMuch() {
        final Port port = new Port(300.0, 100.0);
        final double joules = HeatSource.heater(1_000.0, 400.0).apply(port, 0.5);
        assertEquals(500.0, joules, 1e-9);
        assertEquals(305.0, port.temperature(), 1e-9);
    }

    @Test
    void heatSource_refusesALimitBelowNothing() {
        assertThrows(IllegalArgumentException.class, () -> HeatSource.heater(1_000.0, -1.0));
    }

    /* A body of a fixed capacity, as a block's would be. */
    private static final class Port implements IHeatPort {

        private double temperature;
        private final double capacity;

        private Port(final double temperature, final double capacity) {
            this.temperature = temperature;
            this.capacity = capacity;
        }

        @Override
        public double temperature() {
            return this.temperature;
        }

        @Override
        public double capacity() {
            return this.capacity;
        }

        @Override
        public double conductivity() {
            return 1.0;
        }

        @Override
        public void exchange(final double joules) {
            this.temperature += joules / this.capacity;
        }
    }
}
