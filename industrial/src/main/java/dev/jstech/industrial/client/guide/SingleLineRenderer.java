/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.client.guide;

import dev.jstech.core.api.client.IGuideBlockRenderer;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import dev.jstech.industrial.guide.PlantDrawingTexts;
import java.util.Map;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * A single-line diagram, the real drawing of an electric grid: one line for every wire of a circuit, generators,
 * breakers, transformers and the machines they feed in the symbols of IEC 60617, each tagged; or, for a legend, the
 * symbols a row each with their names.
 */
public final class SingleLineRenderer implements IGuideBlockRenderer {

    /** How tall a row of the legend is. */
    static final int LEGEND_ROW = 19;
    /** Where a legend's names start, past their symbols. */
    static final int LEGEND_NAME = 26;
    /** The radius of a generator's or a machine's circle. */
    private static final int ROTOR = 7;
    /** Half the side of a breaker's square. */
    private static final int BREAKER = 3;
    /** The radius of each of a transformer's two windings, and how far apart their centres stand. */
    private static final int WINDING = 5;
    private static final int WINDINGS_APART = 4;
    /** How thick a busbar is drawn. */
    private static final int BUSBAR = 3;
    private static final Map<String, TextKey> NAMES = Map.of("generator", PlantDrawingTexts.GENERATOR,
            "breaker", PlantDrawingTexts.BREAKER, "transformer", PlantDrawingTexts.TRANSFORMER,
            "bus", PlantDrawingTexts.BUS, "line", PlantDrawingTexts.LINE, "motor", PlantDrawingTexts.MOTOR);

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
            pen.line(line.getInt("x0"), line.getInt("y0"), line.getInt("x1"), line.getInt("y1"),
                    line.getBoolean("dashed"), pen.ink());
        }
        for (final Tag each : data.getList("symbols", Tag.TAG_COMPOUND)) {
            final CompoundTag symbol = (CompoundTag) each;
            final int sx = symbol.getInt("x");
            final int sy = symbol.getInt("y");
            final String kind = symbol.getString("kind");
            if ("bus".equals(kind)) {
                pen.fill(sx, sy, symbol.getInt("to"), sy + BUSBAR, pen.ink());
                pen.small(DrawingPen.words(symbol, "tag"), sx, sy + BUSBAR + 2, pen.faint());
                continue;
            }
            symbol(pen, kind, sx, sy);
            final int reach = "breaker".equals(kind) ? BREAKER : ROTOR;
            final String tag = DrawingPen.words(symbol, "tag");
            final String note = DrawingPen.words(symbol, "note");
            if (symbol.getBoolean("under")) {
                pen.smallCentred(tag, sx, sy + reach + 3, pen.ink());
                pen.smallCentred(note, sx, sy + reach + 9, pen.faint());
            } else {
                pen.small(tag, sx + reach + 4, sy - 4, pen.faint());
                pen.small(note, sx + reach + 4, sy + 2, pen.faint());
            }
        }
    }

    /* One symbol of the diagram, its middle at x, y. */
    static void symbol(final DrawingPen pen, final String kind, final int x, final int y) {
        switch (kind) {
            case "generator" -> rotor(pen, x, y, "G");
            case "motor" -> rotor(pen, x, y, "M");
            case "breaker" -> {
                pen.fill(x - BREAKER, y - BREAKER, x + BREAKER + 1, y + BREAKER + 1, pen.paper());
                pen.frame(x - BREAKER, y - BREAKER, x + BREAKER, y + BREAKER, pen.ink());
            }
            case "transformer" -> {
                pen.circle(x, y - WINDINGS_APART, WINDING, pen.ink(), true);
                pen.circle(x, y + WINDINGS_APART, WINDING, pen.ink(), false);
            }
            default -> {
                // A kind this drawing does not know is left out, as a block of a missing mod is.
            }
        }
    }

    /* A circle with its letter: a generator's G, a machine's M. */
    private static void rotor(final DrawingPen pen, final int x, final int y, final String letter) {
        pen.circle(x, y, ROTOR, pen.ink(), true);
        pen.text(letter, x - 2, y - 3, pen.ink());
    }

    private static void legend(final DrawingPen pen, final ListTag kinds, final int width) {
        int rowY = 0;
        for (int i = 0; i < kinds.size(); i++) {
            final String kind = kinds.getString(i);
            switch (kind) {
                case "bus" -> pen.fill(2, rowY + 3, 18, rowY + 3 + BUSBAR, pen.ink());
                case "line" -> pen.line(2, rowY + 4, 18, rowY + 4, false, pen.ink());
                default -> symbol(pen, kind, 10, rowY + 4);
            }
            rowY += legendRow(pen, NAMES.get(kind), rowY, width);
        }
    }

    /* A legend's name beside its symbol; how tall its row is, a line more for each line the name runs on. */
    static int legendRow(final DrawingPen pen, final TextKey name, final int rowY, final int width) {
        final int lines = name == null ? 1
                : pen.wrapped(GameText.resolve(name), LEGEND_NAME, rowY, width - LEGEND_NAME, pen.ink());
        return LEGEND_ROW + (lines - 1) * DrawingPen.LINE;
    }
}
