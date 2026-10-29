/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.rocket;

import dev.jstech.core.heat.BoilOff;
import dev.jstech.core.heat.Thermal;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.Materials;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * The cold escaping a rocket's tanks: a liquid kept below where it boils at the tank's pressure boils off as the
 * place's heat leaks in through the tank's foam, by the same rule as J's Core's tanks. The heat that leaks into a tank
 * is shared among its materials by the room each takes, and each loses what that heat is worth by its heat of
 * boiling: liquid hydrogen fastest, oxygen slower, methane slower still, and a liquid that keeps at the place's warmth,
 * as hydrazine, nothing. The foam is a rocket tank's real insulation; where the place is as cold as space, nothing
 * leaks in.
 */
public final class RocketBoilOff {

    /** A rocket tank's foam, in watts a square metre a kelvin. */
    public static final double INSULATION = BoilOff.INSULATED;
    /** The pressure a rocket tank is kept at, in kilopascals, as its vents hold it. */
    public static final double TANK_KILOPASCALS = 200;

    private RocketBoilOff() {
    }

    /**
     * The kilograms of a liquid boiling off in {@code seconds}, the heat leaking in through {@code conductance} watts a
     * kelvin, the place {@code warmer} kelvin warmer than where it boils, a kilogram of it taking {@code heatOfBoiling}
     * joules to boil; none where the place is no warmer.
     */
    public static double kilograms(final double conductance, final double warmer, final double heatOfBoiling,
                                   final double seconds) {
        if (!(conductance > 0) || !(warmer > 0) || !(heatOfBoiling > 0) || !(seconds > 0)) {
            return 0;
        }
        return conductance * warmer * seconds / heatOfBoiling;
    }

    /** The side of a tank {@code diameter} blocks across and {@code height} tall, in square metres. */
    public static double surface(final int diameter, final int height) {
        return Math.PI * diameter * height;
    }

    /**
     * What {@code load} holds after {@code seconds} in a tank of {@code squareMetres} of side, at a place
     * {@code kelvin} warm. A material the Core does not know, or whose liquid it knows nothing of, keeps.
     */
    public static TankLoad boil(final TankLoad load, final double squareMetres, final double kelvin,
                                final double seconds) {
        double room = 0;
        for (final Map.Entry<ResourceLocation, Double> held : load.kilograms().entrySet()) {
            room += litres(held.getKey(), held.getValue());
        }
        if (!(room > 0) || !(seconds > 0)) {
            return load;
        }
        TankLoad left = load;
        for (final Map.Entry<ResourceLocation, Double> held : load.kilograms().entrySet()) {
            final Optional<Thermal> thermal = thermal(held.getKey());
            if (thermal.isEmpty() || held.getValue() <= 0) {
                continue;
            }
            final double share = litres(held.getKey(), held.getValue()) / room;
            final double warmer = kelvin - thermal.get().boilingAt(TANK_KILOPASCALS);
            final double boiled = kilograms(INSULATION * squareMetres * share, warmer,
                    thermal.get().heatOfBoiling(), seconds);
            if (boiled > 0) {
                left = left.with(held.getKey(), held.getValue() - boiled);
            }
        }
        return left;
    }

    /* The room {@code kilograms} of a material's liquid takes, in litres; none for one whose liquid is not known. */
    private static double litres(final ResourceLocation material, final double kilograms) {
        final double perLitre = thermal(material).map(Thermal::litreKilograms).orElse(Double.NaN);
        return perLitre > 0 ? Math.max(0, kilograms) / perLitre : 0;
    }

    private static Optional<Thermal> thermal(final ResourceLocation material) {
        final Optional<Material> known = Materials.byId(material);
        return known.map(found -> Thermal.of(found, false));
    }
}
