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
 * A desktop raised an error box. The box lives on the player's screen alone, so the desktop tells the machine, and
 * the machine plays its system's error sound out of its monitors for everyone near it to hear.
 *
 * @param hostPos the machine whose desktop raised it
 */
public record SystemErrorSoundPayload(BlockPos hostPos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SystemErrorSoundPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "system_error_sound"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SystemErrorSoundPayload> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, SystemErrorSoundPayload::hostPos,
                    SystemErrorSoundPayload::new);

    @Override
    public CustomPacketPayload.Type<SystemErrorSoundPayload> type() {
        return TYPE;
    }
}
