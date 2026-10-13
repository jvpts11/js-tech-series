/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.printer;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: a Print dialog of the computer at {@code hostPos} wants the printers it can print on. The server
 * answers with a {@link PrintersPayload}.
 */
public record RequestPrintersPayload(BlockPos hostPos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RequestPrintersPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "request_printers"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestPrintersPayload> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, RequestPrintersPayload::hostPos, RequestPrintersPayload::new);

    @Override
    public CustomPacketPayload.Type<RequestPrintersPayload> type() {
        return TYPE;
    }
}
