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
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * One terminal line on its way to a screen: the coloured runs it is made of.
 *
 * <p>One shape for both terminals, the prompt that is the whole glass of a machine with no desktop and the
 * window on one that has. They used to carry a line each in a record of their own, identical down to the
 * field names, and everything the machine said on its own had to be copied from one into the other before it
 * could be sent to both.
 *
 * <p>Whatever is handed over is cut to what the wire takes rather than sent as it is: a string past its cap
 * does not arrive truncated, it fails the whole packet, and a line too long to send is a line that should
 * have been wrapped long before it got here.
 */
public record WireLine(List<Span> spans, boolean over) {

    /** The most one line carries, in characters, across all of its runs. */
    public static final int MAX_TEXT = 512;

    /** The most runs one line is cut into; past it the rest of the line goes out in the last one's colour. */
    public static final int MAX_SPANS = 48;

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

    /** A line that is one run in one colour. */
    public WireLine(final String text, final int style) {
        this(List.of(new Span(text == null ? "" : text, style)), false);
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
            out.add(new CliSpan(span.text(), CliStyle.byId(span.style())));
        }
        return new CliLine(out);
    }

    /** What the line says, colours aside. */
    public String text() {
        if (this.spans.size() == 1) {
            return this.spans.getFirst().text();
        }
        final StringBuilder out = new StringBuilder();
        for (final Span span : this.spans) {
            out.append(span.text());
        }
        return out.toString();
    }

    /** The colour the line opens in, which for a line of one run is simply its colour. */
    public int style() {
        return this.spans.isEmpty() ? 0 : this.spans.getFirst().style();
    }

    private static List<Span> spansOf(final CliLine line) {
        final List<Span> out = new ArrayList<>(line.spans().size());
        for (final CliSpan span : line.spans()) {
            out.add(new Span(span.text(), span.style().id()));
        }
        return out;
    }

    /** The runs cut to what the wire takes: no more of them than it allows, and no more text between them. */
    private static List<Span> fitted(final List<Span> spans) {
        if (spans == null || spans.isEmpty()) {
            return List.of();
        }
        final List<Span> out = new ArrayList<>(Math.min(spans.size(), MAX_SPANS));
        int room = MAX_TEXT;
        for (final Span span : spans) {
            if (room <= 0) {
                break;
            }
            final String text = span.text().length() <= room ? span.text() : span.text().substring(0, room);
            room -= text.length();
            if (out.size() == MAX_SPANS) {
                /* Out of runs but not of room: the rest rides in the last run rather than being dropped. */
                final Span last = out.removeLast();
                out.add(new Span(last.text() + text, last.style()));
            } else {
                out.add(text.equals(span.text()) ? span : new Span(text, span.style()));
            }
        }
        return List.copyOf(out);
    }

    /** One run: its text and the id of the {@code CliStyle} that colours it. */
    public record Span(String text, int style) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Span> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.stringUtf8(MAX_TEXT), Span::text,
                        ByteBufCodecs.VAR_INT, Span::style,
                        Span::new);
    }
}
