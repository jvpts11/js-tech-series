/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.ComputingModule;
import dev.jstech.core.blockentity.FieldFluidTank;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * A plain fluid tank, the minimal external fluid I/O point used to exercise the network's everything-is-data storage:
 * an Import Bus pulls its fluid into the network and an Export Bus pushes fluid back into it, exactly as the same
 * buses move items. Its fluid is saved, shown to the players who see it and offered to pipes on every side.
 */
public class TankBlockEntity extends SyncedBlockEntity {

    private final FieldFluidTank tank = fields().fluid("Tank", CAPACITY_MB).save().toClient().exposed();

    public static final int CAPACITY_MB = 16 * 1000;

    public TankBlockEntity(final BlockPos pos, final BlockState state) {
        super(ComputingModule.TANK_BE.get(), pos, state);
    }

    public IFluidHandler fluidHandler() {
        return tank;
    }

    public FluidStack fluid() {
        return tank.getFluid();
    }

    public float fillFraction() {
        return CAPACITY_MB <= 0 ? 0F : (float) tank.getFluidAmount() / CAPACITY_MB;
    }
}
