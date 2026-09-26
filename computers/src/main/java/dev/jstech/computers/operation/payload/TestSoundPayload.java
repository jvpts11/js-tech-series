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
 * The Test button on a system's Sound settings: the machine plays its system's startup sound, out of the outputs its
 * system chose and at its volume, so the player hears what a change of them did.
 *
 * @param hostPos the machine whose sound is tried
 */
public record TestSoundPayload(BlockPos hostPos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TestSoundPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "test_sound"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TestSoundPayload> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, TestSoundPayload::hostPos, TestSoundPayload::new);

    @Override
    public CustomPacketPayload.Type<TestSoundPayload> type() {
        return TYPE;
    }
}
