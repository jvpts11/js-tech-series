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
 * An instruction set architecture (ISA): the instructions a machine's processor understands, and whose programs it
 * will run. It is not the processor's microarchitecture, the design that carries those instructions out (two chips
 * of one ISA can be built very differently); this is only the contract a program is compiled against. Nor is it the
 * ISA expansion bus of the early boards ({@link ExpansionBus#ISA}), which shares the three letters and nothing else.
 *
 * <p>The word size is the ISA's own rather than the era's. The two agree for this mod's own chips, but they are
 * separate things, and a mod is free to bring a 64-bit processor of an early era or the other way round.
 *
 * <p>Like a socket, the id is text in the {@code namespace:path} shape and nothing here touches Minecraft: which
 * programs a machine will run is worked out where the hardware is, and that is tested without the game.
 */
public record IsaSpec(String id, String name, int bits, Set<String> runs) {

    public IsaSpec {
        Objects.requireNonNull(id, "an ISA must have an id");
        Objects.requireNonNull(name, "an ISA must have a name");
        Objects.requireNonNull(runs, "an ISA must say what it runs");
        final int colon = id.indexOf(':');
        if (colon <= 0 || colon == id.length() - 1) {
            throw new IllegalArgumentException("an ISA id reads namespace:path; got '" + id + "'");
        }
        if (bits <= 0) {
            throw new IllegalArgumentException("an ISA has a word size; got " + bits);
        }
        runs = Set.copyOf(runs);
        if (!runs.contains(id)) {
            throw new IllegalArgumentException("'" + id + "' must run its own programs, and its list says it does not");
        }
    }

    /** Whether a program built for that ISA runs on a machine of this one. */
    public boolean runs(final String isa) {
        return this.runs.contains(isa);
    }

    /** Whether a program built for that ISA runs on a machine of this one. */
    public boolean runs(final IsaSpec isa) {
        return isa != null && runs(isa.id());
    }
}
