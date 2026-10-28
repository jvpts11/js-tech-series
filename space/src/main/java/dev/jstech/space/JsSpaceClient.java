/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space;

import dev.jstech.core.client.advancement.WideBackgrounds;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/** The space mod's entry point on a player's game. */
@Mod(value = JsSpace.MODID, dist = Dist.CLIENT)
public final class JsSpaceClient {

    /* The night sky behind the mod's advancement tabs: a 16-pixel tile of it, and the whole sky, 400 by 200. */
    private static final String SKY = "jsspace:textures/gui/advancements/sky.png";
    private static final String WIDE_SKY = "jsspace:textures/gui/advancements/sky_wide.png";
    private static final int WIDE_SKY_WIDTH = 400;
    private static final int WIDE_SKY_HEIGHT = 200;

    public JsSpaceClient(final IEventBus modEventBus, final ModContainer modContainer) {
        // A tab that names the tile as its background is drawn with the whole sky, once, sliding slower than the tree.
        WideBackgrounds.declare(SKY, WIDE_SKY, WIDE_SKY_WIDTH, WIDE_SKY_HEIGHT);
    }
}
