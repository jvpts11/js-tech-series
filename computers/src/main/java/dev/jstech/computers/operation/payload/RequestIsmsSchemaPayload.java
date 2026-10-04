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
 * Client to server: the IQL Server Management Studio window {@code window} on the computer at {@code hostPos} wants
 * the network as its Object Explorer shows it. The server answers with an {@link IsmsSchemaPayload}.
 */
public record RequestIsmsSchemaPayload(BlockPos hostPos, int window) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RequestIsmsSchemaPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "request_isms_schema"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestIsmsSchemaPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RequestIsmsSchemaPayload::hostPos,
                    ByteBufCodecs.VAR_INT, RequestIsmsSchemaPayload::window,
                    RequestIsmsSchemaPayload::new);

    @Override
    public CustomPacketPayload.Type<RequestIsmsSchemaPayload> type() {
        return TYPE;
    }
}
