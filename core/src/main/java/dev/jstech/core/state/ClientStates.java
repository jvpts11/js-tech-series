/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.state;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What a player's game was last sent of each synced state: one value a state, the one that is the player's to see.
 * Forgotten when they leave the server, so another server's values never show.
 */
public final class ClientStates {

    private static final Map<CoreState<?>, Object> VALUES = new ConcurrentHashMap<>();

    private ClientStates() {
    }

    /** Forgets every value, as the player leaves a server. */
    public static void clear() {
        VALUES.clear();
    }

    static void receive(final CoreState<?> state, final Object value) {
        VALUES.put(state, value);
    }

    @SuppressWarnings("unchecked")
    static <T> T get(final CoreState<T> state) {
        final Object value = VALUES.get(state);
        return value == null ? state.defaultValue() : (T) value;
    }
}
