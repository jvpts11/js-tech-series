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
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.ToIntBiFunction;

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
    private ToIntBiFunction<Integer, ItemStack> stackLimit = (slot, stack) -> STACK;
    private BooleanSupplier locked = () -> false;
    private boolean exposed;
    private boolean drops;
    private boolean beyondTheStack;

    /** The vanilla stack size, which a slot limit never exceeds. */
    private static final int STACK = 64;
    /* The save's keys: the items, each with its slot and, past what the game saves of one stack, how many it holds. */
    private static final String ITEMS = "Items";
    private static final String SIZE = "Size";
    private static final String SLOT = "Slot";
    private static final String HELD = "Held";

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

    /**
     * The most of an item a slot holds, besides the slot's own limit: an upgrade slot takes no more of an upgrade than
     * the machine has room for.
     */
    public FieldItemHandler stackLimit(final ToIntBiFunction<Integer, ItemStack> rule) {
        stackLimit = rule;
        return this;
    }

    /**
     * Lets a slot hold more of an item than the item's own stack, as many as {@link #stackLimit} says, as a machine
     * whose upgrades give its slots more room does. What a slot holds past the most the game saves of one stack is
     * saved beside it.
     */
    public FieldItemHandler beyondTheStack() {
        beyondTheStack = true;
        return this;
    }

    /** The most of {@code stack} slot {@code slot} holds: its own limit, the item's stack and the declared rule. */
    public int stackLimitOf(final int slot, final ItemStack stack) {
        return getStackLimit(slot, stack);
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
        // Beyond the stack, what the rule gives a stack of the game's size: what the slot holds, read from outside.
        return beyondTheStack ? Math.max(STACK, stackLimit.applyAsInt(slot, ItemStack.EMPTY))
                : Math.min(slotLimit, super.getSlotLimit(slot));
    }

    @Override
    public boolean isItemValid(final int slot, final ItemStack stack) {
        return accepts.test(slot, stack);
    }

    @Override
    public CompoundTag serializeNBT(final HolderLookup.Provider registries) {
        final ListTag items = new ListTag();
        for (int slot = 0; slot < stacks.size(); slot++) {
            final ItemStack stack = stacks.get(slot);
            if (!stack.isEmpty()) {
                final CompoundTag item = new CompoundTag();
                item.putInt(SLOT, slot);
                if (stack.getCount() > Item.ABSOLUTE_MAX_STACK_SIZE) {
                    item.putInt(HELD, stack.getCount());
                }
                items.add(stack.copyWithCount(Math.min(stack.getCount(), Item.ABSOLUTE_MAX_STACK_SIZE))
                        .save(registries, item));
            }
        }
        final CompoundTag saved = new CompoundTag();
        saved.put(ITEMS, items);
        saved.putInt(SIZE, stacks.size());
        return saved;
    }

    @Override
    public void deserializeNBT(final HolderLookup.Provider registries, final CompoundTag saved) {
        setSize(saved.contains(SIZE, Tag.TAG_INT) ? saved.getInt(SIZE) : stacks.size());
        final ListTag items = saved.getList(ITEMS, Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            final CompoundTag item = items.getCompound(i);
            final int slot = item.getInt(SLOT);
            if (slot >= 0 && slot < stacks.size()) {
                ItemStack.parse(registries, item).ifPresent(stack -> stacks.set(slot, item.contains(HELD, Tag.TAG_INT)
                        ? stack.copyWithCount(item.getInt(HELD)) : stack));
            }
        }
        onLoad();
    }

    @Override
    protected int getStackLimit(final int slot, final ItemStack stack) {
        final int rule = stackLimit.applyAsInt(slot, stack);
        return Math.max(0, beyondTheStack ? rule : Math.min(super.getStackLimit(slot, stack), rule));
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
    @ApiStatus.Internal
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
