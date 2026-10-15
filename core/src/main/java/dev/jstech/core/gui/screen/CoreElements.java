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
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * The kinds of component the Core's screens are made of, as a screen's file names them: the boxes that lay others out
 * ({@code column}, {@code row}, {@code grid}, {@code stack}, and the {@code panel} and {@code well} that draw a ground
 * under what they hold), and the components themselves ({@code title}, {@code label}, {@code button}, {@code slot},
 * {@code slots}, {@code inventory}, {@code progress}, {@code image}, {@code spacer}, {@code separator},
 * {@code checkbox}, {@code text_field}).
 *
 * <p>A text is a translation key, so a screen speaks every language its mod does; a value the screen shows is bound by
 * name to one its menu declares ({@code "bind": "progress"}), and what a press does is named for the screen's code
 * ({@code "action": "start"}).
 */
public final class CoreElements {

    /** A box laying out what it holds top to bottom. */
    public static final ElementType<Unit> COLUMN = holder("column");
    /** A box laying out what it holds left to right. */
    public static final ElementType<Unit> ROW = holder("row");
    /** A box laying out what it holds in the cells of its columns. */
    public static final ElementType<Unit> GRID = holder("grid");
    /** A box laying what it holds over each other. */
    public static final ElementType<Unit> STACK = holder("stack");
    /** A raised panel of the style, laying out what it holds top to bottom. */
    public static final ElementType<Unit> PANEL = holder("panel");
    /** A sunken well of the style, laying out what it holds top to bottom. */
    public static final ElementType<Unit> WELL = holder("well");

    /** The screen's title, in the style's colour for titles. */
    public static final ElementType<Unit> TITLE = leaf("title", MapCodec.unit(Unit.INSTANCE));

    /** A text: a translation key, or a value of the menu written through one. */
    public static final ElementType<LabelProps> LABEL = leaf("label", RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("text").forGetter(LabelProps::text),
            Codec.STRING.optionalFieldOf("bind").forGetter(LabelProps::bind),
            Codec.STRING.optionalFieldOf("format").forGetter(LabelProps::format),
            ScreenCodecs.named(Tone.class).optionalFieldOf("tone", Tone.TEXT).forGetter(LabelProps::tone),
            ScreenCodecs.named(TextAlign.class).optionalFieldOf("text_align", TextAlign.LEFT)
                    .forGetter(LabelProps::textAlign),
            Codec.floatRange(0.25f, 4f).optionalFieldOf("scale", 1f).forGetter(LabelProps::scale),
            Codec.BOOL.optionalFieldOf("wrap", false).forGetter(LabelProps::wrap)
    ).apply(i, LabelProps::new)));

    /** A push button: what it says, and what pressing it does. */
    public static final ElementType<ButtonProps> BUTTON = leaf("button", RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("text").forGetter(ButtonProps::text),
            Codec.STRING.optionalFieldOf("action").forGetter(ButtonProps::action),
            Codec.intRange(0, 255).optionalFieldOf("menu_button").forGetter(ButtonProps::menuButton),
            Codec.BOOL.optionalFieldOf("primary", false).forGetter(ButtonProps::primary)
    ).apply(i, ButtonProps::new)));

    /** One slot of the screen's menu, by its place among the menu's slots. */
    public static final ElementType<SlotProps> SLOT = leaf("slot", Codec.intRange(0, 4096).fieldOf("slot")
            .xmap(SlotProps::new, SlotProps::slot));

    /** A run of the menu's slots laid out in a grid. */
    public static final ElementType<SlotsProps> SLOTS = leaf("slots", RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(0, 4096).fieldOf("from").forGetter(SlotsProps::from),
            Codec.intRange(1, 4096).fieldOf("count").forGetter(SlotsProps::count),
            Codec.intRange(1, 64).optionalFieldOf("per_row", 9).forGetter(SlotsProps::perRow)
    ).apply(i, SlotsProps::new)));

    /** The player's inventory and hotbar, laid out as the game lays them, its label above when asked. */
    public static final ElementType<InventoryProps> INVENTORY = leaf("inventory",
            Codec.BOOL.optionalFieldOf("label", true).xmap(InventoryProps::new, InventoryProps::label));

    /** A bar filled as far as a value of the menu is of its largest. */
    public static final ElementType<ProgressProps> PROGRESS = leaf("progress", RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("bind").forGetter(ProgressProps::bind),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("max", 100).forGetter(ProgressProps::max),
            Codec.STRING.optionalFieldOf("max_bind").forGetter(ProgressProps::maxBind)
    ).apply(i, ProgressProps::new)));

    /** A sprite of the game's or a resource pack's GUI atlas. */
    public static final ElementType<ImageProps> IMAGE = leaf("image", RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("sprite").forGetter(ImageProps::sprite),
            Codec.intRange(1, 1024).fieldOf("image_width").forGetter(ImageProps::imageWidth),
            Codec.intRange(1, 1024).fieldOf("image_height").forGetter(ImageProps::imageHeight)
    ).apply(i, ImageProps::new)));

    /** Room that grows to take what is left: a spring between two components. */
    public static final ElementType<Unit> SPACER = leaf("spacer", MapCodec.unit(Unit.INSTANCE));

    /** A line between two parts, in the style's separator. */
    public static final ElementType<Unit> SEPARATOR = leaf("separator", MapCodec.unit(Unit.INSTANCE));

    /** A check box with its text, ticked by a value of the menu or of the screen's code. */
    public static final ElementType<CheckboxProps> CHECKBOX = leaf("checkbox", RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("text").forGetter(CheckboxProps::text),
            Codec.STRING.optionalFieldOf("bind").forGetter(CheckboxProps::bind),
            Codec.STRING.optionalFieldOf("action").forGetter(CheckboxProps::action)
    ).apply(i, CheckboxProps::new)));

    /** A field to type in, its text handed to the screen's code. */
    public static final ElementType<TextFieldProps> TEXT_FIELD = leaf("text_field",
            RecordCodecBuilder.mapCodec(i -> i.group(
                    Codec.STRING.optionalFieldOf("action").forGetter(TextFieldProps::action),
                    Codec.intRange(1, 32_767).optionalFieldOf("max_length", 64).forGetter(TextFieldProps::maxLength),
                    Codec.STRING.optionalFieldOf("placeholder").forGetter(TextFieldProps::placeholder),
                    Codec.STRING.optionalFieldOf("bind").forGetter(TextFieldProps::bind)
            ).apply(i, TextFieldProps::new)));

    private CoreElements() {
    }

    /** Loads the Core's kinds, from the Core's constructor, so a screen's file can name them. */
    public static void declare() {
        // Reading a field of this class declares them all.
    }

    private static ElementType<Unit> holder(final String name) {
        return ElementTypes.declare(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, name),
                MapCodec.unit(Unit.INSTANCE), true);
    }

    private static <P> ElementType<P> leaf(final String name, final MapCodec<P> props) {
        return ElementTypes.declare(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, name), props, false);
    }

    /** Nothing of its own. */
    public enum Unit {
        /** The one value. */
        INSTANCE
    }

    /** Which of the style's colours a text is drawn in. */
    public enum Tone {
        /** The main text's. */
        TEXT,
        /** The quieter text's. */
        DIM,
        /** The accent's. */
        ACCENT,
        /** A title's. */
        TITLE
    }

    /** Where a text stands in its box. */
    public enum TextAlign {
        /** At the left. */
        LEFT,
        /** In the middle. */
        CENTER,
        /** At the right. */
        RIGHT
    }

    /**
     * A text.
     *
     * @param text      its translation key, or the key it writes a bound value through when it has a {@code bind}
     * @param bind      the name of the menu's value it shows
     * @param format    the translation key a bound value is written through ({@code "%s RF"}), when not {@code text}
     * @param tone      its colour
     * @param textAlign where it stands
     * @param scale     its size, the font's own being 1
     * @param wrap      whether it breaks into lines at its words when it is too wide
     */
    public record LabelProps(Optional<String> text, Optional<String> bind, Optional<String> format, Tone tone,
                             TextAlign textAlign, float scale, boolean wrap) {
    }

    /**
     * A push button.
     *
     * @param text       its translation key
     * @param action     the name of what pressing it does, for the screen's code
     * @param menuButton the menu's button it presses on the server, the game's way
     * @param primary    whether it is the screen's main action
     */
    public record ButtonProps(Optional<String> text, Optional<String> action, Optional<Integer> menuButton,
                              boolean primary) {
    }

    /**
     * One slot.
     *
     * @param slot its place among the menu's slots
     */
    public record SlotProps(int slot) {
    }

    /**
     * A run of slots.
     *
     * @param from   the place of the first among the menu's slots
     * @param count  how many
     * @param perRow how many in a row
     */
    public record SlotsProps(int from, int count, int perRow) {
    }

    /**
     * The player's inventory.
     *
     * @param label whether the inventory's name is written above it
     */
    public record InventoryProps(boolean label) {
    }

    /**
     * A bar.
     *
     * @param bind    the name of the menu's value it shows
     * @param max     the value that fills it
     * @param maxBind the name of a value of the menu that fills it, over {@code max}
     */
    public record ProgressProps(String bind, int max, Optional<String> maxBind) {
    }

    /**
     * A picture.
     *
     * @param sprite      the sprite
     * @param imageWidth  how wide it is drawn
     * @param imageHeight how tall it is drawn
     */
    public record ImageProps(ResourceLocation sprite, int imageWidth, int imageHeight) {
    }

    /**
     * A check box.
     *
     * @param text   its translation key
     * @param bind   the name of the value that ticks it
     * @param action the name of what a click does, for the screen's code
     */
    public record CheckboxProps(Optional<String> text, Optional<String> bind, Optional<String> action) {
    }

    /**
     * A field to type in.
     *
     * @param action      the name the screen's code hears the text by when it is committed
     * @param maxLength   how long the text may be
     * @param placeholder the translation key of what it shows while empty
     * @param bind        the name of a value it shows while it does not have the keyboard
     */
    public record TextFieldProps(Optional<String> action, int maxLength, Optional<String> placeholder,
                                 Optional<String> bind) {
    }
}
