/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.line;

import dev.jstech.core.connect.Connection;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.grid.GridPolicies;
import dev.jstech.core.transit.CapsuleRules;
import dev.jstech.industrial.JsIndustrial;
import net.minecraft.resources.ResourceLocation;

/**
 * J's Industrial's pneumatic post (industrial/docs/design/conveyors.md), on J's Core's capsule stations: capsule tubes,
 * a line of their own whose every connected run joins the stations on it; a capsule of up to four stacks a second
 * from each station, about twenty blocks a second through the tubes, as Paris' pneumatic post (1866) and the tubes of
 * department stores and hospitals carried theirs; and each capsule shot by compressed air ({@link PneumaticStation}).
 */
public final class PneumaticPost {

    /** The grid of the tubes: each connected run groups the stations it joins. */
    public static final GridKind TUBES = GridKind.declare(id("capsule_tube"), GridPolicies.GROUP);
    /** The line of the tubes, of one generation. */
    public static final Connection LINE = Connection.of(id("capsule_tube"));
    /** Four stacks a capsule, a capsule a second, twenty blocks a second. */
    public static final CapsuleRules RULES = new CapsuleRules(4, 20, 20.0);
    /** The air a station spends to shoot a capsule, in millibuckets. */
    public static final long AIR_A_SEND = 100L;
    /** How many capsules' air a station holds. */
    public static final int HOLDS_SENDS = 10;

    private PneumaticPost() {
    }

    /** Declares the grid and the line, from the mod's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsIndustrial.MODID, path);
    }
}
