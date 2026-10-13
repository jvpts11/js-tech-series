/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.crash;

import dev.jstech.core.text.TextBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: a window the player opened on that machine's desktop did not fit in what is left of its memory,
 * and the machine answers the way its system does, which on a system that cannot protect itself is a crash.
 *
 * @param host    the machine
 * @param program the name of the program whose window asked, as the desktop shows it
 */
public record MemoryShortPayload(BlockPos host, String program) implements CustomPacketPayload {

    /** The longest a program's name may be. */
    public static final int MAX_NAME = 64;

    public static final CustomPacketPayload.Type<MemoryShortPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "memory_short"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MemoryShortPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.host());
                buf.writeUtf(TextBounds.clip(p.program(), MAX_NAME), MAX_NAME);
            },
            buf -> new MemoryShortPayload(buf.readBlockPos(), buf.readUtf(MAX_NAME)));

    @Override
    public CustomPacketPayload.Type<MemoryShortPayload> type() {
        return TYPE;
    }
}
