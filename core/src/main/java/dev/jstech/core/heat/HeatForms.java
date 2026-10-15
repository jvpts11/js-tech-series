/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.heat;

import dev.jstech.core.util.DeclarationError;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * A liquid's two forms, cold and hot, as a loop that carries heat has them: the oil of a solar trough, cold coming back
 * from the boiler and hot going to it, or a reactor's coolant. Each form is a fluid of its own at its own temperature,
 * and a millibucket of it changing form gives or takes the heat that the change of temperature is worth, by its
 * density and specific heat. The fluids are named by id, so a pair of an absent mod's sleeps.
 *
 * <pre>{@code
 * HeatForms.declare(rl("mymod", "thermal_oil"), rl("mymod", "hot_thermal_oil"));   // from the mod's constructor
 * }</pre>
 */
public final class HeatForms {

    private static final Map<ResourceLocation, ResourceLocation> HOTTER = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, ResourceLocation> COLDER = new ConcurrentHashMap<>();

    private HeatForms() {
    }

    /**
     * Declares that {@code hot} is {@code cold} heated: the two forms of one liquid.
     *
     * @throws DeclarationError when the two are one fluid, or either has a form declared already
     */
    public static void declare(final ResourceLocation cold, final ResourceLocation hot) {
        Objects.requireNonNull(cold, "cold");
        Objects.requireNonNull(hot, "hot");
        if (cold.equals(hot)) {
            throw DeclarationError.of(cold.getNamespace(), "the forms of " + cold, "are one fluid",
                    "give its hot form a fluid of its own");
        }
        if (HOTTER.containsKey(cold) || COLDER.containsKey(cold) || HOTTER.containsKey(hot)
                || COLDER.containsKey(hot)) {
            throw DeclarationError.of(cold.getNamespace(), "the forms " + cold + " and " + hot,
                    "take a fluid that has a form already", "give each fluid one other form");
        }
        HOTTER.put(cold, hot);
        COLDER.put(hot, cold);
    }

    /** The hot form of {@code fluid}, for its cold form; none otherwise or while that mod is absent. */
    public static Optional<Fluid> hotterOf(final Fluid fluid) {
        return other(HOTTER, fluid);
    }

    /** The cold form of {@code fluid}, for its hot form; none otherwise or while that mod is absent. */
    public static Optional<Fluid> colderOf(final Fluid fluid) {
        return other(COLDER, fluid);
    }

    /**
     * The joules a millibucket of {@code cold} takes to become {@code hot}, its other form, or gives back going the
     * other way: what it weighs, times its specific heat, times the kelvin between the two.
     */
    public static double joulesPerMillibucket(final FluidStack cold, final FluidStack hot) {
        final double kelvin = Math.max(0.0, FluidHeat.temperature(hot) - FluidHeat.temperature(cold));
        return FluidHeat.litreKilograms(cold) * FluidHeat.specificHeat(cold) * kelvin;
    }

    private static Optional<Fluid> other(final Map<ResourceLocation, ResourceLocation> forms, final Fluid fluid) {
        final ResourceLocation found = forms.get(BuiltInRegistries.FLUID.getKey(fluid));
        if (found == null) {
            return Optional.empty();
        }
        final Fluid other = BuiltInRegistries.FLUID.get(found);
        return other == Fluids.EMPTY ? Optional.empty() : Optional.of(other);
    }
}
