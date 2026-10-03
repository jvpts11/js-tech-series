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

/** Client to server: a system's Device Manager asking the machine for its hardware and ports. */
public record RequestDeviceMapPayload(BlockPos hostPos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RequestDeviceMapPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "request_device_map"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestDeviceMapPayload> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, RequestDeviceMapPayload::hostPos,
                    RequestDeviceMapPayload::new);

    @Override
    public CustomPacketPayload.Type<RequestDeviceMapPayload> type() {
        return TYPE;
    }
}
