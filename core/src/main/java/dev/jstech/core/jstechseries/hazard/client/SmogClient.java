/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.hazard.client;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.field.ChunkFieldsClient;
import dev.jstech.core.jstechseries.hazard.SeriesPollution;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.level.material.FogType;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The haze soot lays over a chunk on the player's game: past a smog worth noticing the end of sight closes in and the
 * air turns a dirty grey-brown, thicker as the soot thickens, read from what the server sends of the chunks round the
 * player.
 */
@PaletteHolder
public final class SmogClient {

    public static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "smog", new Colours(
            0xFF << 24 | 0x8A8070));
    /* From how much soot the haze shows, how much is as thick as it gets, and how much of sight it takes then. */
    private static final float SHOWS_FROM = 75;
    private static final float THICKEST_AT = 500;
    private static final float MOST_TAKEN = 0.8F;
    /* Where the fog starts, as a share of where it ends. */
    private static final float STARTS = 0.1F;

    private SmogClient() {
    }

    /** Hands the game's bus the haze, once, from the Core's client. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ViewportEvent.ComputeFogColor.class, SmogClient::colour);
        NeoForge.EVENT_BUS.addListener(ViewportEvent.RenderFog.class, SmogClient::fog);
    }

    /** How thick the haze is where the player stands, 0 for none to the most it takes. */
    public static float thickness() {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return 0;
        }
        final float soot = ChunkFieldsClient.value(SeriesPollution.SOOT.field(), minecraft.player.chunkPosition());
        return Math.clamp((soot - SHOWS_FROM) / (THICKEST_AT - SHOWS_FROM), 0, 1) * MOST_TAKEN;
    }

    private static void colour(final ViewportEvent.ComputeFogColor event) {
        final float pull = thickness();
        if (pull <= 0 || event.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        final int haze = PALETTE.get().haze();
        event.setRed(event.getRed() + ((haze >> 16 & 0xFF) / 255F - event.getRed()) * pull);
        event.setGreen(event.getGreen() + ((haze >> 8 & 0xFF) / 255F - event.getGreen()) * pull);
        event.setBlue(event.getBlue() + ((haze & 0xFF) / 255F - event.getBlue()) * pull);
    }

    private static void fog(final ViewportEvent.RenderFog event) {
        final float taken = thickness();
        if (taken <= 0 || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN
                || event.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        final float far = event.getFarPlaneDistance() * (1 - taken);
        event.setFarPlaneDistance(far);
        event.setNearPlaneDistance(Math.min(event.getNearPlaneDistance(), far * STARTS));
        event.setCanceled(true);
    }

    /**
     * The haze's colours.
     *
     * @param haze the colour the air turns
     */
    public record Colours(int haze) {
    }
}
