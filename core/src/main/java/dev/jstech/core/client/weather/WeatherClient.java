/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.weather;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.effect.ClientEffects;
import dev.jstech.core.weather.WeatherKind;
import dev.jstech.core.weather.Weathers;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

/**
 * The weathers on the player's game: what falls round them in the weather's colour (the Core's effects
 * {@code jscore:weather_rain}, {@code weather_snow} and {@code weather_dust}, tinted, more of them the thicker it
 * falls), and the fog it closes in, in its colour. Only the weathers the player stands in are drawn.
 */
public final class WeatherClient {

    private static final ResourceLocation RAIN = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "weather_rain");
    private static final ResourceLocation SNOW = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "weather_snow");
    private static final ResourceLocation DUST = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "weather_dust");
    /* What falls starts this high over the player; dust blows at their height. */
    private static final double FALLS_FROM = 14;
    private static final double BLOWS_AT = 3;
    private static final int MOST_LAYERS = 3;
    /* The thickest fog brings the end of sight to this share of the view, and starts at this share of that. */
    private static final float THICKEST = 0.95F;
    private static final float FOG_STARTS = 0.05F;
    private static final Vec3 DOWN = new Vec3(0, -1, 0);
    private static final Set<Held> HELD = new HashSet<>();

    private WeatherClient() {
    }

    /** Hands the game's bus what this answers, once, from the Core's client. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> tick());
        NeoForge.EVENT_BUS.addListener(ViewportEvent.ComputeFogColor.class, WeatherClient::fogColour);
        NeoForge.EVENT_BUS.addListener(ViewportEvent.RenderFog.class, WeatherClient::fog);
    }

    /** The weathers the player stands in now, as last sent. */
    public static List<WeatherKind> around() {
        final Minecraft minecraft = Minecraft.getInstance();
        final LocalPlayer player = minecraft.player;
        if (minecraft.level == null || player == null) {
            return List.of();
        }
        return Weathers.at(minecraft.level, player.blockPosition());
    }

    /* Holds what falls for each weather the player stands in, and lets go of what no longer falls. */
    private static void tick() {
        final Set<Held> wanted = new HashSet<>();
        final LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            for (final WeatherKind kind : around()) {
                final ResourceLocation effect = switch (kind.falls()) {
                    case RAIN -> RAIN;
                    case SNOW -> SNOW;
                    case DUST -> DUST;
                    case NONE -> null;
                };
                if (effect == null) {
                    continue;
                }
                final int layers = 1 + (int) Math.round(kind.density() * (MOST_LAYERS - 1));
                for (int layer = 0; layer < layers; layer++) {
                    wanted.add(new Held(effect, kind.colour(), layer));
                }
            }
        }
        for (final Held gone : Set.copyOf(HELD)) {
            if (!wanted.contains(gone)) {
                ClientEffects.release(gone);
                HELD.remove(gone);
            }
        }
        if (player == null) {
            return;
        }
        for (final Held held : wanted) {
            if (HELD.add(held)) {
                final double over = held.effect().equals(DUST) ? BLOWS_AT : FALLS_FROM;
                ClientEffects.hold(held, held.effect(), () -> player.position().add(0, over, 0), () -> DOWN,
                        held.colour());
            }
        }
    }

    /* The fog in the colour of the thickest weather the player stands in that has its own. */
    private static void fogColour(final ViewportEvent.ComputeFogColor event) {
        if (event.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        final WeatherKind thickest = thickest();
        if (thickest == null || thickest.fogColour().isEmpty()) {
            return;
        }
        final float pull = (float) Math.min(1.0, thickest.sight() * 1.5);
        final int colour = thickest.fogColour().get();
        final ClientLevel level = Minecraft.getInstance().level;
        final float daylight = level == null ? 1F : Math.max(0.2F, level.getSkyDarken((float) event.getPartialTick()));
        event.setRed(event.getRed() + ((colour >> 16 & 0xFF) / 255F * daylight - event.getRed()) * pull);
        event.setGreen(event.getGreen() + ((colour >> 8 & 0xFF) / 255F * daylight - event.getGreen()) * pull);
        event.setBlue(event.getBlue() + ((colour & 0xFF) / 255F * daylight - event.getBlue()) * pull);
    }

    /* The view closed in by the thickest weather the player stands in. */
    private static void fog(final ViewportEvent.RenderFog event) {
        if (event.getMode() != FogRenderer.FogMode.FOG_TERRAIN
                || event.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        final WeatherKind thickest = thickest();
        if (thickest == null || thickest.sight() <= 0) {
            return;
        }
        final float far = event.getFarPlaneDistance() * (1 - THICKEST * (float) thickest.sight());
        event.setFarPlaneDistance(far);
        event.setNearPlaneDistance(Math.min(event.getNearPlaneDistance(), far * FOG_STARTS));
        event.setCanceled(true);
    }

    /* The weather the player stands in that closes the view in most, if any does. */
    private static @Nullable WeatherKind thickest() {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return null;
        }
        WeatherKind thickest = null;
        for (final WeatherKind kind : around()) {
            final BlockPos eyes = BlockPos.containing(minecraft.player.getEyePosition());
            if (Weathers.outIn(minecraft.level, eyes, kind) && (thickest == null || kind.sight() > thickest.sight())) {
                thickest = kind;
            }
        }
        return thickest;
    }

    /* One layer of what falls in a weather, which holds one effect. */
    private record Held(ResourceLocation effect, int colour, int layer) {
    }
}
