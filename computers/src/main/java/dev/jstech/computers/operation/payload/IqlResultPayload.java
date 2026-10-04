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
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: what one statement of a studio's query tab came to: whether it went, what it says (read in the
 * player's language), and, for a read, the table it brought back with its real columns. The Operations the statement
 * started come back by their short ids, so the tab can say so and stop them.
 *
 * @param window  the studio window that asked
 * @param tab     the query tab that asked
 * @param seq     which statement of the run this answers, from zero
 * @param columns the columns of the table read, empty when the statement read none
 * @param rows    the rows, a cell for each column
 * @param started the short ids of the Operations the statement started
 */
@TextHolder
public record IqlResultPayload(int window, int tab, int seq, boolean ok, Text message, List<String> columns,
                               List<List<Text>> rows, List<String> started) implements CustomPacketPayload {

    public static final int MAX_ROWS = 256;
    public static final int MAX_COLUMNS = 12;
    private static final int MAX_NAME = 32;
    private static final int MAX_STARTED = 16;

    /* A statement sent to a network that has nothing to run it. */
    public static final TextKey NO_MAINFRAME =
            TextKey.of("jsc.isms.no_mainframe", "the network has no running Mainframe");

    public static final CustomPacketPayload.Type<IqlResultPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "iql_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, IqlResultPayload> STREAM_CODEC =
            StreamCodec.of((buf, result) -> result.write(buf), IqlResultPayload::read);

    /* Copied and cut on the way in, so what the client is handed cannot change under it and always fits. */
    public IqlResultPayload {
        columns = columns.stream().limit(MAX_COLUMNS).map(IqlResultPayload::clip).toList();
        final int width = columns.size();
        final List<List<Text>> cut = new ArrayList<>(Math.min(MAX_ROWS, rows.size()));
        for (final List<Text> row : rows) {
            if (cut.size() >= MAX_ROWS) {
                break;
            }
            cut.add(List.copyOf(row.subList(0, Math.min(width, row.size()))));
        }
        rows = List.copyOf(cut);
        started = started.stream().limit(MAX_STARTED).map(IqlResultPayload::clip).toList();
        message = message == null ? Text.EMPTY : message;
    }

    /** A statement's answer with no table: what an action, a definition or a refusal comes back as. */
    public static IqlResultPayload said(final int window, final int tab, final int seq, final boolean ok,
                                        final Text message) {
        return new IqlResultPayload(window, tab, seq, ok, message, List.of(), List.of(), List.of());
    }

    @Override
    public CustomPacketPayload.Type<IqlResultPayload> type() {
        return TYPE;
    }

    private static String clip(final String text) {
        return text == null ? "" : text.length() <= MAX_NAME ? text : text.substring(0, MAX_NAME);
    }

    private void write(final RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(window);
        buf.writeVarInt(tab);
        buf.writeVarInt(seq);
        buf.writeBoolean(ok);
        TextCodecs.STREAM_CODEC.encode(buf, message);
        buf.writeVarInt(columns.size());
        for (final String column : columns) {
            ByteBufCodecs.stringUtf8(MAX_NAME).encode(buf, column);
        }
        buf.writeVarInt(rows.size());
        for (final List<Text> row : rows) {
            buf.writeVarInt(row.size());
            for (final Text cell : row) {
                TextCodecs.STREAM_CODEC.encode(buf, cell);
            }
        }
        buf.writeVarInt(started.size());
        for (final String id : started) {
            ByteBufCodecs.stringUtf8(MAX_NAME).encode(buf, id);
        }
    }

    private static IqlResultPayload read(final RegistryFriendlyByteBuf buf) {
        final int window = buf.readVarInt();
        final int tab = buf.readVarInt();
        final int seq = buf.readVarInt();
        final boolean ok = buf.readBoolean();
        final Text message = TextCodecs.STREAM_CODEC.decode(buf);
        final int columnCount = Math.min(buf.readVarInt(), MAX_COLUMNS);
        final List<String> columns = new ArrayList<>(columnCount);
        for (int i = 0; i < columnCount; i++) {
            columns.add(ByteBufCodecs.stringUtf8(MAX_NAME).decode(buf));
        }
        final int rowCount = Math.min(buf.readVarInt(), MAX_ROWS);
        final List<List<Text>> rows = new ArrayList<>(rowCount);
        for (int r = 0; r < rowCount; r++) {
            final int cellCount = Math.min(buf.readVarInt(), MAX_COLUMNS);
            final List<Text> row = new ArrayList<>(cellCount);
            for (int c = 0; c < cellCount; c++) {
                row.add(TextCodecs.STREAM_CODEC.decode(buf));
            }
            rows.add(row);
        }
        final int startedCount = Math.min(buf.readVarInt(), MAX_STARTED);
        final List<String> started = new ArrayList<>(startedCount);
        for (int i = 0; i < startedCount; i++) {
            started.add(ByteBufCodecs.stringUtf8(MAX_NAME).decode(buf));
        }
        return new IqlResultPayload(window, tab, seq, ok, message, columns, rows, started);
    }
}
