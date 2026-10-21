/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Example.
 */
package dev.jstech.example.part;

import dev.jstech.core.multipart.IFacePart;
import dev.jstech.core.multipart.IPartHost;
import dev.jstech.core.multipart.PartType;
import dev.jstech.core.text.Text;
import dev.jstech.example.ExampleContent;
import dev.jstech.example.ExampleTexts;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The level sensor: a plate mounted on a face of a cable, which reads how full the block its face points at is, of
 * energy or else of fluid, and gives that out as a redstone signal from 0 for empty to 15 for full, as a comparator
 * reads a chest. The cable's other wires and pipes run on through the block beside it.
 */
public final class LevelSensorPart implements IFacePart {

    private @Nullable IPartHost host;
    private Direction face = Direction.NORTH;
    private int strength;

    /** The strongest signal, for a block that is full. */
    public static final int FULL = 15;
    private static final String STRENGTH = "strength";

    @Override
    public PartType<?> type() {
        return ExampleContent.SENSOR.get();
    }

    @Override
    public void attach(final IPartHost on, final Direction at) {
        this.host = on;
        this.face = at;
    }

    @Override
    public void serverTick() {
        final IPartHost on = this.host;
        if (on == null) {
            return;
        }
        final int now = reading(on.partLevel(), on.partPos().relative(face), face.getOpposite());
        if (now != strength) {
            strength = now;
            on.partSignalChanged(face);
        }
    }

    @Override
    public int signal() {
        return strength;
    }

    @Override
    public int comparator() {
        return strength;
    }

    @Override
    public void describe(final List<Text> lines, final boolean details) {
        lines.add(ExampleTexts.SENSOR_READS.with(strength));
    }

    @Override
    public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
        tag.putInt(STRENGTH, strength);
    }

    @Override
    public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
        strength = tag.getInt(STRENGTH);
    }

    @Override
    public ItemStack partItem() {
        return new ItemStack(ExampleContent.SENSOR.item().get());
    }

    /** What the sensor reads of the block at {@code pos} seen from {@code side}: its energy, or else its first tank. */
    public static int reading(final Level level, final BlockPos pos, final Direction side) {
        final IEnergyStorage energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side);
        if (energy != null && energy.getMaxEnergyStored() > 0) {
            return strengthOf(energy.getEnergyStored(), energy.getMaxEnergyStored());
        }
        final IFluidHandler fluid = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
        if (fluid != null && fluid.getTanks() > 0 && fluid.getTankCapacity(0) > 0) {
            return strengthOf(fluid.getFluidInTank(0).getAmount(), fluid.getTankCapacity(0));
        }
        return 0;
    }

    /* A share as a signal: nothing is 0, anything at all is at least 1, and only full is 15. */
    private static int strengthOf(final long amount, final long most) {
        if (amount <= 0) {
            return 0;
        }
        return (int) Math.max(1, Math.min(FULL, amount * FULL / most));
    }
}
