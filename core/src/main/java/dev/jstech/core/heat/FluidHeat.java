/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.heat;

import dev.jstech.core.material.MaterialKind;
import dev.jstech.core.material.MaterialLiquids;
import java.util.Optional;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Heat as a liquid meets it: how hot a fluid is, which is its type's temperature (the game's water at 300 K, its lava
 * at 1,300 K, a material's liquid at what its mods said), and what its material does with heat, for a liquid that is a
 * material's or is linked to one ({@link MaterialLiquids#link}). A liquid of no material is taken as water is, by its
 * own density.
 */
public final class FluidHeat {

    /* A thousand litres, or millibuckets, in a cubic metre. */
    private static final double LITRES = 1_000.0;

    private FluidHeat() {
    }

    /** What the liquid {@code fluid}'s material does with heat, or none for a liquid of no material. */
    public static Optional<Thermal> of(final Fluid fluid) {
        return MaterialLiquids.materialOf(fluid).map(material -> Thermal.of(material, false));
    }

    /** How hot {@code fluid} is, in kelvin. */
    public static double temperature(final FluidStack fluid) {
        return fluid.getFluidType().getTemperature(fluid);
    }

    /** How many kilograms a millibucket of {@code fluid} weighs: its material's liquid's, or its type's density's. */
    public static double litreKilograms(final FluidStack fluid) {
        final double known = of(fluid.getFluid()).map(Thermal::litreKilograms).orElse(Double.NaN);
        return Double.isNaN(known) ? Math.max(1, fluid.getFluidType().getDensity(fluid)) / LITRES : known;
    }

    /** How many joules a kilogram of {@code fluid} holds for each kelvin: its material's, or water's. */
    public static double specificHeat(final FluidStack fluid) {
        return of(fluid.getFluid()).map(Thermal::specificHeat)
                .orElse(Thermal.ofKind(MaterialKind.LIQUID).specificHeat());
    }
}
