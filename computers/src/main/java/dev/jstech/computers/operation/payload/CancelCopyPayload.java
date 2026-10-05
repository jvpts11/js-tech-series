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

/** Client to server: the player pressed Cancel on the copy {@code job} under way on the machine at {@code hostPos}. */
public record CancelCopyPayload(BlockPos hostPos, long job) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CancelCopyPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "cancel_copy"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CancelCopyPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, CancelCopyPayload::hostPos,
                    ByteBufCodecs.VAR_LONG, CancelCopyPayload::job,
                    CancelCopyPayload::new);

    @Override
    public CustomPacketPayload.Type<CancelCopyPayload> type() {
        return TYPE;
    }
}
