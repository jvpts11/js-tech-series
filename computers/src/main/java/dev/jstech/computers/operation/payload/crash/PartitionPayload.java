/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.crash;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: a program's Get Info on a classic Quince system set the memory block it opens in.
 *
 * @param host        the machine
 * @param program     the program's id path
 * @param minimumMb   the least it opens in
 * @param preferredMb what it opens in when that much is free
 */
public record PartitionPayload(BlockPos host, String program, int minimumMb, int preferredMb)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PartitionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "memory_partition"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PartitionPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PartitionPayload::host,
            ByteBufCodecs.stringUtf8(32), PartitionPayload::program,
            ByteBufCodecs.VAR_INT, PartitionPayload::minimumMb,
            ByteBufCodecs.VAR_INT, PartitionPayload::preferredMb,
            PartitionPayload::new);

    @Override
    public CustomPacketPayload.Type<PartitionPayload> type() {
        return TYPE;
    }
}
