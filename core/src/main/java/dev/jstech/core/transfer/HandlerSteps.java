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
    public static Batch.IStep insert(final IItemHandler handler, final ItemStack stack) {
        return new Insert(Objects.requireNonNull(handler, "handler"), stack.copy());
    }

    /** Takes {@code count} items like {@code like} (the same item and components) out of {@code handler}. */
    public static Batch.IStep extract(final IItemHandler handler, final ItemStack like, final int count) {
        return new Extract(Objects.requireNonNull(handler, "handler"), like.copyWithCount(1), count);
    }

    /** Fills {@code handler} with all of {@code fluid}. */
    public static Batch.IStep fill(final IFluidHandler handler, final FluidStack fluid) {
        final FluidStack wanted = fluid.copy();
        return new Batch.IStep() {
            private int moved;

            @Override
            public long wants() {
                return wanted.getAmount();
            }

            @Override
            public long simulate() {
                return handler.fill(wanted.copy(), IFluidHandler.FluidAction.SIMULATE);
            }

            @Override
            public long execute() {
                this.moved = handler.fill(wanted.copy(), IFluidHandler.FluidAction.EXECUTE);
                return this.moved;
            }

            @Override
            public boolean undo() {
                return this.moved == 0 || handler.drain(wanted.copyWithAmount(this.moved),
                        IFluidHandler.FluidAction.EXECUTE).getAmount() == this.moved;
            }
        };
    }

    /** Drains all of {@code fluid} out of {@code handler}. */
    public static Batch.IStep drain(final IFluidHandler handler, final FluidStack fluid) {
        final FluidStack wanted = fluid.copy();
        return new Batch.IStep() {
            private int moved;

            @Override
            public long wants() {
                return wanted.getAmount();
            }

            @Override
            public long simulate() {
                return handler.drain(wanted.copy(), IFluidHandler.FluidAction.SIMULATE).getAmount();
            }

            @Override
            public long execute() {
                this.moved = handler.drain(wanted.copy(), IFluidHandler.FluidAction.EXECUTE).getAmount();
                return this.moved;
            }

            @Override
            public boolean undo() {
                return this.moved == 0 || handler.fill(wanted.copyWithAmount(this.moved),
                        IFluidHandler.FluidAction.EXECUTE) == this.moved;
            }
        };
    }

    /** Gives {@code storage} {@code amount} FE. */
    public static Batch.IStep receive(final IEnergyStorage storage, final int amount) {
        return new Batch.IStep() {
            private int moved;

            @Override
            public long wants() {
                return amount;
            }

            @Override
            public long simulate() {
                return storage.receiveEnergy(amount, true);
            }

            @Override
            public long execute() {
                this.moved = storage.receiveEnergy(amount, false);
                return this.moved;
            }

            @Override
            public boolean undo() {
                return this.moved == 0 || storage.extractEnergy(this.moved, false) == this.moved;
            }
        };
    }

    /** Takes {@code amount} FE out of {@code storage}. */
    public static Batch.IStep extract(final IEnergyStorage storage, final int amount) {
        return new Batch.IStep() {
            private int moved;

            @Override
            public long wants() {
                return amount;
            }

            @Override
            public long simulate() {
                return storage.extractEnergy(amount, true);
            }

            @Override
            public long execute() {
                this.moved = storage.extractEnergy(amount, false);
                return this.moved;
            }

            @Override
            public boolean undo() {
                return this.moved == 0 || storage.receiveEnergy(this.moved, false) == this.moved;
            }
        };
    }

    /* Puts a stack in slot after slot, remembering how many went into each. */
    private static final class Insert implements Batch.IStep {

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
    private static final class Extract implements Batch.IStep {

        private final IItemHandler handler;
        private final ItemStack like;
        private final int count;
        private final List<ItemStack> taken = new ArrayList<>();
        private final List<Integer> from = new ArrayList<>();

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
                whole &= this.handler.insertItem(this.from.get(i), this.taken.get(i), false).isEmpty();
            }
            this.taken.clear();
            this.from.clear();
            return whole;
        }
    }
}
