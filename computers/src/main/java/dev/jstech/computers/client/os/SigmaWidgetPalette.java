/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;

/**
 * The few colours a program's widgets have whatever system draws them, {@code jsc:sigma/widgets}: a log's warnings and
 * errors stand out the same way on every desktop, and a component nothing on this game can draw is marked the same way
 * everywhere. Everything else a widget wears is its system's.
 */
@PaletteHolder
final class SigmaWidgetPalette {

    static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "sigma/widgets",
            new Colours(0xFF2A5AA0, 0xFFB86E00, 0xFFC0302A, 0xFF8A8A8A, 0xFF3A3F4A, 0xFF2E333D, 0xFFD8DCE4));

    private SigmaWidgetPalette() {
    }

    /** The colours as the loaded palette has them now. */
    static Colours get() {
        return PALETTE.get();
    }

    /**
     * The widgets' own colours.
     *
     * @param logTime          the time at the start of a log line
     * @param logWarn          a log line saying WARN
     * @param logError         a log line saying ERROR
     * @param chartGrid        the faint lines behind a chart
     * @param placeholder      the ground of a component nothing on this game can draw
     * @param placeholderHatch the stripes across it
     * @param placeholderText  what it says
     */
    record Colours(int logTime, int logWarn, int logError, int chartGrid, int placeholder, int placeholderHatch,
                   int placeholderText) {
    }
}
