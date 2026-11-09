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
 * Client to server: the player chose what to force quit from the machine's out-of-memory question, by the tokens the
 * question named them by ({@link MemoryAskPayload}).
 *
 * @param host   the machine
 * @param tokens what to force quit
 */
public record ForceQuitPayload(BlockPos host, List<String> tokens) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ForceQuitPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "force_quit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ForceQuitPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.host());
                final List<String> sent = p.tokens().subList(0,
                        Math.min(MemoryAskPayload.MAX_PROGRAMS, p.tokens().size()));
                buf.writeVarInt(sent.size());
                for (final String token : sent) {
                    buf.writeUtf(TextBounds.clip(token, MemoryAskPayload.MAX_TEXT), MemoryAskPayload.MAX_TEXT);
                }
            },
            buf -> {
                final BlockPos host = buf.readBlockPos();
                final int count = Math.min(MemoryAskPayload.MAX_PROGRAMS, Math.max(0, buf.readVarInt()));
                final List<String> tokens = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    tokens.add(buf.readUtf(MemoryAskPayload.MAX_TEXT));
                }
                return new ForceQuitPayload(host, tokens);
            });

    public ForceQuitPayload {
        tokens = List.copyOf(tokens);
    }

    @Override
    public CustomPacketPayload.Type<ForceQuitPayload> type() {
        return TYPE;
    }
}
