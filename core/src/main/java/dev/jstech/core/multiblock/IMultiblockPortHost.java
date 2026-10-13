/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The block entity of a multiblock's controller, as the structure's ports reach it: what a pipe or a cable beside a
 * port gets when it asks the port for items, fluid or energy. A port asks only for what its kind moves, and a host
 * answers null for a port it does not serve, or while the structure is not formed.
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
}
