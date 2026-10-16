/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.effect;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

/**
 * The pictures of particles, from the game's particle sheet, which holds every {@code textures/particle/} picture of
 * every namespace: a sprite by name, or its frames {@code <name>_0}, {@code <name>_1} and on, as the game names a
 * particle's frames. A name the sheet does not hold shows the game's plain particle.
 */
final class EffectSprites {

    private static final ResourceLocation PLAIN = ResourceLocation.withDefaultNamespace("generic_0");
    private static final Map<String, List<TextureAtlasSprite>> FRAMES = new ConcurrentHashMap<>();

    private EffectSprites() {
    }

    /** The frames of {@code sprite}, {@code frames} of them, or one; the game's plain particle for none. */
    static List<TextureAtlasSprite> of(final String sprite, final int frames) {
        return FRAMES.computeIfAbsent(sprite + "#" + frames, key -> find(sprite, frames));
    }

    /** Forgets the sprites found, as the sheet is made again. */
    static void forget() {
        FRAMES.clear();
    }

    private static List<TextureAtlasSprite> find(final String sprite, final int frames) {
        final AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(
                TextureAtlas.LOCATION_PARTICLES);
        if (!(texture instanceof TextureAtlas sheet)) {
            return List.of();
        }
        final ResourceLocation named = sprite.isEmpty() ? PLAIN : ResourceLocation.tryParse(sprite);
        if (named == null) {
            return List.of(sheet.getSprite(PLAIN));
        }
        final List<TextureAtlasSprite> out = new ArrayList<>();
        if (frames <= 1) {
            out.add(held(sheet, named));
        } else {
            for (int frame = 0; frame < frames; frame++) {
                out.add(held(sheet, named.withSuffix("_" + frame)));
            }
        }
        return List.copyOf(out);
    }

    /* The sprite {@code name}, or the plain one where the sheet holds none by that name. */
    private static TextureAtlasSprite held(final TextureAtlas sheet, final ResourceLocation name) {
        final TextureAtlasSprite found = sheet.getSprite(name);
        return found.contents().name().equals(MissingTextureAtlasSprite.getLocation()) ? sheet.getSprite(PLAIN)
                : found;
    }
}
