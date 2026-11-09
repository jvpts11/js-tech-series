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
 * Server to client: the machine ran out of memory, and its system paused what it runs and asks the player what to force
 * quit, as Quince's later systems ask (computers/docs/design/quince.md); an empty list closes the question, the memory
 * being enough again.
 *
 * @param host     the machine
 * @param programs what the player may force quit, each with what it holds
 */
public record MemoryAskPayload(BlockPos host, List<Asked> programs) implements CustomPacketPayload {

    /** The most entries one question lists, far more than a desktop runs at once. */
    public static final int MAX_PROGRAMS = 64;
    /** The longest a name or a token may be. */
    public static final int MAX_TEXT = 128;

    public static final CustomPacketPayload.Type<MemoryAskPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "memory_ask"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MemoryAskPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.host());
                final List<Asked> sent = p.programs().subList(0, Math.min(MAX_PROGRAMS, p.programs().size()));
                buf.writeVarInt(sent.size());
                for (final Asked one : sent) {
                    buf.writeUtf(TextBounds.clip(one.token(), MAX_TEXT), MAX_TEXT);
                    buf.writeUtf(TextBounds.clip(one.name(), MAX_TEXT), MAX_TEXT);
                    buf.writeVarInt(Math.max(0, one.mb()));
                }
            },
            buf -> {
                final BlockPos host = buf.readBlockPos();
                final int count = Math.min(MAX_PROGRAMS, Math.max(0, buf.readVarInt()));
                final List<Asked> programs = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    programs.add(new Asked(buf.readUtf(MAX_TEXT), buf.readUtf(MAX_TEXT), buf.readVarInt()));
                }
                return new MemoryAskPayload(host, programs);
            });

    public MemoryAskPayload {
        programs = List.copyOf(programs);
    }

    @Override
    public CustomPacketPayload.Type<MemoryAskPayload> type() {
        return TYPE;
    }

    /**
     * One thing the player may force quit.
     *
     * @param token what names it back to the machine ({@code RamLedger.Entry#token})
     * @param name  what the list shows it as
     * @param mb    the megabytes it holds
     */
    public record Asked(String token, String name, int mb) {
    }
}
