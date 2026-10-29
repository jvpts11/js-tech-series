/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.flight;

import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.space.JsSpace;
import dev.jstech.space.client.universe.MapGround;
import dev.jstech.space.network.FarMapPayload;
import dev.jstech.space.network.LandingMapPayload;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

/**
 * The last landing map a Flight Computer was sent, drawn as the game's own map item draws a world: each pixel the map
 * colour nearest its ground's own, shaded lighter where the ground rises from the pixel north of it and darker where it
 * falls, the water darker the deeper it is, and every other pixel a little apart, as a map's are.
 */
public final class LandingMapsClient {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID,
            "flight_computer/landing_map");
    /* The map colours a pixel is drawn in: every one of the game's but none. */
    private static final int COLOURS = 64;
    /* How steep a slope shades a pixel, and how deep water darkens, as the map item shades them. */
    private static final double SLOPE = 0.6;
    private static final double SHALLOW = 0.5;
    private static final double DEEP = 0.9;
    private static final int CHANNEL = 0xFF;
    private static @Nullable Shown shown;
    private static @Nullable DynamicTexture texture;

    private LandingMapsClient() {
    }

    /** The landing map last sent, if one was. */
    public static @Nullable Shown shown() {
        return shown;
    }

    /** A landing map arrived: drawn into its texture, in place of the one before. */
    public static void receive(final LandingMapPayload payload) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        final FarMapPayload map = payload.map();
        final int size = map.size();
        final NativeImage image = new NativeImage(size, size, false);
        final List<Holder<Biome>> biomes = map.biomes().stream().map(id -> (Holder<Biome>) minecraft.level
                .registryAccess().registryOrThrow(Registries.BIOME).getHolder(ResourceKey.create(Registries.BIOME,
                        id)).orElse(null)).toList();
        final int scale = Math.max(1, Math.min(16, map.blocks() / 8));
        // A map has few colours of ground among its many pixels: each is matched to its map colour once.
        final Map<Integer, MapColor> matched = new HashMap<>();
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                final int at = row * size + column;
                final int height = FarMapPayload.blocks(map.heights()[at]);
                final Holder<Biome> biome = biomes.get(map.pixels()[at] & CHANNEL);
                final int ground = biome == null ? 0 : MapGround.colour(biome, height, map.sea());
                final MapColor colour = matched.computeIfAbsent(ground, LandingMapsClient::nearest);
                final MapColor.Brightness light;
                final double apart = ((row + column) & 1) - 0.5;
                if (height < map.sea()) {
                    final double depth = (map.sea() - height) * 0.1 / scale + ((row + column) & 1) * 0.2;
                    light = depth < SHALLOW ? MapColor.Brightness.HIGH : depth > DEEP ? MapColor.Brightness.LOW
                            : MapColor.Brightness.NORMAL;
                } else {
                    final int north = row == 0 ? height : FarMapPayload.blocks(map.heights()[at - size]);
                    final double slope = (height - north) * 4.0 / (scale + 4) + apart * 0.4;
                    light = slope > SLOPE ? MapColor.Brightness.HIGH : slope < -SLOPE ? MapColor.Brightness.LOW
                            : MapColor.Brightness.NORMAL;
                }
                image.setPixelRGBA(column, row, colour.calculateRGBColor(light));
            }
        }
        if (texture == null) {
            texture = new DynamicTexture(image);
            minecraft.getTextureManager().register(TEXTURE, texture);
        } else {
            texture.setPixels(image);
        }
        texture.upload();
        shown = new Shown(payload.world(), payload.width(), map.centreX(), map.centreZ(), map.blocks(), size,
                TEXTURE);
    }

    /* The map colour nearest {@code rgb}. */
    private static MapColor nearest(final int rgb) {
        MapColor best = MapColor.STONE;
        long closest = Long.MAX_VALUE;
        for (int id = 1; id < COLOURS; id++) {
            final MapColor colour = MapColor.byId(id);
            if (colour == MapColor.NONE || colour.col == 0) {
                continue;
            }
            final long dr = (rgb >> 16 & CHANNEL) - (colour.col >> 16 & CHANNEL);
            final long dg = (rgb >> 8 & CHANNEL) - (colour.col >> 8 & CHANNEL);
            final long db = (rgb & CHANNEL) - (colour.col & CHANNEL);
            final long distance = dr * dr + dg * dg + db * db;
            if (distance < closest) {
                closest = distance;
                best = colour;
            }
        }
        return best;
    }

    /**
     * A landing map as drawn.
     *
     * @param world   the world it is a map of
     * @param width   how wide that world is, in blocks
     * @param centreX the block its middle pixel is at, west to east
     * @param centreZ the block its middle pixel is at, north to south
     * @param blocks  how many blocks wide each pixel is
     * @param size    how many pixels it is each way
     * @param texture its texture
     */
    public record Shown(ResourceLocation world, double width, int centreX, int centreZ, int blocks, int size,
                        ResourceLocation texture) {
    }
}
