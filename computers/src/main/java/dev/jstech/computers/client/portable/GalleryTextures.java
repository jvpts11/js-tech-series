/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.portable;

import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.operation.payload.portable.PortableGalleryPayload;
import dev.jstech.computers.portable.PortablePicture;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * The small copies of the pictures in the galleries of the portables the player carries, as textures to draw: made
 * from the PNG the server keeps of each when the gallery comes, let go when the picture leaves the gallery and when
 * the player leaves the world.
 */
public final class GalleryTextures {

    private static final Map<Key, ResourceLocation> KEPT = new HashMap<>();
    /** How wide and how tall a small copy is, in its texels. */
    public static final int WIDTH = PortablePicture.THUMBNAIL_WIDTH;
    public static final int HEIGHT = PortablePicture.THUMBNAIL_HEIGHT;
    /** Where the square about a small copy's middle starts across it, which a gallery's grid shows. */
    public static final int SQUARE_U = (WIDTH - HEIGHT) / 2;
    private static long nextTexture;

    private GalleryTextures() {
    }

    /** The texture of the picture numbered {@code number} in the gallery of {@code device}, or null for none. */
    @Nullable
    public static ResourceLocation of(final UUID device, final int number) {
        return KEPT.get(new Key(device, number));
    }

    /** Makes a texture of each picture of {@code gallery} not made yet, and lets go those no longer in it. */
    static void keep(final PortableGalleryPayload gallery) {
        final Set<Key> listed = new HashSet<>();
        for (final PortableGalleryPayload.Picture picture : gallery.pictures()) {
            final Key key = new Key(gallery.device(), picture.number());
            listed.add(key);
            if (!KEPT.containsKey(key) && picture.thumbnail().length > 0) {
                final ResourceLocation made = texture(picture.thumbnail());
                if (made != null) {
                    KEPT.put(key, made);
                }
            }
        }
        final Iterator<Map.Entry<Key, ResourceLocation>> kept = KEPT.entrySet().iterator();
        while (kept.hasNext()) {
            final Map.Entry<Key, ResourceLocation> entry = kept.next();
            if (entry.getKey().device().equals(gallery.device()) && !listed.contains(entry.getKey())) {
                Minecraft.getInstance().getTextureManager().release(entry.getValue());
                kept.remove();
            }
        }
    }

    /** Lets every texture go. */
    static void clear() {
        for (final ResourceLocation texture : KEPT.values()) {
            Minecraft.getInstance().getTextureManager().release(texture);
        }
        KEPT.clear();
    }

    /* A texture of the PNG {@code png}, or null for one that does not read. */
    @Nullable
    private static ResourceLocation texture(final byte[] png) {
        try {
            final NativeImage image = NativeImage.read(png);
            final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                    "portable_picture/" + nextTexture++);
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
            return id;
        } catch (final IOException unreadable) {
            return null;
        }
    }

    /* A picture of a device's gallery. */
    private record Key(UUID device, int number) {
    }
}
