/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.blockentity.IFieldPart;
import java.util.Objects;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * Input slots locked to an item, so automation never fills them with the wrong thing: a locked slot takes only that
 * item, and its screen shows it as a ghost while the slot is empty. An unlocked slot takes anything its machine takes.
 */
public final class SlotLocks implements IFieldPart {

    private final NonNullList<ItemStack> locked;
    private final Runnable changed;

    private static final String KEY = "Locks";
    private static final String SLOT = "slot";
    private static final String ITEM = "item";

    /** Locks for {@code slots} slots, none locked yet, which tell {@code changed} when one is set. */
    public SlotLocks(final int slots, final Runnable changed) {
        this.locked = NonNullList.withSize(slots, ItemStack.EMPTY);
        this.changed = Objects.requireNonNull(changed, "changed");
    }

    /** How many slots it locks. */
    public int size() {
        return this.locked.size();
    }

    /** What {@code slot} is locked to; nothing for a slot that is not locked. */
    public ItemStack locked(final int slot) {
        return slot >= 0 && slot < this.locked.size() ? this.locked.get(slot) : ItemStack.EMPTY;
    }

    /** Locks {@code slot} to the item of {@code stack}; to nothing, unlocking it, for an empty stack. */
    public void lock(final int slot, final ItemStack stack) {
        if (slot < 0 || slot >= this.locked.size()) {
            return;
        }
        final ItemStack to = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        if (!ItemStack.matches(this.locked.get(slot), to)) {
            this.locked.set(slot, to);
            this.changed.run();
        }
    }

    /** Unlocks {@code slot}. */
    public void unlock(final int slot) {
        lock(slot, ItemStack.EMPTY);
    }

    /** Whether {@code slot} takes {@code stack}: anything when unlocked, only its item when locked. */
    public boolean allows(final int slot, final ItemStack stack) {
        final ItemStack lock = locked(slot);
        return lock.isEmpty() || ItemStack.isSameItemSameComponents(lock, stack);
    }

    @Override
    public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
        final ListTag list = new ListTag();
        for (int slot = 0; slot < this.locked.size(); slot++) {
            if (!this.locked.get(slot).isEmpty()) {
                final CompoundTag one = new CompoundTag();
                one.putInt(SLOT, slot);
                one.put(ITEM, this.locked.get(slot).save(registries));
                list.add(one);
            }
        }
        tag.put(KEY, list);
    }

    @Override
    public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
        for (int slot = 0; slot < this.locked.size(); slot++) {
            this.locked.set(slot, ItemStack.EMPTY);
        }
        final ListTag list = tag.getList(KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            final CompoundTag one = list.getCompound(i);
            final int slot = one.getInt(SLOT);
            if (slot >= 0 && slot < this.locked.size()) {
                this.locked.set(slot, ItemStack.parseOptional(registries, one.getCompound(ITEM)));
            }
        }
    }

    /** Takes the locks {@code tag}, written by {@link #save} on a machine of the same kind, says. */
    public void paste(final CompoundTag tag, final HolderLookup.Provider registries) {
        load(tag, registries);
        this.changed.run();
    }

    @Override
    public void writeClient(final CompoundTag tag, final HolderLookup.Provider registries) {
        save(tag, registries);
    }

    @Override
    public void readClient(final CompoundTag tag, final HolderLookup.Provider registries) {
        load(tag, registries);
    }
}
