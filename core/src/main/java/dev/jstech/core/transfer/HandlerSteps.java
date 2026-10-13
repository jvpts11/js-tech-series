/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.transfer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.function.LongUnaryOperator;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The steps of a {@link Batch} over the game's stores: putting items into an item handler and taking them out,
 * filling and draining a fluid handler, and giving and taking energy. Each remembers what it moved, slot by slot for
 * items, so undoing puts back exactly that.
 */
public final class HandlerSteps {

    private HandlerSteps() {
    }

    /** Puts all of {@code stack} into {@code handler}, slot after slot. */
    public static Recoverable insert(final IItemHandler handler, final ItemStack stack) {
        return new Insert(Objects.requireNonNull(handler, "handler"), stack.copy());
    }

    /** Takes {@code count} items like {@code like} (the same item and components) out of {@code handler}. */
    public static Recoverable extract(final IItemHandler handler, final ItemStack like, final int count) {
        return new Extract(Objects.requireNonNull(handler, "handler"), like.copyWithCount(1), count);
    }

    /** Fills {@code handler} with all of {@code fluid}. */
    public static Recoverable fill(final IFluidHandler handler, final FluidStack fluid) {
        final FluidStack wanted = fluid.copy();
        return new FluidStep(wanted,
                () -> handler.fill(wanted.copy(), IFluidHandler.FluidAction.SIMULATE),
                () -> handler.fill(wanted.copy(), IFluidHandler.FluidAction.EXECUTE),
                moved -> handler.drain(wanted.copyWithAmount((int) moved), IFluidHandler.FluidAction.EXECUTE)
                        .getAmount());
    }

    /** Drains all of {@code fluid} out of {@code handler}. */
    public static Recoverable drain(final IFluidHandler handler, final FluidStack fluid) {
        final FluidStack wanted = fluid.copy();
        return new FluidStep(wanted,
                () -> handler.drain(wanted.copy(), IFluidHandler.FluidAction.SIMULATE).getAmount(),
                () -> handler.drain(wanted.copy(), IFluidHandler.FluidAction.EXECUTE).getAmount(),
                moved -> handler.fill(wanted.copyWithAmount((int) moved), IFluidHandler.FluidAction.EXECUTE));
    }

    /** Gives {@code storage} {@code amount} FE. */
    public static Recoverable receive(final IEnergyStorage storage, final int amount) {
        return new AmountStep(amount,
                () -> storage.receiveEnergy(amount, true),
                () -> storage.receiveEnergy(amount, false),
                moved -> storage.extractEnergy((int) moved, false));
    }

    /** Takes {@code amount} FE out of {@code storage}. */
    public static Recoverable extract(final IEnergyStorage storage, final int amount) {
        return new AmountStep(amount,
                () -> storage.extractEnergy(amount, true),
                () -> storage.extractEnergy(amount, false),
                moved -> storage.receiveEnergy((int) moved, false));
    }

    /**
     * A step that can tell what it could not put back when it was undone, so the caller can place it somewhere
     * instead of letting it vanish.
     */
    public interface Recoverable extends Batch.IStep {

        /** The items the last undo could not give back to where they came from; empty when it all went back. */
        default List<ItemStack> unreturnedItems() {
            return List.of();
        }

        /** The fluid the last undo could not give back to where it came from; empty when it all went back. */
        default FluidStack unreturnedFluid() {
            return FluidStack.EMPTY;
        }
    }

    /* Puts a stack in slot after slot, remembering how many went into each. */
    private static final class Insert implements Recoverable {

        private final IItemHandler handler;
        private final ItemStack stack;
        private final List<int[]> placed = new ArrayList<>();

        private Insert(final IItemHandler handler, final ItemStack stack) {
            this.handler = handler;
            this.stack = stack;
        }

        @Override
        public long wants() {
            return this.stack.getCount();
        }

        @Override
        public long simulate() {
            ItemStack left = this.stack.copy();
            for (int slot = 0; slot < this.handler.getSlots() && !left.isEmpty(); slot++) {
                left = this.handler.insertItem(slot, left, true);
            }
            return this.stack.getCount() - left.getCount();
        }

        @Override
        public long execute() {
            this.placed.clear();
            ItemStack left = this.stack.copy();
            for (int slot = 0; slot < this.handler.getSlots() && !left.isEmpty(); slot++) {
                final int before = left.getCount();
                left = this.handler.insertItem(slot, left, false);
                if (left.getCount() < before) {
                    this.placed.add(new int[] {slot, before - left.getCount()});
                }
            }
            return this.stack.getCount() - left.getCount();
        }

        @Override
        public boolean undo() {
            boolean whole = true;
            for (int i = this.placed.size() - 1; i >= 0; i--) {
                final int[] put = this.placed.get(i);
                whole &= this.handler.extractItem(put[0], put[1], false).getCount() == put[1];
            }
            this.placed.clear();
            return whole;
        }
    }

    /* Takes items like a stack out slot after slot, remembering how many came from each. */
    private static final class Extract implements Recoverable {

        private final IItemHandler handler;
        private final ItemStack like;
        private final int count;
        private final List<ItemStack> taken = new ArrayList<>();
        private final List<Integer> from = new ArrayList<>();
        private final List<ItemStack> unreturned = new ArrayList<>();

        private Extract(final IItemHandler handler, final ItemStack like, final int count) {
            this.handler = handler;
            this.like = like;
            this.count = count;
        }

        @Override
        public long wants() {
            return this.count;
        }

        @Override
        public long simulate() {
            int found = 0;
            for (int slot = 0; slot < this.handler.getSlots() && found < this.count; slot++) {
                if (ItemStack.isSameItemSameComponents(this.handler.getStackInSlot(slot), this.like)) {
                    found += this.handler.extractItem(slot, this.count - found, true).getCount();
                }
            }
            return found;
        }

        @Override
        public long execute() {
            this.taken.clear();
            this.from.clear();
            this.unreturned.clear();
            int found = 0;
            for (int slot = 0; slot < this.handler.getSlots() && found < this.count; slot++) {
                if (ItemStack.isSameItemSameComponents(this.handler.getStackInSlot(slot), this.like)) {
                    final ItemStack got = this.handler.extractItem(slot, this.count - found, false);
                    if (!got.isEmpty()) {
                        this.taken.add(got);
                        this.from.add(slot);
                        found += got.getCount();
                    }
                }
            }
            return found;
        }

        @Override
        public boolean undo() {
            boolean whole = true;
            for (int i = this.taken.size() - 1; i >= 0; i--) {
                // The slot it came from may refuse it now (an output-only slot, or one that filled up again), so any
                // other slot gets a try before the items count as not returned.
                ItemStack left = this.handler.insertItem(this.from.get(i), this.taken.get(i), false);
                for (int slot = 0; slot < this.handler.getSlots() && !left.isEmpty(); slot++) {
                    left = this.handler.insertItem(slot, left, false);
                }
                if (!left.isEmpty()) {
                    this.unreturned.add(left);
                    whole = false;
                }
            }
            this.taken.clear();
            this.from.clear();
            return whole;
        }

        @Override
        public List<ItemStack> unreturnedItems() {
            return List.copyOf(this.unreturned);
        }
    }

    /* A step over a quantity: one call to ask, one to move, one to give back what moved. */
    private static class AmountStep implements Recoverable {

        private final long wanted;
        private final LongSupplier simulate;
        private final LongSupplier execute;
        private final LongUnaryOperator giveBack;
        private long moved;
        private long unreturned;

        private AmountStep(final long wanted, final LongSupplier simulate, final LongSupplier execute,
                           final LongUnaryOperator giveBack) {
            this.wanted = wanted;
            this.simulate = simulate;
            this.execute = execute;
            this.giveBack = giveBack;
        }

        @Override
        public long wants() {
            return this.wanted;
        }

        @Override
        public long simulate() {
            return this.simulate.getAsLong();
        }

        @Override
        public long execute() {
            this.unreturned = 0;
            this.moved = this.execute.getAsLong();
            return this.moved;
        }

        @Override
        public boolean undo() {
            if (this.moved == 0) {
                return true;
            }
            this.unreturned = Math.max(0, this.moved - this.giveBack.applyAsLong(this.moved));
            this.moved = 0;
            return this.unreturned == 0;
        }

        protected long unreturned() {
            return this.unreturned;
        }
    }

    /* A step over a fluid, which can say how much of it the last undo left unreturned. */
    private static final class FluidStep extends AmountStep {

        private final FluidStack fluid;

        private FluidStep(final FluidStack fluid, final LongSupplier simulate, final LongSupplier execute,
                          final LongUnaryOperator giveBack) {
            super(fluid.getAmount(), simulate, execute, giveBack);
            this.fluid = fluid;
        }

        @Override
        public FluidStack unreturnedFluid() {
            return unreturned() > 0 ? this.fluid.copyWithAmount((int) unreturned()) : FluidStack.EMPTY;
        }
    }
}
