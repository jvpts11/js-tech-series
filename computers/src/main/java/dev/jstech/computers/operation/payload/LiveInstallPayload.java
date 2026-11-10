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
 * Client to server: the live desktop's Install Linux Mint, which starts the system's own installer over the live
 * session, from the medium the machine started from (computers/docs/design/operating-systems.md).
 */
public record LiveInstallPayload(BlockPos monitorPos, BlockPos hostPos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<LiveInstallPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "live_install"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LiveInstallPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, LiveInstallPayload::monitorPos,
                    BlockPos.STREAM_CODEC, LiveInstallPayload::hostPos,
                    LiveInstallPayload::new);

    @Override
    public CustomPacketPayload.Type<LiveInstallPayload> type() {
        return TYPE;
    }
}
