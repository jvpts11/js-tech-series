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
 * Server to client: one picture of an image sequence, as a PNG brought down to the size the Media Player shows it at,
 * or no bytes for a file that is no picture.
 *
 * @param hostPos the computer
 * @param path    the picture
 * @param png     its pixels
 */
public record MediaFramePayload(BlockPos hostPos, String path, byte[] png) implements CustomPacketPayload {

    /** The most bytes a picture travels in: a PNG of the travelling size, with room to spare. */
    public static final int MOST_BYTES = 256 * 1024;

    public static final CustomPacketPayload.Type<MediaFramePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "media_frame"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MediaFramePayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, MediaFramePayload::hostPos,
                    ByteBufCodecs.stringUtf8(MediaLibraryPayload.MOST_TEXT), MediaFramePayload::path,
                    ByteBufCodecs.byteArray(MOST_BYTES), MediaFramePayload::png,
                    MediaFramePayload::new);

    public MediaFramePayload {
        png = png.clone();
    }

    @Override
    public CustomPacketPayload.Type<MediaFramePayload> type() {
        return TYPE;
    }
}
