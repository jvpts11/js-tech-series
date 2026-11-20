/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.plant;

import dev.jstech.core.inventory.FaceFluidHandler;
import dev.jstech.core.inventory.FaceMode;
import dev.jstech.core.inventory.ItemFilter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * A liquid-cooled rack's coolant (computers/docs/design/temperature.md): the Supercomputer Rack and the AI Rack take
 * Cold Water in at one top port and give Hot Water out at the other, as much as their heat over what the cabinet sheds
 * by air, up to {@link #MOST_A_SECOND} millibuckets a second, and shed {@link CoolingWater#KILOWATT_A_MILLIBUCKET} kW
 * more for each. Short of cold water, or with the hot water's way out full, a rack sheds only what went through. The
 * water moves once a second, and what it carried counts for the next second.
 */
public final class RackCoolant {

    private final FluidTank cold = new FluidTank(TANK, stack -> stack.is(CoolingWater.COLD.get()));
    private final FluidTank hot = new FluidTank(TANK, stack -> stack.is(CoolingWater.HOT.get()));
    private final IFluidHandler inlet = new ColdInlet(cold);
    private final IFluidHandler outlet = new FaceFluidHandler(hot, FaceMode.OUT, ItemFilter.EVERYTHING);
    private int cooledWatts;
    private int askedWatts;
    private int ticks;

    /** The most water a rack moves a second, in millibuckets: 8,000 W at most. */
    public static final int MOST_A_SECOND = 8;
    /* What each port holds, in millibuckets: some seconds of a full rack's water either way. */
    private static final int TANK = 1_000;
    private static final int WATTS_A_MILLIBUCKET = CoolingWater.KILOWATT_A_MILLIBUCKET * 1_000;
    private static final int SECOND = 20;
    private static final String COLD = "cold";
    private static final String HOT = "hot";
    private static final String COOLED = "cooled";
    private static final String ASKED = "asked";

    /**
     * Moves the second's water, once every twenty ticks: as many millibuckets as the {@code overWatts} the cabinet
     * cannot shed by air need, rounded up, at most {@link #MOST_A_SECOND}, as far as the cold water and the hot
     * water's room let it.
     */
    public void tick(final int overWatts) {
        if (++this.ticks < SECOND) {
            return;
        }
        this.ticks = 0;
        final int need = Math.min(MOST_A_SECOND, Math.max(0, (overWatts + WATTS_A_MILLIBUCKET - 1)
                / WATTS_A_MILLIBUCKET));
        final int moved = Math.min(need, Math.min(this.cold.getFluidAmount(), this.hot.getSpace()));
        if (moved > 0) {
            this.cold.drain(moved, IFluidHandler.FluidAction.EXECUTE);
            this.hot.fill(new FluidStack(CoolingWater.HOT.get(), moved), IFluidHandler.FluidAction.EXECUTE);
        }
        this.cooledWatts = moved * WATTS_A_MILLIBUCKET;
        this.askedWatts = need * WATTS_A_MILLIBUCKET;
    }

    /** The watts the water carried away in the last second, which the cabinet sheds besides its air. */
    public int cooledWatts() {
        return this.cooledWatts;
    }

    /** The watts the cabinet asked of its water in the last second: as many as it carried when none was short. */
    public int askedWatts() {
        return this.askedWatts;
    }

    /** The watts it would have carried at most: what its cabinet asked of it, capped by the most it moves. */
    public static int mostWatts() {
        return MOST_A_SECOND * WATTS_A_MILLIBUCKET;
    }

    /** The top port the cold water comes in at: filled only, with Cold Water or plain water taken as it. */
    public IFluidHandler inlet() {
        return this.inlet;
    }

    /** The top port the hot water leaves at: drained only. */
    public IFluidHandler outlet() {
        return this.outlet;
    }

    public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
        tag.put(COLD, this.cold.writeToNBT(registries, new CompoundTag()));
        tag.put(HOT, this.hot.writeToNBT(registries, new CompoundTag()));
        tag.putInt(COOLED, this.cooledWatts);
        tag.putInt(ASKED, this.askedWatts);
    }

    public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
        this.cold.readFromNBT(registries, tag.getCompound(COLD));
        this.hot.readFromNBT(registries, tag.getCompound(HOT));
        this.cooledWatts = tag.getInt(COOLED);
        this.askedWatts = tag.getInt(ASKED);
    }
}
