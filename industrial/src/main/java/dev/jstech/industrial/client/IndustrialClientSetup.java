/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.client;

import dev.jstech.industrial.IndustrialModule;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Client-only wiring for the Industrial module: the Tank's renderer, which draws the fluid it holds. Its machines need
 * no screen of their own: they open the one every machine of the Core's has.
 */
public final class IndustrialClientSetup {

    private IndustrialClientSetup() {
    }

    /** Hands the mod's bus what this answers, once, from the mod's client constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, IndustrialClientSetup::registerRenderers);
    }

    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(IndustrialModule.TANK_BE.get(), TankRenderer::new);
    }
}
