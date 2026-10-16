/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.effect;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterSpriteSourceTypesEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The effects kit on a player's game: the files read with the resources, the effects played each tick and drawn after
 * the game's own particles, the marks on the world, what effects do to the screen, and all of it forgotten as the
 * world closes.
 */
public final class EffectsClient {

    private EffectsClient() {
    }

    /** Hands the buses what the kit answers, once, from the Core's client. */
    public static void listen(final IEventBus modBus) {
        EffectFiles.listen(modBus);
        ScreenEffects.listen(modBus);
        modBus.addListener(RegisterSpriteSourceTypesEvent.class, BrightSprites::register);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> EffectEngine.tick());
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, EffectsClient::onRender);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> {
            EffectEngine.clear();
            WorldMarks.clear();
            ScreenEffects.clear();
        });
    }

    private static void onRender(final RenderLevelStageEvent event) {
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || level == null) {
            return;
        }
        final MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        WorldMarks.render(level, event.getCamera().getPosition(), buffers);
        EffectEngine.render(event.getCamera(), event.getPartialTick().getGameTimeDeltaPartialTick(false), buffers);
    }
}
