/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.printer;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: what became of a document the computer at {@code hostPos} printed, which its desktop says in the
 * notification area.
 *
 * @param hostPos the computer
 * @param ok      whether a printer took it
 * @param message what to say: the printer it went to, or why none did
 */
public record PrintedPayload(BlockPos hostPos, boolean ok, Text message) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PrintedPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "printed"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PrintedPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PrintedPayload::hostPos,
            ByteBufCodecs.BOOL, PrintedPayload::ok,
            TextCodecs.STREAM_CODEC, PrintedPayload::message,
            PrintedPayload::new);

    @Override
    public CustomPacketPayload.Type<PrintedPayload> type() {
        return TYPE;
    }
}
