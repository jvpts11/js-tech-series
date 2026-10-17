/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.room;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * The sealed rooms of a dimension, kept with the world: each room, which room each block of air belongs to, and the
 * places a block asked to know the room of, where a room is looked for again when the blocks round them change.
 */
public final class RoomStore extends SavedData {

    private final Long2ObjectOpenHashMap<Room> rooms = new Long2ObjectOpenHashMap<>();
    private final Long2LongOpenHashMap index = new Long2LongOpenHashMap();
    private final LongOpenHashSet seeds = new LongOpenHashSet();
    private long nextId = 1;

    /** The file the rooms are kept in. */
    public static final String FILE = "jscore_rooms";
    private static final String ROOMS = "Rooms";
    private static final String SEEDS = "Seeds";
    private static final String NEXT = "Next";
    private static final long NONE = -1L;

    private RoomStore() {
        this.index.defaultReturnValue(NONE);
    }

    /** The rooms of {@code level}, made when first asked for. */
    public static RoomStore of(final ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(RoomStore::new, RoomStore::load), FILE);
    }

    /** The rooms of {@code level} when it keeps any, without making them; none for a level that never had one. */
    public static @Nullable RoomStore find(final ServerLevel level) {
        return level.getDataStorage().get(new SavedData.Factory<>(RoomStore::new, RoomStore::load), FILE);
    }

    /** Every room. */
    public Collection<Room> rooms() {
        return this.rooms.values();
    }

    /** The room {@code id}, if there is one. */
    public @Nullable Room room(final long id) {
        return this.rooms.get(id);
    }

    /** The room the block of air {@code cell} belongs to, if any. */
    public @Nullable Room roomAt(final long cell) {
        final long id = this.index.get(cell);
        return id == NONE ? null : this.rooms.get(id);
    }

    /** Whether it holds no room and no place asking for one: nothing to do. */
    public boolean idle() {
        return this.rooms.isEmpty() && this.seeds.isEmpty();
    }

    /** The places blocks asked to know the room of. */
    public LongSet seeds() {
        return this.seeds;
    }

    /** Remembers {@code cell} as a place a block asked to know the room of. */
    public void seed(final long cell) {
        if (this.seeds.add(cell)) {
            setDirty();
        }
    }

    /** Forgets {@code cell} as a place asking for a room. */
    public void unseed(final long cell) {
        if (this.seeds.remove(cell)) {
            setDirty();
        }
    }

    /** A new room, with no blocks yet. */
    Room create() {
        final Room room = new Room(this.nextId++);
        this.rooms.put(room.id(), room);
        setDirty();
        return room;
    }

    /** Gives {@code room} the blocks it holds now, in place of {@code before}. */
    void reindex(final Room room, final LongSet before) {
        final LongIterator old = before.iterator();
        while (old.hasNext()) {
            final long cell = old.nextLong();
            if (this.index.get(cell) == room.id()) {
                this.index.remove(cell);
            }
        }
        final LongIterator now = room.cells().iterator();
        while (now.hasNext()) {
            this.index.put(now.nextLong(), room.id());
        }
        setDirty();
    }

    /** Takes {@code room} away, and its blocks with it. */
    void remove(final Room room) {
        final LongIterator each = room.cells().iterator();
        while (each.hasNext()) {
            final long cell = each.nextLong();
            if (this.index.get(cell) == room.id()) {
                this.index.remove(cell);
            }
        }
        this.rooms.remove(room.id());
        setDirty();
    }

    @Override
    public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
        final ListTag kept = new ListTag();
        for (final Room room : this.rooms.values()) {
            kept.add(room.save());
        }
        tag.put(ROOMS, kept);
        tag.put(SEEDS, new LongArrayTag(this.seeds.toLongArray()));
        tag.putLong(NEXT, this.nextId);
        return tag;
    }

    private static RoomStore load(final CompoundTag tag, final HolderLookup.Provider registries) {
        final RoomStore store = new RoomStore();
        final List<Room> loaded = new ArrayList<>();
        for (final Tag each : tag.getList(ROOMS, Tag.TAG_COMPOUND)) {
            loaded.add(Room.load((CompoundTag) each));
        }
        for (final Room room : loaded) {
            store.rooms.put(room.id(), room);
            final LongIterator cells = room.cells().iterator();
            while (cells.hasNext()) {
                store.index.put(cells.nextLong(), room.id());
            }
        }
        for (final long seed : tag.getLongArray(SEEDS)) {
            store.seeds.add(seed);
        }
        store.nextId = Math.max(1, tag.getLong(NEXT));
        return store;
    }
}
