/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.blockentity;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * What a block entity offers its neighbours, counting what they put in and take out into its {@link FieldFlow}: the
 * same inventory, energy store or tanks, with every real move, never one only simulated, added up.
 */
final class CountedCapabilities {

    private CountedCapabilities() {
    }

    /** {@code handler}, counting into {@code flow}; none for none. */
    static Object counted(final Object handler, final FieldFlow flow) {
        if (handler instanceof IItemHandler items) {
            return new Items(items, flow);
        }
        if (handler instanceof IEnergyStorage energy) {
            return new Energy(energy, flow);
        }
        if (handler instanceof IFluidHandler fluid) {
            return new Fluid(fluid, flow);
        }
        return handler;
    }

    private record Items(IItemHandler inner, FieldFlow flow) implements IItemHandler {

        @Override
        public int getSlots() {
            return this.inner.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(final int slot) {
            return this.inner.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
            final ItemStack left = this.inner.insertItem(slot, stack, simulate);
            if (!simulate) {
                this.flow.items(stack.getCount() - left.getCount(), 0);
            }
            return left;
        }

        @Override
        public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
            final ItemStack taken = this.inner.extractItem(slot, amount, simulate);
            if (!simulate) {
                this.flow.items(0, taken.getCount());
            }
            return taken;
        }

        @Override
        public int getSlotLimit(final int slot) {
            return this.inner.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(final int slot, final ItemStack stack) {
            return this.inner.isItemValid(slot, stack);
        }
    }

    private record Energy(IEnergyStorage inner, FieldFlow flow) implements IEnergyStorage {

        @Override
        public int receiveEnergy(final int toReceive, final boolean simulate) {
            final int received = this.inner.receiveEnergy(toReceive, simulate);
            if (!simulate) {
                this.flow.energy(received, 0);
            }
            return received;
        }

        @Override
        public int extractEnergy(final int toExtract, final boolean simulate) {
            final int extracted = this.inner.extractEnergy(toExtract, simulate);
            if (!simulate) {
                this.flow.energy(0, extracted);
            }
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            return this.inner.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return this.inner.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return this.inner.canExtract();
        }

        @Override
        public boolean canReceive() {
            return this.inner.canReceive();
        }
    }

    private record Fluid(IFluidHandler inner, FieldFlow flow) implements IFluidHandler {

        @Override
        public int getTanks() {
            return this.inner.getTanks();
        }

        @Override
        public FluidStack getFluidInTank(final int tank) {
            return this.inner.getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(final int tank) {
            return this.inner.getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(final int tank, final FluidStack stack) {
            return this.inner.isFluidValid(tank, stack);
        }

        @Override
        public int fill(final FluidStack resource, final FluidAction action) {
            final int filled = this.inner.fill(resource, action);
            if (action.execute()) {
                this.flow.fluid(filled, 0);
            }
            return filled;
        }

        @Override
        public FluidStack drain(final FluidStack resource, final FluidAction action) {
            final FluidStack drained = this.inner.drain(resource, action);
            if (action.execute()) {
                this.flow.fluid(0, drained.getAmount());
            }
            return drained;
        }

        @Override
        public FluidStack drain(final int maxDrain, final FluidAction action) {
            final FluidStack drained = this.inner.drain(maxDrain, action);
            if (action.execute()) {
                this.flow.fluid(0, drained.getAmount());
            }
            return drained;
        }
    }
}
