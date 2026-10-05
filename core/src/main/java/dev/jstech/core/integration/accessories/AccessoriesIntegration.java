/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.accessories;

import dev.jstech.core.worn.WornItems;
import net.neoforged.fml.ModList;

/**
 * Soft integration with Accessories: when the mod is present, what a player wears in its slots counts as worn for
 * every mod that asks {@link WornItems}, and so do the Curios slots it carries. Nothing here touches an Accessories
 * class unless {@link #isLoaded()} is true.
 */
public final class AccessoriesIntegration {

    public static final String MOD_ID = "accessories";

    private AccessoriesIntegration() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    /** Hands the Core Accessories as a source of worn things; a no-op when Accessories is absent. */
    public static void bootstrap() {
        if (isLoaded()) {
            useAccessories();
        }
    }

    // Kept in its own method so the source class (and the Accessories API behind it) is only loaded here.
    private static void useAccessories() {
        WornItems.addSource(new AccessoriesWornSource());
    }
}
