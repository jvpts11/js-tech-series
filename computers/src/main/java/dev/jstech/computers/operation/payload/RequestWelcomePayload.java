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

/** Client to server: the welcome window asking the machine what it is. */
public record RequestWelcomePayload(BlockPos hostPos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RequestWelcomePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "request_welcome"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestWelcomePayload> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, RequestWelcomePayload::hostPos,
                    RequestWelcomePayload::new);

    @Override
    public CustomPacketPayload.Type<RequestWelcomePayload> type() {
        return TYPE;
    }
}
