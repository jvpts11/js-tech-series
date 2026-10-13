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
    private long sweptAt;

    /** How many ticks a want lasts after it was last said: long enough for every bus to have ticked once. */
    public static final long LASTS = 2L;
    /** How many ticks pass between two sweeps that drop the wants nobody says any more. */
    public static final long SWEEP_EVERY = 1200L;

    /**
     * Says that a bus of {@code priority} wants {@code what} on {@code network} at {@code tick}, and answers whether it
     * may go now: no bus of a higher priority wanted it in the last {@link #LASTS} ticks.
     */
    public boolean mayGo(final NetworkUuid network, final String what, final int priority, final long tick) {
        sweepIfDue(tick);
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

    /** How many wants are held, expired ones included until the next sweep. */
    public int size() {
        int total = 0;
        for (final Map<String, Claim> wants : claims.values()) {
            total += wants.size();
        }
        return total;
    }

    /*
     * Drops the wants that have lapsed and the networks left with none, now and then, so a network that is gone or an
     * item nobody wants any more does not stay in the maps for as long as the level is loaded.
     */
    private void sweepIfDue(final long tick) {
        if (tick >= sweptAt && tick - sweptAt < SWEEP_EVERY) {
            return;
        }
        sweptAt = tick;
        claims.values().removeIf(wants -> {
            wants.values().removeIf(claim -> tick - claim.tick() > LASTS);
            return wants.isEmpty();
        });
    }

    /* The highest priority that wanted a thing, and when it last said so. */
    private record Claim(int priority, long tick) {
    }
}
