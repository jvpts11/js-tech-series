/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.monitor;

import dev.jstech.computers.monitor.IMonitorPicture;
import dev.jstech.computers.monitor.MonitorPicturePayload;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * What each monitor near this player shows, as the server last described it. A monitor the server has said nothing
 * about is dark glass; everything is forgotten on leaving the world.
 */
public final class MonitorPictureCache {

    private static final Map<BlockPos, IMonitorPicture> PICTURES = new HashMap<>();

    private MonitorPictureCache() {
    }

    /** Hands the game's bus what this answers, once, from the mod's client constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, MonitorPictureCache::onLeave);
    }

    /** Keeps what the server says that monitor shows. */
    public static void accept(final MonitorPicturePayload payload) {
        PICTURES.put(payload.monitor().immutable(), payload.picture());
    }

    /** What the monitor at {@code pos} shows, dark glass when the server has not said. */
    public static IMonitorPicture of(final BlockPos pos) {
        return PICTURES.getOrDefault(pos, IMonitorPicture.DARK);
    }

    /** Lets go of what the monitor at {@code pos} showed, once that monitor is gone from this client. */
    public static void forget(final BlockPos pos) {
        PICTURES.remove(pos);
    }

    public static void onLeave(final ClientPlayerNetworkEvent.LoggingOut event) {
        PICTURES.clear();
    }
}
