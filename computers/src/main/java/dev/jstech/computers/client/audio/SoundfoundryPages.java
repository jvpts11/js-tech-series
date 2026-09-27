/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.audio;

import dev.jstech.computers.operation.payload.SoundfoundryPagePayload;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * The page of the Standard Soundfoundry each machine last answered, so its window draws it whenever it draws, and
 * when it came, so the window knows how stale it is.
 */
public final class SoundfoundryPages {

    private static final Map<BlockPos, Known> KNOWN = new HashMap<>();

    private SoundfoundryPages() {
    }

    /** A page a machine answered, and when. */
    public record Known(SoundfoundryPagePayload page, long receivedAt) {

        /** Whether this is the page asked for: the same page, of the same thing. */
        public boolean is(final int page, final String arg) {
            return this.page.page() == page && this.page.arg().equals(arg);
        }
    }

    /** The machine answered a page. */
    public static void accept(final SoundfoundryPagePayload payload) {
        KNOWN.put(payload.hostPos(), new Known(payload, Util.getMillis()));
    }

    /** The page that machine last answered, or null. */
    @Nullable
    public static Known of(final BlockPos host) {
        return KNOWN.get(host);
    }

    /** The player left the server. */
    public static void clear() {
        KNOWN.clear();
    }
}
