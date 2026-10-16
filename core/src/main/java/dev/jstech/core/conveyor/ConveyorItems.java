/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.conveyor;

import dev.jstech.core.blockentity.IFieldPart;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * The items on a block of conveyor, as its block entity keeps them: the lane of them in order along the block, saved
 * whole, and sent to the players who see it with how fast the belt moved when they were sent, so a player's game moves
 * them on by itself between one word from the server and the next.
 */
public final class ConveyorItems implements IFieldPart {

    private final BeltLane<ItemStack> lane;
    /* How fast the belt moved, in blocks a tick, when the items were last sent; on a player's game, as sent. */
    private double speed;
    /* On a player's game, the game time the items were last sent at, from which they are moved on. */
    private long sentAt;
    /* The item a machine above holds still while it works it, by its place in the lane; none when below 0. */
    private int held = NONE_HELD;

    /** Where the items are saved. */
    public static final String KEY = "Belt";
    /** What {@link #held()} says when no item is held. */
    public static final int NONE_HELD = -1;
    private static final String ITEMS = "Items";
    private static final String ITEM = "Item";
    private static final String AT = "At";
    private static final String SPEED = "Speed";
    private static final String HELD = "Held";

    public ConveyorItems(final double gap) {
        this.lane = new BeltLane<>(gap);
    }

    /** The items, the first along the belt first. */
    public BeltLane<ItemStack> lane() {
        return this.lane;
    }

    /** How fast the belt moved when the items were last sent, in blocks a tick. */
    public double speed() {
        return this.speed;
    }

    /** Sets how fast the belt moves, in blocks a tick, sent with the items. */
    public void moving(final double blocksATick) {
        this.speed = blocksATick;
    }

    /** On a player's game, the game time the items were last sent at. */
    public long sentAt() {
        return this.sentAt;
    }

    /** On a player's game, notes the game time the items arrived at. */
    public void arrivedAt(final long gameTime) {
        this.sentAt = gameTime;
    }

    /** The place in the lane of the item a machine above holds still, sent with the items; {@link #NONE_HELD}: none. */
    public int held() {
        return this.held;
    }

    /** Sets the item a machine above holds still, by its place in the lane, or {@link #NONE_HELD}. */
    public void hold(final int index) {
        this.held = index;
    }

    @Override
    public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
        tag.put(ITEMS, write(registries));
    }

    @Override
    public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
        read(tag.getList(ITEMS, Tag.TAG_COMPOUND), registries);
    }

    @Override
    public void writeClient(final CompoundTag tag, final HolderLookup.Provider registries) {
        tag.put(ITEMS, write(registries));
        tag.putDouble(SPEED, this.speed);
        tag.putInt(HELD, this.held);
    }

    @Override
    public void readClient(final CompoundTag tag, final HolderLookup.Provider registries) {
        read(tag.getList(ITEMS, Tag.TAG_COMPOUND), registries);
        this.speed = tag.getDouble(SPEED);
        this.held = tag.contains(HELD) ? tag.getInt(HELD) : NONE_HELD;
    }

    private ListTag write(final HolderLookup.Provider registries) {
        final ListTag list = new ListTag();
        for (final BeltLane.Entry<ItemStack> entry : this.lane.entries()) {
            final CompoundTag one = new CompoundTag();
            one.put(ITEM, entry.item().save(registries));
            one.putDouble(AT, entry.at());
            list.add(one);
        }
        return list;
    }

    /* An item that no longer reads, of a mod that is gone, is not kept. */
    private void read(final ListTag list, final HolderLookup.Provider registries) {
        final List<BeltLane.Entry<ItemStack>> read = new ArrayList<>(list.size());
        for (final Tag element : list) {
            final CompoundTag one = (CompoundTag) element;
            ItemStack.parse(registries, one.getCompound(ITEM))
                    .ifPresent(stack -> read.add(new BeltLane.Entry<>(stack, one.getDouble(AT))));
        }
        while (!this.lane.isEmpty()) {
            this.lane.takeFirst();
        }
        for (final BeltLane.Entry<ItemStack> entry : read) {
            this.lane.put(entry.item(), entry.at());
        }
    }
}
