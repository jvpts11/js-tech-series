/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.rocket;

import com.mojang.math.Axis;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.space.JsSpace;
import dev.jstech.space.rocket.CabinLamp;
import dev.jstech.space.rocket.CabinReadings;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The live instruments of a capsule's panel, drawn as the approved panel paints them, each into its own region of a
 * face in pixels of {@link #DENSITY} a block: the strip indicators of the stage's fuel and oxidiser, the climb's
 * upright strip, the bank of lamps, the heading's tape, the mission clock in seven segments, and the two round gauges,
 * the cabin's air on the left and the rocket's propellant on the right, its scale banded red, amber and green.
 */
@PaletteHolder
public final class CabinArt {

    /** How many pixels a block each instrument is drawn in, as the approved panel was painted. */
    public static final int DENSITY = 1400;
    public static final Palette<Colours> PALETTE = Palettes.declare(JsSpace.MODID, "rocket_cabin", new Colours(
            0xFF282B30, 0xFFE0E5E5, 0xFF1E2126, 0xFF2666CC, 0xFF848C93, 0xFF23262B, 0xFF3D4247, 0xFF0C0C0C,
            0xFF8C9199, 0xFF4CF272, 0xFFFFB22D, 0xFFF23D2D, 0xFF191C21, 0xFFE5EAED, 0xFF050505, 0xFF110C03,
            0xFF7A7F89, 0xFF0A0C0F, 0xFFDBE0E5, 0xFFFFF9E5, 0xFF9EA3AD));

    /* A strip's frame, and where its ticks and pointer start inside it, in pixels. */
    private static final int STRIP_FRAME = 3;
    private static final int STRIP_INSET = 4;
    private static final int STRIP_END = 6;
    private static final int STRIP_TICKS = 20;
    private static final int UPRIGHT_TICKS = 12;
    private static final int TICK = 2;
    /* A lamp's glass inside its housing, and its label's marks, in pixels. */
    private static final int LAMP_GLASS = 3;
    private static final int LABEL_FROM = 6;
    private static final int LABEL_UNIT = 2;
    /* The tape: its ticks across, which are long, and how many degrees a tick is. */
    private static final int TAPE_TICKS = 12;
    private static final int TAPE_LONG = 3;
    private static final float TAPE_DEGREES = 5;
    private static final int TAPE_EDGE = 3;
    /* A gauge's scale, from its lowest to its highest, in degrees from the right, clockwise; and its ticks. */
    private static final float GAUGE_FROM = 135;
    private static final float GAUGE_SWEEP = 270;
    private static final int GAUGE_TICKS = 21;
    /* A gauge's dial is this much of its face's width, and its region this many times its dial's radius across. */
    private static final float GAUGE_OF_FACE = 0.34F;
    private static final float GAUGE_REGION = 2.4F;
    private static final float GAUGE_UNITS = 60;
    /* The clock's figures keep this far from its window's sides, and are at most this many times as tall as wide. */
    private static final float TIMER_EDGE = 4;
    private static final float TIMER_TALL = 2.4F;
    /* Seven segments, as each figure lights them. */
    private static final String[] SEGMENTS = {"abcdef", "bc", "abged", "abgcd", "fgbc", "afgcd", "afgedc", "abc",
        "abcdefg", "abcdfg"};
    /* Lines are drawn this finely, in parts of a pixel. */
    private static final float FINE = 64;
    private static final long LABEL_MULTIPLIER = 1_103_515_245L;
    private static final long LABEL_STEP = 12_345L;
    private static final long LABEL_MASK = 0x7FFF_FFFFL;

    private CabinArt() {
    }

    /** A strip indicator across {@code w} by {@code h}: its dark window, its scale, its pointer at {@code level}. */
    public static void strip(final GuiGraphics g, final int w, final int h, final float level) {
        final Colours c = PALETTE.get();
        g.fill(0, 0, w, h, c.stripEdge());
        g.fill(STRIP_FRAME, STRIP_FRAME, w - STRIP_FRAME, h - STRIP_FRAME, c.stripFace());
        for (int k = 0; k < STRIP_TICKS; k++) {
            final float x = STRIP_END + (w - 2F * STRIP_END) * k / (STRIP_TICKS - 1);
            final float length = h * (k % 2 == 1 ? 0.45F : 0.25F);
            fill(g, x - TICK / 2F, h - STRIP_INSET - length, x + TICK / 2F, h - STRIP_INSET, c.ink());
        }
        final float at = STRIP_END + (w - 2F * STRIP_END) * level;
        fill(g, at - 2, STRIP_INSET, at + 2, h - STRIP_INSET, c.pointer());
    }

    /** The upright strip across {@code w} by {@code h}, its pointer at {@code level} from its foot. */
    public static void uprightStrip(final GuiGraphics g, final int w, final int h, final float level) {
        final Colours c = PALETTE.get();
        g.fill(0, 0, w, h, c.stripEdge());
        g.fill(STRIP_FRAME, STRIP_FRAME, w - STRIP_FRAME, h - STRIP_FRAME, c.stripFace());
        for (int k = 0; k < UPRIGHT_TICKS; k++) {
            final float y = STRIP_END + (h - 2F * STRIP_END) * k / (UPRIGHT_TICKS - 1);
            fill(g, STRIP_INSET, y - TICK / 2F, STRIP_INSET + w * (k % 2 == 1 ? 0.5F : 0.3F), y + TICK / 2F,
                    c.ink());
        }
        // The pointer, a wedge pointing at the scale from the strip's right.
        final float y = h - STRIP_END - (h - 2F * STRIP_END) * level;
        final float tip = w - STRIP_INSET;
        final float back = tip - w * 0.4F;
        for (int dy = -4; dy <= 4; dy++) {
            final float reach = (tip - back) * (1 - Math.abs(dy) / 4F);
            fill(g, tip - reach, y + dy - 0.5F, tip, y + dy + 0.5F, c.pointer());
        }
    }

    /**
     * The bank of lamps across {@code w} by {@code h}, its top left at {@code (x0, y0)} of its face: each lamp at its
     * own box of {@code lamps}, in the face's pixels, lit as {@code readings} say. A lit lamp shines of itself, so its
     * glass is drawn again over the bank, lit ({@link #glass}, {@link #marks}).
     */
    public static void lamps(final GuiGraphics g, final int w, final int h, final float x0, final float y0,
                             final List<float[]> lamps, final CabinReadings readings) {
        final Colours c = PALETTE.get();
        g.fill(0, 0, w, h, c.inset());
        for (int k = 0; k < lamps.size() && k < CabinLamp.BANK.size(); k++) {
            final float[] box = lamps.get(k);
            final CabinLamp lamp = CabinLamp.BANK.get(k);
            final boolean lit = readings.lit(lamp);
            fill(g, box[0] - x0, box[1] - y0, box[2] - x0, box[3] - y0, c.housing());
            final float[] glass = glass(box);
            fill(g, glass[0] - x0, glass[1] - y0, glass[2] - x0, glass[3] - y0, lit ? colour(lamp, c)
                    : c.lampOff());
            final float[] marks = marks(box, k);
            for (int m = 0; m + 3 < marks.length; m += 4) {
                fill(g, marks[m] - x0, marks[m + 1] - y0, marks[m + 2] - x0, marks[m + 3] - y0,
                        lit ? c.labelLit() : c.labelOff());
            }
        }
    }

    /** The glass of the lamp whose housing is {@code box}, left, top, right and bottom, in the face's pixels. */
    public static float[] glass(final float[] box) {
        return new float[] {box[0] + LAMP_GLASS, box[1] + LAMP_GLASS, box[2] - LAMP_GLASS, box[3] - LAMP_GLASS};
    }

    /**
     * The marks of the label engraved on the lamp {@code k}, whose housing is {@code box}: words read from a seat
     * away, four numbers a mark, left, top, right and bottom, in the face's pixels.
     */
    public static float[] marks(final float[] box, final int k) {
        final float across = (box[2] - box[0]) / 1.8F;
        return marks(box[0] + LABEL_FROM, box[1] + (box[3] - box[1]) * 0.45F, across * 1.2F, LABEL_UNIT, k);
    }

    /** The colour a lamp lights in. */
    public static int colour(final CabinLamp lamp) {
        return colour(lamp, PALETTE.get());
    }

    /** The colour a lit lamp's label is in. */
    public static int litLabel() {
        return PALETTE.get().labelLit();
    }

    /** The heading's tape across {@code w} by {@code h}: its ticks slid by {@code heading}, its middle marked. */
    public static void tape(final GuiGraphics g, final int w, final int h, final float heading) {
        final Colours c = PALETTE.get();
        g.fill(0, 0, w, h, c.dark());
        final float ticks = heading / TAPE_DEGREES;
        final int whole = (int) Math.floor(ticks);
        final float part = ticks - whole;
        final float step = w / (2F * TAPE_TICKS);
        for (int k = -TAPE_TICKS; k <= TAPE_TICKS; k++) {
            final float x = w / 2F + (k - part) * step;
            if (x > TAPE_EDGE && x < w - TAPE_EDGE) {
                final boolean big = Math.floorMod(k + whole, TAPE_LONG) == 0;
                final float length = h * (big ? 0.45F : 0.22F);
                fill(g, x - TICK / 2F, h - STRIP_INSET - length, x + TICK / 2F, h - STRIP_INSET, c.white());
            }
        }
        fill(g, w / 2F - 1.5F, 2, w / 2F + 1.5F, h - 2, c.amber());
    }

    /**
     * The mission clock across {@code w} by {@code h}, in lit seven segments over the faint unlit ones, its figures as
     * the approved panel draws them but no wider than the clock's window holds.
     */
    public static void timer(final GuiGraphics g, final int w, final int h, final String shown) {
        final Colours c = PALETTE.get();
        g.fill(0, 0, w, h, c.timerFace());
        // Each figure takes its width and three strokes after it, a stroke a quarter of a figure; the colon three.
        final float across = shown.replace(":", "").length() * 1.75F + (shown.contains(":") ? 0.75F : 0);
        final float dw = Math.min(h * 0.25F, (w - 2F * TIMER_EDGE) / across);
        final float dh = Math.min(h * 0.6F, dw * TIMER_TALL);
        final float t = Math.max(1, (int) (dw / 4));
        float x = TIMER_EDGE;
        final float y = (h - dh) / 2;
        for (int i = 0; i < shown.length(); i++) {
            final char ch = shown.charAt(i);
            if (ch == ':') {
                fill(g, x, y + dh * 0.3F, x + t, y + dh * 0.3F + t, c.amber());
                fill(g, x, y + dh * 0.7F, x + t, y + dh * 0.7F + t, c.amber());
                x += t * 3;
                continue;
            }
            final String lit = ch >= '0' && ch <= '9' ? SEGMENTS[ch - '0'] : ch == '-' || ch == '+' ? "g" : "";
            for (final char segment : "abcdefg".toCharArray()) {
                segment(g, x, y, dw, dh, t, segment, lit.indexOf(segment) >= 0 ? c.amber() : c.segmentOff());
            }
            if (ch == '+') {
                fill(g, x + dw / 2 - t / 2, y + dh * 0.2F, x + dw / 2 + t / 2, y + dh * 0.8F, c.amber());
            }
            x += dw + t * 3;
        }
    }

    /**
     * A round gauge filling its region, {@code size} across, its needle at {@code level} of its scale; its scale
     * banded red, amber and green where {@code bands}.
     */
    public static void gauge(final GuiGraphics g, final int size, final float level, final boolean bands) {
        final Colours c = PALETTE.get();
        g.fill(0, 0, size, size, c.housing());
        final float cx = size / 2F;
        final float cy = size / 2F;
        final float r = size / GAUGE_REGION;
        final float u = Math.max(1, r / GAUGE_OF_FACE / GAUGE_UNITS);
        disc(g, cx, cy, r + 2 * u, c.bezel());
        disc(g, cx, cy, r + u, c.stripEdge());
        disc(g, cx, cy, r, c.dialFace());
        final float band = Math.max(1, (int) (u * 1.3F));
        if (bands) {
            final float mid = r - 2 * u - band / 2;
            arc(g, cx, cy, mid, band, GAUGE_FROM, 170, c.red());
            arc(g, cx, cy, mid, band, 170, 220, c.amber());
            arc(g, cx, cy, mid, band, 220, 405, c.green());
        }
        for (int k = 0; k < GAUGE_TICKS; k++) {
            final double angle = Math.toRadians(GAUGE_FROM + k * GAUGE_SWEEP / (GAUGE_TICKS - 1));
            final float length = r * (k % 2 == 0 ? 0.22F : 0.12F);
            final float cos = (float) Math.cos(angle);
            final float sin = (float) Math.sin(angle);
            line(g, cx + (r - u) * cos, cy + (r - u) * sin, cx + (r - u - length) * cos, cy + (r - u - length) * sin,
                    Math.max(1, u * (k % 2 == 0 ? 0.6F : 0.4F)), c.dialInk());
        }
        label(g, cx - r * 0.35F, cy + r * 0.38F, r * 0.7F, u * 0.6F, c.dialInk(), (int) cx);
        final double needle = Math.toRadians(GAUGE_FROM + Math.max(0, Math.min(1, level)) * GAUGE_SWEEP);
        final float cos = (float) Math.cos(needle);
        final float sin = (float) Math.sin(needle);
        line(g, cx - r * 0.18F * cos, cy - r * 0.18F * sin, cx + r * 0.82F * cos, cy + r * 0.82F * sin,
                Math.max(1, (int) u), c.needle());
        disc(g, cx, cy, 1.6F * u, c.pivot());
    }

    private static int colour(final CabinLamp lamp, final Colours c) {
        return switch (lamp.colour()) {
            case GREEN -> c.green();
            case AMBER -> c.amber();
            case RED -> c.red();
        };
    }

    /* One of a figure's seven segments, a line {@code t} thick along its edge. */
    private static void segment(final GuiGraphics g, final float x, final float y, final float dw, final float dh,
                                final float t, final char segment, final int colour) {
        final float half = t / 2;
        switch (segment) {
            case 'a' -> fill(g, x, y - half, x + dw, y + half, colour);
            case 'b' -> fill(g, x + dw - half, y, x + dw + half, y + dh / 2, colour);
            case 'c' -> fill(g, x + dw - half, y + dh / 2, x + dw + half, y + dh, colour);
            case 'd' -> fill(g, x, y + dh - half, x + dw, y + dh + half, colour);
            case 'e' -> fill(g, x - half, y + dh / 2, x + half, y + dh, colour);
            case 'f' -> fill(g, x - half, y, x + half, y + dh / 2, colour);
            default -> fill(g, x, y + dh / 2 - half, x + dw, y + dh / 2 + half, colour);
        }
    }

    /* An engraved label as words read from a seat away, drawn from its {@link #marks}. */
    private static void label(final GuiGraphics g, final float x, final float y, final float length, final float u,
                              final int colour, final int seed) {
        final float[] marks = marks(x, y, length, u, seed);
        for (int m = 0; m + 3 < marks.length; m += 4) {
            fill(g, marks[m], marks[m + 1], marks[m + 2], marks[m + 3], colour);
        }
    }

    /*
     * A label's marks: short marks and gaps along {@code length}, laid by the same sequence of numbers each time for
     * the same {@code seed}, four numbers a mark.
     */
    private static float[] marks(final float x, final float y, final float length, final float u, final int seed) {
        final List<Float> out = new ArrayList<>();
        long k = seed;
        float at = x;
        final float tall = Math.max(1, Math.round(u * 0.7F));
        while (at < x + length) {
            k = (k * LABEL_MULTIPLIER + LABEL_STEP) & LABEL_MASK;
            final float wide = Math.max(1, (int) (u * (0.6F + (k % 3) * 0.5F)));
            out.add(at);
            out.add(y);
            out.add(Math.min(x + length, at + wide));
            out.add(y + tall);
            at += wide + Math.max(1, (int) (u * (k % 5 != 0 ? 0.5F : 1.6F)));
        }
        final float[] marks = new float[out.size()];
        for (int i = 0; i < marks.length; i++) {
            marks[i] = out.get(i);
        }
        return marks;
    }

    /* A round disc, row by row. */
    private static void disc(final GuiGraphics g, final float cx, final float cy, final float r, final int colour) {
        for (int row = (int) Math.floor(cy - r); row < Math.ceil(cy + r); row++) {
            final float dy = row + 0.5F - cy;
            if (Math.abs(dy) > r) {
                continue;
            }
            final float half = (float) Math.sqrt(r * r - dy * dy);
            fill(g, cx - half, row, cx + half, row + 1, colour);
        }
    }

    /* A band {@code width} thick along a circle's arc from {@code from} to {@code to} degrees, clockwise. */
    private static void arc(final GuiGraphics g, final float cx, final float cy, final float r, final float width,
                            final float from, final float to, final int colour) {
        final float step = 3;
        for (float a = from; a < to; a += step) {
            final double a0 = Math.toRadians(a);
            final double a1 = Math.toRadians(Math.min(to, a + step) + 0.5);
            line(g, cx + r * (float) Math.cos(a0), cy + r * (float) Math.sin(a0), cx + r * (float) Math.cos(a1),
                    cy + r * (float) Math.sin(a1), width, colour);
        }
    }

    /* A straight line {@code width} thick from one point to another, drawn finer than a pixel where it slants. */
    private static void line(final GuiGraphics g, final float x0, final float y0, final float x1, final float y1,
                             final float width, final int colour) {
        final float length = (float) Math.hypot(x1 - x0, y1 - y0);
        if (length <= 0) {
            return;
        }
        g.pose().pushPose();
        g.pose().translate(x0, y0, 0);
        g.pose().mulPose(Axis.ZP.rotation((float) Math.atan2(y1 - y0, x1 - x0)));
        g.pose().scale(1 / FINE, 1 / FINE, 1);
        g.fill(0, Math.round(-width / 2 * FINE), Math.round(length * FINE), Math.round(width / 2 * FINE), colour);
        g.pose().popPose();
    }

    /* A box between two corners finer than a pixel. */
    private static void fill(final GuiGraphics g, final float x0, final float y0, final float x1, final float y1,
                             final int colour) {
        g.pose().pushPose();
        g.pose().scale(1 / FINE, 1 / FINE, 1);
        g.fill(Math.round(x0 * FINE), Math.round(y0 * FINE), Math.round(x1 * FINE), Math.round(y1 * FINE), colour);
        g.pose().popPose();
    }

    /**
     * The colours of the panel's live instruments.
     *
     * @param stripEdge  a strip's dark frame, and a gauge's ring
     * @param stripFace  a strip's white face
     * @param ink        the ticks on a strip
     * @param pointer    a strip's pointer
     * @param inset      the inset the lamps stand in
     * @param housing    a lamp's housing, and a gauge's ground
     * @param lampOff    a lamp's glass unlit
     * @param labelLit   a label on a lamp lit
     * @param labelOff   a label on a lamp unlit
     * @param green      what is working
     * @param amber      what wants an eye, and the clock's segments
     * @param red        what is wrong
     * @param dark       the tape's ground
     * @param white      the tape's ticks
     * @param timerFace  the clock's ground
     * @param segmentOff the clock's segments unlit
     * @param bezel      a gauge's bezel
     * @param dialFace   a gauge's face
     * @param dialInk    a gauge's ticks and label
     * @param needle     a gauge's needle
     * @param pivot      a gauge's pivot
     */
    public record Colours(int stripEdge, int stripFace, int ink, int pointer, int inset, int housing, int lampOff,
                          int labelLit, int labelOff, int green, int amber, int red, int dark, int white,
                          int timerFace, int segmentOff, int bezel, int dialFace, int dialInk, int needle,
                          int pivot) {
    }
}
