/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.wan;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: BorderLink asks for its gateway's state, and with {@code port} past 0 sets that port's switches to
 * {@code bits} first.
 *
 * @param hostPos the gateway
 * @param port    the port to set, 1 to 4, or 0 to set nothing
 * @param bits    its switches
 */
public record BorderLinkRequestPayload(BlockPos hostPos, int port, int bits) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BorderLinkRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "borderlink_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BorderLinkRequestPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, BorderLinkRequestPayload::hostPos,
                    ByteBufCodecs.VAR_INT, BorderLinkRequestPayload::port,
                    ByteBufCodecs.VAR_INT, BorderLinkRequestPayload::bits,
                    BorderLinkRequestPayload::new);

    /** Asks for the state and sets nothing. */
    public static BorderLinkRequestPayload ask(final BlockPos hostPos) {
        return new BorderLinkRequestPayload(hostPos, 0, 0);
    }

    @Override
    public CustomPacketPayload.Type<BorderLinkRequestPayload> type() {
        return TYPE;
    }
}
