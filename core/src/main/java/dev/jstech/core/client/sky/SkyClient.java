/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.sky;

import dev.jstech.core.sky.Skies;
import dev.jstech.core.sky.SkySpec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.level.material.FogType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The sky kit on the player's game: the kind of place drawn as {@link Skies#EFFECTS}, and the fog of its dimensions,
 * in the colour its sky gives and closed in by its haze.
 */
public final class SkyClient {

    /* How close the thickest haze brings the end of sight, as a share of the view, and where the fog starts. */
    private static final float THICKEST = 0.92F;
    private static final float FOG_STARTS = 0.1F;

    private SkyClient() {
    }

    /** Hands the buses what this answers, once, from the Core's client. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterDimensionSpecialEffectsEvent.class,
                event -> event.register(Skies.EFFECTS, new SkyEffects()));
        NeoForge.EVENT_BUS.addListener(ViewportEvent.ComputeFogColor.class, SkyClient::fogColour);
        NeoForge.EVENT_BUS.addListener(ViewportEvent.RenderFog.class, SkyClient::fog);
    }

    /* The fog in the colour of the sky's fog, or drawn towards its haze's, as bright as the day. */
    private static void fogColour(final ViewportEvent.ComputeFogColor event) {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !(level.effects() instanceof SkyEffects)
                || event.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        final SkySpec sky = Skies.of(level);
        final float daylight = SkyPlace.of(level, sky).skyLight();
        sky.fogColour().ifPresent(colour -> {
            final float[] rgb = SkyRenderer.rgb(colour);
            event.setRed(rgb[0] * daylight);
            event.setGreen(rgb[1] * daylight);
            event.setBlue(rgb[2] * daylight);
        });
        sky.haze().ifPresent(haze -> {
            final float pull = (float) Math.min(1.0, haze.density() * 1.5);
            final float[] rgb = SkyRenderer.rgb(haze.colour());
            event.setRed(event.getRed() + (rgb[0] * daylight - event.getRed()) * pull);
            event.setGreen(event.getGreen() + (rgb[1] * daylight - event.getGreen()) * pull);
            event.setBlue(event.getBlue() + (rgb[2] * daylight - event.getBlue()) * pull);
        });
    }

    /* The sky's haze closing in the end of sight. */
    private static void fog(final ViewportEvent.RenderFog event) {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !(level.effects() instanceof SkyEffects)
                || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN
                || event.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        Skies.of(level).haze().ifPresent(haze -> {
            final float far = event.getFarPlaneDistance() * (1 - THICKEST * (float) haze.density());
            event.setFarPlaneDistance(far);
            event.setNearPlaneDistance(Math.min(event.getNearPlaneDistance(), far * FOG_STARTS));
            event.setCanceled(true);
        });
    }
}
