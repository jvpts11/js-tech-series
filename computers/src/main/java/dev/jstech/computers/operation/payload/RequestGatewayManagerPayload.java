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
 * Client to server: the Gateway Manager asks for its state. The selection names the Gateway the right
 * pane shows (by position), so the answer carries that one in detail.
 *
 * @param hostPos  the computer running the manager
 * @param selected the selected Gateway's position as a long, or 0 for none
 */
public record RequestGatewayManagerPayload(BlockPos hostPos, long selected) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RequestGatewayManagerPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "request_gateway_manager"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestGatewayManagerPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RequestGatewayManagerPayload::hostPos,
                    ByteBufCodecs.VAR_LONG, RequestGatewayManagerPayload::selected,
                    RequestGatewayManagerPayload::new);

    @Override
    public CustomPacketPayload.Type<RequestGatewayManagerPayload> type() {
        return TYPE;
    }
}
