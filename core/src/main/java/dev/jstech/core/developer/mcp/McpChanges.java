/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.mcp;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * What a call changed in the game, told from two glimpses of it ({@link IMcpHost#glimpse}), one taken before the call
 * and one after: the game's tick after it, how many ticks went by while it ran, and every reading that is not what it
 * was, with what it was and what it is. The game goes on between an agent's calls, so the ticks are how the agent
 * knows how much of it went by, and the changes what its action did, without asking again.
 */
public final class McpChanges {

    /** The reading of a glimpse that is the game's tick: told as the time, never as a change. */
    public static final String TICK = "game_tick";
    /** How many of the game's ticks went by from the glimpse before a call to the one after it. */
    public static final String TICKS_TAKEN = "game_ticks_taken";
    /** The readings that changed, each with what it was ({@link #FROM}) and what it is ({@link #TO}). */
    public static final String CHANGED = "changed";
    public static final String FROM = "from";
    public static final String TO = "to";

    private McpChanges() {
    }

    /**
     * What is told of a call from the glimpse {@code before} it and the one {@code after} it: nothing when the game
     * showed nothing after it; only the time when it showed nothing before. A reading missing from one of them was
     * nothing there, written as null.
     */
    public static JsonObject told(final JsonObject before, final JsonObject after) {
        final JsonObject out = new JsonObject();
        if (after.isEmpty()) {
            return out;
        }
        if (isTick(after.get(TICK))) {
            out.add(TICK, after.get(TICK));
            if (isTick(before.get(TICK))) {
                out.addProperty(TICKS_TAKEN, after.get(TICK).getAsLong() - before.get(TICK).getAsLong());
            }
        }
        if (before.isEmpty()) {
            return out;
        }
        final Set<String> names = new LinkedHashSet<>(before.keySet());
        names.addAll(after.keySet());
        names.remove(TICK);
        final JsonObject changed = new JsonObject();
        for (final String name : names) {
            final JsonElement was = reading(before, name);
            final JsonElement is = reading(after, name);
            if (!was.equals(is)) {
                final JsonObject change = new JsonObject();
                change.add(FROM, was);
                change.add(TO, is);
                changed.add(name, change);
            }
        }
        if (!changed.isEmpty()) {
            out.add(CHANGED, changed);
        }
        return out;
    }

    private static boolean isTick(final JsonElement value) {
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber();
    }

    private static JsonElement reading(final JsonObject glimpse, final String name) {
        return glimpse.has(name) ? glimpse.get(name) : JsonNull.INSTANCE;
    }
}
