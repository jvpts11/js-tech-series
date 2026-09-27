/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.audio;

import dev.jstech.computers.operation.payload.SoundfoundryShareStatePayload;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * How Soundfoundry's sharing last stood on each machine this client has asked about, with what the last search found
 * and the songs the machine shares kept from the last time they were sent, so a window reads them whenever it draws.
 */
public final class SoundfoundryShares {

    private static final Map<BlockPos, Known> KNOWN = new HashMap<>();

    private SoundfoundryShares() {
    }

    /**
     * What the machine last said.
     *
     * @param state      its last answer
     * @param found      what its last search found
     * @param shared     the names of the songs it shares, as last sent
     * @param receivedAt when its last answer came, by the client's clock
     */
    public record Known(SoundfoundryShareStatePayload state, List<SoundfoundryShareStatePayload.Found> found,
                       List<String> shared, long receivedAt) {

        public Known {
            found = List.copyOf(found);
            shared = List.copyOf(shared);
        }
    }

    /** The machine said how its sharing stands. */
    public static void accept(final SoundfoundryShareStatePayload payload) {
        final Known before = KNOWN.get(payload.hostPos());
        final List<SoundfoundryShareStatePayload.Found> found = payload.found()
                .orElse(before != null ? before.found() : List.of());
        final List<String> shared = payload.shared().orElse(before != null ? before.shared() : List.of());
        KNOWN.put(payload.hostPos(), new Known(payload, found, shared, Util.getMillis()));
    }

    /** What that machine last said, or null. */
    @Nullable
    public static Known of(final BlockPos host) {
        return KNOWN.get(host);
    }

    /** The player left the server. */
    public static void clear() {
        KNOWN.clear();
    }
}
