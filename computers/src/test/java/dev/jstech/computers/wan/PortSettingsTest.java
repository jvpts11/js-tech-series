/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.wan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortSettingsTest {

    @Test
    void newPort_isOnAndOnlyLetsTheFarNetworkSee() {
        assertTrue(PortSettings.NEW.on());
        assertTrue(PortSettings.NEW.allows(WanRight.SEE));
        assertFalse(PortSettings.NEW.allows(WanRight.TAKE));
        assertFalse(PortSettings.NEW.allows(WanRight.SEND));
        assertFalse(PortSettings.NEW.allows(WanRight.CRAFT));
        assertFalse(PortSettings.NEW.relay());
    }

    @Test
    void ofPort_readsBackEachPortSetOnItsOwn() {
        final int packed = PortSettings.newPorts(4);
        final PortSettings changed = PortSettings.NEW.with(WanRight.CRAFT, true).withRelay(true).withOn(false);
        final int after = changed.into(packed, 3);
        assertEquals(changed, PortSettings.ofPort(after, 3));
        assertEquals(PortSettings.NEW, PortSettings.ofPort(after, 2), "its neighbours keep theirs");
        assertEquals(PortSettings.NEW, PortSettings.ofPort(after, 4));
    }

    @Test
    void bits_comeBackAsTheSameSwitches() {
        for (int bits = 0; bits < 1 << PortSettings.BITS; bits++) {
            assertEquals(bits, PortSettings.of(bits).bits());
        }
    }
}
