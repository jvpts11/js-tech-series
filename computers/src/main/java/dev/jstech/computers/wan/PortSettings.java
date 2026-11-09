/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.wan;

/**
 * The switches of one port of a WAN Gateway Computer, as Nouvell BorderLink sets them: what the network at the far end
 * of its line may do on this one when the two are federated (see its items, take them, send items here, have them
 * crafted here), whether the port passes requests from that network on to the other networks this one is federated
 * with, and whether the port is on at all (computers/docs/design/network.md).
 *
 * <p>Kept as six bits a port, so a gateway's four ports are one number on the block entity.
 *
 * @param see   the far network sees this one's items
 * @param take  the far network takes items from this one
 * @param send  the far network sends items into this one
 * @param craft the far network has items crafted here
 * @param relay requests from the far network pass on through this one to the networks beyond it
 * @param on    the port is on; a port off carries nothing
 */
public record PortSettings(boolean see, boolean take, boolean send, boolean craft, boolean relay, boolean on) {

    /** A new port: on, its far network seeing this one's items and doing nothing else, relaying nothing. */
    public static final PortSettings NEW = new PortSettings(true, false, false, false, false, true);
    /** How many bits a port's switches take. */
    public static final int BITS = 6;
    private static final int SEE = 1;
    private static final int TAKE = 2;
    private static final int SEND = 4;
    private static final int CRAFT = 8;
    private static final int RELAY = 16;
    private static final int ON = 32;
    private static final int MASK = (1 << BITS) - 1;

    /** The switches written in {@code bits}. */
    public static PortSettings of(final int bits) {
        return new PortSettings((bits & SEE) != 0, (bits & TAKE) != 0, (bits & SEND) != 0, (bits & CRAFT) != 0,
                (bits & RELAY) != 0, (bits & ON) != 0);
    }

    /** The switches of port {@code port}, 1 to 4, in {@code packed}, every port's bits side by side. */
    public static PortSettings ofPort(final int packed, final int port) {
        return of(packed >>> (BITS * (port - 1)) & MASK);
    }

    /** All the ports of a new gateway. */
    public static int newPorts(final int ports) {
        int packed = 0;
        for (int port = 1; port <= ports; port++) {
            packed = NEW.into(packed, port);
        }
        return packed;
    }

    /** {@code packed} with port {@code port}'s switches set to these. */
    public int into(final int packed, final int port) {
        final int shift = BITS * (port - 1);
        return packed & ~(MASK << shift) | bits() << shift;
    }

    /** These switches as six bits. */
    public int bits() {
        return (see ? SEE : 0) | (take ? TAKE : 0) | (send ? SEND : 0) | (craft ? CRAFT : 0) | (relay ? RELAY : 0)
                | (on ? ON : 0);
    }

    /** Whether the far network may do {@code right} here. */
    public boolean allows(final WanRight right) {
        return switch (right) {
            case SEE -> see;
            case TAKE -> take;
            case SEND -> send;
            case CRAFT -> craft;
        };
    }

    /** These switches with {@code right} set to {@code allowed}. */
    public PortSettings with(final WanRight right, final boolean allowed) {
        return switch (right) {
            case SEE -> new PortSettings(allowed, take, send, craft, relay, on);
            case TAKE -> new PortSettings(see, allowed, send, craft, relay, on);
            case SEND -> new PortSettings(see, take, allowed, craft, relay, on);
            case CRAFT -> new PortSettings(see, take, send, allowed, relay, on);
        };
    }

    public PortSettings withRelay(final boolean relaying) {
        return new PortSettings(see, take, send, craft, relaying, on);
    }

    public PortSettings withOn(final boolean switchedOn) {
        return new PortSettings(see, take, send, craft, relay, switchedOn);
    }
}
