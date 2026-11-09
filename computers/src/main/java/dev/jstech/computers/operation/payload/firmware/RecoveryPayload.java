/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.firmware;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: a choice made in quinceOS's recovery, which its start-up options open
 * (computers/docs/design/quince.md): put a fresh copy of the system on its disk from the network, or run First Aid on
 * that disk.
 *
 * @param hostPos    the machine
 * @param monitorPos the screen the choice was made at
 * @param action     {@link #REINSTALL} or {@link #FIRST_AID}
 */
public record RecoveryPayload(BlockPos hostPos, BlockPos monitorPos, byte action) implements CustomPacketPayload {

    /** Puts a fresh copy of the system the machine runs on its disk, from the network. */
    public static final byte REINSTALL = 0;
    /** Checks and repairs the disk the machine starts from. */
    public static final byte FIRST_AID = 1;

    public static final CustomPacketPayload.Type<RecoveryPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "recovery"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RecoveryPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, RecoveryPayload::hostPos,
            BlockPos.STREAM_CODEC, RecoveryPayload::monitorPos,
            ByteBufCodecs.BYTE, RecoveryPayload::action,
            RecoveryPayload::new);

    @Override
    public CustomPacketPayload.Type<RecoveryPayload> type() {
        return TYPE;
    }
}
