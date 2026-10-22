/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.screen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.JsCore;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * The richer kinds of component a screen's file may hold, beside the Core's first ones ({@link CoreElements}):
 * {@code slider}, {@code switch}, {@code radio}, {@code dropdown} and {@code color} to set a value; {@code tree},
 * {@code table}, {@code chart}, {@code scale} and {@code graph} to show and edit what a machine knows; {@code model},
 * {@code map}, {@code tank}, {@code energy} and {@code ghost_slot} to show things of the world.
 *
 * <p>A value is bound by name ({@code "bind"}), and what a component hands over (a number, a choice, a colour, an
 * item) goes to the handler of the screen's code its {@code "action"} names. A tree, a table, a chart, a graph, a map
 * and a 3D view are bound to what the screen's code hands over under their name: their model, their series, their
 * place.
 */
public final class ControlElements {

    /** A slider: a number of the menu between two ends, free or in steps, written on it through a key. */
    public static final ElementType<SliderProps> SLIDER = leaf("slider", RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("bind").forGetter(SliderProps::bind),
            Codec.DOUBLE.optionalFieldOf("min", 0.0).forGetter(SliderProps::min),
            Codec.DOUBLE.optionalFieldOf("max", 1.0).forGetter(SliderProps::max),
            Codec.intRange(0, 1024).optionalFieldOf("steps", 0).forGetter(SliderProps::steps),
            Codec.STRING.optionalFieldOf("format").forGetter(SliderProps::format),
            Codec.STRING.optionalFieldOf("action").forGetter(SliderProps::action)
    ).apply(i, SliderProps::new)));

    /** A switch with its text, on or off by a value of the menu. */
    public static final ElementType<ToggleProps> SWITCH = leaf("switch", RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("text").forGetter(ToggleProps::text),
            Codec.STRING.optionalFieldOf("bind").forGetter(ToggleProps::bind),
            Codec.STRING.optionalFieldOf("action").forGetter(ToggleProps::action)
    ).apply(i, ToggleProps::new)));

    /** Option buttons, one under the other, the chosen one a value of the menu. */
    public static final ElementType<ChoiceProps> RADIO = leaf("radio", choice());

    /** A drop-down list, the chosen option a value of the menu, written on its button through a key. */
    public static final ElementType<ChoiceProps> DROPDOWN = leaf("dropdown", choice());

    /** A colour, picked on a picker with its ready colours. */
    public static final ElementType<ColourProps> COLOR = leaf("color", RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("bind").forGetter(ColourProps::bind),
            Codec.STRING.optionalFieldOf("action").forGetter(ColourProps::action),
            Codec.STRING.listOf().optionalFieldOf("swatches", List.of()).forGetter(ColourProps::swatches)
    ).apply(i, ColourProps::new)));

    /** A tree of what the screen's code hands over, its chosen node handed to the action. */
    public static final ElementType<ModelProps> TREE = leaf("tree", model());

    /** A table of what the screen's code hands over, a chosen row handed to the action. */
    public static final ElementType<ModelProps> TABLE = leaf("table", model());

    /** A node graph the screen's code hands over, a new node asked of the action at a point. */
    public static final ElementType<ModelProps> GRAPH = leaf("graph", model());

    /** A 3D view of the block, the blocks, the item or the entity the screen's code hands over. */
    public static final ElementType<ModelProps> MODEL = leaf("model", model());

    /** A chart of lines, bars or a pie, of the series the screen's code hands over. */
    public static final ElementType<ChartProps> CHART = leaf("chart", RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("bind").forGetter(ChartProps::bind),
            Codec.STRING.optionalFieldOf("kind", "line").forGetter(ChartProps::kind),
            Codec.STRING.optionalFieldOf("title").forGetter(ChartProps::title),
            Codec.STRING.optionalFieldOf("format").forGetter(ChartProps::format),
            Codec.STRING.optionalFieldOf("from").forGetter(ChartProps::from),
            Codec.STRING.optionalFieldOf("to").forGetter(ChartProps::to)
    ).apply(i, ChartProps::new)));

    /**
     * A scale: a value along a bar coloured in bands, its marks and their words under it, from the reading the screen's
     * code hands over; the value fills the bar up to it, or stands on it as a pointer.
     */
    public static final ElementType<ScaleProps> SCALE = leaf("scale", RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("bind").forGetter(ScaleProps::bind),
            ScreenCodecs.named(Pointer.class).optionalFieldOf("pointer", Pointer.FILL).forGetter(ScaleProps::pointer)
    ).apply(i, ScaleProps::new)));

    /** A map of the land round the place the screen's code hands over, with its markers. */
    public static final ElementType<MapProps> MAP = leaf("map", RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("bind").forGetter(MapProps::bind),
            Codec.STRING.optionalFieldOf("markers").forGetter(MapProps::markers)
    ).apply(i, MapProps::new)));

    /** A tank's gauge of a fluid of the menu, against its capacity. */
    public static final ElementType<AmountProps> TANK = leaf("tank", amount());

    /** An energy store's gauge of a value of the menu, against its capacity. */
    public static final ElementType<AmountProps> ENERGY = leaf("energy", amount());

    /** A ghost slot, showing an item of the menu without holding it, set by a click with an item. */
    public static final ElementType<ToggleProps> GHOST_SLOT = leaf("ghost_slot", RecordCodecBuilder.mapCodec(i ->
            i.group(
                    Codec.STRING.optionalFieldOf("text").forGetter(ToggleProps::text),
                    Codec.STRING.optionalFieldOf("bind").forGetter(ToggleProps::bind),
                    Codec.STRING.optionalFieldOf("action").forGetter(ToggleProps::action)
            ).apply(i, ToggleProps::new)));

    private ControlElements() {
    }

    /** Loads these kinds, from the Core's constructor, so a screen's file can name them. */
    public static void declare() {
        // Reading a field of this class declares them all.
    }

    private static <P> ElementType<P> leaf(final String name, final MapCodec<P> props) {
        return ElementTypes.declare(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, name), props, false);
    }

    private static MapCodec<ChoiceProps> choice() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.STRING.listOf().fieldOf("options").forGetter(ChoiceProps::options),
                Codec.STRING.optionalFieldOf("bind").forGetter(ChoiceProps::bind),
                Codec.STRING.optionalFieldOf("format").forGetter(ChoiceProps::format),
                Codec.STRING.optionalFieldOf("action").forGetter(ChoiceProps::action)
        ).apply(i, ChoiceProps::new));
    }

    private static MapCodec<ModelProps> model() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.STRING.fieldOf("bind").forGetter(ModelProps::bind),
                Codec.STRING.optionalFieldOf("action").forGetter(ModelProps::action)
        ).apply(i, ModelProps::new));
    }

    private static MapCodec<AmountProps> amount() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.STRING.fieldOf("bind").forGetter(AmountProps::bind),
                Codec.LONG.optionalFieldOf("capacity", 1000L).forGetter(AmountProps::capacity),
                Codec.STRING.optionalFieldOf("capacity_bind").forGetter(AmountProps::capacityBind),
                Codec.STRING.optionalFieldOf("format").forGetter(AmountProps::format)
        ).apply(i, AmountProps::new));
    }

    /**
     * A slider.
     *
     * @param bind   the value it shows, between {@code min} and {@code max}
     * @param min    its start's value
     * @param max    its end's value
     * @param steps  how many places it stops at; under 2 for any
     * @param format the key its value is written on it through
     * @param action the handler of a new value
     */
    public record SliderProps(String bind, double min, double max, int steps, Optional<String> format,
                              Optional<String> action) {
    }

    /**
     * A switch or a ghost slot.
     *
     * @param text   its text's key
     * @param bind   the value it shows
     * @param action the handler of a click
     */
    public record ToggleProps(Optional<String> text, Optional<String> bind, Optional<String> action) {
    }

    /**
     * Option buttons or a drop-down list.
     *
     * @param options their options' keys
     * @param bind    the place of the chosen one
     * @param format  the key the chosen one is written on a list's button through
     * @param action  the handler of a choice
     */
    public record ChoiceProps(List<String> options, Optional<String> bind, Optional<String> format,
                              Optional<String> action) {

        public ChoiceProps {
            options = List.copyOf(options);
        }
    }

    /**
     * A colour.
     *
     * @param bind     the colour shown
     * @param action   the handler of a new colour
     * @param swatches its ready colours, {@code #RRGGBB}
     */
    public record ColourProps(String bind, Optional<String> action, List<String> swatches) {

        public ColourProps {
            swatches = List.copyOf(swatches);
        }
    }

    /**
     * A tree, a table, a graph or a 3D view.
     *
     * @param bind   what the screen's code hands over to show
     * @param action the handler of a choice in it
     */
    public record ModelProps(String bind, Optional<String> action) {
    }

    /**
     * A chart.
     *
     * @param bind   its series
     * @param kind   {@code line}, {@code bar} or {@code pie}
     * @param title  its title's key
     * @param format the key a value is written through
     * @param from   the key of what the time at its left is called
     * @param to     the key of what the time at its right is called
     */
    public record ChartProps(String bind, String kind, Optional<String> title, Optional<String> format,
                             Optional<String> from, Optional<String> to) {
    }

    /**
     * A map.
     *
     * @param bind    the place it is centred on
     * @param markers the places marked on it
     */
    public record MapProps(String bind, Optional<String> markers) {
    }

    /** How a scale shows its value. */
    public enum Pointer {
        /** The bar's bands are drawn up to the value, the rest of it empty: how much has built up. */
        FILL,
        /** The whole bar's bands are drawn, a pointer standing at the value: where it is among them. */
        ARROW
    }

    /**
     * A scale.
     *
     * @param bind    the reading it shows
     * @param pointer how it shows the value
     */
    public record ScaleProps(String bind, Pointer pointer) {
    }

    /**
     * A tank's or an energy store's gauge.
     *
     * @param bind         what it holds
     * @param capacity     what it holds when full
     * @param capacityBind a value of the menu that is its capacity, over {@code capacity}
     * @param format       the key its tooltip is written through, given what it holds and its capacity
     */
    public record AmountProps(String bind, long capacity, Optional<String> capacityBind, Optional<String> format) {
    }
}
