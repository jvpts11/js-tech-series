/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.bus;

import dev.jstech.core.uuid.NetworkUuid;

import java.util.HashMap;
import java.util.Map;

/**
 * Which of a network's buses goes first when they want the same thing: each bus that wants something says so with its
 * priority every tick it wants it, and a bus goes only while no bus of a higher priority has wanted the same thing on
 * the same network in the last couple of ticks. What is wanted is named by a word: an item's id for the buses taking
 * items out of the network, or the network's room for the ones bringing items in when it is full.
 */
public final class BusClaims {

    private final Map<NetworkUuid, Map<String, Claim>> claims = new HashMap<>();

    /** How many ticks a want lasts after it was last said: long enough for every bus to have ticked once. */
    public static final long LASTS = 2L;

    /**
     * Says that a bus of {@code priority} wants {@code what} on {@code network} at {@code tick}, and answers whether it
     * may go now: no bus of a higher priority wanted it in the last {@link #LASTS} ticks.
     */
    public boolean mayGo(final NetworkUuid network, final String what, final int priority, final long tick) {
        final Map<String, Claim> wants = claims.computeIfAbsent(network, n -> new HashMap<>());
        final Claim held = wants.get(what);
        if (held != null && held.priority() > priority && tick - held.tick() <= LASTS) {
            return false;
        }
        if (held == null || priority >= held.priority() || tick - held.tick() > LASTS) {
            wants.put(what, new Claim(priority, tick));
        }
        return true;
    }

    /** Forgets every want of {@code network}, as when it is gone. */
    public void forget(final NetworkUuid network) {
        claims.remove(network);
    }

    /* The highest priority that wanted a thing, and when it last said so. */
    private record Claim(int priority, long tick) {
    }
}
