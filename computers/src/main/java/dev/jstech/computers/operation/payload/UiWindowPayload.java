/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.cannon.run.Numbers;
import dev.jstech.computers.cannon.run.Values;
import dev.jstech.computers.cannon.ui.UiWidgets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * One window a Cannon program has open on a machine, as the desktop draws it: what it is called, how big
 * it asked to be, and every widget in it with what that widget holds.
 *
 * <p>The whole window goes over at once whenever anything in it changes, because a window is small: a
 * handful of widgets, a few words each. What is not sent is how any of it looks, because that is the
 * looking machine's to decide from its own system.
 *
 * @param program the number the machine lists the program under, so an event can find it again
 * @param open    false once, when the window is gone, so the desktop takes it off the screen
 */
public record UiWindowPayload(BlockPos hostPos, int program, long window, String title, int width, int height,
                              boolean ask, boolean open, List<Widget> widgets) implements CustomPacketPayload {

    /** Whether a widget shows, whether it answers, and whether it is ticked. */
    public static final int SHOWS = 1;
    public static final int ANSWERS = 2;
    public static final int TICKED = 4;

    /** Where a widget the program placed itself is; laid out widgets have this instead of a place. */
    public static final int LAID_OUT = -1;

    private static final int MOST_WIDGETS = UiWidgets.MOST_WIDGETS;
    private static final int MOST_ROWS = UiWidgets.MOST_ROWS;
    private static final int MOST_STROKES = UiWidgets.MOST_STROKES;
    private static final int MOST_TEXT = 256;

    public UiWindowPayload {
        widgets = List.copyOf(widgets);
    }

    /**
     * One widget: what kind it is, whose it is, and everything it holds.
     *
     * @param parent  the widget that holds it, or zero for the one the window shows
     * @param numbers the spacing, the value, the least, the most and what is picked, in that order
     * @param rows    what a list holds, and {@code details} what is written at the right of each row
     */
    public record Widget(long id, String kind, long parent, int weight, int flags, int width, int height,
                         int x, int y, String text, List<Integer> numbers, List<String> rows,
                         List<String> details, List<Stroke> drawing) {

        public Widget {
            numbers = List.copyOf(numbers);
            rows = List.copyOf(rows);
            details = List.copyOf(details);
            drawing = List.copyOf(drawing);
        }

        public boolean shows() {
            return (this.flags & SHOWS) != 0;
        }

        public boolean answers() {
            return (this.flags & ANSWERS) != 0;
        }

        public boolean ticked() {
            return (this.flags & TICKED) != 0;
        }

        /** One of the five numbers a widget carries, or zero when it carries none. */
        public int number(final int at) {
            return at < this.numbers.size() ? this.numbers.get(at) : 0;
        }
    }

    /** One thing a canvas was asked to draw. */
    public record Stroke(String kind, int x, int y, int x2, int y2, int colour, String text) {
    }

    /** The window as it stands, flattened for the wire; null when that object is not a window. */
    public static UiWindowPayload of(final BlockPos host, final int program, final Values.Obj window) {
        if (window == null || !UiWidgets.WINDOW.equals(window.type())) {
            return null;
        }
        final List<Widget> widgets = new ArrayList<>();
        if (window.get(UiWidgets.CONTENT) instanceof Values.Obj content) {
            flatten(content, 0, 0, LAID_OUT, LAID_OUT, 0, 0, widgets);
        }
        if (window.get(UiWidgets.PLACED) instanceof Values.ListValue placed) {
            for (final Object one : placed.items()) {
                if (one instanceof Values.Obj where && where.get("Widget") instanceof Values.Obj widget) {
                    flatten(widget, 0, 0, Numbers.toInt(where.get("X")), Numbers.toInt(where.get("Y")),
                            Numbers.toInt(where.get(UiWidgets.WIDTH)), Numbers.toInt(where.get(UiWidgets.HEIGHT)),
                            widgets);
                }
            }
        }
        return new UiWindowPayload(host, program, Numbers.toLong(window.get(UiWidgets.ID)),
                text(window.get(UiWidgets.TITLE)), Numbers.toInt(window.get(UiWidgets.WIDTH)),
                Numbers.toInt(window.get(UiWidgets.HEIGHT)),
                Boolean.TRUE.equals(window.get(UiWidgets.ASK)), true, widgets);
    }

    /** A window that has gone, which is all the desktop needs to take it off the screen. */
    public static UiWindowPayload gone(final BlockPos host, final int program, final long window) {
        return new UiWindowPayload(host, program, window, "", 0, 0, false, false, List.of());
    }

    private static void flatten(final Values.Obj widget, final long parent, final int weight, final int x,
                                final int y, final int placedW, final int placedH, final List<Widget> into) {
        if (into.size() >= MOST_WIDGETS) {
            return;
        }
        int flags = Boolean.TRUE.equals(widget.get(UiWidgets.VISIBLE)) ? SHOWS : 0;
        flags |= Boolean.TRUE.equals(widget.get(UiWidgets.ENABLED)) ? ANSWERS : 0;
        flags |= Boolean.TRUE.equals(widget.get(UiWidgets.CHECKED)) ? TICKED : 0;
        final List<Integer> numbers = List.of(Numbers.toInt(widget.get(UiWidgets.SPACING)),
                Numbers.toInt(widget.get(UiWidgets.VALUE)), Numbers.toInt(widget.get(UiWidgets.LEAST)),
                Numbers.toInt(widget.get(UiWidgets.MOST)), Numbers.toInt(widget.get(UiWidgets.SELECTED)));
        final List<String> rows = names(widget.get(UiWidgets.ITEMS));
        final List<String> details = names(widget.get(UiWidgets.RIGHTS));
        final List<Stroke> drawing = strokes(widget.get(UiWidgets.DRAWING));
        final long id = Numbers.toLong(widget.get(UiWidgets.ID));
        into.add(new Widget(id, widget.type(), parent, weight, flags,
                placedW > 0 ? placedW : Numbers.toInt(widget.get(UiWidgets.WIDTH)),
                placedH > 0 ? placedH : Numbers.toInt(widget.get(UiWidgets.HEIGHT)), x, y,
                text(widget.get(UiWidgets.TEXT)), numbers, rows, details, drawing));
        if (!(widget.get(UiWidgets.CHILDREN) instanceof Values.ListValue children)) {
            return;
        }
        final List<Object> weights = widget.get(UiWidgets.WEIGHTS) instanceof Values.ListValue given
                ? given.items() : List.of();
        for (int i = 0; i < children.items().size(); i++) {
            if (children.items().get(i) instanceof Values.Obj child) {
                flatten(child, id, i < weights.size() ? Numbers.toInt(weights.get(i)) : 0, LAID_OUT, LAID_OUT,
                        0, 0, into);
            }
        }
    }

    private static List<String> names(final Object held) {
        if (!(held instanceof Values.ListValue list)) {
            return List.of();
        }
        final List<String> out = new ArrayList<>(Math.min(list.items().size(), MOST_ROWS));
        for (final Object one : list.items()) {
            if (out.size() >= MOST_ROWS) {
                break;
            }
            out.add(text(one));
        }
        return out;
    }

    private static List<Stroke> strokes(final Object held) {
        if (!(held instanceof Values.ListValue list)) {
            return List.of();
        }
        final List<Stroke> out = new ArrayList<>(Math.min(list.items().size(), MOST_STROKES));
        for (final Object one : list.items()) {
            if (out.size() >= MOST_STROKES) {
                break;
            }
            if (one instanceof Values.Obj stroke) {
                out.add(new Stroke(text(stroke.get("Kind")), Numbers.toInt(stroke.get("X")),
                        Numbers.toInt(stroke.get("Y")), Numbers.toInt(stroke.get("X2")),
                        Numbers.toInt(stroke.get("Y2")), Numbers.toInt(stroke.get("Colour")),
                        text(stroke.get(UiWidgets.TEXT))));
            }
        }
        return out;
    }

    private static String text(final Object value) {
        if (value == null) {
            return "";
        }
        final String said = String.valueOf(value);
        return said.length() > MOST_TEXT ? said.substring(0, MOST_TEXT) : said;
    }

    public static final CustomPacketPayload.Type<UiWindowPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "ui_window"));

    private static final StreamCodec<io.netty.buffer.ByteBuf, String> WORDS = ByteBufCodecs.stringUtf8(MOST_TEXT);

    private static final StreamCodec<RegistryFriendlyByteBuf, Stroke> STROKE =
            StreamCodec.of((buf, stroke) -> {
                WORDS.encode(buf, stroke.kind());
                ByteBufCodecs.VAR_INT.encode(buf, stroke.x());
                ByteBufCodecs.VAR_INT.encode(buf, stroke.y());
                ByteBufCodecs.VAR_INT.encode(buf, stroke.x2());
                ByteBufCodecs.VAR_INT.encode(buf, stroke.y2());
                ByteBufCodecs.VAR_INT.encode(buf, stroke.colour());
                WORDS.encode(buf, stroke.text());
            }, buf -> new Stroke(WORDS.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), WORDS.decode(buf)));

    private static final StreamCodec<RegistryFriendlyByteBuf, Widget> WIDGET =
            StreamCodec.of(UiWindowPayload::writeWidget, UiWindowPayload::readWidget);

    public static final StreamCodec<RegistryFriendlyByteBuf, UiWindowPayload> STREAM_CODEC =
            StreamCodec.of(UiWindowPayload::write, UiWindowPayload::read);

    private static void writeWidget(final RegistryFriendlyByteBuf buf, final Widget widget) {
        ByteBufCodecs.VAR_LONG.encode(buf, widget.id());
        WORDS.encode(buf, widget.kind());
        ByteBufCodecs.VAR_LONG.encode(buf, widget.parent());
        ByteBufCodecs.VAR_INT.encode(buf, widget.weight());
        ByteBufCodecs.VAR_INT.encode(buf, widget.flags());
        ByteBufCodecs.VAR_INT.encode(buf, widget.width());
        ByteBufCodecs.VAR_INT.encode(buf, widget.height());
        buf.writeInt(widget.x());
        buf.writeInt(widget.y());
        WORDS.encode(buf, widget.text());
        ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(8)).encode(buf, widget.numbers());
        WORDS.apply(ByteBufCodecs.list(MOST_ROWS)).encode(buf, widget.rows());
        WORDS.apply(ByteBufCodecs.list(MOST_ROWS)).encode(buf, widget.details());
        STROKE.apply(ByteBufCodecs.list(MOST_STROKES)).encode(buf, widget.drawing());
    }

    private static Widget readWidget(final RegistryFriendlyByteBuf buf) {
        return new Widget(ByteBufCodecs.VAR_LONG.decode(buf), WORDS.decode(buf), ByteBufCodecs.VAR_LONG.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), buf.readInt(), buf.readInt(),
                WORDS.decode(buf), ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(8)).decode(buf),
                WORDS.apply(ByteBufCodecs.list(MOST_ROWS)).decode(buf),
                WORDS.apply(ByteBufCodecs.list(MOST_ROWS)).decode(buf),
                STROKE.apply(ByteBufCodecs.list(MOST_STROKES)).decode(buf));
    }

    private static void write(final RegistryFriendlyByteBuf buf, final UiWindowPayload payload) {
        BlockPos.STREAM_CODEC.encode(buf, payload.hostPos());
        ByteBufCodecs.VAR_INT.encode(buf, payload.program());
        ByteBufCodecs.VAR_LONG.encode(buf, payload.window());
        WORDS.encode(buf, payload.title());
        ByteBufCodecs.VAR_INT.encode(buf, payload.width());
        ByteBufCodecs.VAR_INT.encode(buf, payload.height());
        ByteBufCodecs.BOOL.encode(buf, payload.ask());
        ByteBufCodecs.BOOL.encode(buf, payload.open());
        WIDGET.apply(ByteBufCodecs.list(MOST_WIDGETS)).encode(buf, payload.widgets());
    }

    private static UiWindowPayload read(final RegistryFriendlyByteBuf buf) {
        return new UiWindowPayload(BlockPos.STREAM_CODEC.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.VAR_LONG.decode(buf), WORDS.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.BOOL.decode(buf), ByteBufCodecs.BOOL.decode(buf),
                WIDGET.apply(ByteBufCodecs.list(MOST_WIDGETS)).decode(buf));
    }

    @Override
    public CustomPacketPayload.Type<UiWindowPayload> type() {
        return TYPE;
    }
}
