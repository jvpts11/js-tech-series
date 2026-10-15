/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.heat;

import dev.jstech.core.blockentity.IFieldPart;
import dev.jstech.core.transfer.Batch;
import dev.jstech.core.transfer.HandlerSteps;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * A liquid passing through a block's wall and changing form ({@link HeatForms}) as it gives the wall its heat or takes
 * the wall's: a hot form comes out cold where the wall is colder than the cold form, and the wall warms by what it
 * gave; a cold form comes out hot where the wall is hotter than the hot form, and the wall cools by what it took. Two
 * passes through one wall are a heat exchanger between two liquids, the wall between them; a pass beside a
 * {@link Boiler} is the boiler of a solar trough or of a reactor's loop.
 *
 * <p>The heat a pass moves in a step is the wall's conductance times how far the liquid's mean temperature is from the
 * wall's; it keeps the heat of the millibucket now passing until it has the whole of it, and is a part of its block
 * entity's fields, saved with it.
 *
 * <pre>{@code
 * private final FluidPass pass = FluidPass.of(10_000);   // 10 kW a kelvin of wall
 * ...
 * fields().part("Pass", pass).save();
 * ...
 * pass.run(heat, hotIn, coldOut, HeatMath.TICK_SECONDS);   // in its tick
 * }</pre>
 */
public final class FluidPass implements IFieldPart {

    private final double conductance;
    private double held;

    private static final String KEY = "FluidPass";

    private FluidPass(final double conductance) {
        if (!(conductance > 0.0)) {
            throw new IllegalArgumentException("a pass passes heat through its wall; got " + conductance + " W/K");
        }
        this.conductance = conductance;
    }

    /** A pass whose wall passes {@code wattsPerKelvin} between the liquid and the block. */
    public static FluidPass of(final double wattsPerKelvin) {
        return new FluidPass(wattsPerKelvin);
    }

    /**
     * Passes the liquid in {@code in}, of a form with another, into {@code out} in its other form, as much as the heat
     * through {@code wall} in {@code seconds} changes; says how many millibuckets changed form. Nothing passes while
     * the wall cannot take the liquid to its other form's temperature.
     */
    public long run(final HeatBody wall, final IFluidHandler in, final IFluidHandler out, final double seconds) {
        final FluidStack inside = in.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        if (inside.isEmpty() || !wall.known() || seconds <= 0.0) {
            return 0L;
        }
        final Optional<Fluid> colder = HeatForms.colderOf(inside.getFluid());
        final Optional<Fluid> other = colder.or(() -> HeatForms.hotterOf(inside.getFluid()));
        if (other.isEmpty()) {
            return 0L;
        }
        final FluidStack becomes = new FluidStack(other.get(), inside.getAmount());
        final boolean cooling = colder.isPresent();
        final FluidStack cold = cooling ? becomes : inside;
        final FluidStack hot = cooling ? inside : becomes;
        final double coldKelvin = FluidHeat.temperature(cold);
        final double hotKelvin = FluidHeat.temperature(hot);
        final double perMillibucket = HeatForms.joulesPerMillibucket(cold, hot);
        // A hot form cools no lower than a wall colder than its cold form; a cold one heats only on a wall hotter.
        final double margin = cooling ? coldKelvin - wall.temperature() : wall.temperature() - hotKelvin;
        if (!(margin > 0.0) || !(perMillibucket > 0.0)) {
            return 0L;
        }
        final long most = Math.min(inside.getAmount(), out.fill(becomes, IFluidHandler.FluidAction.SIMULATE));
        if (most <= 0L) {
            return 0L;
        }
        final double mean = (coldKelvin + hotKelvin) / 2.0;
        final double drive = cooling ? mean - wall.temperature() : wall.temperature() - mean;
        final double heat = Math.max(0.0, Math.min(Math.min(this.conductance * drive * seconds,
                margin * wall.capacity()), most * perMillibucket - this.held));
        this.held += heat;
        wall.exchange(cooling ? heat : -heat);
        final long passing = Math.min(most, (long) Math.floor(this.held / perMillibucket));
        if (passing <= 0L) {
            return 0L;
        }
        final Batch.Outcome outcome = new Batch().add(HandlerSteps.drain(in, inside.copyWithAmount((int) passing)))
                .add(HandlerSteps.fill(out, becomes.copyWithAmount((int) passing))).commit();
        if (!outcome.done()) {
            return 0L;
        }
        this.held -= passing * perMillibucket;
        return passing;
    }

    /** The joules it holds of the millibucket now passing. */
    public double held() {
        return this.held;
    }

    @Override
    public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
        tag.putDouble(KEY, this.held);
    }

    @Override
    public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
        this.held = tag.getDouble(KEY);
    }
}
