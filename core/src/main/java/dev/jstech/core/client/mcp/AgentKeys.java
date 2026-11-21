/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.mcp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.ApiStatus;

/**
 * The keys an agent holds down on the player's game: when the game asks whether a key is down, as it does for a
 * modifier, Control, Shift or Alt, and for a key a mod reads itself, it is told so of these as of the player's own
 * hand ({@code InputConstantsMixin}), with no message sent to the window. Each key is counted, so two calls holding it
 * at once both let go before it is up.
 */
@ApiStatus.Internal
public final class AgentKeys {

    private static final Map<Integer, Integer> HELD = new ConcurrentHashMap<>();

    private AgentKeys() {
    }

    /** Whether an agent holds the key {@code key}, a GLFW key code, down. */
    public static boolean holds(final int key) {
        return !HELD.isEmpty() && HELD.containsKey(key);
    }

    /** Holds the key {@code key} down, once more. */
    static void hold(final int key) {
        HELD.merge(key, 1, Integer::sum);
    }

    /** Lets go of the key {@code key} once; it is up when every hold of it let go. */
    static void release(final int key) {
        HELD.computeIfPresent(key, (code, holds) -> holds > 1 ? holds - 1 : null);
    }
}
