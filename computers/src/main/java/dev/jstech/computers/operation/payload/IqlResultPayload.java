/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextCodecs;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Server to client: the result of a Network Management Studio statement: a status message (and whether
 * it succeeded), read in the player's language, plus the result-set rows for a read, which the Studio
 * renders as a grid.
 */
@TextHolder
public record IqlResultPayload(boolean ok, Text message, List<Row> rows) implements CustomPacketPayload {

    public static final int MAX_ROWS = 256;

    /* A statement sent to a network that has nothing to run it. */
    public static final TextKey NO_MAINFRAME =
            TextKey.of("jsc.nms.no_mainframe", "the network has no running Mainframe");

    public static final CustomPacketPayload.Type<IqlResultPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "iql_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, IqlResultPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, IqlResultPayload::ok,
                    TextCodecs.STREAM_CODEC, IqlResultPayload::message,
                    Row.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_ROWS)), IqlResultPayload::rows,
                    IqlResultPayload::new);

    /* Copied on the way in, so what the client is handed cannot change under it after it arrives. */
    public IqlResultPayload {
        rows = List.copyOf(rows);
    }

    @Override
    public CustomPacketPayload.Type<IqlResultPayload> type() {
        return TYPE;
    }

    /** One result row: an item label, read in the player's language where it is a thing's name, and its quantity. */
    public record Row(Text label, long quantity) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Row> STREAM_CODEC =
                StreamCodec.composite(
                        TextCodecs.STREAM_CODEC, Row::label,
                        ByteBufCodecs.VAR_LONG, Row::quantity,
                        Row::new);
    }
}
