/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.theme;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.tier.HardwareEra;

/**
 * The look of the physical monitor frame a software screen is shown inside, chosen by the host computer's
 * hardware era. This is the bezel around the glass: an early machine shows a thick cream CRT shell with a dark
 * inner frame, a mid-era machine a slim grey LCD bezel, a modern machine a thin near-black flat bezel. Each frame
 * carries a model label and a power LED on a "chin" strip below the glass.
 *
 * <p>Pure data and geometry, with no Minecraft dependency, so the rectangle math stays unit-testable;
 * {@code MonitorFrame} does the actual drawing. All geometry is square (no rounded corners). The colours are the
 * palettes {@code jsc:monitor/crt}, {@code jsc:monitor/lcd} and {@code jsc:monitor/flat}, which a resource pack can
 * recolour.
 *
 * @param bezelThickness px of shell around the glass on every side
 * @param innerThickness px of the inner frame ({@code 0} = none)
 * @param chinHeight     px of extra shell below the glass that holds the model label and the LED
 * @param model          the model label printed on the chin
 * @param palette        the frame's colours
 */
@PaletteHolder
public record MonitorFrameStyle(
        int bezelThickness,
        int innerThickness,
        int chinHeight,
        String model,
        Palette<Colours> palette) {

    /** Cream CRT shell with a dark inner frame and an amber power LED. */
    public static final MonitorFrameStyle CRT = new MonitorFrameStyle(12, 4, 18, "JSC-1400",
            Palettes.declare(JsComputers.MODID, "monitor/crt", new Colours(
                    0xFFBDB295, 0xFFD8CFB5, 0xFF8A8068, 0xFF15140F, 0xFF6E6650, 0xFFFFB347, 0xFF8A8068)));

    /** Slim grey LCD bezel with a green power LED. */
    public static final MonitorFrameStyle LCD = new MonitorFrameStyle(8, 0, 16, "JSC LCD-700",
            Palettes.declare(JsComputers.MODID, "monitor/lcd", new Colours(
                    0xFFC4C6CC, 0xFFE0E1E5, 0xFF9A9CA2, 0, 0xFF73757B, 0xFF5EE06E, 0xFF9A9CA2)));

    /** Thin near-black flat bezel with a blue power LED. */
    public static final MonitorFrameStyle FLAT = new MonitorFrameStyle(5, 0, 12, "JSC",
            Palettes.declare(JsComputers.MODID, "monitor/flat", new Colours(
                    0xFF161618, 0xFF2A2A2E, 0xFF050506, 0, 0xFF5A5A60, 0xFF7FB8FF, 0)));

    /**
     * The frame for a hardware era. Vintage is a cream CRT, Legacy a grey LCD, Standard a thin flat bezel. The
     * future eras (Advanced, Exa, Singularity) reuse the flat bezel until they get their own look, mirroring how
     * {@code EraThemes} resolves them to the Standard skin.
     */
    public static MonitorFrameStyle forEra(final HardwareEra era) {
        return switch (era) {
            case VINTAGE -> CRT;
            case LEGACY -> LCD;
            case STANDARD, ADVANCED, EXA, SINGULARITY -> FLAT;
        };
    }

    /** Total border between the glass and the outer shell edge (shell plus dark inner frame). */
    public int border() {
        return bezelThickness + innerThickness;
    }

    /** The outer plastic shell color. */
    public int bezelColor() {
        return palette.get().bezel();
    }

    /** A 1px sheen line along the top edge ({@code 0} = off). */
    public int bezelHighlight() {
        return palette.get().highlight();
    }

    /** A 1px shadow line along the bottom edge ({@code 0} = off). */
    public int bezelShadow() {
        return palette.get().shadow();
    }

    /** The dark inner frame between shell and glass ({@code 0} = none, for bezel-less LCD/flat). */
    public int innerColor() {
        return palette.get().inner();
    }

    /** The model label color. */
    public int labelColor() {
        return palette.get().label();
    }

    /** The power LED color (amber CRT, green LCD, blue flat). */
    public int ledColor() {
        return palette.get().led();
    }

    /** The color of the two vent slots on the chin ({@code 0} = no vents). */
    public int ventColor() {
        return palette.get().vent();
    }

    /**
     * The outer rectangle of the whole monitor for a glass (software screen) rectangle at {@code (sx, sy)} sized
     * {@code sw x sh}. The shell wraps the glass by {@link #border()} on every side and adds {@link #chinHeight}
     * below it for the label/LED strip.
     */
    public Geometry geometry(final int sx, final int sy, final int sw, final int sh) {
        final int b = border();
        final int x = sx - b;
        final int y = sy - b;
        final int w = sw + 2 * b;
        final int h = sh + 2 * b + chinHeight;
        final int chinY = sy + sh + b; // the chin strip sits directly below the bottom border
        return new Geometry(x, y, w, h, chinY);
    }

    /** Absolute pixel rectangle of the monitor body, plus the y where the chin (label/LED) strip starts. */
    public record Geometry(int x, int y, int w, int h, int chinY) {

        public int right() {
            return x + w;
        }

        public int bottom() {
            return y + h;
        }
    }

    /**
     * A frame's colours: the shell, its top sheen and bottom shadow, the inner frame, the model label, the LED and
     * the vents. A sheen, a shadow, an inner frame or vents that the frame does not have are {@code 0}.
     */
    public record Colours(int bezel, int highlight, int shadow, int inner, int label, int led, int vent) {
    }
}
