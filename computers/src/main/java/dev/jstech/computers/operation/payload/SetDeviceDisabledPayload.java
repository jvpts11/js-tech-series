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
 * Client to server: a system's Device Manager disabling the device at {@code device} (packed) on the machine at
 * {@code hostPos}, or enabling it again. The machine answers with its map as it then stands.
 */
public record SetDeviceDisabledPayload(BlockPos hostPos, long device, boolean disabled)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SetDeviceDisabledPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "set_device_disabled"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetDeviceDisabledPayload> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, SetDeviceDisabledPayload::hostPos,
                    ByteBufCodecs.VAR_LONG, SetDeviceDisabledPayload::device,
                    ByteBufCodecs.BOOL, SetDeviceDisabledPayload::disabled, SetDeviceDisabledPayload::new);

    @Override
    public CustomPacketPayload.Type<SetDeviceDisabledPayload> type() {
        return TYPE;
    }
}
