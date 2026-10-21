/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.crafttweaker;

import dev.jstech.core.optional.OptionalMod;

/**
 * Soft integration with CraftTweaker: when it is installed, its scripts reach the Core under {@code mods.jscore}
 * ({@link ProcessingZen}, {@link CoreZen}, {@link CoreZenEvents}). CraftTweaker finds those classes itself, by their
 * annotation; what is left here is passing on the events the Core posts on its own bus. Nothing here touches a
 * CraftTweaker class unless the mod is loaded.
 */
public final class CraftTweakerIntegration {

    public static final String MOD_ID = "crafttweaker";
    /** CraftTweaker, behind its guard. */
    public static final OptionalMod MOD = OptionalMod.of(MOD_ID);

    private CraftTweakerIntegration() {
    }

    /** Passes the Core's own events on to CraftTweaker's scripts; a no-op when CraftTweaker is absent. */
    public static void bootstrap() {
        MOD.ifLoaded(() -> CraftTweakerIntegration::listen);
    }

    // Kept in its own method so CraftTweaker's classes are only loaded here.
    private static void listen() {
        CoreZenEvents.listen();
    }
}
