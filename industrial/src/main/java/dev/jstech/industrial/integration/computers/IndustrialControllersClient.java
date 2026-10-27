/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.integration.computers;

import dev.jstech.computers.client.ComputerRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * The Industrial Controller Computers on a player's game: each case drawn by J's Computers' small computer renderer,
 * from this mod's models. Its item is drawn by J's Computers already, as every cabinet's item is.
 */
final class IndustrialControllersClient {

    private IndustrialControllersClient() {
    }

    /** Listens for the renderers' registration; through the guard, from the mod's client entry point. */
    static void listen(final IEventBus modEventBus) {
        modEventBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event ->
                event.registerBlockEntityRenderer(IndustrialControllers.ENTITY.get(), ComputerRenderer::create));
    }
}
