/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.blockentity;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

/**
 * The items a block entity holds, declared with where they go: {@link #save()}, {@link #toClient()}, and two more an
 * inventory has: {@link #exposed()}, offered to pipes and hoppers on every side, and {@link #dropsWhenBroken()},
 * spilled on the ground when the block goes. What a slot takes and how many is declared the same way.
 */
public final class FieldItemHandler extends ItemStackHandler implements IField {

    private final FieldFlags flags;
    private final List<IntConsumer> changeListeners = new ArrayList<>();
    private final List<Runnable> loadListeners = new ArrayList<>();
    private int slotLimit = STACK;
    private BiPredicate<Integer, ItemStack> accepts = (slot, stack) -> true;
    private BooleanSupplier locked = () -> false;
    private boolean exposed;
    private boolean drops;

    /** The vanilla stack size, which a slot limit never exceeds. */
    private static final int STACK = 64;

    FieldItemHandler(final FieldFlags flags, final int slots) {
        super(slots);
        this.flags = flags;
    }

    /** Keeps the items in the save. */
    public FieldItemHandler save() {
        flags.save();
        return this;
    }

    /** Sends the items to the players who see the block. */
    public FieldItemHandler toClient() {
        flags.toClient();
        return this;
    }

    /** Offers the items to pipes, hoppers and machines on every side. */
    public FieldItemHandler exposed() {
        exposed = true;
        return this;
    }

    /** Spills the items on the ground when the block is broken or replaced. */
    public FieldItemHandler dropsWhenBroken() {
        drops = true;
        return this;
    }

    /** The most items any slot holds. */
    public FieldItemHandler slotLimit(final int limit) {
        slotLimit = limit;
        return this;
    }

    /** Which items which slot takes; a slot that takes nothing can still be filled by the block entity itself. */
    public FieldItemHandler accepts(final BiPredicate<Integer, ItemStack> rule) {
        accepts = rule;
        return this;
    }

    /** Gives nothing out while {@code rule} holds, as a burner keeps the disc it is writing. */
    public FieldItemHandler lockedWhile(final BooleanSupplier rule) {
        locked = rule;
        return this;
    }

    /** Runs whenever a slot's contents change, with that slot, on whichever side changed it. */
    public FieldItemHandler onChange(final IntConsumer listener) {
        changeListeners.add(listener);
        return this;
    }

    /** Runs once the items are read back from a save or from the server. */
    public FieldItemHandler onLoad(final Runnable listener) {
        loadListeners.add(listener);
        return this;
    }

    boolean isExposed() {
        return exposed;
    }

    boolean dropsOnBreak() {
        return drops;
    }

    @Override
    public int getSlotLimit(final int slot) {
        return Math.min(slotLimit, super.getSlotLimit(slot));
    }

    @Override
    public boolean isItemValid(final int slot, final ItemStack stack) {
        return accepts.test(slot, stack);
    }

    @Override
    public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
        return locked.getAsBoolean() ? ItemStack.EMPTY : super.extractItem(slot, amount, simulate);
    }

    @Override
    protected void onContentsChanged(final int slot) {
        flags.changed();
        for (final IntConsumer listener : changeListeners) {
            listener.accept(slot);
        }
    }

    @Override
    protected void onLoad() {
        for (final Runnable listener : loadListeners) {
            listener.run();
        }
    }

    @Override
    public FieldFlags flags() {
        return flags;
    }

    @Override
    public void write(final CompoundTag tag, final HolderLookup.Provider registries) {
        tag.put(flags.key(), serializeNBT(registries));
    }

    @Override
    public void read(final CompoundTag tag, final HolderLookup.Provider registries) {
        deserializeNBT(registries, tag.getCompound(flags.key()));
    }

    @Override
    public int menuSlots() {
        return 0;
    }
}
