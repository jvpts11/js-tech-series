/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

class NetworkAddressesTest {

    @Test
    void card_isSixBytesInTheVirtualCardsBlock() {
        final String card = NetworkAddresses.card(42L);
        assertTrue(card.matches("52 54 00 [0-9A-F]{2} [0-9A-F]{2} [0-9A-F]{2}"), card);
    }

    @Test
    void card_isTheSameForTheSameMachine() {
        assertEquals(NetworkAddresses.card(-7L), NetworkAddresses.card(-7L));
    }

    @Test
    void host_staysInsideTheNetworkAndOffTheMainframe() {
        for (long seed = -500; seed < 500; seed++) {
            final String host = NetworkAddresses.host(seed);
            assertTrue(host.startsWith("10.0.0."), host);
            final int last = Integer.parseInt(host.substring("10.0.0.".length()));
            assertTrue(last >= 2 && last <= 254, host);
            assertNotEquals(NetworkAddresses.MAINFRAME, host);
        }
    }

    @Test
    void host_spreadsNeighbouringMachines() {
        final Set<String> hosts = new HashSet<>();
        for (long seed = 0; seed < 32; seed++) {
            hosts.add(NetworkAddresses.host(seed));
        }
        assertTrue(hosts.size() > 24, hosts.toString());
    }
}
