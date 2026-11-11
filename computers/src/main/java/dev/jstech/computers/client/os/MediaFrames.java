/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.operation.payload.MediaFramePayload;
import dev.jstech.computers.operation.payload.MediaRequestPayload;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The pictures of the image sequences one Media Player shows, as textures: each asked of the computer once, kept
 * while the window is open, and let go with it. A few are asked for at a time, so a sequence of hundreds does not
 * crowd the line to the server; the player asks for the next ones a little ahead of the one it shows.
 */
final class MediaFrames {

    private final BlockPos host;
    private final Map<String, Frame> known = new HashMap<>();
    private final Set<String> asked = new HashSet<>();

    /** The most pictures asked for and not yet come. */
    static final int MOST_ASKED = 4;
    private static int nextTexture;

    MediaFrames(final BlockPos host) {
        this.host = host;
    }

    /**
     * One picture as a texture.
     *
     * @param texture the texture, or null for a file that is no picture
     * @param width   its width
     * @param height  its height
     */
    record Frame(@Nullable ResourceLocation texture, int width, int height) {
    }

    /** The picture at {@code path}, asking for it when it is not here yet; null until it comes. */
    @Nullable
    Frame get(final String path) {
        final Frame frame = known.get(path);
        if (frame == null) {
            ask(path);
        }
        return frame;
    }

    /** Asks for the picture at {@code path} ahead of being shown, when there is room in the line. */
    void ask(final String path) {
        if (path.isEmpty() || known.containsKey(path) || asked.contains(path) || asked.size() >= MOST_ASKED) {
            return;
        }
        asked.add(path);
        PacketDistributor.sendToServer(new MediaRequestPayload(host, MediaRequestPayload.FRAME, path));
    }

    /** Takes a picture the computer sent, when this player asked for it. */
    void accept(final MediaFramePayload payload) {
        if (!payload.hostPos().equals(host) || !asked.remove(payload.path())) {
            return;
        }
        known.put(payload.path(), texture(payload.png()));
    }

    /** Lets every texture go, as the window closes. */
    void clear() {
        for (final Frame frame : known.values()) {
            if (frame.texture() != null) {
                Minecraft.getInstance().getTextureManager().release(frame.texture());
            }
        }
        known.clear();
        asked.clear();
    }

    private static Frame texture(final byte[] png) {
        if (png.length == 0) {
            return new Frame(null, 0, 0);
        }
        try {
            final NativeImage image = NativeImage.read(png);
            final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                    "media_frame/" + nextTexture++);
            final Frame frame = new Frame(id, image.getWidth(), image.getHeight());
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
            return frame;
        } catch (final IOException unreadable) {
            return new Frame(null, 0, 0);
        }
    }
}
