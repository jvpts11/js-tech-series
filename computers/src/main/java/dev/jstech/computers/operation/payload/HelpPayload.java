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
 * {@code man} prints at a terminal. Its words travel as text still to be put in a language, so the window shows
 * them in its player's.
 *
 * @param entries one row of the list each: the group it is under, the name, and its one line
 * @param page    the name whose page this is
 * @param lines   that page, laid out
 */
public record HelpPayload(BlockPos hostPos, List<Entry> entries, String page, List<WireLine> lines)
        implements CustomPacketPayload {

    /** One command in the list: what it is filed under, what it is called, and what it is for. */
    public record Entry(Text group, String name, Text summary) {
    }

    /** How many commands and lines travel, which is far past what a machine really has. */
    public static final int MOST = 256;

    /** How long a name may be. */
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
            TextCodecs.STREAM_CODEC.encode(buf, entry.group());
            buf.writeUtf(clip(entry.name()), MOST_LETTERS);
            TextCodecs.STREAM_CODEC.encode(buf, entry.summary());
        }
        buf.writeUtf(clip(payload.page), MOST_LETTERS);
        buf.writeVarInt(Math.min(payload.lines.size(), MOST));
        for (int i = 0; i < payload.lines.size() && i < MOST; i++) {
            WireLine.STREAM_CODEC.encode(buf, payload.lines.get(i));
        }
    }

    private static HelpPayload decode(final RegistryFriendlyByteBuf buf) {
        final BlockPos host = buf.readBlockPos();
        final int count = Math.min(buf.readVarInt(), MOST);
        final List<Entry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            final Text group = TextCodecs.STREAM_CODEC.decode(buf);
            final String name = buf.readUtf(MOST_LETTERS);
            entries.add(new Entry(group, name, TextCodecs.STREAM_CODEC.decode(buf)));
        }
        final String page = buf.readUtf(MOST_LETTERS);
        final int lineCount = Math.min(buf.readVarInt(), MOST);
        final List<WireLine> lines = new ArrayList<>(lineCount);
        for (int i = 0; i < lineCount; i++) {
            lines.add(WireLine.STREAM_CODEC.decode(buf));
        }
        return new HelpPayload(host, entries, page, lines);
    }

    /** A name no longer than one may be: a cap that throws on the way out is a dropped connection. */
    private static String clip(final String text) {
        return text.length() <= MOST_LETTERS ? text : text.substring(0, MOST_LETTERS);
    }

    @Override
    public CustomPacketPayload.Type<HelpPayload> type() {
        return TYPE;
    }
}
