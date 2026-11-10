/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.boot;

import java.util.Locale;

/**
 * The addresses a machine's network card reads out when it boots from the network: its own hardware address, the
 * address the network hands it, and the address of the Mainframe that answers.
 *
 * <p>The network keeps no addresses of its own, so each is worked out from what does stay the same about a machine,
 * the number it is given: the same machine always reads out the same addresses, and two machines on one network
 * rarely share one. The Mainframe is the network's first address, as a router usually is, and every machine stands
 * in the one small network under it.
 */
public final class NetworkAddresses {

    /** The network's mask: one small network of up to 254 machines. */
    public static final String MASK = "255.255.255.0";

    /** The Mainframe's address, the first of the network, which hands every other machine its own. */
    public static final String MAINFRAME = "10.0.0.1";

    /* The first three bytes of every card's address: the block given to virtual network cards. */
    private static final String CARD_PREFIX = "52 54 00";
    /* The network's first three numbers, and the range a machine's last number is taken from: 2 to 254. */
    private static final String NETWORK_PREFIX = "10.0.0.";
    private static final int FIRST_HOST = 2;
    private static final int HOSTS = 253;

    private NetworkAddresses() {
    }

    /** The card's hardware address, six bytes in pairs of hex digits: "52 54 00 3A 91 0C". */
    public static String card(final long seed) {
        final long mixed = mix(seed);
        return String.format(Locale.ROOT, "%s %02X %02X %02X", CARD_PREFIX, (mixed >>> 16) & 0xFF,
                (mixed >>> 8) & 0xFF, mixed & 0xFF);
    }

    /** The address the network hands the machine: "10.0.0.23", never the Mainframe's own. */
    public static String host(final long seed) {
        return NETWORK_PREFIX + (FIRST_HOST + Math.floorMod(mix(seed) >>> 24, HOSTS));
    }

    /* Spreads a seed's bits so neighbouring machines get unlike addresses (the finaliser of SplitMix64). */
    private static long mix(final long seed) {
        long z = seed + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
