/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.mekanism;

import dev.jstech.core.chemical.ChemicalBridges;
import net.neoforged.fml.ModList;

/**
 * Soft integration with Mekanism: when the mod is present, its chemicals can be moved and stored like fluids through a
 * {@link MekanismChemicalBridge}. Nothing here touches a Mekanism class unless {@link #isLoaded()} is true, so every
 * mod of the series runs unchanged without Mekanism.
 */
public final class MekanismIntegration {

    public static final String MOD_ID = "mekanism";

    private MekanismIntegration() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    /** Registers the chemical bridge; a no-op when Mekanism is absent. */
    public static void bootstrap() {
        if (isLoaded()) {
            registerBridge();
        }
    }

    // Kept in its own method so the bridge class (and the Mekanism API behind it) is only loaded here.
    private static void registerBridge() {
        ChemicalBridges.register(new MekanismChemicalBridge());
    }
}
