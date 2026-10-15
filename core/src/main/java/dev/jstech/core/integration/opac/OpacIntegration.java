/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.opac;

import dev.jstech.core.optional.OptionalMod;
import dev.jstech.core.protection.Protection;

/**
 * Soft integration with Open Parties and Claims: when the mod is present, the Core's protection asks its claims whether
 * what the series' robots, machines and structures would do may be done there. Nothing here touches an Open Parties
 * and Claims class unless the mod is loaded.
 */
public final class OpacIntegration {

    public static final String MOD_ID = "openpartiesandclaims";
    /** Open Parties and Claims, behind its guard. */
    public static final OptionalMod MOD = OptionalMod.of(MOD_ID);

    private OpacIntegration() {
    }

    /** Hands the Core's protection Open Parties and Claims' claims; a no-op when it is absent. */
    public static void bootstrap() {
        MOD.ifLoaded(() -> OpacIntegration::askClaims);
    }

    // Kept in its own method so the adapter class (and the mod's API behind it) is only loaded here.
    private static void askClaims() {
        Protection.register(MOD_ID, new OpacProtection());
    }
}
