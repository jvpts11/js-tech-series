/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.rocket;

import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.SharedCatalogue;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialLiquids;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.space.JsSpace;
import dev.jstech.space.api.Propellant;
import dev.jstech.space.api.Propellants;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;

/**
 * The propellant of J's Space's skeleton, methalox: liquid methane and liquid oxygen, burnt three and a half parts of
 * oxygen to one of methane by mass, as Raptor burns them. Methane is what can be made on Mars from the carbon dioxide
 * of its air, the real reason Starship burns it. Both are J's Core's, liquid at their boiling points, so both boil off
 * a tank left standing, the oxygen faster than the methane.
 */
@TextHolder
public final class SkeletonPropellants {

    public static final TextKey METHALOX_NAME = TextKey.of("jsspace.propellant.methalox", "Methalox");
    /** Methalox' id, the propellant the skeleton's engines burn. */
    public static final ResourceLocation METHALOX = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "methalox");
    /* Raptor's mixture: 3.6 kilograms of oxygen to each of methane. */
    private static final double OXYGEN_TO_METHANE = 3.6;
    /* What a good methalox engine gets of it in a vacuum, in seconds. */
    private static final double VACUUM_ISP = 380;
    /* Where each liquid is kept, at its boiling point, in kelvin. */
    private static final int LIQUID_METHANE_KELVIN = 112;
    private static final int LIQUID_OXYGEN_KELVIN = 90;
    private static final Material OXYGEN = Elements.of(Element.OXYGEN);
    /** Liquid methane and liquid oxygen, which J's Space uses, so they exist whatever else is installed. */
    public static final Supplier<Fluid> LIQUID_METHANE = MaterialLiquids.use(JsSpace.MODID, SharedCatalogue.METHANE,
            LIQUID_METHANE_KELVIN);
    public static final Supplier<Fluid> LIQUID_OXYGEN = MaterialLiquids.use(JsSpace.MODID, OXYGEN,
            LIQUID_OXYGEN_KELVIN);

    private SkeletonPropellants() {
    }

    /** Declares the propellant, once, from the mod's constructor. */
    public static void declare() {
        final double methane = 1 / (1 + OXYGEN_TO_METHANE);
        Propellants.declare(METHALOX, new Propellant(METHALOX_NAME.key(), List.of(
                new Propellant.Component(SharedCatalogue.METHANE.id(), MatterState.LIQUID, methane),
                new Propellant.Component(OXYGEN.id(), MatterState.LIQUID, 1 - methane)), VACUUM_ISP, false));
    }
}
