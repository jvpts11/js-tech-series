/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Client to server: the Network Interactor pauses an Operation in flight, or lets it go on from where it stopped. The
 * server answers with a fresh {@link ActiveOperationsPayload}, where it reads as held, or as running again.
 *
 * @param host        the computer the desktop is bound to
 * @param monitorPos  the monitor used, validated against the player's reach
 * @param operationId the live Operation, as reported by {@link OperationRecord#id()}
 * @param pause       true to pause it, false to let it go on
 */
public record PauseOperationPayload(BlockPos host, BlockPos monitorPos, UUID operationId, boolean pause)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PauseOperationPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "pause_operation"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PauseOperationPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, PauseOperationPayload::host,
                    BlockPos.STREAM_CODEC, PauseOperationPayload::monitorPos,
                    UUIDUtil.STREAM_CODEC, PauseOperationPayload::operationId,
                    ByteBufCodecs.BOOL, PauseOperationPayload::pause,
                    PauseOperationPayload::new);

    @Override
    public CustomPacketPayload.Type<PauseOperationPayload> type() {
        return TYPE;
    }
}
