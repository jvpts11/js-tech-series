/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer;

import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Whether the tools for whoever makes a mod are on: the inspector of screens, the editor of screens and the new
 * screen from a block. They are on in a development run, never in a game a player plays.
 */
public final class DeveloperMode {

    private DeveloperMode() {
    }

    /** Whether the developer's tools are on. */
    public static boolean on() {
        return !FMLEnvironment.production;
    }
}
