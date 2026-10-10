/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.blockentity;

import dev.jstech.core.blockentity.FieldFluidTank;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.item.ItemStates;
import dev.jstech.industrial.IndustrialModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * A plain fluid tank of sixteen buckets. Its fluid is saved, shown to the players who see it and offered to pipes on
 * every side, and it goes into the tank's item when the tank is broken, so the tank is placed again as it was.
 */
public class TankBlockEntity extends SyncedBlockEntity {

    private final FieldFluidTank tank = fields().fluid("Tank", CAPACITY_MB).save().toClient().exposed();

    public static final int CAPACITY_MB = 16 * 1000;

    public TankBlockEntity(final BlockPos pos, final BlockState state) {
        super(IndustrialModule.TANK_BE.get(), pos, state);
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

    /* What the tank holds goes into its item, which the loot table copies from the block entity. */
    @Override
    protected void collectImplicitComponents(final DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!tank.isEmpty()) {
            components.set(ItemStates.FLUID.get(), SimpleFluidContent.copyOf(tank.getFluid()));
        }
    }

    /* A tank placed from an item that holds a fluid starts with it. */
    @Override
    protected void applyImplicitComponents(final DataComponentInput input) {
        super.applyImplicitComponents(input);
        final SimpleFluidContent kept = input.getOrDefault(ItemStates.FLUID.get(), SimpleFluidContent.EMPTY);
        if (!kept.isEmpty()) {
            tank.setFluid(kept.copy());
            setChanged();
        }
    }
}
