/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.blockentity;

import dev.jstech.core.blockentity.IntField;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.industrial.IndustrialModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * The Coal Generator, the first FE source: burns furnace fuel (coal, charcoal and the like) to make
 * {@value #FE_PER_TICK} FE per tick while it has burn time left, and pushes what it stores into the energy consumers
 * beside it each tick.
 */
public class CoalGeneratorBlockEntity extends MachineBlockEntity {

    private final IntField burnTime;
    private final IntField maxBurnTime;

    public static final int FUEL_SLOT = 0;
    public static final int FE_PER_TICK = 20;
    private static final int ENERGY_CAPACITY = 16_000;
    private static final int ENERGY_MAX_EXTRACT = 1_000;

    public CoalGeneratorBlockEntity(final BlockPos pos, final BlockState state) {
        super(IndustrialModule.COAL_GENERATOR_BE.get(), pos, state, 1, ENERGY_CAPACITY, 0, ENERGY_MAX_EXTRACT);
        this.burnTime = fields().integer("BurnTime", 0).save().toMenu();
        this.maxBurnTime = fields().integer("MaxBurnTime", 0).save().toMenu();
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final CoalGeneratorBlockEntity generator) {
        generator.tick(level);
    }

    public int getBurnTime() {
        return burnTime.get();
    }

    public int getMaxBurnTime() {
        return maxBurnTime.get();
    }

    public boolean isBurning() {
        return burnTime.get() > 0;
    }

    private void tick(final Level level) {
        final boolean hasRoom = getEnergy().getEnergyStored() < getEnergy().getMaxEnergyStored();
        if (burnTime.get() > 0) {
            burnTime.add(-1);
            if (hasRoom) {
                getEnergy().generate(FE_PER_TICK);
            }
        } else if (hasRoom) {
            final ItemStack fuel = getInventory().getStackInSlot(FUEL_SLOT);
            final int duration = fuel.getBurnTime(null);
            if (duration > 0) {
                // A fuel that leaves something behind (a lava bucket's empty bucket) gives it back, as a furnace does.
                final ItemStack remainder = fuel.getCraftingRemainingItem();
                getInventory().extractItem(FUEL_SLOT, 1, false);
                if (!remainder.isEmpty() && getInventory().getStackInSlot(FUEL_SLOT).isEmpty()) {
                    getInventory().setStackInSlot(FUEL_SLOT, remainder);
                }
                burnTime.set(duration);
                maxBurnTime.set(duration);
                getEnergy().generate(FE_PER_TICK);
            }
        }
        pushEnergy(level);
    }

    private void pushEnergy(final Level level) {
        for (final Direction direction : Direction.values()) {
            if (getEnergy().getEnergyStored() <= 0) {
                break;
            }
            final IEnergyStorage neighbor = level.getCapability(Capabilities.EnergyStorage.BLOCK,
                    worldPosition.relative(direction), direction.getOpposite());
            if (neighbor == null || !neighbor.canReceive()) {
                continue;
            }
            final int offered = getEnergy().extractEnergy(ENERGY_MAX_EXTRACT, true);
            final int accepted = neighbor.receiveEnergy(offered, false);
            if (accepted > 0) {
                getEnergy().extractEnergy(accepted, false);
            }
        }
    }
}
