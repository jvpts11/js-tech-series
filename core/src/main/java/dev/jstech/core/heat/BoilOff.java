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
import org.jetbrains.annotations.Nullable;

/**
 * A cold liquid kept in a tank boiling off as the heat of the place leaks in: liquid nitrogen, liquid oxygen, liquid
 * hydrogen, kept at their boiling points, lose to gas what the leak is worth by their heat of boiling. The leak is the
 * tank's insulation, in watts a square metre a kelvin, times its surface, times how much warmer the place is than
 * where the liquid boils at the place's pressure: a bare cubic metre of liquid nitrogen is gone in three and a half
 * hours, a vacuum-jacketed one in two months. What boils off goes into a tank of gas when the block has one with room,
 * and into the air otherwise.
 *
 * <pre>{@code
 * private final BoilOff boilOff = BoilOff.of(BoilOff.INSULATED, 6.0);   // a cubic metre's six square metres
 * ...
 * fields().part("BoilOff", boilOff).save();
 * ...
 * boilOff.vent(level, pos, tank, null, HeatMath.TICK_SECONDS);   // in its tick: into the air
 * }</pre>
 *
 * <p>A server may turn it off ({@link HeatBalance#boilOff}).
 */
public final class BoilOff implements IFieldPart {

    private final double conductance;
    private double held;
    private double owed;

    /** A bare metal tank in still air, in watts a square metre a kelvin. */
    public static final double BARE = 10.0;
    /** A tank under a hand's breadth of foam or rock wool. */
    public static final double INSULATED = 0.5;
    /** A vacuum-jacketed tank, as a dewar is. */
    public static final double VACUUM_JACKETED = 0.02;

    private static final String KEY = "BoilOff";
    private static final String HELD = "held";
    private static final String OWED = "owed";

    private BoilOff(final double conductance) {
        this.conductance = conductance;
    }

    /** A tank of {@code squareMetres} of surface insulated as {@code insulation} says, in W/(m²·K). */
    public static BoilOff of(final double insulation, final double squareMetres) {
        if (!(insulation >= 0.0) || !(squareMetres >= 0.0)) {
            throw new IllegalArgumentException("a tank's insulation and surface are no less than nothing");
        }
        return new BoilOff(insulation * squareMetres);
    }

    /**
     * Boils off what the heat leaking in for {@code seconds} is worth of the liquid in {@code tank}, at {@code pos} of
     * {@code level}, into {@code into} as far as it has room and into the air beyond; says how many millibuckets of
     * liquid boiled off. Nothing boils off of a liquid of no material, of one whose heat of boiling or liquid is not
     * known, nor where the place is no warmer than where it boils.
     */
    public long vent(final Level level, final BlockPos pos, final IFluidHandler tank,
                     @Nullable final IMatterHandler into, final double seconds) {
        if (!HeatBalance.boilOff() || seconds <= 0.0) {
            return 0L;
        }
        final FluidStack inside = tank.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        if (inside.isEmpty()) {
            return 0L;
        }
        final Optional<Material> material = MaterialLiquids.materialOf(inside.getFluid());
        if (material.isEmpty()) {
            return 0L;
        }
        final Thermal thermal = Thermal.of(material.get(), false);
        final double perMillibucket = thermal.litreKilograms() * thermal.heatOfBoiling();
        final HeatClimate.Air air = HeatClimate.air(level, pos);
        final double warmer = air.kelvin() - thermal.boilingAt(air.kiloPascals());
        if (!(perMillibucket > 0.0) || !(warmer > 0.0)) {
            return 0L;
        }
        this.held += this.conductance * warmer * seconds;
        final long boiling = Math.min(inside.getAmount(), (long) Math.floor(this.held / perMillibucket));
        if (boiling <= 0L) {
            return 0L;
        }
        final int boiled = tank.drain(inside.copyWithAmount((int) boiling), IFluidHandler.FluidAction.EXECUTE)
                .getAmount();
        this.held = boiled >= inside.getAmount() ? 0.0 : this.held - boiled * perMillibucket;
        if (into != null && into.state() == MatterState.GAS && material.get().takes(MatterState.GAS)) {
            final double made = boiled * HeatMath.gasLitres(thermal.litreKilograms(), thermal.molarMass())
                    + this.owed;
            final long whole = (long) Math.floor(made);
            this.owed = made - whole;
            if (whole > 0L) {
                into.insert(new MatterStack(material.get(), whole), false);
            }
        }
        return boiled;
    }

    /** The heat that has leaked in towards the next millibucket, in joules. */
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
