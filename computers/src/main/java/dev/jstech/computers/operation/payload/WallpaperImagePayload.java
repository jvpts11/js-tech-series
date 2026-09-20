/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: the picture a machine is using as its wallpaper.
 *
 * <p>A channel of its own rather than the ordinary file reply, because the desktop asks for this while an
 * editor may be waiting for a file of its own, and the two answers must not be able to take each other's
 * place. Empty content means the picture is gone, and the desktop falls back to the plain wallpaper.
 */
public record WallpaperImagePayload(String path, String content) implements CustomPacketPayload {

    /** A picture is run length encoded and bounded by the canvas size, so this is room to spare. */
    public static final int MAX_CONTENT = 65_536;

    public static final CustomPacketPayload.Type<WallpaperImagePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "wallpaper_image"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WallpaperImagePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.stringUtf8(160), WallpaperImagePayload::path,
                    ByteBufCodecs.stringUtf8(MAX_CONTENT), WallpaperImagePayload::content,
                    WallpaperImagePayload::new);

    @Override
    public CustomPacketPayload.Type<WallpaperImagePayload> type() {
        return TYPE;
    }
}
