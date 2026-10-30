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

import dev.jstech.space.exploration.ReliefCoverage;
import java.util.List;
import org.junit.jupiter.api.Test;

class ContactTest {

    private static final double CLOSE = 1e-9;

    @Test
    void heard_geostationaryOnlyByAnAntennaUnderItThatHearsThatFar() {
        final Contact.Target over = new Contact.Target(Contact.Where.GEOSTATIONARY, 10, false);
        assertTrue(Contact.heard(station(AntennaSize.MEDIUM, 0, 0), over, 0), "a medium antenna under it");
        assertFalse(Contact.heard(station(AntennaSize.SMALL, 0, 0), over, 0), "a small one does not hear so far");
        assertFalse(Contact.heard(station(AntennaSize.MEDIUM, 120, 0), over, 0), "one round the world does not see it");
        assertTrue(Contact.heard(station(AntennaSize.MEDIUM, 0, 0), over, Long.MAX_VALUE / 2)
                && !Contact.heard(station(AntennaSize.MEDIUM, 120, 0), over, Long.MAX_VALUE / 2),
                "and it stays where it is: in sight or not, whatever the time");
    }

    @Test
    void heard_lowOrbitOnlyWhileItPassesOver() {
        final Contact.Target low = new Contact.Target(Contact.Where.LOW, 0, false);
        final GroundSystem under = station(AntennaSize.SMALL, 0, 0);
        assertTrue(Contact.heard(under, low, 0), "it crosses the antenna's place as it sets out");
        assertFalse(Contact.heard(under, low, ReliefCoverage.PASS_TICKS / 2), "half a pass on, round the world");
        int heard = 0;
        for (long tick = 0; tick < ReliefCoverage.PASS_TICKS * ReliefCoverage.PASSES; tick += 10) {
            heard += Contact.heard(under, low, tick) ? 1 : 0;
        }
        assertTrue(heard > 0 && heard < ReliefCoverage.PASS_TICKS * ReliefCoverage.PASSES / 10 / 4,
                "over a day it talks in passes, a small part of the time: " + heard);
    }

    @Test
    void heard_theMoonByAMediumAntennaTheOtherWorldsByALargeOne() {
        final Contact.Target moon = new Contact.Target(Contact.Where.MOON, 0, false);
        final Contact.Target mars = new Contact.Target(Contact.Where.DEEP_SPACE, 0, false);
        assertFalse(Contact.heard(station(AntennaSize.SMALL, 0, 0), moon, 0));
        assertTrue(Contact.heard(station(AntennaSize.MEDIUM, 0, 0), moon, 0));
        assertFalse(Contact.heard(station(AntennaSize.MEDIUM, 0, 0), mars, 0));
        assertTrue(Contact.heard(station(AntennaSize.LARGE, 0, 0), mars, 0));
    }

    @Test
    void heard_alwaysThroughARelayButNeverByASystemThatDoesNotWork() {
        final Contact.Target relayed = new Contact.Target(Contact.Where.DEEP_SPACE, 0, true);
        assertTrue(Contact.heard(station(AntennaSize.SMALL, 0, 0), relayed, 0));
        assertFalse(Contact.heard(new GroundSystem(List.of(GroundSystem.Device.antenna(AntennaSize.LARGE, 0, 0))),
                relayed, 0));
    }

    @Test
    void apart_isTheGreatCircleBetweenTwoPoints() {
        assertEquals(90, Contact.apart(0, 0, 90, 0), CLOSE);
        assertEquals(90, Contact.apart(0, 0, 0, 90), CLOSE);
        assertEquals(0, Contact.apart(30, 20, 30, 20), CLOSE);
    }

    /* A working ground system with one antenna of {@code size} at {@code longitude}, {@code latitude}. */
    private static GroundSystem station(final AntennaSize size, final double longitude, final double latitude) {
        return new GroundSystem(List.of(GroundSystem.Device.antenna(size, longitude, latitude),
                GroundSystem.Device.of(TelemetryRole.RECEIVER), GroundSystem.Device.of(TelemetryRole.DECODER),
                GroundSystem.Device.of(TelemetryRole.GROUND_STATION)));
    }
}
