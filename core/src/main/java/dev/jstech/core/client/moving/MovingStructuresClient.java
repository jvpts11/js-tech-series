/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.moving;

import dev.jstech.core.moving.MovingStructures;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * The moving structures on a player's game: holding the structures it sees, drawing their blocks and block entities,
 * aiming at them, and the life of their blocks round the player.
 */
public final class MovingStructuresClient {

    private MovingStructuresClient() {
    }

    /** Hands the buses what the player's game answers, once, as it starts. */
    public static void listen(final IEventBus modBus) {
        ClientMovingStructures.listen();
        StructureLayers.listen();
        StructureAim.listen();
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event -> event.registerEntityRenderer(
                MovingStructures.ENTITY.get(), MovingStructureRenderer::new));
    }
}
