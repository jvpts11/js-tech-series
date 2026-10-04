/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.crafting;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.storage.IDataSink;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.persistence.SavedValue;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;

/**
 * A pool of its own, kept apart from the network, where another craft's planner could take a half-made intermediate:
 * what is put in is held here, and what is asked for comes from here first, then the network. A pipeline passes what
 * one machine stage makes to the next through one; a machine step that was part of a craft when the world was saved
 * holds what it makes in one until it settles, when everything held goes into the network at once, a tick before the
 * craft is planned again.
 */
public final class HeldIo implements ICraftIo {

    private final ServerLevel level;
    private final NetworkUuid network;
    private final Map<StorageKey, Long> held = new LinkedHashMap<>();

    public HeldIo(final ServerLevel level, final NetworkUuid network) {
        this.level = level;
        this.network = network;
    }

    @Override
    public long select(final StorageKey key, final long amount, final IDataSink into) {
        long moved = 0L;
        final long pooled = held.getOrDefault(key, 0L);
        if (pooled > 0L) {
            final long inserted = into.insert(key, Math.min(amount, pooled), false);
            if (inserted > 0L) {
                held.merge(key, -inserted, Long::sum);
                moved += inserted;
            }
        }
        if (moved < amount) {
            moved += NetworkStorage.of(level, network).select(key, amount - moved, into);
        }
        return moved;
    }

    @Override
    public long insert(final StorageKey key, final long amount) {
        if (amount > 0L) {
            held.merge(key, amount, Long::sum);
        }
        return amount;
    }

    /** Puts everything held into the network; what the network has no room for stays held. */
    public void flush() {
        final NetworkStorage storage = NetworkStorage.of(level, network);
        held.replaceAll((key, amount) -> amount <= 0L ? 0L : amount - storage.insert(key, amount));
        held.values().removeIf(amount -> amount <= 0L);
    }

    /** Whether it holds nothing. */
    public boolean isEmpty() {
        return held.values().stream().noneMatch(amount -> amount > 0L);
    }

    /** What it holds, as a save keeps it. */
    public ListTag save(final HolderLookup.Provider registries) {
        final RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries);
        final ListTag rows = new ListTag();
        held.forEach((key, amount) -> {
            if (amount > 0L) {
                StorageKey.CODEC.encodeStart(ops, key).result().ifPresent(keyTag -> {
                    final CompoundTag row = new CompoundTag();
                    row.put("Key", keyTag);
                    row.putLong("Amount", amount);
                    rows.add(row);
                });
            }
        });
        return rows;
    }

    /** Puts back what a save kept. */
    public void load(final ListTag rows, final HolderLookup.Provider registries) {
        final RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries);
        for (int i = 0; i < rows.size(); i++) {
            final CompoundTag row = rows.getCompound(i);
            final StorageKey key = SavedValue.readOr(StorageKey.CODEC.parse(ops, row.get("Key")), JsComputers.LOGGER,
                    "what a pipeline held between its stages", null);
            if (key != null) {
                insert(key, row.getLong("Amount"));
            }
        }
    }
}
