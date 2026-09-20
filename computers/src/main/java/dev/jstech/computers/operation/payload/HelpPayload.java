/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: what this computer can do, and one page of it.
 *
 * <p>The list is what the machine really offers, through the same filter the prompt uses, so the Help window
 * cannot teach a command that is not there. The page is the command's own manual page, the very one
 * {@code man} prints at a terminal.
 *
 * @param entries one row of the list each: the group it is under, the name, and its one line
 * @param page    the name whose page this is
 * @param lines   that page, laid out
 */
public record HelpPayload(BlockPos hostPos, List<Entry> entries, String page, List<String> lines)
        implements CustomPacketPayload {

    /** One command in the list: what it is filed under, what it is called, and what it is for. */
    public record Entry(String group, String name, String summary) {
    }

    /** How many commands and lines travel, which is far past what a machine really has. */
    public static final int MOST = 256;

    /** How long a line may be: what a terminal of this world shows, and the window is no wider. */
    public static final int MOST_LETTERS = 96;

    public static final CustomPacketPayload.Type<HelpPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "help"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HelpPayload> STREAM_CODEC =
            StreamCodec.of(HelpPayload::encode, HelpPayload::decode);

    public HelpPayload {
        entries = List.copyOf(entries);
        lines = List.copyOf(lines);
    }

    private static void encode(final RegistryFriendlyByteBuf buf, final HelpPayload payload) {
        buf.writeBlockPos(payload.hostPos);
        buf.writeVarInt(Math.min(payload.entries.size(), MOST));
        for (int i = 0; i < payload.entries.size() && i < MOST; i++) {
            final Entry entry = payload.entries.get(i);
            buf.writeUtf(clip(entry.group()), MOST_LETTERS);
            buf.writeUtf(clip(entry.name()), MOST_LETTERS);
            buf.writeUtf(clip(entry.summary()), MOST_LETTERS);
        }
        buf.writeUtf(clip(payload.page), MOST_LETTERS);
        buf.writeVarInt(Math.min(payload.lines.size(), MOST));
        for (int i = 0; i < payload.lines.size() && i < MOST; i++) {
            buf.writeUtf(clip(payload.lines.get(i)), MOST_LETTERS);
        }
    }

    private static HelpPayload decode(final RegistryFriendlyByteBuf buf) {
        final BlockPos host = buf.readBlockPos();
        final int count = Math.min(buf.readVarInt(), MOST);
        final List<Entry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(new Entry(buf.readUtf(MOST_LETTERS), buf.readUtf(MOST_LETTERS),
                    buf.readUtf(MOST_LETTERS)));
        }
        final String page = buf.readUtf(MOST_LETTERS);
        final int lineCount = Math.min(buf.readVarInt(), MOST);
        final List<String> lines = new ArrayList<>(lineCount);
        for (int i = 0; i < lineCount; i++) {
            lines.add(buf.readUtf(MOST_LETTERS));
        }
        return new HelpPayload(host, entries, page, lines);
    }

    /** A line no longer than one may be: a cap that throws on the way out is a dropped connection. */
    private static String clip(final String text) {
        return text.length() <= MOST_LETTERS ? text : text.substring(0, MOST_LETTERS);
    }

    @Override
    public CustomPacketPayload.Type<HelpPayload> type() {
        return TYPE;
    }
}
