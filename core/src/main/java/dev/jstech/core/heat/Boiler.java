/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.heat;

import dev.jstech.core.blockentity.IFieldPart;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialLiquids;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.IMatterHandler;
import dev.jstech.core.matter.MatterStack;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * A boiler's water and its steam, by the real enthalpy: a block's hot wall boils the liquid in a tank into its gas in
 * a tank of gas, each millibucket (a litre) taking the heat that brings it from its own temperature to where it boils
 * and then boils it, 2.26 MJ for every kilogram of water. Any liquid of a material that has a gas boils so, by its
 * material's data ({@link Thermal}): the game's water is the series' water.
 *
 * <p>Where it boils is the pressure's to say: the place's air, unless the boiler is a vessel of its own pressure, so
 * water boils lower on Mars and higher in a pressure vessel. The heat that passes in a step is the wall's conductance
 * times how far the wall is above the boiling point, and never takes the wall below it. The gas is counted as gases
 * are, a mole to 22.4 litres, so a millibucket of water makes about 1,240 of steam.
 *
 * <pre>{@code
 * private final Boiler boiler = Boiler.of(50_000);   // 50 kW a kelvin of tubes
 * ...
 * fields().part("Boiler", boiler).save();
 * ...
 * boiler.boil(level, pos, heat, water, steam, HeatMath.TICK_SECONDS);   // in its tick
 * }</pre>
 */
public final class Boiler implements IFieldPart {

    private final double conductance;
    private double kiloPascals = Double.NaN;
    private double held;
    private double owed;

    private static final String KEY = "Boiler";
    private static final String HELD = "held";
    private static final String OWED = "owed";

    private Boiler(final double conductance) {
        if (!(conductance > 0.0)) {
            throw new IllegalArgumentException("a boiler passes heat through its tubes; got " + conductance + " W/K");
        }
        this.conductance = conductance;
    }

    /** A boiler whose tubes pass {@code wattsPerKelvin} between its wall and what boils. */
    public static Boiler of(final double wattsPerKelvin) {
        return new Boiler(wattsPerKelvin);
    }

    /** The same, a vessel holding its own {@code pressure} in kilopascals instead of the place's air's. */
    public Boiler at(final double pressure) {
        if (!(pressure > 0.0)) {
            throw new IllegalArgumentException("a vessel holds a pressure above nothing; got " + pressure + " kPa");
        }
        this.kiloPascals = pressure;
        return this;
    }

    /**
     * Boils the liquid in {@code liquid} into its gas in {@code gas}, by the heat of {@code wall} in {@code seconds},
     * for the block at {@code pos}; says how many millibuckets of liquid boiled. Nothing boils of a liquid of no
     * material, of one whose material has no gas or whose heat of boiling or molar mass is not known, nor while the
     * wall is no hotter than where it boils.
     */
    public long boil(final Level level, final BlockPos pos, final HeatBody wall, final IFluidHandler liquid,
                     final IMatterHandler gas, final double seconds) {
        final FluidStack inside = liquid.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        if (inside.isEmpty() || !wall.known() || seconds <= 0.0 || gas.state() != MatterState.GAS) {
            return 0L;
        }
        final Optional<Material> material = MaterialLiquids.materialOf(inside.getFluid())
                .filter(found -> found.takes(MatterState.GAS));
        if (material.isEmpty()) {
            return 0L;
        }
        final Thermal thermal = Thermal.of(material.get(), false);
        final double perLitre = thermal.litreKilograms();
        final double gasPerLitre = HeatMath.gasLitres(perLitre, thermal.molarMass());
        final double boils = thermal.boilingAt(pressure(level, pos));
        final double margin = wall.temperature() - boils;
        if (!(margin > 0.0) || !(gasPerLitre > 0.0) || Double.isNaN(thermal.heatOfBoiling())) {
            return 0L;
        }
        final double perMillibucket = perLitre * (thermal.specificHeat()
                * Math.max(0.0, boils - FluidHeat.temperature(inside)) + thermal.heatOfBoiling());
        final long room = gas.insert(new MatterStack(material.get(), Long.MAX_VALUE / 2), true);
        final long most = Math.min(inside.getAmount(), (long) Math.floor((room - this.owed) / gasPerLitre));
        if (most <= 0L) {
            return 0L;
        }
        final double heat = Math.max(0.0, Math.min(Math.min(this.conductance * margin * seconds,
                margin * wall.capacity()), most * perMillibucket - this.held));
        this.held += heat;
        wall.exchange(-heat);
        final long boiling = Math.min(most, (long) Math.floor(this.held / perMillibucket));
        if (boiling <= 0L) {
            return 0L;
        }
        final int boiled = liquid.drain(inside.copyWithAmount((int) boiling), IFluidHandler.FluidAction.EXECUTE)
                .getAmount();
        final double made = boiled * gasPerLitre + this.owed;
        final long whole = (long) Math.floor(made);
        if (whole > 0L) {
            gas.insert(new MatterStack(material.get(), whole), false);
        }
        this.owed = made - whole;
        this.held -= boiled * perMillibucket;
        return boiled;
    }

    /** The pressure it boils at for the block at {@code pos}: its own as a vessel, or the place's air's. */
    public double pressure(final Level level, final BlockPos pos) {
        return Double.isNaN(this.kiloPascals) ? HeatClimate.pressure(level, pos) : this.kiloPascals;
    }

    /** The joules it holds of the millibucket now boiling. */
    public double held() {
        return this.held;
    }

    @Override
    public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
        final CompoundTag own = new CompoundTag();
        own.putDouble(HELD, this.held);
        own.putDouble(OWED, this.owed);
        tag.put(KEY, own);
    }

    @Override
    public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
        final CompoundTag own = tag.getCompound(KEY);
        this.held = own.getDouble(HELD);
        this.owed = own.getDouble(OWED);
    }
}
