/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.audio;

import dev.jstech.computers.operation.payload.SoundfoundryStatePayload;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * How Soundfoundry last stood on each machine this client has asked about, with the playlist kept from the last time
 * it was sent, so a window reads it whenever it draws.
 */
public final class SoundfoundryStates {

    private static final Map<BlockPos, Known> KNOWN = new HashMap<>();

    private SoundfoundryStates() {
    }

    /** What the machine last said, with its playlist; null when it has said nothing yet. */
    public record Known(SoundfoundryStatePayload state, List<SoundfoundryStatePayload.Song> songs, long receivedAt) {

        public Known {
            songs = List.copyOf(songs);
        }

        /** How far into its song it has got by now, counting on from when it last said. */
        public long position() {
            if (state.status() != SoundfoundryStatePayload.PLAYING) {
                return state.position();
            }
            final long now = state.position() + (Util.getMillis() - receivedAt);
            return state.playing().millis() > 0 ? Math.min(now, state.playing().millis()) : now;
        }
    }

    /** The machine said how it stands. */
    public static void accept(final SoundfoundryStatePayload payload) {
        final Known before = KNOWN.get(payload.hostPos());
        final List<SoundfoundryStatePayload.Song> songs = payload.songs()
                .orElse(before != null ? before.songs() : List.of());
        KNOWN.put(payload.hostPos(), new Known(payload, songs, Util.getMillis()));
    }

    /** What that machine last said, or null. */
    @Nullable
    public static Known of(final BlockPos host) {
        return KNOWN.get(host);
    }

    /** The revision of the playlist this client holds for that machine, or -1 for none. */
    public static int revisionOf(final BlockPos host) {
        final Known known = KNOWN.get(host);
        return known == null ? -1 : known.state().revision();
    }

    /** The player left the server. */
    public static void clear() {
        KNOWN.clear();
    }
}
