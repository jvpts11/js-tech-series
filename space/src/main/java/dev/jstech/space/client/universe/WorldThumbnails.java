/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.universe;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import dev.jstech.space.JsSpace;
import dev.jstech.space.universe.OverworldMap;
import dev.jstech.space.universe.SolarBody;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Small maps of whole worlds, as their spheres are painted, for a corner of a screen: the Moon's and Mars' from their
 * maps, the Overworld's drawn from its map number once that has come. Each is made once, the first time it is asked
 * for.
 */
public final class WorldThumbnails {

    /** How many pixels round and from pole to pole a small map is. */
    public static final int WIDE = 64;
    public static final int TALL = 32;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<SolarBody, ResourceLocation> MADE = new EnumMap<>(SolarBody.class);

    private WorldThumbnails() {
    }

    /** {@code body}'s small map, made the first time; none while it cannot be made yet. On the render thread. */
    public static @Nullable ResourceLocation of(final SolarBody body) {
        final ResourceLocation made = MADE.get(body);
        if (made != null) {
            return made;
        }
        final NativeImage image = body == SolarBody.OVERWORLD ? overworld() : read(body);
        if (image == null) {
            return null;
        }
        final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID,
                "thumbnail/" + body.name().toLowerCase(Locale.ROOT));
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
        MADE.put(body, id);
        return id;
    }

    /** Lets every small map go, as the player leaves the world or the Overworld's map number changes. */
    static void clear() {
        for (final ResourceLocation id : MADE.values()) {
            Minecraft.getInstance().getTextureManager().release(id);
        }
        MADE.clear();
    }

    /* The Overworld's small map from its map number; none before the number has come. */
    private static @Nullable NativeImage overworld() {
        final OptionalLong seed = WorldSpheres.seed();
        if (seed.isEmpty()) {
            return null;
        }
        final int[] colours = OverworldMap.maps(WIDE, seed.getAsLong()).colours();
        final NativeImage image = new NativeImage(WIDE, TALL, false);
        for (int y = 0; y < TALL; y++) {
            for (int x = 0; x < WIDE; x++) {
                // The image keeps its pixels as ABGR; the map's colours are opaque.
                image.setPixelRGBA(x, y, FastColor.ABGR32.fromArgb32(colours[y * WIDE + x] | 0xFF00_0000));
            }
        }
        return image;
    }

    /* The Moon's or Mars' small map, the nearest texel of its sphere's map to each pixel's middle. */
    private static @Nullable NativeImage read(final SolarBody body) {
        final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID,
                "textures/environment/world/" + body.name().toLowerCase(Locale.ROOT) + ".png");
        final Resource resource = Minecraft.getInstance().getResourceManager().getResource(id).orElse(null);
        if (resource == null) {
            return null;
        }
        try (InputStream in = resource.open(); NativeImage map = NativeImage.read(NativeImage.Format.RGBA, in)) {
            final NativeImage image = new NativeImage(WIDE, TALL, false);
            for (int y = 0; y < TALL; y++) {
                for (int x = 0; x < WIDE; x++) {
                    image.setPixelRGBA(x, y, map.getPixelRGBA((int) ((x + 0.5) * map.getWidth() / WIDE),
                            (int) ((y + 0.5) * map.getHeight() / TALL)) | 0xFF00_0000);
                }
            }
            return image;
        } catch (final IOException broken) {
            LOGGER.error("J's Space could not read the map of {}", body, broken);
            return null;
        }
    }
}
