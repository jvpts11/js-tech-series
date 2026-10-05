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
 * Client to server: the player pressed a copy window's pause button, pausing the copies under way on the machine at
 * {@code hostPos} when {@code pause} is set and taking them up again when it is not.
 */
public record PauseCopyPayload(BlockPos hostPos, boolean pause) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PauseCopyPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "pause_copy"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PauseCopyPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, PauseCopyPayload::hostPos,
                    ByteBufCodecs.BOOL, PauseCopyPayload::pause,
                    PauseCopyPayload::new);

    @Override
    public CustomPacketPayload.Type<PauseCopyPayload> type() {
        return TYPE;
    }
}
