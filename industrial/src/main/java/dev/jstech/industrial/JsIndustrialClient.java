/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial;

import dev.jstech.industrial.client.IndustrialClientSetup;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/** The industrial mod's entry point on a player's game: its machines' screens and renderers. */
@Mod(value = JsIndustrial.MODID, dist = Dist.CLIENT)
public final class JsIndustrialClient {

    public JsIndustrialClient(final IEventBus modEventBus, final ModContainer modContainer) {
        IndustrialClientSetup.listen(modEventBus);
    }
}
