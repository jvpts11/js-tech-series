/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.crafting;

import dev.jstech.core.uuid.NodeUuid;
import java.util.Set;

/**
 * How a craft is to be carried out, beside what it makes: where it takes its raw materials from and how many of its
 * stages may run at once. An engine decides this with the plan; the Operations core only follows it.
 *
 * @param prefer      the servers the raw materials are taken from first, before any other
 * @param avoid       the servers nothing is taken from while another has it
 * @param maxParallel how many stages may run at once, and how many computers a craft fans out to; 0 for no cap
 */
public record CraftRouting(Set<NodeUuid> prefer, Set<NodeUuid> avoid, int maxParallel) {

    /** No preference: the index picks the servers and the hardware sets how much runs at once. */
    public static final CraftRouting NONE = new CraftRouting(Set.of(), Set.of(), 0);

    public CraftRouting {
        prefer = Set.copyOf(prefer);
        avoid = Set.copyOf(avoid);
        maxParallel = Math.max(0, maxParallel);
    }

    /** Whether this routes anything differently from what the core would do on its own. */
    public boolean any() {
        return !prefer.isEmpty() || !avoid.isEmpty() || maxParallel > 0;
    }

    /** {@code available} held to the cap, when there is one; never below one. */
    public int capped(final int available) {
        return Math.max(1, maxParallel > 0 ? Math.min(maxParallel, available) : available);
    }
}
