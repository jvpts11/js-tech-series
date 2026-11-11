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
 * Client to server: what a Media Player asks of its computer, its library and the sequence of a folder, or one
 * picture of a sequence.
 *
 * @param hostPos the computer
 * @param what    {@link #LIBRARY} or {@link #FRAME}
 * @param path    the folder whose sequence the library brings too, or empty; the picture a frame is
 */
public record MediaRequestPayload(BlockPos hostPos, int what, String path) implements CustomPacketPayload {

    /** The library, and the sequence of the folder named. */
    public static final int LIBRARY = 0;
    /** One picture. */
    public static final int FRAME = 1;

    public static final CustomPacketPayload.Type<MediaRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "media_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MediaRequestPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, MediaRequestPayload::hostPos,
                    ByteBufCodecs.VAR_INT, MediaRequestPayload::what,
                    ByteBufCodecs.stringUtf8(MediaLibraryPayload.MOST_TEXT), MediaRequestPayload::path,
                    MediaRequestPayload::new);

    @Override
    public CustomPacketPayload.Type<MediaRequestPayload> type() {
        return TYPE;
    }
}
