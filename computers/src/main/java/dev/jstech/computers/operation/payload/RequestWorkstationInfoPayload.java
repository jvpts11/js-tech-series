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

/** Client to server: CDE's Workstation Info asking the machine what it is and how much of it is in use. */
public record RequestWorkstationInfoPayload(BlockPos hostPos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RequestWorkstationInfoPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "request_workstation_info"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestWorkstationInfoPayload> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, RequestWorkstationInfoPayload::hostPos,
                    RequestWorkstationInfoPayload::new);

    @Override
    public CustomPacketPayload.Type<RequestWorkstationInfoPayload> type() {
        return TYPE;
    }
}
