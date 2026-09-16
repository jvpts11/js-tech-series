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
 * Server to client: the system is coming up on this machine, and this much of it is left.
 *
 * <p>Sent when the self-test hands over and again whenever a monitor is opened on a machine still coming up, so a
 * player who walked away and came back joins it where it has got to. The machine keeps the time, as it does for the
 * self-test: this only says where it is and which system is doing it.
 */
public record OpenSystemBootPayload(BlockPos hostPos, BlockPos monitorPos, String osId, String osName,
                                    int remainingTicks, int totalTicks) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenSystemBootPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "open_system_boot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenSystemBootPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, OpenSystemBootPayload::hostPos,
                    BlockPos.STREAM_CODEC, OpenSystemBootPayload::monitorPos,
                    ByteBufCodecs.STRING_UTF8, OpenSystemBootPayload::osId,
                    ByteBufCodecs.STRING_UTF8, OpenSystemBootPayload::osName,
                    ByteBufCodecs.VAR_INT, OpenSystemBootPayload::remainingTicks,
                    ByteBufCodecs.VAR_INT, OpenSystemBootPayload::totalTicks,
                    OpenSystemBootPayload::new);

    @Override
    public CustomPacketPayload.Type<OpenSystemBootPayload> type() {
        return TYPE;
    }
}
