/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.light;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.core.stepaside.StepAside;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The lighting kit on a player's game: coloured light tinting the world ({@link ColouredLightModel}), dynamic light
 * ({@link DynamicLightsClient}), and the glow of bright things, the halos ({@link Halos}) and the shafts of
 * spotlights ({@link BeamVisuals}), drawn after the world's particles. Coloured light and glow step aside for Shimmer.
 */
public final class LightingClient {

    private LightingClient() {
    }

    /** Hands the buses what the kit answers, once, from the Core's client. */
    public static void listen(final IEventBus modBus) {
        // After the connected textures of resource packs, so the faces they pick are the ones tinted.
        modBus.addListener(EventPriority.LOW, ModelEvent.ModifyBakingResult.class, ColouredLightModel::wrap);
        ColouredLights.listen();
        DynamicLightsClient.listen();
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, LightingClient::onRender);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> BeamVisuals.forget());
    }

    /** The model a block's model is beneath the kit's tint: itself where coloured light does not reach it. */
    public static BakedModel beneath(final BakedModel model) {
        return model instanceof ColouredLightModel lit ? lit.base() : model;
    }

    private static void onRender(final RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            // The models' glowing parts were all drawn with the block entities and entities.
            KitGlows.endFrame();
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || level == null
                || StepAside.COLOURED_LIGHT.steps()) {
            return;
        }
        final MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        final PoseStack poses = event.getPoseStack();
        BeamVisuals.draw(level, event.getCamera(), poses, buffers);
        Halos.draw(level, event.getCamera(), poses, buffers);
        buffers.endBatch(LightRenderTypes.GLOW);
    }
}
