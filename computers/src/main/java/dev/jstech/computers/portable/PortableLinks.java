/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.portable;

import dev.jstech.computers.wireless.WirelessLink;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * The portables on a network right now, by level, for its Network Manager to list (computers/docs/design/wireless.md):
 * each device on, carried where an access point of that network reaches it, says so once a second, and one not heard
 * from for a few seconds has left. Nothing here is saved: a device that is not carried is on no network.
 */
public final class PortableLinks {

    /* A device not heard from in this many ticks has left its network: it says so every second while it is on one. */
    private static final long GONE_AFTER = 60L;
    private static final Map<ServerLevel, Map<UUID, Joined>> BY_LEVEL = new WeakHashMap<>();

    private PortableLinks() {
    }

    /** The device of {@code joined} is on its network, at the level's present game time. */
    public static void seen(final ServerLevel level, final Joined joined) {
        BY_LEVEL.computeIfAbsent(level, any -> new HashMap<>()).put(joined.device(), joined);
    }

    /** The device {@code device} is on no network any more: switched off, or out of every access point's reach. */
    public static void left(final ServerLevel level, final UUID device) {
        final Map<UUID, Joined> known = BY_LEVEL.get(level);
        if (known != null) {
            known.remove(device);
        }
    }

    /** Where the device {@code device} is joined in that level, or null when it is on no network there. */
    @Nullable
    public static Joined of(final ServerLevel level, final UUID device) {
        final Map<UUID, Joined> known = BY_LEVEL.get(level);
        final Joined joined = known == null ? null : known.get(device);
        return joined != null && fresh(level, joined) ? joined : null;
    }

    /** The devices on {@code network} in that level, those gone a while dropped as they are met. */
    public static List<Joined> on(final ServerLevel level, final NetworkUuid network) {
        final Map<UUID, Joined> known = BY_LEVEL.get(level);
        final List<Joined> out = new ArrayList<>();
        if (known == null) {
            return out;
        }
        known.values().removeIf(joined -> !fresh(level, joined));
        for (final Joined joined : known.values()) {
            if (joined.network().equals(network)) {
                out.add(joined);
            }
        }
        return out;
    }

    private static boolean fresh(final ServerLevel level, final Joined joined) {
        return level.getGameTime() - joined.seenAt() <= GONE_AFTER;
    }

    /**
     * A device on a network: which it is, of what model, the network, its link through the access point, the game time
     * it was switched on and the one it was last heard from.
     */
    public record Joined(UUID device, PortableModel model, NetworkUuid network, WirelessLink link, long onSince,
                         long seenAt) {
    }
}
