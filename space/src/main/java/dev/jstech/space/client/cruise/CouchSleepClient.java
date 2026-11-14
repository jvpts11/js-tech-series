/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.cruise;

import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.input.KeyActionsClient;
import dev.jstech.core.moving.StructureSeatEntity;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.space.JsSpace;
import dev.jstech.space.cruise.CouchSleep;
import dev.jstech.space.cruise.CruiseTexts;
import dev.jstech.space.network.SpacePayloads;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Sleeping in a crew seat on a player's game: their view darkens as in a bed, over the same hundred ticks to the same
 * dark, a line under it saying which key wakes them, and clears again as they wake. Its key is sent to the server only
 * while they sit in a structure's seat.
 */
@PaletteHolder
public final class CouchSleepClient {

    /** Its colours: the dark over the view, the game's own over a bed, and the line saying which key wakes. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsSpace.MODID, "couch_sleep", new Colours(
            0xFF101020, 0xFFFFFFFF));
    private static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "couch_sleep");
    /* The game's own sleep overlay: coming over a bed's hundred ticks to its most dark, and going in ten. */
    private static final float MOST = 220F;
    private static final float FALLING = 100F;
    private static final float WAKING = 10F;
    private static final int ALPHA_SHIFT = 24;
    private static final int RGB = 0xFFFFFF;
    /* How far up from the screen's foot the line saying which key wakes stands, where a bed's button stands. */
    private static final int LINE_UP = 40;
    private static boolean asleep;
    /* When the player last fell asleep or woke, by their level's tick, and how dark their view was as they woke. */
    private static long since;
    private static float woke;

    private CouchSleepClient() {
    }

    /** Hands the buses the darkened view and the key, once, from the mod's player-side entry point. */
    public static void listen(final IEventBus modEventBus) {
        SpacePayloads.onCouchSleep(CouchSleepClient::told);
        modEventBus.addListener(RegisterGuiLayersEvent.class,
                event -> event.registerAbove(VanillaGuiLayers.SLEEP_OVERLAY, LAYER, CouchSleepClient::draw));
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> asleep = false);
        KeyActionsClient.sendsWhen(CouchSleep.KEY, () -> Minecraft.getInstance().player != null
                && Minecraft.getInstance().player.getVehicle() instanceof StructureSeatEntity);
    }

    /** Whether the player sleeps in a crew seat, as the server last told their game. */
    public static boolean asleep() {
        return asleep;
    }

    private static void told(final boolean now) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        if (!now) {
            woke = dark(minecraft.level.getGameTime() - since);
        }
        asleep = now;
        since = minecraft.level.getGameTime();
    }

    private static void draw(final GuiGraphics graphics, final DeltaTracker delta) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        final float ticks = minecraft.level.getGameTime() - since + delta.getGameTimeDeltaPartialTick(false);
        final boolean seated = minecraft.player.getVehicle() instanceof StructureSeatEntity;
        final float alpha = asleep && seated ? dark(ticks) : !asleep && ticks < WAKING ? woke * (1F - ticks / WAKING)
                : 0F;
        if (alpha <= 0F) {
            return;
        }
        final Colours c = PALETTE.get();
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), Math.round(alpha) << ALPHA_SHIFT
                | c.dark() & RGB);
        if (asleep && seated) {
            final String key = KeyActionsClient.mapping(CouchSleep.KEY).getTranslatedKeyMessage().getString();
            Draw.textCentered(graphics, minecraft.font, GameText.resolve(CruiseTexts.WAKE.with(key)),
                    graphics.guiWidth() / 2, graphics.guiHeight() - LINE_UP, c.line());
        }
    }

    /* How dark the view is {@code ticks} after falling asleep, as the game's own overlay over a bed. */
    private static float dark(final float ticks) {
        return MOST * Math.min(1F, Math.max(0F, ticks) / FALLING);
    }

    /**
     * The colours of sleeping in a crew seat.
     *
     * @param dark the dark laid over the view, its alpha set by how long the player has slept
     * @param line the line saying which key wakes them
     */
    public record Colours(int dark, int line) {
    }
}
