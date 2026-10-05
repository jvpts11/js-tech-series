/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.vm.program.ComponentValues;
import dev.jstech.computers.vm.program.Numbers;
import dev.jstech.computers.vm.program.UiWidgets;
import dev.jstech.computers.vm.program.Values;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * One window a Σ# program has open on a machine, as the desktop draws it: what it is called, how big
 * it asked to be, and every widget in it with what that widget holds.
 *
 * <p>The whole window goes over at once whenever anything in it changes, because a window is small: a
 * handful of widgets, a few words each. What is not sent is how any of it looks, because that is the
 * looking machine's to decide from its own system.
 *
 * <p>A window carries at most {@link #MOST_BYTES} however much its program holds. Its widgets are written in the
 * order they are drawn, and once the window is full, what the rest hold (rows, strokes, a component's value) is left
 * out and the widget is marked as cut, so a screen still draws every widget and knows which of them show less than
 * the program holds.
 *
 * @param program the number the machine lists the program under, so an event can find it again
 * @param open    false once, when the window is gone, so the desktop takes it off the screen
 */
public record UiWindowPayload(BlockPos hostPos, int program, long window, String title, int width, int height,
                              boolean ask, boolean open, List<Widget> widgets) implements CustomPacketPayload {

    /** Whether a widget shows, whether it answers, and whether it is ticked (or a radio group lies across). */
    public static final int SHOWS = 1;
    public static final int ANSWERS = 2;
    public static final int TICKED = 4;
    /** A canvas carrying only what has been drawn since it last went over, to add to what the screen has. */
    public static final int APPENDS = 8;
    /** A widget some of whose rows, strokes or value were left out, because its window was full. */
    public static final int CUT = 16;

    /** Where a widget the program placed itself is; laid out widgets have this instead of a place. */
    public static final int LAID_OUT = -1;

    /** The most one window takes on the wire, whatever its program holds. */
    public static final int MOST_BYTES = 128 * 1024;

    /** The longest single text the wire carries; longer ones go as a run of these, one row each. */
    public static final int MOST_TEXT = 256;

    /*
     * Roughly what a window, a widget and a stroke cost on the wire. Counted by shape rather than by running the
     * encoding a second time: near enough to spend a budget by, and cheap enough to ask of every window that goes.
     */
    private static final int A_WINDOW = 32;
    private static final int A_WIDGET = 40;
    private static final int A_STROKE = 20;
    private static final int MOST_WIDGETS = UiWidgets.MOST_WIDGETS;
    private static final int MOST_ROWS = UiWidgets.MOST_ROWS;
    private static final int MOST_STROKES = UiWidgets.MOST_STROKES;

    public UiWindowPayload {
        widgets = List.copyOf(widgets);
    }

    /**
     * One widget: what kind it is, whose it is, and everything it holds.
     *
     * @param parent  the widget that holds it, or zero for the one the window shows
     * @param numbers the spacing, the value, the least, the most, what is picked and the step, in that order
     * @param rows    what a list holds, and {@code details} what is written at the right of each row
     * @param epoch   which drawing a canvas is on, moved on every time it is cleared, or how many times a dialog was
     *                shown; zero for anything else
     */
    public record Widget(long id, String kind, long parent, int weight, int flags, int width, int height,
                         int x, int y, String text, List<Integer> numbers, List<String> rows,
                         List<String> details, List<Stroke> drawing, int epoch) {

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

        /** Whether some of what the program holds in it was left out, because its window was full. */
        public boolean cut() {
            return (this.flags & CUT) != 0;
        }

        /** Whether these strokes are to be added to the ones the screen already has, rather than to replace them. */
        public boolean appends() {
            return (this.flags & APPENDS) != 0;
        }

        /** The same widget holding all of those strokes, which is what a screen keeps once it has added them. */
        public Widget withStrokes(final List<Stroke> strokes) {
            return new Widget(this.id, this.kind, this.parent, this.weight, this.flags & ~APPENDS, this.width,
                    this.height, this.x, this.y, this.text, this.numbers, this.rows, this.details, strokes,
                    this.epoch);
        }

        /**
         * The same widget carrying only the strokes from that one on, to be added to what the screen already
         * has. A canvas only ever grows between clears, so what a screen is missing is always its tail.
         */
        public Widget strokesFrom(final int index) {
            if (index <= 0) {
                return this;
            }
            final int from = Math.min(index, this.drawing.size());
            return new Widget(this.id, this.kind, this.parent, this.weight, this.flags | APPENDS, this.width,
                    this.height, this.x, this.y, this.text, this.numbers, this.rows, this.details,
                    this.drawing.subList(from, this.drawing.size()), this.epoch);
        }

        /** One of the numbers a widget carries, or zero when it carries none. */
        public int number(final int at) {
            return at < this.numbers.size() ? this.numbers.get(at) : 0;
        }

        /** The rows joined back into the one long text they were cut from: a text area's, or a component's value. */
        public String joined() {
            return String.join("", this.rows);
        }

        /** One of the rows, or empty when it has fewer. */
        public String row(final int at) {
            return at >= 0 && at < this.rows.size() ? this.rows.get(at) : "";
        }
    }

    /** One thing a canvas was asked to draw. */
    public record Stroke(String kind, int x, int y, int x2, int y2, int colour, String text) {
    }

    /** Roughly how many bytes this window takes on the wire, for a machine to spend its budget by. */
    public int weight() {
        int bytes = A_WINDOW + this.title.length();
        for (final Widget widget : this.widgets) {
            bytes += A_WIDGET + widget.text().length();
            for (final String row : widget.rows()) {
                bytes += row.length() + 2;
            }
            for (final String detail : widget.details()) {
                bytes += detail.length() + 2;
            }
            for (final Stroke stroke : widget.drawing()) {
                bytes += A_STROKE + stroke.text().length();
            }
        }
        return bytes;
    }

    /** The same window carrying those widgets, for sending one player less of it than another is sent. */
    public UiWindowPayload withWidgets(final List<Widget> widgets) {
        return new UiWindowPayload(this.hostPos, this.program, this.window, this.title, this.width, this.height,
                this.ask, this.open, widgets);
    }

    /** The window as it stands, flattened for the wire; null when that object is not a window. */
    public static UiWindowPayload of(final BlockPos host, final int program, final Values.Obj window) {
        if (window == null || !UiWidgets.WINDOW.equals(window.type())) {
            return null;
        }
        final Flattening flat = new Flattening(MOST_BYTES - A_WINDOW - text(window.get(UiWidgets.TITLE)).length());
        if (window.get(UiWidgets.CONTENT) instanceof Values.Obj content) {
            flat.widget(content, 0, 0, LAID_OUT, LAID_OUT, 0, 0);
        }
        if (window.get(UiWidgets.PLACED) instanceof Values.ListValue placed) {
            for (final Object one : placed.items()) {
                if (one instanceof Values.Obj where && where.get("Widget") instanceof Values.Obj widget) {
                    flat.widget(widget, 0, 0, Numbers.toInt(where.get("X")), Numbers.toInt(where.get("Y")),
                            Numbers.toInt(where.get(UiWidgets.WIDTH)), Numbers.toInt(where.get(UiWidgets.HEIGHT)));
                }
            }
        }
        if (window.get(UiWidgets.DIALOG) instanceof Values.Obj dialog) {
            flat.widget(dialog, 0, 0, LAID_OUT, LAID_OUT, 0, 0);
        }
        return new UiWindowPayload(host, program, Numbers.toLong(window.get(UiWidgets.ID)),
                text(window.get(UiWidgets.TITLE)), Numbers.toInt(window.get(UiWidgets.WIDTH)),
                Numbers.toInt(window.get(UiWidgets.HEIGHT)),
                Boolean.TRUE.equals(window.get(UiWidgets.ASK)), true, flat.into);
    }

    /** A window that has gone, which is all the desktop needs to take it off the screen. */
    public static UiWindowPayload gone(final BlockPos host, final int program, final long window) {
        return new UiWindowPayload(host, program, window, "", 0, 0, false, false, List.of());
    }

    /* A window being written out for the wire, widget by widget, out of what is left of its bytes. */
    private static final class Flattening {

        private final List<Widget> into = new ArrayList<>();
        private final Set<Values.Obj> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        private int left;
        private boolean cut;

        Flattening(final int bytes) {
            this.left = bytes;
        }

        void widget(final Values.Obj widget, final long parent, final int weight, final int x, final int y,
                    final int placedW, final int placedH) {
            // A widget reached a second time is sent once, as the runtime counts it once.
            if (this.into.size() >= MOST_WIDGETS || !this.seen.add(widget)) {
                return;
            }
            this.cut = false;
            final String type = widget.type();
            final String text = this.spend(text(widget.get(textField(type))), A_WIDGET);
            final List<String> rows = this.rows(widget);
            final List<String> details = this.texts(widget.get(detailField(type)), MOST_ROWS);
            final List<Stroke> drawing = this.strokes(widget.get(UiWidgets.DRAWING));
            int flags = Boolean.TRUE.equals(widget.get(UiWidgets.VISIBLE)) ? SHOWS : 0;
            flags |= Boolean.TRUE.equals(widget.get(UiWidgets.ENABLED)) ? ANSWERS : 0;
            flags |= Boolean.TRUE.equals(widget.get(UiWidgets.CHECKED))
                    || Boolean.TRUE.equals(widget.get(UiWidgets.ACROSS)) ? TICKED : 0;
            flags |= this.cut ? CUT : 0;
            final long id = Numbers.toLong(widget.get(UiWidgets.ID));
            this.into.add(new Widget(id, type, parent, weight, flags,
                    placedW > 0 ? placedW : Numbers.toInt(widget.get(UiWidgets.WIDTH)),
                    placedH > 0 ? placedH : Numbers.toInt(widget.get(UiWidgets.HEIGHT)), x, y,
                    text, numbers(widget), rows, details, drawing, Numbers.toInt(widget.get(UiWidgets.EPOCH))));
            final List<Object> weights = widget.get(UiWidgets.WEIGHTS) instanceof Values.ListValue given
                    ? given.items() : List.of();
            if (widget.get(UiWidgets.CHILDREN) instanceof Values.ListValue children) {
                for (int i = 0; i < children.items().size(); i++) {
                    if (children.items().get(i) instanceof Values.Obj child) {
                        this.widget(child, id, i < weights.size() ? Numbers.toInt(weights.get(i)) : 0, LAID_OUT,
                                LAID_OUT, 0, 0);
                    }
                }
            }
            if (widget.get(UiWidgets.CONTENT) instanceof Values.Obj content) {
                this.widget(content, id, 0, LAID_OUT, LAID_OUT, 0, 0);
            }
            if (widget.get(UiWidgets.MENU) instanceof Values.Obj menu) {
                this.widget(menu, id, 0, LAID_OUT, LAID_OUT, 0, 0);
            }
        }

        /* What a widget holds as rows: its list, or the long text or value it holds cut into rows. */
        private List<String> rows(final Values.Obj widget) {
            return switch (widget.type()) {
                case UiWidgets.TEXT_AREA -> this.chunks(text(widget.get(UiWidgets.TEXT), UiWidgets.MOST_AREA));
                case UiWidgets.GENERIC -> this.chunks(widget.get(UiWidgets.DATA) == null ? ""
                        : ComponentValues.encode(widget.get(UiWidgets.DATA)));
                case UiWidgets.CHART -> this.points(widget.get(UiWidgets.POINTS));
                case UiWidgets.OPERATION_VIEW -> this.texts(Arrays.asList(widget.get(UiWidgets.STATE),
                        widget.get(UiWidgets.COMPUTER)));
                case UiWidgets.OPEN_FILE_DIALOG, UiWidgets.SAVE_FILE_DIALOG -> this.texts(Arrays.asList(
                        widget.get(UiWidgets.PATH), widget.get(UiWidgets.FILTER), widget.get(UiWidgets.NAME)));
                default -> this.texts(widget.get(UiWidgets.ITEMS), MOST_ROWS);
            };
        }

        /*
         * A long text as rows of the most the wire takes at once; all of it or none, since half a component's value
         * means nothing and half a text area would be taken for all of it.
         */
        private List<String> chunks(final String whole) {
            if (whole.isEmpty()) {
                return List.of();
            }
            final int rows = (whole.length() + MOST_TEXT - 1) / MOST_TEXT;
            if (whole.length() + rows * 2 > this.left) {
                this.cut = true;
                return List.of();
            }
            this.left -= whole.length() + rows * 2;
            final List<String> out = new ArrayList<>(rows);
            for (int at = 0; at < whole.length(); at += MOST_TEXT) {
                out.add(whole.substring(at, Math.min(whole.length(), at + MOST_TEXT)));
            }
            return out;
        }

        private List<String> points(final Object held) {
            if (!(held instanceof Values.ListValue list)) {
                return List.of();
            }
            final List<Object> written = new ArrayList<>(list.size());
            for (final Object one : list.items()) {
                final double value = Numbers.toDouble(one);
                written.add(value == Math.rint(value) && Math.abs(value) < 1e15
                        ? String.valueOf((long) value) : String.valueOf(value));
            }
            return this.texts(written);
        }

        private List<String> texts(final List<Object> held) {
            final Values.ListValue list = new Values.ListValue();
            list.items().addAll(held);
            return this.texts(list, MOST_ROWS);
        }

        private List<String> texts(final Object held, final int most) {
            if (!(held instanceof Values.ListValue list)) {
                return List.of();
            }
            final List<String> out = new ArrayList<>(Math.min(list.items().size(), most));
            for (final Object one : list.items()) {
                final String said = text(one);
                if (out.size() >= most || said.length() + 2 > this.left) {
                    this.cut = this.cut || out.size() < list.items().size();
                    break;
                }
                this.left -= said.length() + 2;
                out.add(said);
            }
            return out;
        }

        private List<Stroke> strokes(final Object held) {
            if (!(held instanceof Values.ListValue list)) {
                return List.of();
            }
            final List<Stroke> out = new ArrayList<>(Math.min(list.items().size(), MOST_STROKES));
            for (final Object one : list.items()) {
                if (out.size() >= MOST_STROKES) {
                    break;
                }
                if (one instanceof Values.Obj stroke) {
                    final String said = text(stroke.get(UiWidgets.TEXT));
                    if (A_STROKE + said.length() > this.left) {
                        this.cut = true;
                        break;
                    }
                    this.left -= A_STROKE + said.length();
                    out.add(new Stroke(text(stroke.get("Kind")), Numbers.toInt(stroke.get("X")),
                            Numbers.toInt(stroke.get("Y")), Numbers.toInt(stroke.get("X2")),
                            Numbers.toInt(stroke.get("Y2")), Numbers.toInt(stroke.get("Colour")), said));
                }
            }
            return out;
        }

        /* A widget's own text and its place on the wire, which always go, budget or no: a window shows every widget. */
        private String spend(final String text, final int shape) {
            this.left = Math.max(0, this.left - shape - text.length());
            return text;
        }
    }

    /* The field a widget's one text comes from, which is not called the same on every kind. */
    private static String textField(final String type) {
        return switch (type) {
            case UiWidgets.IMAGE -> UiWidgets.PATH;
            case UiWidgets.ITEM_SLOT -> UiWidgets.ITEM;
            case UiWidgets.GENERIC -> UiWidgets.KIND;
            case UiWidgets.OPEN_FILE_DIALOG, UiWidgets.SAVE_FILE_DIALOG -> UiWidgets.TITLE;
            default -> UiWidgets.TEXT;
        };
    }

    /* The field written at the right of each row, or over a table's columns, or under each node of a tree. */
    private static String detailField(final String type) {
        return switch (type) {
            case UiWidgets.TABLE -> UiWidgets.COLUMNS;
            case UiWidgets.TREE_VIEW -> UiWidgets.PARENTS;
            default -> UiWidgets.RIGHTS;
        };
    }

    /* The spacing, the value, the least, the most, what is picked and the step; an item slot's value is its amount. */
    private static List<Integer> numbers(final Values.Obj widget) {
        final String type = widget.type();
        final int value;
        final int most;
        if (UiWidgets.ITEM_SLOT.equals(type)) {
            value = Numbers.toInt(widget.get(UiWidgets.AMOUNT));
            most = 0;
        } else if (UiWidgets.OPERATION_VIEW.equals(type)) {
            value = (int) Math.min(Integer.MAX_VALUE, Numbers.toLong(widget.get(UiWidgets.DONE)));
            most = (int) Math.min(Integer.MAX_VALUE, Numbers.toLong(widget.get(UiWidgets.TOTAL)));
        } else {
            value = Numbers.toInt(widget.get(UiWidgets.VALUE));
            most = Numbers.toInt(widget.get(UiWidgets.MOST));
        }
        return List.of(Numbers.toInt(widget.get(UiWidgets.SPACING)), value, Numbers.toInt(widget.get(UiWidgets.LEAST)),
                most, Numbers.toInt(widget.get(UiWidgets.SELECTED)), Numbers.toInt(widget.get(UiWidgets.STEP)));
    }

    private static String text(final Object value) {
        return text(value, MOST_TEXT);
    }

    private static String text(final Object value, final int most) {
        if (value == null) {
            return "";
        }
        final String said = String.valueOf(value);
        return said.length() > most ? said.substring(0, most) : said;
    }

    public static final CustomPacketPayload.Type<UiWindowPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "ui_window"));

    private static final StreamCodec<ByteBuf, String> WORDS = ByteBufCodecs.stringUtf8(MOST_TEXT);

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
        ByteBufCodecs.VAR_INT.encode(buf, widget.epoch());
    }

    private static Widget readWidget(final RegistryFriendlyByteBuf buf) {
        return new Widget(ByteBufCodecs.VAR_LONG.decode(buf), WORDS.decode(buf), ByteBufCodecs.VAR_LONG.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), buf.readInt(), buf.readInt(),
                WORDS.decode(buf), ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(8)).decode(buf),
                WORDS.apply(ByteBufCodecs.list(MOST_ROWS)).decode(buf),
                WORDS.apply(ByteBufCodecs.list(MOST_ROWS)).decode(buf),
                STROKE.apply(ByteBufCodecs.list(MOST_STROKES)).decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf));
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
