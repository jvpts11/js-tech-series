/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import java.util.Set;
import org.jetbrains.annotations.ApiStatus;

/**
 * The former name of an {@link IsaSpec}, kept for one cycle so that an addon built against it still loads.
 *
 * <p>What the mod used to call a processor's architecture is its instruction set architecture, and the word
 * architecture now belongs to the design of a chip (Haswell, Zen 2), which is another thing. Build an
 * {@link IsaSpec} instead; this one only carries the same four values across to it.
 *
 * @deprecated use {@link IsaSpec}; this goes in the next cycle of the series
 */
@Deprecated(since = "0.5.0a", forRemoval = true)
public record ArchitectureSpec(String id, String name, int bits, Set<String> runs) {

    /** The same instruction set under its present name. */
    @ApiStatus.Experimental
    public IsaSpec toIsa() {
        return new IsaSpec(id, name, bits, runs);
    }
}
