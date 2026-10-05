/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.multiblock.IMatchResult;
import dev.jstech.core.multiblock.IMultiblockPortHost;
import dev.jstech.core.multiblock.MultiblockPartBlockEntity;
import dev.jstech.core.multiblock.MultiblockPatterns;
import dev.jstech.core.multiblock.MultiblockPorts;
import dev.jstech.core.multiblock.PortKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;
import org.jetbrains.annotations.Nullable;

/**
 * The test furnace's controller: an input slot and an output slot, and some energy, reached only through the ports of
 * its formed structure. It makes nothing; the tests only see what goes through the ports.
 */
public final class TestFurnaceBlockEntity extends BlockEntity implements IMultiblockPortHost {

    private final ItemStackHandler slots = new ItemStackHandler(2);
    private final EnergyStorage energy = new EnergyStorage(10_000);
    private boolean formed;

    public static final int INPUT = 0;
    public static final int OUTPUT = 1;

    public TestFurnaceBlockEntity(final BlockPos pos, final BlockState state) {
        super(TestMultiblocks.CONTROLLER_BE.get(), pos, state);
    }

    /** Matches the pattern round the controller and, when it fits, binds its parts and stamps its ports. */
    public IMatchResult form() {
        final IMatchResult match = MultiblockPatterns.match(TestMultiblocks.FURNACE, level, worldPosition);
        formed = match instanceof IMatchResult.Success;
        if (match instanceof IMatchResult.Success success) {
            for (final long encoded : success.slavePositions()) {
                if (level.getBlockEntity(MultiblockPatterns.decode(encoded))
                        instanceof MultiblockPartBlockEntity part) {
                    part.setController(worldPosition);
                }
            }
            MultiblockPorts.stamp(level, success);
        }
        return match;
    }

    public ItemStackHandler slots() {
        return slots;
    }

    public IEnergyStorage energy() {
        return energy;
    }

    @Override
    @Nullable
    public IItemHandler itemPort(final PortKind kind, final BlockPos part, @Nullable final Direction side) {
        if (!formed) {
            return null;
        }
        return switch (kind) {
            case ITEM_INPUT -> new RangedWrapper(slots, INPUT, INPUT + 1);
            case ITEM_OUTPUT -> new RangedWrapper(slots, OUTPUT, OUTPUT + 1);
            default -> null;
        };
    }

    @Override
    @Nullable
    public IEnergyStorage energyPort(final PortKind kind, final BlockPos part, @Nullable final Direction side) {
        return formed && kind == PortKind.ENERGY_INPUT ? energy : null;
    }
}
