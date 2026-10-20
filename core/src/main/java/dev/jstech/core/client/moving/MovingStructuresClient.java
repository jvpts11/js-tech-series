/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.moving;

import dev.jstech.core.moving.MovingStructures;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * The moving structures on a player's game: holding the structures it sees, drawing their blocks and block entities,
 * their joints and what they show of themselves going, aiming at them, and the life of their blocks round the player.
 */
public final class MovingStructuresClient {

    private MovingStructuresClient() {
    }

    /** Hands the buses what the player's game answers, once, as it starts. */
    public static void listen(final IEventBus modBus) {
        ClientMovingStructures.listen();
        StructureLayers.listen();
        StructureAim.listen();
        JointRenderer.listen();
        ZiplineClient.listen();
        StructureEffectsClient.listen();
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event -> {
            event.registerEntityRenderer(MovingStructures.ENTITY.get(), MovingStructureRenderer::new);
            // A seat and a hold on a rope are unseen: their rider is what is drawn.
            event.registerEntityRenderer(MovingStructures.SEAT.get(), NoopRenderer::new);
            event.registerEntityRenderer(MovingStructures.ZIPLINE.get(), NoopRenderer::new);
        });
    }
}
