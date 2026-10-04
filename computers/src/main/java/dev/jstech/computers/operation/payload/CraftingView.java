/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.crafting.CraftingLog;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * What the window of a crafting part shows besides a bus's settings: lines of what it is part of (the interface whose
 * cable a router is on, the face it feeds, the routes it carries; the interfaces a Receiving Bus credits, what it
 * credits now), warnings, the interfaces a Receiving Bus can be tied to by hand, and what it credited lately.
 *
 * @param lines what it shows, a line each
 * @param picks the interfaces it can be tied to by hand, with whether it is
 * @param log   what it credited lately, newest first
 */
public record CraftingView(List<Line> lines, List<Pick> picks, List<CraftingLog.Entry> log) {

    /** Nothing: the window of a bus that is no crafting part. */
    public static final CraftingView NONE = new CraftingView(List.of(), List.of(), List.of());
    /** A line of a word and what it is. */
    public static final byte PLAIN = 0;
    /** A line whose value is good news, in green. */
    public static final byte GOOD = 1;
    /** A warning in amber: something to look at. */
    public static final byte WARN = 2;
    /** A warning in red: something that keeps it from working. */
    public static final byte BAD = 3;
    /** A note in a dim box: what is so, with nothing to do. */
    public static final byte INFO = 4;
    /** One item of a list under a line's word: its value where the values go, with no word of its own. */
    public static final byte LIST = 5;
    private static final int MOST_LINES = 16;
    private static final int MOST_PICKS = 32;
    private static final int MOST_LOG = 16;
    private static final int MOST_TEXT = 64;

    public CraftingView {
        lines = List.copyOf(lines);
        picks = List.copyOf(picks);
        log = List.copyOf(log);
    }

    /**
     * One line: with a word, the word and its value, and a dim note under them; without one, a box of the note's
     * colour around its value, wrapped.
     *
     * @param tone  {@link #PLAIN}, {@link #GOOD}, {@link #WARN}, {@link #BAD} or {@link #INFO}
     * @param label its word, or empty for a box
     * @param value what it is
     * @param note  what that means, or empty
     */
    public record Line(byte tone, Text label, Text value, Text note) {

        /** Whether it is drawn as a box rather than as a word and a value. */
        public boolean boxed() {
            return tone == WARN || tone == BAD || tone == INFO;
        }
    }

    /**
     * An interface a Receiving Bus can be tied to.
     *
     * @param name    what it is called
     * @param shortId the first characters of its id, which tell two of one name apart
     * @param on      whether the bus is tied to it by hand
     */
    public record Pick(Text name, String shortId, boolean on) {
    }

    /** Writes {@code view} onto {@code buf}. */
    public static void write(final RegistryFriendlyByteBuf buf, final CraftingView view) {
        final List<Line> lines = view.lines.subList(0, Math.min(MOST_LINES, view.lines.size()));
        buf.writeVarInt(lines.size());
        for (final Line line : lines) {
            buf.writeByte(line.tone());
            TextCodecs.STREAM_CODEC.encode(buf, line.label());
            TextCodecs.STREAM_CODEC.encode(buf, line.value());
            TextCodecs.STREAM_CODEC.encode(buf, line.note());
        }
        final List<Pick> picks = view.picks.subList(0, Math.min(MOST_PICKS, view.picks.size()));
        buf.writeVarInt(picks.size());
        for (final Pick pick : picks) {
            TextCodecs.STREAM_CODEC.encode(buf, pick.name());
            buf.writeUtf(pick.shortId(), MOST_TEXT);
            buf.writeBoolean(pick.on());
        }
        final List<CraftingLog.Entry> log = view.log.subList(0, Math.min(MOST_LOG, view.log.size()));
        buf.writeVarInt(log.size());
        for (final CraftingLog.Entry entry : log) {
            buf.writeVarLong(entry.time());
            buf.writeUtf(cut(entry.what()), MOST_TEXT * 2);
            buf.writeVarLong(entry.amount());
            buf.writeVarLong(entry.total());
            buf.writeByte(entry.kind());
            buf.writeUtf(cut(entry.note()), MOST_TEXT * 2);
        }
    }

    /** Reads a view {@link #write} wrote. */
    public static CraftingView read(final RegistryFriendlyByteBuf buf) {
        final int lineCount = Math.min(MOST_LINES, buf.readVarInt());
        final List<Line> lines = new ArrayList<>();
        for (int i = 0; i < lineCount; i++) {
            lines.add(new Line(buf.readByte(), TextCodecs.STREAM_CODEC.decode(buf), TextCodecs.STREAM_CODEC.decode(buf),
                    TextCodecs.STREAM_CODEC.decode(buf)));
        }
        final int pickCount = Math.min(MOST_PICKS, buf.readVarInt());
        final List<Pick> picks = new ArrayList<>();
        for (int i = 0; i < pickCount; i++) {
            picks.add(new Pick(TextCodecs.STREAM_CODEC.decode(buf), buf.readUtf(MOST_TEXT), buf.readBoolean()));
        }
        final int logCount = Math.min(MOST_LOG, buf.readVarInt());
        final List<CraftingLog.Entry> log = new ArrayList<>();
        for (int i = 0; i < logCount; i++) {
            log.add(new CraftingLog.Entry(buf.readVarLong(), buf.readUtf(MOST_TEXT * 2), buf.readVarLong(),
                    buf.readVarLong(), buf.readByte(), buf.readUtf(MOST_TEXT * 2)));
        }
        return new CraftingView(lines, picks, log);
    }

    private static String cut(final String text) {
        return text.length() > MOST_TEXT * 2 ? text.substring(0, MOST_TEXT * 2) : text;
    }
}
