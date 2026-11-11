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

/**
 * Client to server: a player touched the glass of a touch monitor whose screen their game is not holding yet. The
 * server answers as it answers a click on a monitor, opening the screen its machine shows; the player's game keeps
 * that screen on the glass instead of over the game, and the touches after this one go to it there.
 *
 * @param monitor the monitor whose glass was touched
 */
public record TouchMonitorPayload(BlockPos monitor) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TouchMonitorPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "touch_monitor"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TouchMonitorPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, TouchMonitorPayload::monitor,
                    TouchMonitorPayload::new);

    /** How far from a touch monitor's middle a touch reaches it: a little past the game's own reach. */
    public static final double REACH = 8.0;

    @Override
    public CustomPacketPayload.Type<TouchMonitorPayload> type() {
        return TYPE;
    }
}
