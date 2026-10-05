/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.curios;

import dev.jstech.core.worn.WornItems;
import net.neoforged.fml.ModList;

/**
 * Soft integration with Curios: when the mod is present, what a player wears in its slots counts as worn for every
 * mod that asks {@link WornItems}. Nothing here touches a Curios class unless {@link #isLoaded()} is true. When
 * Accessories is installed as well, it is the one asked: it carries the Curios slots too, and asking both would count
 * each thing twice.
 */
public final class CuriosIntegration {

    public static final String MOD_ID = "curios";
    private static final String ACCESSORIES = "accessories";

    private CuriosIntegration() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    /** Hands the Core Curios as a source of worn things; a no-op when Curios is absent or Accessories present. */
    public static void bootstrap() {
        if (isLoaded() && !ModList.get().isLoaded(ACCESSORIES)) {
            useCurios();
        }
    }

    // Kept in its own method so the source class (and the Curios API behind it) is only loaded here.
    private static void useCurios() {
        WornItems.addSource(new CuriosWornSource());
    }
}
