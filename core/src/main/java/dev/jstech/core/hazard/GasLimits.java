/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import com.mojang.serialization.Codec;
import dev.jstech.core.JsCore;
import dev.jstech.core.format.UnitFormatter;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialProperties;
import dev.jstech.core.material.MaterialProperty;
import dev.jstech.core.material.MaterialValues;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.OptionalDouble;
import net.minecraft.resources.ResourceLocation;

/**
 * How much of a gas in the air does harm, as real safety data gives it, for the clouds of {@link GasClouds}: the share
 * of the air between which a flammable gas explodes (its lower and upper explosive limits, 4% and 75% for hydrogen),
 * and the concentration of a toxic gas immediately dangerous to life and health (its IDLH, 10 ppm for chlorine). A data
 * pack changes them as it does any of a material's values.
 *
 * <pre>{@code
 * Materials.material(rl("hydrogen_sulfide"), MaterialKind.GAS, NAME)
 *         .with(GasLimits.LOWER_EXPLOSIVE, 0.04).with(GasLimits.UPPER_EXPLOSIVE, 0.46)
 *         .with(GasLimits.IDLH, 50.0).declare();
 * }</pre>
 */
@TextHolder
public final class GasLimits {

    public static final TextKey LOWER_NAME = TextKey.of("jscore.material_property.lower_explosive",
            "Explodes from");
    public static final TextKey UPPER_NAME = TextKey.of("jscore.material_property.upper_explosive", "Explodes up to");
    public static final TextKey IDLH_NAME = TextKey.of("jscore.material_property.idlh", "Dangerous from");
    public static final TextKey SHARE = TextKey.of("jscore.material_property.share_of_air", "%s%% of the air");
    public static final TextKey PARTS_PER_MILLION = TextKey.of("jscore.material_property.ppm", "%s ppm");
    /** A flammable gas's lower explosive limit, as a share of the air: below it, too lean to burn. */
    public static final MaterialProperty<Double> LOWER_EXPLOSIVE = MaterialProperties.property(id("lower_explosive"),
            Codec.doubleRange(0, 1), LOWER_NAME).shown(MaterialProperty.Shown.SHIFT).format(GasLimits::share)
            .declare();
    /** A flammable gas's upper explosive limit, as a share of the air: above it, too rich to burn. */
    public static final MaterialProperty<Double> UPPER_EXPLOSIVE = MaterialProperties.property(id("upper_explosive"),
            Codec.doubleRange(0, 1), UPPER_NAME).shown(MaterialProperty.Shown.SHIFT).format(GasLimits::share)
            .declare();
    /** A toxic gas's concentration immediately dangerous to life and health, in parts per million of the air. */
    public static final MaterialProperty<Double> IDLH = MaterialProperties.property(id("idlh"),
            Codec.doubleRange(0, 1_000_000), IDLH_NAME).shown(MaterialProperty.Shown.SHIFT)
            .format(ppm -> PARTS_PER_MILLION.with(number(ppm))).declare();
    /**
     * What a toxic gas with no data of its own is taken to be dangerous from, in parts per million: the order of the
     * common industrial poisons, hydrogen sulfide's and sulfur dioxide's 100.
     */
    public static final double UNKNOWN_IDLH = 100;

    private GasLimits() {
    }

    /** Declares the properties, from the Core's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** {@code gas}'s lower explosive limit, a share of the air; none for one with no data. */
    public static OptionalDouble lowerExplosive(final Material gas) {
        return value(gas, LOWER_EXPLOSIVE);
    }

    /** {@code gas}'s upper explosive limit, a share of the air; none for one with no data. */
    public static OptionalDouble upperExplosive(final Material gas) {
        return value(gas, UPPER_EXPLOSIVE);
    }

    /** The parts per million of {@code gas} immediately dangerous: its own, or {@link #UNKNOWN_IDLH}. */
    public static double idlh(final Material gas) {
        return value(gas, IDLH).orElse(UNKNOWN_IDLH);
    }

    private static OptionalDouble value(final Material gas, final MaterialProperty<Double> property) {
        return MaterialValues.value(gas, property, false).map(OptionalDouble::of).orElse(OptionalDouble.empty());
    }

    private static Text share(final double share) {
        return SHARE.with(number(share * 100));
    }

    private static Text number(final double value) {
        return Text.literal(UnitFormatter.forCurrentLocale().decimal(value));
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path);
    }
}
