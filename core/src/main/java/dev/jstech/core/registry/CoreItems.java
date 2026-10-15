/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.registry;

import dev.jstech.core.JsCore;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.material.MaterialItems;
import net.neoforged.bus.api.IEventBus;

/**
 * The Core's own content. The things of the materials are not declared here: the Core registers those the installed
 * mods use, as the game registers its things ({@code MaterialUses}).
 */
public final class CoreItems {

    public static final ModContent CONTENT = new ModContent(JsCore.MODID);

    static {
        // The old fixed catalogue keeps its things registered by id, before anything can look one up.
        MaterialItems.register(CONTENT);
    }

    private CoreItems() {
    }

    public static void register(final IEventBus modEventBus) {
        CONTENT.register(modEventBus);
    }
}
