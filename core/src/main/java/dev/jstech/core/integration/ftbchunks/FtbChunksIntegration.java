/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.ftbchunks;

import dev.jstech.core.optional.OptionalMod;
import dev.jstech.core.protection.Protection;

/**
 * Soft integration with FTB Chunks: when the mod is present, the Core's protection asks its claims whether what the
 * series' robots, machines and structures would do may be done there, and on the player's game the map markers are
 * icons on its map and minimap. Nothing here touches an FTB Chunks class unless the mod is loaded.
 */
public final class FtbChunksIntegration {

    public static final String MOD_ID = "ftbchunks";
    /** FTB Chunks, behind its guard. */
    public static final OptionalMod MOD = OptionalMod.of(MOD_ID);

    private FtbChunksIntegration() {
    }

    /** Hands the Core's protection FTB Chunks' claims; a no-op when FTB Chunks is absent. */
    public static void bootstrap() {
        MOD.ifLoaded(() -> FtbChunksIntegration::askClaims);
    }

    /** Adds the map markers to FTB Chunks' maps, once, as the player's game starts; a no-op without the mod. */
    public static void listen() {
        MOD.ifLoaded(() -> FtbChunksMarkers::listen);
    }

    /** Asks FTB Chunks to draw its icons again, the markers having changed; a no-op without the mod. */
    public static void refresh() {
        MOD.ifLoaded(() -> FtbChunksMarkers::refresh);
    }

    // Kept in its own method so the adapter class (and the FTB Chunks API behind it) is only loaded here.
    private static void askClaims() {
        Protection.register(MOD_ID, new FtbChunksProtection());
    }
}
