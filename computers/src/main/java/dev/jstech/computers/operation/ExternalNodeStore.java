/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation;

import dev.jstech.computers.block.part.ExternalStorageBusPart;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.util.Sizes;
import net.minecraft.world.item.Item;

import java.util.Map;

/**
 * An {@link INodeStore} over the inventory an External Storage Bus faces: what its filter lets through, read and
 * written as its access allows. Its megabytes are counted as the network counts an item, four to one, a bucket of a
 * liquid or a gas weighing what an item does.
 */
final class ExternalNodeStore implements INodeStore {

    private final ExternalStorageBusPart bus;

    /** What an item takes, in megabytes, in storage that is not a drive's. */
    private static final long MB_PER_ITEM = 4L;

    ExternalNodeStore(final ExternalStorageBusPart bus) {
        this.bus = bus;
    }

    @Override
    public Map<StorageKey, Long> view() {
        return bus.visible();
    }

    @Override
    public long count(final StorageKey key) {
        return bus.count(key);
    }

    @Override
    public long count(final Item item) {
        return bus.count(StorageKey.of(item));
    }

    @Override
    public long extract(final StorageKey key, final long amount) {
        return bus.take(key, amount);
    }

    @Override
    public long insert(final StorageKey key, final long amount) {
        return bus.give(key, amount);
    }

    @Override
    public long capacity() {
        return used() + bus.room() / StorageKey.MB_EQ_PER_ITEM;
    }

    /* In items' worth: an item is one, a liquid or a gas a bucket's worth to one, as the room the bus has is told. */
    @Override
    public long used() {
        long weight = 0L;
        for (final Map.Entry<StorageKey, Long> held : bus.visible().entrySet()) {
            weight = Sizes.plus(weight, held.getKey().weight(held.getValue()));
        }
        return Math.ceilDiv(Math.max(0L, weight), StorageKey.MB_EQ_PER_ITEM);
    }

    @Override
    public long capacityMb() {
        return capacity() * MB_PER_ITEM;
    }

    @Override
    public long usedMb() {
        return used() * MB_PER_ITEM;
    }
}
