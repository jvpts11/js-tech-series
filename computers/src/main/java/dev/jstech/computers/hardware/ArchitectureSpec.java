/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import java.util.Objects;
import java.util.Set;

/**
 * A processor architecture: what a machine's instruction set is, and whose programs it will run.
 *
 * <p>The word size is the architecture's own rather than the era's. The two agree for this mod's own chips, but
 * they are separate things, and a mod is free to bring a 64-bit processor of an early era or the other way round.
 *
 * <p>Like a socket, the id is text in the {@code namespace:path} shape and nothing here touches Minecraft: which
 * programs a machine will run is worked out where the hardware is, and that is tested without the game.
 */
public record ArchitectureSpec(String id, String name, int bits, Set<String> runs) {

    public ArchitectureSpec {
        Objects.requireNonNull(id, "an architecture must have an id");
        Objects.requireNonNull(name, "an architecture must have a name");
        Objects.requireNonNull(runs, "an architecture must say what it runs");
        final int colon = id.indexOf(':');
        if (colon <= 0 || colon == id.length() - 1) {
            throw new IllegalArgumentException("an architecture id reads namespace:path; got '" + id + "'");
        }
        if (bits <= 0) {
            throw new IllegalArgumentException("an architecture has a word size; got " + bits);
        }
        runs = Set.copyOf(runs);
        if (!runs.contains(id)) {
            throw new IllegalArgumentException("'" + id + "' must run its own programs, and its list says it does not");
        }
    }

    /** Whether a program built for that architecture runs on a machine of this one. */
    public boolean runs(final String architecture) {
        return this.runs.contains(architecture);
    }

    /** Whether a program built for that architecture runs on a machine of this one. */
    public boolean runs(final ArchitectureSpec architecture) {
        return architecture != null && runs(architecture.id());
    }
}
