/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.create;

import dev.jstech.core.integration.PinnedVersions;
import dev.jstech.core.optional.OptionalMod;

/**
 * Soft integration with Create: the Core's moving structures meet Create's contraptions as obstacles that move, and
 * Create moves none of the blocks the kit keeps for its structures, so a bearing, a piston or an assembler of a mod
 * built on Create's contraptions, as Create Aeronautics is, never takes a block out of a structure of the kit's. It
 * reaches Create only through Create's own published API and its contraption entity, and starts only on the versions of
 * Create it is tested against; on another it is left off, with a warning in the log.
 */
public final class CreateIntegration {

    public static final String MOD_ID = "create";
    /** Create, behind its guard. */
    public static final OptionalMod MOD = OptionalMod.of(MOD_ID);
    /** The versions of Create the work with it is tested against. */
    public static final String VERSIONS = "[6.0.11,6.1)";

    private static volatile boolean meeting;

    private CreateIntegration() {
    }

    public static boolean isLoaded() {
        return MOD.isLoaded();
    }

    /** Whether the kit meets Create's contraptions: Create is installed, of a version the work is tested against. */
    public static boolean meeting() {
        return meeting;
    }

    /** Hands the kit Create's contraptions to meet, from the Core's constructor; a no-op when Create is absent. */
    public static void bootstrap() {
        if (MOD.isLoaded() && PinnedVersions.covers(MOD, VERSIONS)) {
            meeting = MOD.ifLoaded(() -> CreateIntegration::meet);
        }
    }

    // Kept in its own method so the contraptions' kind (and Create's API behind it) is only loaded here.
    private static void meet() {
        CreateContraptions.start();
    }
}
