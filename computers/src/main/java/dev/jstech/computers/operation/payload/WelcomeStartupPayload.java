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
 * Client to server: whether the welcome is still wanted on later starts.
 *
 * <p>The one thing a welcome remembers, and it is remembered with the system rather than with the machine, so
 * erasing that disk and installing again brings the welcome back with everything else.
 */
public record WelcomeStartupPayload(BlockPos hostPos, boolean show) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<WelcomeStartupPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "welcome_startup"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WelcomeStartupPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, WelcomeStartupPayload::hostPos,
                    ByteBufCodecs.BOOL, WelcomeStartupPayload::show,
                    WelcomeStartupPayload::new);

    @Override
    public CustomPacketPayload.Type<WelcomeStartupPayload> type() {
        return TYPE;
    }
}
