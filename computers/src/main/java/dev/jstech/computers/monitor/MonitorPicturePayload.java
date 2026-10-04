/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.monitor;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: what the monitor at {@code monitor} shows now, for a player near enough to see its face. Sent
 * when it changes, and to a player who has just come near.
 */
public record MonitorPicturePayload(BlockPos monitor, IMonitorPicture picture) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MonitorPicturePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "monitor_picture"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MonitorPicturePayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, MonitorPicturePayload::monitor,
                    IMonitorPicture.STREAM_CODEC, MonitorPicturePayload::picture,
                    MonitorPicturePayload::new);

    @Override
    public CustomPacketPayload.Type<MonitorPicturePayload> type() {
        return TYPE;
    }
}
