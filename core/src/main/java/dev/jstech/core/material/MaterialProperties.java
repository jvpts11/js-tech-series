/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.jstech.core.JsCore;
import dev.jstech.core.config.ConfigColours;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.DeclarationError;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;

/**
 * Every property a material can carry, open to any mod: the Core's few (symbol, atomic number, atomic weight,
 * formula, colour, state at room temperature), and those a mod registers, such as a molar mass or oxidation states.
 *
 * <pre>{@code
 * public static final MaterialProperty<Double> MOLAR_MASS = MaterialProperties.property(rl("molar_mass"),
 *         Codec.DOUBLE, MOLAR_MASS_NAME).shown(MaterialProperty.Shown.SHIFT).declare();
 * }</pre>
 */
@TextHolder
public final class MaterialProperties {

    private static final Map<ResourceLocation, MaterialProperty<?>> BY_ID = new ConcurrentHashMap<>();
    private static final List<MaterialProperty<?>> ALL = new CopyOnWriteArrayList<>();

    public static final TextKey SYMBOL_NAME = TextKey.of("jscore.material_property.symbol", "Symbol");
    public static final TextKey ATOMIC_NUMBER_NAME = TextKey.of("jscore.material_property.atomic_number",
            "Atomic number");
    public static final TextKey ATOMIC_WEIGHT_NAME = TextKey.of("jscore.material_property.atomic_weight",
            "Atomic weight");
    public static final TextKey FORMULA_NAME = TextKey.of("jscore.material_property.formula", "Formula");
    public static final TextKey COLOUR_NAME = TextKey.of("jscore.material_property.colour", "Colour");
    public static final TextKey ROOM_STATE_NAME = TextKey.of("jscore.material_property.room_state",
            "At room temperature");
    public static final TextKey SOLID = TextKey.of("jscore.matter_state.solid", "solid");
    public static final TextKey LIQUID = TextKey.of("jscore.matter_state.liquid", "liquid");
    public static final TextKey GAS = TextKey.of("jscore.matter_state.gas", "gas");
    public static final TextKey SUPERCRITICAL = TextKey.of("jscore.matter_state.supercritical",
            "supercritical fluid");
    public static final TextKey SLURRY = TextKey.of("jscore.matter_state.slurry", "slurry");
    public static final TextKey PLASMA = TextKey.of("jscore.matter_state.plasma", "plasma");
    public static final TextKey EXOTIC = TextKey.of("jscore.matter_state.exotic", "exotic matter");

    /** How a state of matter is written in a data pack. */
    public static final Codec<MatterState> STATE_CODEC = Codec.STRING.comapFlatMap(
            name -> MatterState.byName(name).map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "no state of matter is called " + name)),
            MatterState::serializedName);

    /** The element's or the compound's chemical symbol: Fe. */
    public static final MaterialProperty<String> SYMBOL = property(id("symbol"), Codec.STRING, SYMBOL_NAME)
            .declare();
    /** An element's atomic number. */
    public static final MaterialProperty<Integer> ATOMIC_NUMBER = property(id("atomic_number"), Codec.INT,
            ATOMIC_NUMBER_NAME).shown(MaterialProperty.Shown.SHIFT).declare();
    /**
     * An element's standard atomic weight, the mean mass of its atoms as the Earth holds them, in daltons: 55.845 for
     * iron; for an element with no stable isotope, the mass number of its longest-lived one.
     */
    public static final MaterialProperty<Double> ATOMIC_WEIGHT = property(id("atomic_weight"), Codec.DOUBLE,
            ATOMIC_WEIGHT_NAME).shown(MaterialProperty.Shown.SHIFT).declare();
    /** A compound's or a mineral's formula, written plainly as CuFeS2 and read with its counts small, CuFeS₂. */
    public static final MaterialProperty<String> FORMULA = property(id("formula"), Codec.STRING, FORMULA_NAME)
            .format(MaterialProperties::formula).declare();
    /** The material's colour, as {@code #RRGGBB}, which its items and fluids are drawn in. */
    public static final MaterialProperty<String> COLOUR = property(id("colour"), ConfigColours.CODEC, COLOUR_NAME)
            .shown(MaterialProperty.Shown.NEVER).declare();
    /** The state it is in at room temperature. */
    public static final MaterialProperty<MatterState> ROOM_STATE = property(id("room_state"), STATE_CODEC,
            ROOM_STATE_NAME).shown(MaterialProperty.Shown.SHIFT).format(MaterialProperties::stateName).declare();

    private MaterialProperties() {
    }

    /** A property called {@code id}, its value written with {@code codec}, its line called {@code name}. */
    public static <T> Builder<T> property(final ResourceLocation id, final Codec<T> codec, final TextKey name) {
        return new Builder<>(id, codec, name);
    }

    /** Every property declared, in the order they were: the order of a tooltip's lines. */
    public static List<MaterialProperty<?>> all() {
        return List.copyOf(ALL);
    }

    /** The property declared as {@code id}, or none. */
    public static Optional<MaterialProperty<?>> byId(final ResourceLocation id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    /** Declares the Core's properties, from its constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** What a state of matter is called. */
    public static Text stateName(final MatterState state) {
        return switch (state) {
            case SOLID -> SOLID.text();
            case LIQUID -> LIQUID.text();
            case GAS -> GAS.text();
            case SUPERCRITICAL -> SUPERCRITICAL.text();
            case SLURRY -> SLURRY.text();
            case PLASMA -> PLASMA.text();
            case EXOTIC -> EXOTIC.text();
        };
    }

    /* A formula as print writes it, its counts small; one that cannot be read, as it is written. */
    private static Text formula(final String written) {
        return Text.literal(ChemicalFormula.read(written).map(ChemicalFormula::subscripted).orElse(written));
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path);
    }

    /**
     * How a property is declared: when its line shows, ALWAYS unless it says, and how its value reads, as itself unless
     * it says.
     *
     * @param <T> what its value is
     */
    public static final class Builder<T> {

        private final ResourceLocation id;
        private final Codec<T> codec;
        private final TextKey name;
        private MaterialProperty.Shown shown = MaterialProperty.Shown.ALWAYS;
        private Function<T, Text> format = value -> Text.literal(String.valueOf(value));

        private Builder(final ResourceLocation id, final Codec<T> codec, final TextKey name) {
            this.id = Objects.requireNonNull(id, "id");
            this.codec = Objects.requireNonNull(codec, "codec");
            this.name = Objects.requireNonNull(name, "name");
        }

        /** When its line shows in a tooltip. */
        public Builder<T> shown(final MaterialProperty.Shown when) {
            this.shown = Objects.requireNonNull(when, "when");
            return this;
        }

        /** How its value reads on its line. */
        public Builder<T> format(final Function<T, Text> reads) {
            this.format = Objects.requireNonNull(reads, "reads");
            return this;
        }

        /**
         * Declares the property.
         *
         * @throws DeclarationError when a property of that id is declared already
         */
        public MaterialProperty<T> declare() {
            final MaterialProperty<T> property = new MaterialProperty<>(this.id, this.codec, this.shown, this.name,
                    this.format);
            if (BY_ID.putIfAbsent(this.id, property) != null) {
                throw DeclarationError.of(this.id.getNamespace(), "the material property " + this.id,
                        "is declared twice", "declare it once, as a static final");
            }
            ALL.add(property);
            return property;
        }
    }
}
