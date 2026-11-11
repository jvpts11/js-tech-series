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
 * Client to server: a system monitor asks for its machine's readings over the last {@code hours} (1, 24 or 168), for
 * its power and temperature graphs; answered with {@link MachineReadingsPayload}.
 */
public record RequestMachineReadingsPayload(BlockPos hostPos, int hours) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RequestMachineReadingsPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "request_machine_readings"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestMachineReadingsPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RequestMachineReadingsPayload::hostPos,
                    ByteBufCodecs.VAR_INT, RequestMachineReadingsPayload::hours,
                    RequestMachineReadingsPayload::new);

    @Override
    public CustomPacketPayload.Type<RequestMachineReadingsPayload> type() {
        return TYPE;
    }
}
