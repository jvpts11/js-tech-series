/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.crafting;

import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.machine.ProcessingMachineBlockEntity;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.MatterTank;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * What a job fed one of the series' machines and no recipe used, taken back out of the machine's inputs into where the
 * job draws from: items from its input slots, liquids from its input tanks, gases and the other states of matter from
 * its input tanks of them. Each move asks first how much the destination takes, so nothing taken out is lost.
 */
final class MachineLeftovers {

    private MachineLeftovers() {
    }

    /** Takes up to {@code most} of {@code key} out of {@code machine}'s inputs into {@code to}; says how much. */
    static long takeBack(final ProcessingMachineBlockEntity machine, final StorageKey key, final long most,
                         final ICraftIo to) {
        if (most <= 0) {
            return 0L;
        }
        if (key.isItem()) {
            return items(machine, key, most, to);
        }
        if (key.isFluid()) {
            return liquid(machine, key, most, to);
        }
        final Optional<MatterState> state = key.kind().matter();
        final Optional<Material> material = key.material();
        return state.isPresent() && material.isPresent() ? matter(machine, state.get(), material.get(), key, most, to)
                : 0L;
    }

    private static long items(final ProcessingMachineBlockEntity machine, final StorageKey key, final long most,
                              final ICraftIo to) {
        final IItemHandler slots = machine.getInventory();
        long taken = 0L;
        for (int slot = 0; slot < machine.layout().firstOutput() && taken < most; slot++) {
            if (StorageKey.of(slots.getStackInSlot(slot)).equals(key)) {
                final int want = (int) Math.min(Integer.MAX_VALUE, most - taken);
                final ItemStack there = slots.extractItem(slot, want, true);
                final long kept = to.insert(key, there.getCount());
                if (kept > 0) {
                    slots.extractItem(slot, (int) kept, false);
                    taken += kept;
                }
            }
        }
        return taken;
    }

    private static long liquid(final ProcessingMachineBlockEntity machine, final StorageKey key, final long most,
                               final ICraftIo to) {
        long taken = 0L;
        for (int tank = 0; tank < machine.layout().inputTanks() && taken < most; tank++) {
            final IFluidHandler held = machine.inputTank(tank);
            final int want = (int) Math.min(Integer.MAX_VALUE, most - taken);
            final FluidStack there = held.drain(key.fluidStack(want), IFluidHandler.FluidAction.SIMULATE);
            if (!there.isEmpty()) {
                final long kept = to.insert(key, there.getAmount());
                if (kept > 0) {
                    held.drain(key.fluidStack((int) kept), IFluidHandler.FluidAction.EXECUTE);
                    taken += kept;
                }
            }
        }
        return taken;
    }

    private static long matter(final ProcessingMachineBlockEntity machine, final MatterState state,
                               final Material material, final StorageKey key, final long most, final ICraftIo to) {
        long taken = 0L;
        for (final MatterTank tank : machine.matter().inputs(state)) {
            if (taken >= most) {
                break;
            }
            final long there = tank.extract(material, most - taken, true);
            if (there > 0) {
                final long kept = to.insert(key, there);
                if (kept > 0) {
                    tank.extract(material, kept, false);
                    taken += kept;
                }
            }
        }
        return taken;
    }
}
