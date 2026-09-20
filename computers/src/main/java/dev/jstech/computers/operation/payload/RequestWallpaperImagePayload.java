/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: the desktop is asking for the picture it has been told to hang on its wall.
 *
 * <p>Asked once, when the chosen wallpaper changes, and never per frame: the answer is held on the client
 * for as long as that picture stays chosen.
 */
public record RequestWallpaperImagePayload(BlockPos hostPos, String path) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RequestWallpaperImagePayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath("jsc", "request_wallpaper_image"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestWallpaperImagePayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RequestWallpaperImagePayload::hostPos,
                    ByteBufCodecs.stringUtf8(160), RequestWallpaperImagePayload::path,
                    RequestWallpaperImagePayload::new);

    @Override
    public CustomPacketPayload.Type<RequestWallpaperImagePayload> type() {
        return TYPE;
    }
}
