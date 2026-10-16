/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.xaero;

import dev.jstech.core.client.map.ClientMarkers;
import dev.jstech.core.optional.OptionalMod;

/**
 * Soft integration with Xaero's Minimap on the player's game: when the mod is present, the map markers are waypoints
 * of its own, which its World Map shows too. Nothing here touches a class of Xaero's unless the mod is installed.
 */
public final class XaeroIntegration {

    /** Xaero's Minimap, behind its guard. */
    public static final OptionalMod MOD = OptionalMod.of("xaerominimap");

    private XaeroIntegration() {
    }

    /** Hands the markers to Xaero's Minimap from then on, once, as the player's game starts; a no-op without it. */
    public static void listen() {
        MOD.ifLoaded(() -> XaeroIntegration::useXaero);
    }

    // Kept in its own method so the adapter (and Xaero's classes behind it) is only loaded here.
    private static void useXaero() {
        ClientMarkers.add(new XaeroMarkers());
    }
}
