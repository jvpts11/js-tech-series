/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.launcher;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * The skins of one formed piece of a launcher of one size, painted once and kept together on the graphics card as one
 * picture, so the piece is drawn in one go, with a white texel its plain parts are tinted from. Pieces of the same
 * size share it.
 */
final class LauncherAtlas {

    private final ResourceLocation texture;
    private final int width;
    private final int height;
    private final Map<String, Region> regions;

    /** The white texel's name. */
    static final String WHITE = "white";
    /* How wide the picture is at the least, and the gap left round each skin so none bleeds into the next. */
    private static final int LEAST_WIDE = 256;
    private static final int GAP = 1;
    private static final int OPAQUE_WHITE = 0xFFFFFFFF;
    private static final int CHANNEL = 0xFF;

    private static final Map<Object, LauncherAtlas> MADE = new HashMap<>();

    private LauncherAtlas(final ResourceLocation texture, final int width, final int height,
                          final Map<String, Region> regions) {
        this.texture = texture;
        this.width = width;
        this.height = height;
        this.regions = regions;
    }

    /** The skins of the piece of {@code size}, painted by {@code paint} the first time one of that size is drawn. */
    static LauncherAtlas of(final Object size, final Supplier<Map<String, LauncherSkins.Picture>> paint) {
        return MADE.computeIfAbsent(size, any -> pack(paint.get()));
    }

    ResourceLocation texture() {
        return this.texture;
    }

    Region region(final String skin) {
        return this.regions.get(skin);
    }

    /** The picture's across at {@code texel} texels into {@code region}, from nought to one. */
    float u(final Region region, final double texel) {
        return (float) ((region.x() + Math.max(0, Math.min(region.width(), texel))) / this.width);
    }

    /** The picture's down at {@code texel} texels into {@code region}, from nought to one. */
    float v(final Region region, final double texel) {
        return (float) ((region.y() + Math.max(0, Math.min(region.height(), texel))) / this.height);
    }

    /* Packs {@code painted} and a white texel in rows, tallest first, and hands the picture to the graphics card. */
    private static LauncherAtlas pack(final Map<String, LauncherSkins.Picture> painted) {
        final Map<String, LauncherSkins.Picture> skins = new LinkedHashMap<>(painted);
        skins.put(WHITE, new LauncherSkins.Picture(2, 2, new int[] {OPAQUE_WHITE, OPAQUE_WHITE, OPAQUE_WHITE,
                OPAQUE_WHITE}));
        final List<String> order = new ArrayList<>(skins.keySet());
        order.sort(Comparator.comparingInt((String skin) -> skins.get(skin).height()).reversed());
        int across = LEAST_WIDE;
        for (final LauncherSkins.Picture picture : skins.values()) {
            across = Math.max(across, picture.width() + 2 * GAP);
        }
        final Map<String, Region> regions = new HashMap<>();
        int x = GAP;
        int y = GAP;
        int row = 0;
        for (final String skin : order) {
            final LauncherSkins.Picture picture = skins.get(skin);
            if (x + picture.width() + GAP > across) {
                x = GAP;
                y += row + GAP;
                row = 0;
            }
            regions.put(skin, new Region(x, y, picture.width(), picture.height()));
            x += picture.width() + GAP;
            row = Math.max(row, picture.height());
        }
        final int down = y + row + GAP;
        final NativeImage image = new NativeImage(across, down, true);
        for (final String skin : order) {
            final LauncherSkins.Picture picture = skins.get(skin);
            final Region at = regions.get(skin);
            for (int py = 0; py < picture.height(); py++) {
                for (int px = 0; px < picture.width(); px++) {
                    image.setPixelRGBA(at.x() + px, at.y() + py, abgr(picture.argb()[py * picture.width() + px]));
                }
            }
        }
        final ResourceLocation texture = Minecraft.getInstance().getTextureManager()
                .register("jsspace_launcher", new DynamicTexture(image));
        return new LauncherAtlas(texture, across, down, regions);
    }

    /* The game's images keep red in their lowest byte, the other way round from how the skins are painted. */
    private static int abgr(final int argb) {
        return argb & (CHANNEL << 24 | CHANNEL << 8) | (argb & CHANNEL) << 16 | argb >>> 16 & CHANNEL;
    }

    /** Where a skin is in the picture, and its size, in texels. */
    record Region(int x, int y, int width, int height) {

        /** Its middle across. */
        double middleX() {
            return this.width / 2.0;
        }

        /** Its middle down. */
        double middleY() {
            return this.height / 2.0;
        }
    }
}
