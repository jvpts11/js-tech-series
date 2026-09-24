/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliSpan;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextCodecs;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

/**
 * One terminal line on its way to a screen: the coloured runs it is made of.
 *
 * <p>One shape for both terminals, the prompt that is the whole glass of a machine with no desktop and the
 * window on one that has. They used to carry a line each in a record of their own, identical down to the
 * field names, and everything the machine said on its own had to be copied from one into the other before it
 * could be sent to both.
 *
 * <p>The words travel as {@link Text}, a declared sentence as its key and what goes into it, so the player at the
 * other end reads them in their own language. Whatever is handed over is cut to what the wire takes rather than
 * sent as it is: a string past its cap does not arrive truncated, it fails the whole packet, and a line too long to
 * send is a line that should have been wrapped long before it got here.
 */
public record WireLine(List<Span> spans, boolean over) {

    /** The most one line carries, in characters of its words that are data, across all of its runs. */
    public static final int MAX_TEXT = 512;

    /** The most runs one line is cut into; past it the rest of the line is left off. */
    public static final int MAX_SPANS = 48;

    /** A run with no dots in it. */
    private static final int NO_FILL = -1;

    public static final StreamCodec<RegistryFriendlyByteBuf, WireLine> STREAM_CODEC =
            StreamCodec.composite(
                    Span.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_SPANS)), WireLine::spans,
                    ByteBufCodecs.BOOL, WireLine::over,
                    WireLine::new);

    /**
     * @param over whether the line is drawn over the one before it rather than under it, which is how a bar
     *             grows where it stands; said of each line, since a tool can print a line, redraw a bar and
     *             print another line all in one tick
     */
    public WireLine {
        spans = fitted(spans);
    }

    /** A line under everything before it. */
    public WireLine(final List<Span> spans) {
        this(spans, false);
    }

    /** A line of words that are data, in one colour. */
    public WireLine(final String text, final int style) {
        this(Text.literal(text), style);
    }

    /** A line of text in one colour, read in its player's language where it is a sentence. */
    public WireLine(final Text text, final int style) {
        this(List.of(new Span(text, style, NO_FILL, false, false)), false);
    }

    /** The line a command wrote, as it goes on the wire. */
    public static WireLine of(final CliLine line) {
        return new WireLine(spansOf(line), false);
    }

    /** The same line drawn again, changed, over the one before it. */
    public static WireLine over(final CliLine line) {
        return new WireLine(spansOf(line), true);
    }

    /** The line as a terminal keeps it, its styles looked up again from their ids. */
    public CliLine toLine() {
        final List<CliSpan> out = new ArrayList<>(this.spans.size());
        for (final Span span : this.spans) {
            out.add(new CliSpan(span.text(), CliStyle.byId(span.style()), span.fill()));
        }
        return new CliLine(out);
    }

    /** What the line says in English, colours aside. */
    public String text() {
        return toLine().text();
    }

    /** The colour the line opens in, which for a line of one run is simply its colour. */
    public int style() {
        return this.spans.isEmpty() ? 0 : this.spans.getFirst().style();
    }

    private static List<Span> spansOf(final CliLine line) {
        final List<Span> out = new ArrayList<>(line.spans().size());
        for (final CliSpan span : line.spans()) {
            final CliSpan.Fill fill = span.fill();
            out.add(new Span(span.text(), span.style().id(), fill == null ? NO_FILL : fill.column(),
                    fill != null && fill.closing(), fill != null && fill.blank()));
        }
        return out;
    }

    /**
     * The runs cut to what the wire takes: no more of them than it allows, and no more words that are data between
     * them. A declared sentence is not cut, since what it says is the other side's to put together; what goes into
     * it is held to size by the text codec.
     */
    private static List<Span> fitted(final List<Span> spans) {
        if (spans == null || spans.isEmpty()) {
            return List.of();
        }
        final List<Span> out = new ArrayList<>(Math.min(spans.size(), MAX_SPANS));
        int room = MAX_TEXT;
        for (final Span span : spans) {
            if (room <= 0 || out.size() == MAX_SPANS) {
                break;
            }
            if (span.text() instanceof Text.Literal literal) {
                final String kept = literal.value().length() <= room ? literal.value()
                        : literal.value().substring(0, room);
                room -= kept.length();
                out.add(kept.equals(literal.value()) ? span
                        : new Span(Text.literal(kept), span.style(), span.fillColumn(), span.closing(), span.blank()));
            } else {
                out.add(span);
            }
        }
        return List.copyOf(out);
    }

    /**
     * One run: its words, the id of the {@code CliStyle} that colours it, and, for the room between a label and its
     * value or between two cells, the column it carries what follows to.
     *
     * @param fillColumn the column of a fill, or {@code -1} for a run of words
     * @param closing    whether what follows a fill ends at its column rather than begins at it
     * @param blank      whether a fill is spaces rather than dots
     */
    public record Span(Text text, int style, int fillColumn, boolean closing, boolean blank) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Span> STREAM_CODEC =
                StreamCodec.composite(
                        TextCodecs.STREAM_CODEC, Span::text,
                        ByteBufCodecs.VAR_INT, Span::style,
                        ByteBufCodecs.VAR_INT.map(column -> column - 1, column -> column + 1), Span::fillColumn,
                        ByteBufCodecs.BOOL, Span::closing,
                        ByteBufCodecs.BOOL, Span::blank,
                        Span::new);

        /** The room this run stands for, or null for a run of words. */
        @Nullable
        public CliSpan.Fill fill() {
            return this.fillColumn < 0 ? null : new CliSpan.Fill(this.fillColumn, this.closing, this.blank);
        }
    }
}
