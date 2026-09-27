/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.audio;

import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.operation.payload.SoundfoundryCoverPayload;
import dev.jstech.computers.operation.payload.SoundfoundryCoverRequestPayload;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The covers the Standard Soundfoundry has been sent, each made into a texture it draws, and those it is waiting for.
 * A cover is asked for the first time a page draws it, a few at a time so a page of many songs does not ask for all of
 * them at once, and a cover the server has none of is remembered as none, so it is drawn made of colours and not asked
 * for again.
 */
public final class SoundfoundryCoverArt {

    private static final Map<String, Cover> KNOWN = new HashMap<>();
    /** The covers to ask for, and the machine whose window wants each, which the server checks the player is at. */
    private static final Map<String, BlockPos> WAITING = new LinkedHashMap<>();
    private static final Set<String> ASKED = new HashSet<>();
    /** How many covers are asked for at once. */
    private static final int AT_ONCE = 6;
    private static final Cover NONE = new Cover(null, 0, 0);
    private static int made;

    private SoundfoundryCoverArt() {
    }

    /**
     * A cover as a texture.
     *
     * @param texture what it is drawn from, or null for a cover there is none of
     */
    public record Cover(@Nullable ResourceLocation texture, int width, int height) {
    }

    /**
     * The cover a page of the window of the machine at {@code host} names, asking for it the first time; null until it
     * has come, or when there is none.
     */
    @Nullable
    public static Cover of(final BlockPos host, final String key) {
        if (key.isEmpty()) {
            return null;
        }
        final Cover known = KNOWN.get(key);
        if (known != null) {
            return known.texture() == null ? null : known;
        }
        if (!ASKED.contains(key) && !WAITING.containsKey(key)) {
            WAITING.put(key, host);
            ask();
        }
        return null;
    }

    /** The server sent a cover, or said it has none. */
    public static void accept(final SoundfoundryCoverPayload payload) {
        ASKED.remove(payload.key());
        KNOWN.put(payload.key(), payload.image().length == 0 ? NONE : texture(payload.image()));
        ask();
    }

    /** The player left the server: every cover is let go of. */
    public static void clear() {
        final Minecraft mc = Minecraft.getInstance();
        for (final Cover cover : KNOWN.values()) {
            if (cover.texture() != null) {
                mc.getTextureManager().release(cover.texture());
            }
        }
        KNOWN.clear();
        WAITING.clear();
        ASKED.clear();
    }

    private static void ask() {
        final Iterator<Map.Entry<String, BlockPos>> waiting = WAITING.entrySet().iterator();
        while (ASKED.size() < AT_ONCE && waiting.hasNext()) {
            final Map.Entry<String, BlockPos> next = waiting.next();
            waiting.remove();
            ASKED.add(next.getKey());
            PacketDistributor.sendToServer(new SoundfoundryCoverRequestPayload(next.getValue(), next.getKey()));
        }
    }

    private static Cover texture(final byte[] png) {
        try {
            final NativeImage image = NativeImage.read(new ByteArrayInputStream(png));
            final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                    "soundfoundry_cover/" + made++);
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
            return new Cover(id, image.getWidth(), image.getHeight());
        } catch (final IOException | RuntimeException unreadable) {
            return NONE;
        }
    }
}
