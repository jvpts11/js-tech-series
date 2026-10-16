/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import dev.jstech.core.energy.IEnergyPort;
import dev.jstech.core.heat.IHeatPort;
import dev.jstech.core.matter.IMatterHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The block entity of a multiblock's controller, as the structure's ports reach it: what a pipe or a cable beside a
 * port gets when it asks the port for items, fluid, energy, another state of matter or heat. A port asks only for what
 * its kind moves, and a host answers null for a port it does not serve, or while the structure is not formed.
 */
public interface IMultiblockPortHost {

    /** The items a port of {@code kind} at {@code part} offers on {@code side}, or null. */
    @Nullable
    default IItemHandler itemPort(final PortKind kind, final BlockPos part, @Nullable final Direction side) {
        return null;
    }

    /** The fluid a port of {@code kind} at {@code part} offers on {@code side}, or null. */
    @Nullable
    default IFluidHandler fluidPort(final PortKind kind, final BlockPos part, @Nullable final Direction side) {
        return null;
    }

    /** The energy a port of {@code kind} at {@code part} offers on {@code side}, or null. */
    @Nullable
    default IEnergyStorage energyPort(final PortKind kind, final BlockPos part, @Nullable final Direction side) {
        return null;
    }

    /**
     * The store of the state of matter of {@code kind} (a gas, a slurry, plasma) a port at {@code part} offers on
     * {@code side}, or null.
     */
    @Nullable
    default IMatterHandler matterPort(final PortKind kind, final BlockPos part, @Nullable final Direction side) {
        return null;
    }

    /**
     * The port of J's Energy, with its voltage class, an energy port of {@code kind} at {@code part} offers on
     * {@code side}, or null: what a cable of a class meets there.
     */
    @Nullable
    default IEnergyPort energyClassPort(final PortKind kind, final BlockPos part, @Nullable final Direction side) {
        return null;
    }

    /** The heat a heat port at {@code part} lets through on {@code side}, or null. */
    @Nullable
    default IHeatPort heatPort(final BlockPos part, @Nullable final Direction side) {
        return null;
    }

    /**
     * What a port of a mod's own at {@code part} offers of {@code capability} on {@code side}, as a line of its own
     * (compressed air) reaches the machine, or null; registered with {@link MultiblockPorts#registerOther}.
     */
    @Nullable
    default <T> T otherPort(final BlockCapability<T, ?> capability, final BlockPos part,
                            @Nullable final Direction side) {
        return null;
    }
}
