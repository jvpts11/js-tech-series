/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.client.guide;

import dev.jstech.core.api.client.IGuideBlockRenderer;
import dev.jstech.core.text.TextKey;
import dev.jstech.industrial.guide.PlantDrawingTexts;
import java.util.Map;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * A piping and instrumentation diagram, as chemical plants are drawn: vessels, pumps, valves and the instruments that
 * read them, in the symbols of ISA 5.1, on pipes whose flow is shown by arrows and signal lines dashed; or, for a
 * legend, the symbols a row each with their names.
 */
public final class PidRenderer implements IGuideBlockRenderer {

    /** The radius of a pump's casing. */
    private static final int PUMP = 6;
    /** Half the length of a valve's body. */
    private static final int VALVE = 5;
    /** The radius of an instrument's bubble. */
    private static final int BUBBLE = 8;
    private static final Map<String, TextKey> NAMES = Map.of("vessel", PlantDrawingTexts.VESSEL,
            "pump", PlantDrawingTexts.PUMP, "valve", PlantDrawingTexts.VALVE,
            "check_valve", PlantDrawingTexts.CHECK_VALVE, "instrument", PlantDrawingTexts.INSTRUMENT,
            "pipe", PlantDrawingTexts.PIPE, "signal", PlantDrawingTexts.SIGNAL);

    @Override
    public void draw(final GuiGraphics graphics, final Font font, final int x, final int y, final int width,
                     final int height, final CompoundTag data, final int mouseX, final int mouseY) {
        final DrawingPen pen = new DrawingPen(graphics, font, x, y);
        if (data.contains("legend")) {
            legend(pen, data.getList("legend", Tag.TAG_STRING), width);
            return;
        }
        for (final Tag each : data.getList("lines", Tag.TAG_COMPOUND)) {
            final CompoundTag line = (CompoundTag) each;
            final int x0 = line.getInt("x0");
            final int y0 = line.getInt("y0");
            final int x1 = line.getInt("x1");
            final int y1 = line.getInt("y1");
            pen.line(x0, y0, x1, y1, line.getBoolean("dashed"), pen.ink());
            if (line.getBoolean("arrow")) {
                pen.arrowHead(x0, y0, x1, y1, pen.ink());
            }
        }
        for (final Tag each : data.getList("symbols", Tag.TAG_COMPOUND)) {
            final CompoundTag symbol = (CompoundTag) each;
            final int sx = symbol.getInt("x");
            final int sy = symbol.getInt("y");
            final String kind = symbol.getString("kind");
            final String tag = DrawingPen.words(symbol, "tag");
            if ("vessel".equals(kind)) {
                final int w = symbol.getInt("w");
                final int h = symbol.getInt("h");
                vessel(pen, sx, sy, sx + w, sy + h);
                pen.smallCentred(tag, sx + w / 2, sy + h + 3, pen.faint());
            } else if ("instrument".equals(kind)) {
                instrument(pen, sx, sy, tag, DrawingPen.words(symbol, "note"));
            } else {
                symbol(pen, kind, sx, sy);
                pen.smallCentred(tag, sx, sy + BUBBLE, pen.faint());
            }
        }
    }

    /* A vessel's outline, its corners cut as a drawn tank's are rounded. */
    private static void vessel(final DrawingPen pen, final int x0, final int y0, final int x1, final int y1) {
        pen.fill(x0 + 1, y0 + 1, x1, y1, pen.paper());
        pen.line(x0, y0 + 2, x0, y1 - 2, false, pen.ink());
        pen.line(x1, y0 + 2, x1, y1 - 2, false, pen.ink());
        pen.line(x0 + 2, y0, x1 - 2, y0, false, pen.ink());
        pen.line(x0 + 2, y1, x1 - 2, y1, false, pen.ink());
        pen.dot(x0 + 1, y0 + 1, pen.ink());
        pen.dot(x1 - 1, y0 + 1, pen.ink());
        pen.dot(x0 + 1, y1 - 1, pen.ink());
        pen.dot(x1 - 1, y1 - 1, pen.ink());
    }

    /* One of the symbols that sit on a pipe, its middle at x, y. */
    private static void symbol(final DrawingPen pen, final String kind, final int x, final int y) {
        switch (kind) {
            case "pump" -> {
                pen.circle(x, y, PUMP, pen.ink(), true);
                pen.line(x - 4, y - 4, x + PUMP, y, false, pen.ink());
                pen.line(x - 4, y + 4, x + PUMP, y, false, pen.ink());
            }
            case "valve" -> {
                for (int i = 0; i < VALVE; i++) {
                    pen.line(x - VALVE + i, y - i, x - VALVE + i, y + i, false, pen.ink());
                    pen.line(x + VALVE - i, y - i, x + VALVE - i, y + i, false, pen.ink());
                }
            }
            case "check_valve" -> {
                pen.line(x - VALVE, y - 4, x - VALVE, y + 4, false, pen.ink());
                pen.line(x + VALVE, y - 4, x + VALVE, y + 4, false, pen.ink());
                pen.line(x - VALVE, y + 4, x + VALVE, y - 4, false, pen.ink());
                pen.fill(x - VALVE, y - 4, x - 1, y + 5, pen.ink());
            }
            default -> {
                // A kind this drawing does not know is left out, as a block of a missing mod is.
            }
        }
    }

    /* An instrument's bubble: its letters over its number, as ISA tags it. */
    private static void instrument(final DrawingPen pen, final int x, final int y, final String letters,
                                   final String number) {
        pen.circle(x, y, BUBBLE, pen.ink(), true);
        pen.smallCentred(letters, x + 1, y - 6, pen.ink());
        pen.smallCentred(number, x + 1, y + 1, pen.ink());
    }

    private static void legend(final DrawingPen pen, final ListTag kinds, final int width) {
        int rowY = 0;
        for (int i = 0; i < kinds.size(); i++) {
            final String kind = kinds.getString(i);
            switch (kind) {
                case "vessel" -> vessel(pen, 4, rowY - 2, 17, rowY + 12);
                case "instrument" -> instrument(pen, 10, rowY + 5, "PI", "1");
                case "pipe" -> pen.line(2, rowY + 4, 18, rowY + 4, false, pen.ink());
                case "signal" -> pen.line(2, rowY + 4, 18, rowY + 4, true, pen.ink());
                default -> symbol(pen, kind, 10, rowY + 4);
            }
            rowY += SingleLineRenderer.legendRow(pen, NAMES.get(kind), rowY, width);
        }
    }
}
