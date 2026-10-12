/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.crash;

import dev.jstech.core.text.TextBounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: the machine ran out of memory and its kernel ended windows of its desktop to make room, the latest
 * of each key named once for each window of it that went, heaviest first.
 *
 * @param host    the machine
 * @param windows the keys of the windows ended
 */
public record MemoryKilledPayload(BlockPos host, List<String> windows) implements CustomPacketPayload {

    /** The longest a window's key may be. */
    public static final int MAX_KEY = 128;
    /** The most windows one answer names; a machine has never had more open than its desktop keeps. */
    public static final int MAX_WINDOWS = 64;

    public static final CustomPacketPayload.Type<MemoryKilledPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "memory_killed"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MemoryKilledPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.host());
                final List<String> sent = p.windows().subList(0, Math.min(MAX_WINDOWS, p.windows().size()));
                buf.writeVarInt(sent.size());
                for (final String key : sent) {
                    buf.writeUtf(TextBounds.clip(key, MAX_KEY), MAX_KEY);
                }
            },
            buf -> {
                final BlockPos host = buf.readBlockPos();
                final int count = Math.min(MAX_WINDOWS, Math.max(0, buf.readVarInt()));
                final List<String> windows = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    windows.add(buf.readUtf(MAX_KEY));
                }
                return new MemoryKilledPayload(host, windows);
            });

    public MemoryKilledPayload {
        windows = List.copyOf(windows);
    }

    @Override
    public CustomPacketPayload.Type<MemoryKilledPayload> type() {
        return TYPE;
    }
}
