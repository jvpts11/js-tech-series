/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model.kit;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

/**
 * What the Core's models learn of each texture they are painted with, read once from the resource packs until they
 * reload: whether it is animated, its frames laid one under another as its {@code .mcmeta} says, as the game's block
 * textures are; and whether a glowing texture lies beside it, the same name with {@code _e} before {@code .png}, drawn
 * over the model at full light. A texture made as the models load (one inside a glTF file) is still and glows nowhere.
 */
public final class KitTextures {

    /** Every texture asked about since the resource packs last loaded, by its place. */
    private static final Map<ResourceLocation, Look> LOOKS = new HashMap<>();
    /* A PNG's width and height are the four bytes each after its signature and the header's length and name. */
    private static final int PNG_SIZE_AT = 16;
    private static final int PNG_SIZE_END = 24;
    private static final String PNG = ".png";
    private static final String GLOWING = "_e";

    private KitTextures() {
    }

    /** What is known of {@code texture}, read the first time it is asked for. */
    public static Look of(final ResourceLocation texture) {
        Look look = LOOKS.get(texture);
        if (look == null) {
            look = read(Minecraft.getInstance().getResourceManager(), texture);
            LOOKS.put(texture, look);
        }
        return look;
    }

    /** Forgets every texture, for the resource packs have loaded again. */
    public static void forget() {
        LOOKS.clear();
    }

    /** The glowing texture beside {@code texture}'s place: the same name with {@code _e} before its extension. */
    public static ResourceLocation glowingOf(final ResourceLocation texture) {
        final String path = texture.getPath();
        final String glowing = path.endsWith(PNG) ? path.substring(0, path.length() - PNG.length()) + GLOWING + PNG
                : path + GLOWING;
        return texture.withPath(glowing);
    }

    private static Look read(final ResourceManager manager, final ResourceLocation texture) {
        final Optional<Resource> resource = manager.getResource(texture);
        if (resource.isEmpty()) {
            return Look.PLAIN;
        }
        final ResourceLocation glowing = glowingOf(texture);
        final ResourceLocation glows = manager.getResource(glowing).isPresent() ? glowing : null;
        return new Look(frames(resource.get()), glows);
    }

    /* The frames its .mcmeta declares, counted from the picture's size; one for a texture that does not move. */
    private static TextureFrames frames(final Resource resource) {
        try {
            final Optional<AnimationMetadataSection> animation = resource.metadata()
                    .getSection(AnimationMetadataSection.SERIALIZER);
            if (animation.isEmpty()) {
                return TextureFrames.STILL;
            }
            final int[] size = pngSize(resource);
            if (size == null) {
                return TextureFrames.STILL;
            }
            final FrameSize frame = animation.get().calculateFrameSize(size[0], size[1]);
            final int count = frame.height() <= 0 ? 1 : size[1] / frame.height();
            final List<int[]> frames = new ArrayList<>();
            animation.get().forEachFrame((index, ticks) -> frames.add(new int[] {index, ticks}));
            return TextureFrames.of(count, frames, animation.get().getDefaultFrameTime());
        } catch (final IOException | RuntimeException broken) {
            // A broken .mcmeta leaves the texture still, as the game leaves a block texture it cannot animate.
            return TextureFrames.STILL;
        }
    }

    private static int @Nullable [] pngSize(final Resource resource) throws IOException {
        try (InputStream in = resource.open()) {
            final byte[] head = in.readNBytes(PNG_SIZE_END);
            if (head.length < PNG_SIZE_END) {
                return null;
            }
            return new int[] {bigEndian(head, PNG_SIZE_AT), bigEndian(head, PNG_SIZE_AT + 4)};
        }
    }

    private static int bigEndian(final byte[] bytes, final int at) {
        return (bytes[at] & 0xFF) << 24 | (bytes[at + 1] & 0xFF) << 16 | (bytes[at + 2] & 0xFF) << 8
                | bytes[at + 3] & 0xFF;
    }

    /**
     * What a texture shows: its frames, and the glowing texture beside it.
     *
     * @param frames  its frames, still for a texture that does not move
     * @param glowing the glowing texture beside it, or none
     */
    public record Look(TextureFrames frames, @Nullable ResourceLocation glowing) {

        /** A texture that neither moves nor glows. */
        public static final Look PLAIN = new Look(TextureFrames.STILL, null);
    }
}
