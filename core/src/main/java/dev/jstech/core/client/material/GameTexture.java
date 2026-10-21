/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.material;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;

/**
 * A texture read for drawing on, as ARGB texels row by row: what the Core's art made as the game loads its resources
 * is drawn from. The game's own textures come from its own files, never a resource pack's, so the art is the same
 * whatever pack a player chose; a mod's from its own jar. Nothing of the game's art ships in a mod this way.
 *
 * @param texels the texels, ARGB, row by row
 * @param width  how wide it is, in texels
 */
public record GameTexture(int[] texels, int width) {

    /** The texture {@code file} ({@code minecraft:textures/block/oak_log.png}), if there is one. */
    public static Optional<GameTexture> read(final ResourceLocation file) {
        try {
            if (ResourceLocation.DEFAULT_NAMESPACE.equals(file.getNamespace())) {
                final IoSupplier<InputStream> found = Minecraft.getInstance().getVanillaPackResources()
                        .getResource(PackType.CLIENT_RESOURCES, file);
                if (found == null) {
                    return Optional.empty();
                }
                try (InputStream in = found.get()) {
                    return Optional.of(read(in));
                }
            }
            for (final IModFileInfo mod : ModList.get().getModFiles()) {
                final Path path = mod.getFile().findResource("assets", file.getNamespace(), file.getPath());
                if (Files.isRegularFile(path)) {
                    try (InputStream in = Files.newInputStream(path)) {
                        return Optional.of(read(in));
                    }
                }
            }
            return Optional.empty();
        } catch (final IOException unreadable) {
            throw new UncheckedIOException(unreadable);
        }
    }

    /**
     * The text of the file {@code file} ({@code othermod:blockstates/limestone.json}), from the game's own files or the
     * jar of the mod whose namespace it is in, as a texture is read; none when there is no such file.
     */
    public static Optional<String> fileText(final ResourceLocation file) {
        try {
            if (ResourceLocation.DEFAULT_NAMESPACE.equals(file.getNamespace())) {
                final IoSupplier<InputStream> found = Minecraft.getInstance().getVanillaPackResources()
                        .getResource(PackType.CLIENT_RESOURCES, file);
                if (found == null) {
                    return Optional.empty();
                }
                try (InputStream in = found.get()) {
                    return Optional.of(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                }
            }
            for (final IModFileInfo mod : ModList.get().getModFiles()) {
                final Path path = mod.getFile().findResource("assets", file.getNamespace(), file.getPath());
                if (Files.isRegularFile(path)) {
                    return Optional.of(Files.readString(path, StandardCharsets.UTF_8));
                }
            }
            return Optional.empty();
        } catch (final IOException unreadable) {
            throw new UncheckedIOException(unreadable);
        }
    }

    /** The file of the texture {@code id} names ({@code minecraft:block/oak_log}). */
    public static ResourceLocation file(final ResourceLocation id) {
        return id.withPath("textures/" + id.getPath() + ".png");
    }

    /** {@code argb}, square and {@code width} texels wide, as the bytes of a PNG file. */
    public static byte[] png(final int[] argb, final int width) {
        try (NativeImage image = new NativeImage(width, width, true)) {
            for (int y = 0; y < width; y++) {
                for (int x = 0; x < width; x++) {
                    image.setPixelRGBA(x, y, swap(argb[y * width + x]));
                }
            }
            return image.asByteArray();
        } catch (final IOException unwritable) {
            throw new UncheckedIOException(unwritable);
        }
    }

    private static GameTexture read(final InputStream in) throws IOException {
        try (NativeImage image = NativeImage.read(in)) {
            // A texture that moves is its frames one under another, each as tall as it is wide: the first is read.
            final int width = image.getWidth();
            final int rows = Math.min(image.getHeight(), width);
            final int[] texels = new int[width * rows];
            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < width; x++) {
                    texels[y * width + x] = swap(image.getPixelRGBA(x, y));
                }
            }
            return new GameTexture(texels, width);
        }
    }

    /* The game's images keep a texel as ABGR; the art is drawn in ARGB. The swap is its own inverse. */
    private static int swap(final int texel) {
        final int alpha = texel >>> 24;
        final int first = texel >> 16 & 0xFF;
        final int green = texel >> 8 & 0xFF;
        final int last = texel & 0xFF;
        return alpha << 24 | last << 16 | green << 8 | first;
    }
}
