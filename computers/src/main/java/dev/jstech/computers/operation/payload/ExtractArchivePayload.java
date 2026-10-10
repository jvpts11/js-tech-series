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
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: take files back out of the archive at {@code archivePath} on the machine at
 * {@code hostPos}, writing them into {@code intoDir}.
 *
 * <p>An empty {@code entry} takes everything out; a name takes that one file. The server does it because
 * unpacking costs disk space again and only the server knows whether there is any.
 */
public record ExtractArchivePayload(BlockPos hostPos, String archivePath, String entry,
                                    String intoDir) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ExtractArchivePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "extract_archive"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ExtractArchivePayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ExtractArchivePayload::hostPos,
                    PayloadCodecs.PATH, ExtractArchivePayload::archivePath,
                    PayloadCodecs.PATH, ExtractArchivePayload::entry,
                    PayloadCodecs.PATH, ExtractArchivePayload::intoDir,
                    ExtractArchivePayload::new);

    @Override
    public CustomPacketPayload.Type<ExtractArchivePayload> type() {
        return TYPE;
    }
}
