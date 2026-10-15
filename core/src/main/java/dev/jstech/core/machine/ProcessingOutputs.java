/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.blockentity.FieldFluidTank;
import dev.jstech.core.blockentity.FieldItemHandler;
import dev.jstech.core.material.Material;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.matter.MatterTank;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Where a processing machine's work goes: its output slots, its fluid output tanks and its output tanks of matter. A
 * machine starts a piece of work only when all of it fits at once, byproducts and all, so it never ends work it cannot
 * finish; and puts it out in the same order it was checked.
 *
 * @param inventory   the machine's inventory
 * @param firstSlot   the first output slot of the lane
 * @param endSlot     the slot after its last
 * @param fluidTanks  the fluid output tanks
 * @param matterTanks the output tanks of matter, every state's
 */
record ProcessingOutputs(FieldItemHandler inventory, int firstSlot, int endSlot, List<FieldFluidTank> fluidTanks,
                         List<MatterTank> matterTanks) {

    /** Whether {@code items}, {@code fluids} and {@code matter} all find room, as if all were put out at once. */
    boolean fit(final List<ItemStack> items, final List<FluidStack> fluids, final List<MatterStack> matter) {
        return itemsFit(items) && fluidsFit(fluids) && matterFits(matter);
    }

    /** Puts out {@code items}, {@code fluids} and {@code matter}, what finds no room lost: check first. */
    void put(final List<ItemStack> items, final List<FluidStack> fluids, final List<MatterStack> matter) {
        for (final ItemStack output : items) {
            ItemStack left = output.copy();
            for (int slot = this.firstSlot; slot < this.endSlot && !left.isEmpty(); slot++) {
                left = putInto(slot, left);
            }
        }
        for (final FluidStack output : fluids) {
            int left = output.getAmount();
            for (final FieldFluidTank tank : this.fluidTanks) {
                if (left <= 0) {
                    break;
                }
                left -= tank.fillInside(output.copyWithAmount(left), IFluidHandler.FluidAction.EXECUTE);
            }
        }
        for (final MatterStack output : matter) {
            long left = output.amount();
            for (final MatterTank tank : this.matterTanks) {
                if (left <= 0) {
                    break;
                }
                left -= tank.insert(output.withAmount(left), false);
            }
        }
    }

    /* Puts what it can of {@code stack} into the output slot, and gives back what is left. */
    private ItemStack putInto(final int slot, final ItemStack stack) {
        final ItemStack held = this.inventory.getStackInSlot(slot);
        final int moved = room(held, stack, slotLimit(slot, stack));
        if (moved <= 0) {
            return stack;
        }
        this.inventory.setStackInSlot(slot, held.isEmpty() ? stack.copyWithCount(moved)
                : held.copyWithCount(held.getCount() + moved));
        return stack.copyWithCount(stack.getCount() - moved);
    }

    private boolean itemsFit(final List<ItemStack> items) {
        final List<ItemStack> slots = new ArrayList<>();
        for (int slot = this.firstSlot; slot < this.endSlot; slot++) {
            slots.add(this.inventory.getStackInSlot(slot).copy());
        }
        for (final ItemStack output : items) {
            int left = output.getCount();
            for (int i = 0; i < slots.size() && left > 0; i++) {
                final ItemStack held = slots.get(i);
                final int moved = room(held, output.copyWithCount(left), slotLimit(this.firstSlot + i, output));
                if (moved <= 0) {
                    continue;
                }
                if (held.isEmpty()) {
                    slots.set(i, output.copyWithCount(moved));
                } else {
                    held.grow(moved);
                }
                left -= moved;
            }
            if (left > 0) {
                return false;
            }
        }
        return true;
    }

    private boolean fluidsFit(final List<FluidStack> fluids) {
        final List<FluidStack> tanks = new ArrayList<>();
        for (final FieldFluidTank tank : this.fluidTanks) {
            tanks.add(tank.getFluid().copy());
        }
        for (final FluidStack output : fluids) {
            int left = output.getAmount();
            for (int i = 0; i < tanks.size() && left > 0; i++) {
                final FluidStack held = tanks.get(i);
                final int moved = fluidRoom(held, output.copyWithAmount(left), this.fluidTanks.get(i).getCapacity());
                if (moved <= 0) {
                    continue;
                }
                if (held.isEmpty()) {
                    tanks.set(i, output.copyWithAmount(moved));
                } else {
                    held.grow(moved);
                }
                left -= moved;
            }
            if (left > 0) {
                return false;
            }
        }
        return true;
    }

    /* Each tank holds one material: an empty one takes the first that comes and fits its state. */
    private boolean matterFits(final List<MatterStack> matter) {
        final Material[] holding = new Material[this.matterTanks.size()];
        final long[] room = new long[this.matterTanks.size()];
        for (int i = 0; i < this.matterTanks.size(); i++) {
            final MatterTank tank = this.matterTanks.get(i);
            holding[i] = tank.held().map(MatterStack::material).orElse(null);
            room[i] = tank.capacity() - tank.held().map(MatterStack::amount).orElse(0L);
        }
        for (final MatterStack output : matter) {
            long left = output.amount();
            for (int i = 0; i < room.length && left > 0; i++) {
                final MatterTank tank = this.matterTanks.get(i);
                if (!output.material().takes(tank.state())
                        || holding[i] != null && holding[i] != output.material() || room[i] <= 0) {
                    continue;
                }
                final long moved = Math.min(room[i], left);
                holding[i] = output.material();
                room[i] -= moved;
                left -= moved;
            }
            if (left > 0) {
                return false;
            }
        }
        return true;
    }

    /* The most of {@code stack} an output slot holds: its own limit, and the stack's size. */
    private int slotLimit(final int slot, final ItemStack stack) {
        return Math.min(this.inventory.getSlotLimit(slot), stack.getMaxStackSize());
    }

    /* How many of {@code incoming} fit on {@code held} up to {@code limit}; the insert and the dry run share it. */
    private static int room(final ItemStack held, final ItemStack incoming, final int limit) {
        if (held.isEmpty()) {
            return Math.min(limit, incoming.getCount());
        }
        if (!ItemStack.isSameItemSameComponents(held, incoming)) {
            return 0;
        }
        return Math.max(0, Math.min(limit - held.getCount(), incoming.getCount()));
    }

    /* How many millibuckets of {@code incoming} fit in a tank holding {@code held} up to {@code capacity}. */
    private static int fluidRoom(final FluidStack held, final FluidStack incoming, final int capacity) {
        if (held.isEmpty()) {
            return Math.min(capacity, incoming.getAmount());
        }
        if (!FluidStack.isSameFluidSameComponents(held, incoming)) {
            return 0;
        }
        return Math.max(0, Math.min(capacity - held.getAmount(), incoming.getAmount()));
    }
}
