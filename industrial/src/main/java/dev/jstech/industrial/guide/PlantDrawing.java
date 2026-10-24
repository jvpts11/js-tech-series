/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.guide;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.jstech.core.text.Text;

/**
 * What a drawing of the Plant Drawings shows, written once where the page is declared and handed to the drawing's
 * renderer as JSON: the symbols of a single-line diagram or of a P&amp;ID and the lines between them, or the layers of
 * ground a geological section cuts through. Every place is in the drawing's own pixels, from its top left corner, in
 * the width of a column.
 *
 * <pre>{@code
 * guide.page("power", section)...
 *         .drawing(PlantDrawing.SINGLE_LINE, 100, PlantDrawing.drawn()
 *                 .symbol("generator", 75, 14, Text.literal("COAL GEN 1"), Text.literal("5 kW"))
 *                 .line(75, 21, 75, 40)
 *                 .bus(5, 40, 145, Text.literal("BUS A"))
 *                 .json(), "A small plant")
 * }</pre>
 *
 * <p>A tag is data, the same in every language, and is written {@code Text.literal}; a note in words is a
 * {@code TextKey}'s text, which the drawing reads in the player's language.
 */
public final class PlantDrawing {

    private final JsonObject root = new JsonObject();
    private final JsonArray symbols = new JsonArray();
    private final JsonArray lines = new JsonArray();
    private final JsonArray columns = new JsonArray();
    private final JsonArray veins = new JsonArray();
    private final JsonArray faults = new JsonArray();
    private final JsonArray boreholes = new JsonArray();
    private JsonArray layers;

    /** A single-line diagram: one line for every wire of a circuit, the symbols of IEC 60617. */
    public static final String SINGLE_LINE = "jsindustrial:single_line";
    /** A piping and instrumentation diagram: vessels, pumps, valves and gauges on the pipes, as ISA 5.1 draws them. */
    public static final String PID = "jsindustrial:pid";
    /** A geological section: the ground as if cut through, each rock in its pattern or in its own colours. */
    public static final String GEOLOGY = "jsindustrial:geology";
    /** What the key of a field's words is written under, beside the field: {@code noteKey}. */
    public static final String KEY = "Key";

    private PlantDrawing() {
        this.root.add("symbols", this.symbols);
        this.root.add("lines", this.lines);
        this.root.add("columns", this.columns);
        this.root.add("veins", this.veins);
        this.root.add("faults", this.faults);
        this.root.add("boreholes", this.boreholes);
    }

    /** An empty drawing, its parts to follow. */
    public static PlantDrawing drawn() {
        return new PlantDrawing();
    }

    /**
     * A symbol of its diagram's kind with its centre at {@code x}, {@code y}, its tag beside it and a note under the
     * tag; either may be empty.
     */
    public PlantDrawing symbol(final String kind, final int x, final int y, final Text tag, final Text note) {
        final JsonObject symbol = place(kind, x, y);
        words(symbol, "tag", tag);
        words(symbol, "note", note);
        this.symbols.add(symbol);
        return this;
    }

    /** A symbol whose tag stands centred under it rather than beside it. */
    public PlantDrawing symbolOver(final String kind, final int x, final int y, final Text tag, final Text note) {
        this.symbol(kind, x, y, tag, note);
        this.symbols.get(this.symbols.size() - 1).getAsJsonObject().addProperty("under", true);
        return this;
    }

    /** A vessel of a P&amp;ID, a tank or a column, from {@code x}, {@code y}, {@code width} by {@code height}. */
    public PlantDrawing vessel(final int x, final int y, final int width, final int height, final Text tag) {
        final JsonObject vessel = place("vessel", x, y);
        vessel.addProperty("w", width);
        vessel.addProperty("h", height);
        words(vessel, "tag", tag);
        this.symbols.add(vessel);
        return this;
    }

    /** A busbar of a single-line diagram, across from {@code x0} to {@code x1} at {@code y}. */
    public PlantDrawing bus(final int x0, final int y, final int x1, final Text tag) {
        final JsonObject bus = place("bus", x0, y);
        bus.addProperty("to", x1);
        words(bus, "tag", tag);
        this.symbols.add(bus);
        return this;
    }

    /** A wire or a pipe, straight from one place to another. */
    public PlantDrawing line(final int x0, final int y0, final int x1, final int y1) {
        return this.addLine(x0, y0, x1, y1, false, false);
    }

    /** A pipe whose flow goes the way it is drawn, an arrow at its end. */
    public PlantDrawing flow(final int x0, final int y0, final int x1, final int y1) {
        return this.addLine(x0, y0, x1, y1, false, true);
    }

    /** A signal line, dashed, from an instrument to what it reads. */
    public PlantDrawing signal(final int x0, final int y0, final int x1, final int y1) {
        return this.addLine(x0, y0, x1, y1, true, false);
    }

    /** The symbols of the diagram's kind, a row each with its name, in the order given: its legend. */
    public PlantDrawing legend(final String... kinds) {
        final JsonArray legend = new JsonArray();
        for (final String kind : kinds) {
            legend.add(kind);
        }
        this.root.add("legend", legend);
        return this;
    }

    /**
     * A column of a geological section, from {@code x0} to {@code x1}, its ground starting at {@code top}: the layers
     * that follow, from the top down, are its own.
     */
    public PlantDrawing column(final int x0, final int x1, final int top) {
        final JsonObject column = new JsonObject();
        column.addProperty("x0", x0);
        column.addProperty("x1", x1);
        column.addProperty("top", top);
        this.layers = new JsonArray();
        column.add("layers", this.layers);
        this.columns.add(column);
        return this;
    }

    /**
     * A layer of the last column, {@code depth} pixels thick: the block it is, drawn in the hatching of its kind of
     * rock ({@code soil}, {@code sandstone}, {@code stone}, {@code granite}, {@code deepslate}, {@code ore}), or in the
     * block's own colours.
     */
    public PlantDrawing layer(final String block, final String pattern, final int depth) {
        if (this.layers == null) {
            throw new IllegalStateException("a layer belongs to a column; begin one with column(...)");
        }
        final JsonObject layer = new JsonObject();
        layer.addProperty("block", block);
        layer.addProperty("pattern", pattern);
        layer.addProperty("depth", depth);
        this.layers.add(layer);
        return this;
    }

    /** A vein of ore, the block it is, from {@code x0}, {@code y0} to {@code x1}, {@code y1}. */
    public PlantDrawing vein(final String block, final int x0, final int y0, final int x1, final int y1) {
        final JsonObject vein = new JsonObject();
        vein.addProperty("block", block);
        vein.addProperty("x0", x0);
        vein.addProperty("y0", y0);
        vein.addProperty("x1", x1);
        vein.addProperty("y1", y1);
        this.veins.add(vein);
        return this;
    }

    /** A fault, where the ground broke and slid, dashed from one place to another. */
    public PlantDrawing fault(final int x0, final int y0, final int x1, final int y1) {
        final JsonObject fault = new JsonObject();
        fault.addProperty("x0", x0);
        fault.addProperty("y0", y0);
        fault.addProperty("x1", x1);
        fault.addProperty("y1", y1);
        this.faults.add(fault);
        return this;
    }

    /** A borehole at {@code x}, from the surface at {@code top} down {@code depth} pixels, tagged over its head. */
    public PlantDrawing borehole(final int x, final int top, final int depth, final Text tag) {
        final JsonObject hole = place("borehole", x, top);
        hole.addProperty("depth", depth);
        words(hole, "tag", tag);
        this.boreholes.add(hole);
        return this;
    }

    /** The rocks of a geological section, a row each in its pattern with the block's name: its legend. */
    public PlantDrawing rocks(final String... blockThenPattern) {
        if (blockThenPattern.length % 2 != 0) {
            throw new IllegalArgumentException("a rock of the legend is its block and its pattern");
        }
        final JsonArray rocks = new JsonArray();
        for (int i = 0; i < blockThenPattern.length; i += 2) {
            final JsonObject rock = new JsonObject();
            rock.addProperty("block", blockThenPattern[i]);
            rock.addProperty("pattern", blockThenPattern[i + 1]);
            rocks.add(rock);
        }
        this.root.add("rocks", rocks);
        return this;
    }

    /** Draws the section's rocks in the colours their blocks have rather than in hatching. */
    public PlantDrawing inColours() {
        this.root.addProperty("colours", true);
        return this;
    }

    /** What the renderer is handed. */
    public String json() {
        return this.root.toString();
    }

    private PlantDrawing addLine(final int x0, final int y0, final int x1, final int y1, final boolean dashed,
                                 final boolean arrow) {
        final JsonObject line = new JsonObject();
        line.addProperty("x0", x0);
        line.addProperty("y0", y0);
        line.addProperty("x1", x1);
        line.addProperty("y1", y1);
        line.addProperty("dashed", dashed);
        line.addProperty("arrow", arrow);
        this.lines.add(line);
        return this;
    }

    /*
     * Words on a drawing: a tag that is data, the same in every language (COAL GEN 1, TK-101), as it is; a note in
     * words as its key, which the renderer reads in the player's language, with its English for a missing one.
     */
    private static void words(final JsonObject into, final String field, final Text text) {
        switch (text) {
            case Text.Literal literal -> into.addProperty(field, literal.value());
            case Text.Translated translated -> {
                into.addProperty(field, translated.key().english());
                into.addProperty(field + KEY, translated.key().key());
            }
        }
    }

    private static JsonObject place(final String kind, final int x, final int y) {
        final JsonObject placed = new JsonObject();
        placed.addProperty("kind", kind);
        placed.addProperty("x", x);
        placed.addProperty("y", y);
        return placed;
    }
}
