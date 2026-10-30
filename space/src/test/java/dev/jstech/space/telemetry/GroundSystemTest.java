/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.telemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GroundSystemTest {

    @Test
    void works_needsAnAntennaAReceiverADecoderAndAGroundStation() {
        assertFalse(new GroundSystem(List.of(GroundSystem.Device.antenna(AntennaSize.SMALL, 0, 0),
                GroundSystem.Device.of(TelemetryRole.RECEIVER), GroundSystem.Device.of(TelemetryRole.DECODER)))
                .works());
        assertTrue(station(AntennaSize.SMALL).works());
    }

    @Test
    void keeps_onlyWithADataRecorder() {
        assertFalse(station(AntennaSize.SMALL).keeps());
        assertTrue(new GroundSystem(List.of(GroundSystem.Device.of(TelemetryRole.RECORDER))).keeps());
    }

    @Test
    void rate_addsTheAntennasUpAndReachIsTheLargest() {
        final GroundSystem two = new GroundSystem(List.of(GroundSystem.Device.antenna(AntennaSize.SMALL, 0, 0),
                GroundSystem.Device.antenna(AntennaSize.LARGE, 10, 0), GroundSystem.Device.of(TelemetryRole.RECEIVER),
                GroundSystem.Device.of(TelemetryRole.DECODER), GroundSystem.Device.of(TelemetryRole.GROUND_STATION)));
        assertEquals(AntennaSize.SMALL.rate() + AntennaSize.LARGE.rate(), two.rate());
        assertEquals(Optional.of(AntennaSize.Reach.DEEP), two.reach());
    }

    @Test
    void rate_isNoneWhileItDoesNotWork() {
        assertEquals(0, new GroundSystem(List.of(GroundSystem.Device.antenna(AntennaSize.LARGE, 0, 0))).rate());
        assertEquals(Optional.empty(), new GroundSystem(List.of()).reach());
    }

    /* A working ground system with one antenna of {@code size} at the world's middle. */
    private static GroundSystem station(final AntennaSize size) {
        return new GroundSystem(List.of(GroundSystem.Device.antenna(size, 0, 0),
                GroundSystem.Device.of(TelemetryRole.RECEIVER), GroundSystem.Device.of(TelemetryRole.DECODER),
                GroundSystem.Device.of(TelemetryRole.GROUND_STATION)));
    }
}
