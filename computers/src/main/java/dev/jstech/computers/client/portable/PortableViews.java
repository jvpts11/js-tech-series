/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.portable;

import dev.jstech.computers.operation.payload.portable.PortableViewPayload;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * What the player's game knows of the portables its player carries: what each keeps on the server, as it was last
 * told, and where each one's system is. Let go when the player leaves the world.
 */
public final class PortableViews {

    private static final Map<UUID, PortableViewPayload> VIEWS = new HashMap<>();
    private static final Map<UUID, MobileSession> SESSIONS = new HashMap<>();

    private PortableViews() {
    }

    /** The server told what the device keeps. */
    public static void take(final PortableViewPayload view) {
        VIEWS.put(view.device(), view);
    }

    /** What the device {@code device} keeps, as last told, or null before it was told anything. */
    @Nullable
    public static PortableViewPayload of(final UUID device) {
        return VIEWS.get(device);
    }

    /** Where the system of the device {@code device} is. */
    public static MobileSession session(final UUID device) {
        return SESSIONS.computeIfAbsent(device, MobileSession::new);
    }

    /** Lets everything go, as the player leaves the world. */
    public static void clear() {
        VIEWS.clear();
        SESSIONS.clear();
    }
}
