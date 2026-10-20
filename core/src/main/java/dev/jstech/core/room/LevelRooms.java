/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.room;

import dev.jstech.core.util.Loaded;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

/**
 * The sealed rooms of one dimension at work: the rooms waiting to be worked out again, the places asking for a room,
 * the search under way, and what the breathing in each room took this second. Every block that changes is noted; a
 * change that can change a room's shape sends it to be worked out again, a little each tick.
 */
final class LevelRooms {

    private final ServerLevel level;
    private @Nullable RoomStore store;
    private final LongLinkedOpenHashSet dirty = new LongLinkedOpenHashSet();
    private final LongLinkedOpenHashSet seedsDue = new LongLinkedOpenHashSet();
    /* When each place found open air may be looked at again, and how long it waits after the next try. */
    private final Long2LongOpenHashMap seedNext = new Long2LongOpenHashMap();
    private final Long2LongOpenHashMap seedWait = new Long2LongOpenHashMap();
    private final LongOpenHashSet changedSections = new LongOpenHashSet();
    /* The rooms and the places asking for one whose search met a chunk not loaded whole, by that chunk. */
    private final Long2ObjectOpenHashMap<LongOpenHashSet> roomsWaiting = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<LongOpenHashSet> seedsWaiting = new Long2ObjectOpenHashMap<>();
    private long searchingSeed = -1;
    private final Long2DoubleOpenHashMap breath = new Long2DoubleOpenHashMap();
    private final LongArrayList fireChecks = new LongArrayList();
    private @Nullable RoomFinder search;
    /* Whether the kit itself is changing blocks, so its own changes do not send rooms to be worked out again. */
    private boolean changing;

    /* How near a place asking for a room a change must be for the room to be looked for there again, in sections. */
    private static final int SEED_SECTIONS = 2;
    /* How long a place found open air first waits before its room is looked for again, and at most, in ticks. */
    private static final long SEED_WAIT = 20;
    private static final long SEED_WAIT_MOST = 1_200;
    /* How long the kit may work rooms out in a tick, in nanoseconds, and how many blocks it reads between looks. */
    private static final long WORK_NANOS = 1_500_000;
    private static final int WORK_STEP = 512;

    LevelRooms(final ServerLevel level) {
        this.level = level;
        this.store = RoomStore.find(level);
        if (this.store != null) {
            for (final Room room : this.store.rooms()) {
                this.dirty.add(room.id());
            }
            this.seedsDue.addAll(this.store.seeds());
        }
    }

    ServerLevel level() {
        return this.level;
    }

    /** The rooms, when the dimension keeps any. */
    @Nullable RoomStore store() {
        return this.store;
    }

    /** The rooms, made when first needed. */
    RoomStore storeMade() {
        if (this.store == null) {
            this.store = RoomStore.of(this.level);
        }
        return this.store;
    }

    /** Whether the kit itself is changing blocks. */
    boolean changing() {
        return this.changing;
    }

    /** Changes the block at {@code pos} as the kit, so the change is not taken for the world's. */
    void set(final BlockPos pos, final BlockState state) {
        this.changing = true;
        try {
            this.level.setBlockAndUpdate(pos, state);
        } finally {
            this.changing = false;
        }
    }

    /** Asks for the room at {@code cell} to be known, and looked for now. */
    void seed(final long cell) {
        storeMade().seed(cell);
        this.seedNext.remove(cell);
        this.seedWait.remove(cell);
        this.seedsDue.add(cell);
    }

    /** Sends the room {@code id} to be worked out again. */
    void dirty(final long id) {
        this.dirty.add(id);
    }

    /**
     * Forgets the rooms reaching into {@code area} and the places in it asking for one, and drops the search under
     * way, which may be finding one of them; how many rooms were forgotten.
     */
    int forget(final AABB area) {
        final RoomStore rooms = this.store;
        if (rooms == null) {
            return 0;
        }
        this.search = null;
        int forgotten = 0;
        for (final Room room : rooms.rooms().toArray(new Room[0])) {
            if (room.bounds().intersects(area)) {
                rooms.remove(room);
                NeoForge.EVENT_BUS.post(new RoomEvent.Removed(this.level, room));
                forgotten++;
            }
        }
        for (final long seed : rooms.seeds().toLongArray()) {
            if (area.contains(Vec3.atCenterOf(BlockPos.of(seed)))) {
                rooms.unseed(seed);
                this.seedsDue.remove(seed);
                this.seedNext.remove(seed);
                this.seedWait.remove(seed);
            }
        }
        return forgotten;
    }

    /** Adds {@code moles} of oxygen breathed in the room {@code id} this second. */
    void breathe(final long id, final double moles) {
        this.breath.addTo(id, moles);
    }

    /** Takes what was breathed in the room {@code id} this second, and starts counting again. */
    double breathed(final long id) {
        return this.breath.remove(id);
    }

    /** Notes a block of the dimension changed from {@code before} to {@code after}. */
    void note(final BlockPos pos, final BlockState before, final BlockState after) {
        if (after.getBlock() instanceof BaseFireBlock && !(before.getBlock() instanceof BaseFireBlock)) {
            this.fireChecks.add(pos.asLong());
        }
        final RoomStore rooms = this.store;
        if (rooms == null || rooms.idle()) {
            return;
        }
        final long cell = pos.asLong();
        final Room inside = rooms.roomAt(cell);
        if (inside != null) {
            final Seal was = Seals.of(this.level, pos, before);
            final Seal now = Seals.of(this.level, pos, after);
            if (was.wall() || now.wall()) {
                dirty(inside.id());
            } else {
                counted(inside, cell, before, after);
            }
        } else if (before.getBlock() != after.getBlock() || Seals.open(before) != Seals.open(after)) {
            // A wall, a door or a hole beside a room changed: what bounds the room there changed.
            for (final Direction side : Direction.values()) {
                final Room beside = rooms.roomAt(BlockPos.offset(cell, side));
                if (beside != null) {
                    dirty(beside.id());
                }
            }
        }
        // Only where the world changed is noted; which places asking for a room it was near is weighed once a second.
        if (!rooms.seeds().isEmpty()) {
            this.changedSections.add(SectionPos.asLong(pos));
        }
    }

    /**
     * A second of the places asking for a room: each that no room holds, the world changed near and whose wait is over
     * is looked for again.
     */
    void second() {
        final RoomStore rooms = this.store;
        if (rooms == null || this.changedSections.isEmpty()) {
            this.changedSections.clear();
            return;
        }
        final long now = this.level.getGameTime();
        final LongIterator seeds = rooms.seeds().iterator();
        while (seeds.hasNext()) {
            final long seed = seeds.nextLong();
            if (rooms.roomAt(seed) == null && now >= this.seedNext.getOrDefault(seed, 0L) && changedNear(seed)) {
                this.seedsDue.add(seed);
            }
        }
        this.changedSections.clear();
    }

    /* Whether the world changed in a section within reach of {@code seed}. */
    private boolean changedNear(final long seed) {
        final int sx = SectionPos.blockToSectionCoord(BlockPos.getX(seed));
        final int sy = SectionPos.blockToSectionCoord(BlockPos.getY(seed));
        final int sz = SectionPos.blockToSectionCoord(BlockPos.getZ(seed));
        for (int x = -SEED_SECTIONS; x <= SEED_SECTIONS; x++) {
            for (int y = -SEED_SECTIONS; y <= SEED_SECTIONS; y++) {
                for (int z = -SEED_SECTIONS; z <= SEED_SECTIONS; z++) {
                    if (this.changedSections.contains(SectionPos.asLong(sx + x, sy + y, sz + z))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /* Counts what changed inside a room without changing its shape: water, lava, fire and plants. */
    private void counted(final Room room, final long cell, final BlockState before, final BlockState after) {
        final boolean wasWater = RoomLiving.water(before);
        final boolean isWater = RoomLiving.water(after);
        if (wasWater != isWater) {
            room.wet(isWater);
        }
        final boolean wasFire = before.getBlock() instanceof BaseFireBlock;
        final boolean isFire = after.getBlock() instanceof BaseFireBlock;
        if (wasFire != isFire) {
            room.fire(cell, isFire);
        }
        final boolean wasPlant = RoomLiving.plant(before);
        final boolean isPlant = RoomLiving.plant(after);
        if (wasPlant != isPlant) {
            room.plant(isPlant);
        }
    }

    /**
     * Sends every room near the chunk {@code chunk} to be worked out again, now that it is loaded, and every search
     * that waited for it to start again, its wait over.
     */
    void loaded(final ChunkPos chunk) {
        final RoomStore rooms = this.store;
        if (rooms == null) {
            return;
        }
        for (final Room room : rooms.rooms()) {
            final int minX = ((int) Math.floor(room.bounds().minX) >> 4) - 1;
            final int maxX = ((int) Math.floor(room.bounds().maxX) >> 4) + 1;
            final int minZ = ((int) Math.floor(room.bounds().minZ) >> 4) - 1;
            final int maxZ = ((int) Math.floor(room.bounds().maxZ) >> 4) + 1;
            if (chunk.x >= minX && chunk.x <= maxX && chunk.z >= minZ && chunk.z <= maxZ) {
                dirty(room.id());
            }
        }
        final LongOpenHashSet waitedRooms = this.roomsWaiting.remove(chunk.toLong());
        if (waitedRooms != null) {
            waitedRooms.forEach(this::dirty);
        }
        final LongOpenHashSet waitedSeeds = this.seedsWaiting.remove(chunk.toLong());
        if (waitedSeeds != null) {
            waitedSeeds.forEach(seed -> {
                this.seedNext.remove(seed);
                this.seedsDue.add(seed);
            });
        }
    }

    /** Works on the rooms waiting, reading at most {@code budget} blocks and for no longer than its share of a tick. */
    void work(final int budget) {
        checkFires();
        final long started = System.nanoTime();
        int left = budget;
        while (left > 0 && System.nanoTime() - started < WORK_NANOS) {
            if (this.search == null) {
                this.search = next();
                if (this.search == null) {
                    return;
                }
            }
            left -= this.search.work(Math.min(left, WORK_STEP));
            if (this.search.waiting()) {
                // It starts again when the chunk it met is loaded whole.
                final Long2ObjectOpenHashMap<LongOpenHashSet> waits = this.search.room() < 0 ? this.seedsWaiting
                        : this.roomsWaiting;
                waits.computeIfAbsent(this.search.waitedOn(), chunk -> new LongOpenHashSet())
                        .add(this.search.room() < 0 ? this.searchingSeed : this.search.room());
                this.search = null;
            } else if (this.search.done()) {
                final RoomFinder done = this.search;
                this.search = null;
                apply(done);
            }
        }
    }

    /* The next search: a room waiting to be worked out again, or a place asking for its room. */
    private @Nullable RoomFinder next() {
        final RoomStore rooms = this.store;
        if (rooms == null) {
            return null;
        }
        while (!this.dirty.isEmpty()) {
            final Room room = rooms.room(this.dirty.removeFirstLong());
            if (room != null) {
                final LongOpenHashSet before = new LongOpenHashSet(room.cells());
                return new RoomFinder(this.level, room.id(), new LongArrayList(before.toLongArray()), before,
                        RoomBalance.largestRoom());
            }
        }
        final long now = this.level.getGameTime();
        while (!this.seedsDue.isEmpty()) {
            final long seed = this.seedsDue.removeFirstLong();
            if (!rooms.seeds().contains(seed) || rooms.roomAt(seed) != null
                    || now < this.seedNext.getOrDefault(seed, 0L)) {
                continue;
            }
            // Looked for again no sooner than its wait, which doubles each time it finds open air.
            final long wait = this.seedWait.getOrDefault(seed, SEED_WAIT);
            this.seedNext.put(seed, now + wait);
            this.seedWait.put(seed, Math.min(SEED_WAIT_MOST, wait * 2));
            this.searchingSeed = seed;
            final LongArrayList start = new LongArrayList();
            start.add(seed);
            return new RoomFinder(this.level, -1, start, new LongOpenHashSet(), RoomBalance.largestRoom());
        }
        return null;
    }

    /* What a finished search found, made into rooms. */
    private void apply(final RoomFinder found) {
        final RoomStore rooms = storeMade();
        if (found.room() < 0) {
            // A room found, its place waits no longer the next time it is open air.
            if (found.parts().stream().anyMatch(part -> !part.open())) {
                this.seedNext.remove(this.searchingSeed);
                this.seedWait.remove(this.searchingSeed);
            }
            for (final RoomFinder.Part part : found.parts()) {
                if (!part.open() && !part.cells().isEmpty()
                        && rooms.roomAt(part.cells().iterator().nextLong()) == null) {
                    form(rooms, part);
                }
            }
            return;
        }
        final Room room = rooms.room(found.room());
        if (room == null) {
            return;
        }
        final List<RoomFinder.Part> parts = found.parts();
        if (parts.isEmpty()) {
            rooms.remove(room);
            NeoForge.EVENT_BUS.post(new RoomEvent.Removed(this.level, room));
            return;
        }
        reshape(rooms, room, parts);
    }

    /* A new room from a closed part: dry if it stands under water, and full of the place's air. */
    private void form(final RoomStore rooms, final RoomFinder.Part part) {
        final Room room = rooms.create();
        room.shape(part.cells(), part.faces(), part.water(), part.lava(), part.plants(), part.fires(), false);
        rooms.reindex(room, new LongOpenHashSet());
        if (room.underWater()) {
            pump(room, room.cells());
        }
        fill(room);
        NeoForge.EVENT_BUS.post(new RoomEvent.Formed(this.level, room));
    }

    /*
     * A room worked out again: the part holding most of it keeps it, the rest of its parts become rooms of their own
     * with their share of its gas, and a closed part takes in every other room it now holds.
     */
    private void reshape(final RoomStore rooms, final Room room, final List<RoomFinder.Part> parts) {
        final LongOpenHashSet before = new LongOpenHashSet(room.cells());
        final Map<ResourceLocation, Double> gas = room.gases();
        final double leakedBefore = leakArea(rooms, room);
        final boolean wasBreached = room.breached();
        int keeper = 0;
        int most = -1;
        final int[] overlap = new int[parts.size()];
        for (int i = 0; i < parts.size(); i++) {
            final LongIterator each = parts.get(i).cells().iterator();
            while (each.hasNext()) {
                if (before.contains(each.nextLong())) {
                    overlap[i]++;
                }
            }
            if (overlap[i] > most) {
                most = overlap[i];
                keeper = i;
            }
        }
        final int total = Math.max(1, before.size());
        for (int i = 0; i < parts.size(); i++) {
            final RoomFinder.Part part = parts.get(i);
            if (part.cells().isEmpty() || overlap[i] == 0 && i != keeper) {
                continue;
            }
            final Room target = i == keeper ? room : rooms.create();
            if (target != room) {
                target.empty();
            } else {
                room.empty();
            }
            for (final Map.Entry<ResourceLocation, Double> each : gas.entrySet()) {
                target.add(each.getKey(), each.getValue() * overlap[i] / total);
            }
            // A closed space that joins the room, a cave or a cellar no room held, brings the place's air with it.
            final int fresh = part.open() ? 0 : fresh(rooms, part, before);
            if (fresh > 0) {
                SealedRooms.fill(target, SealedRooms.placeClimate(this.level, room.bounds().getCenter()),
                        fresh * GasMath.BLOCK);
            }
            if (!part.open()) {
                for (final Room other : othersIn(rooms, part, room)) {
                    if (other != target) {
                        target.absorb(other);
                        rooms.remove(other);
                        NeoForge.EVENT_BUS.post(new RoomEvent.Removed(this.level, other));
                    }
                }
            }
            final LongOpenHashSet old = target == room ? before : new LongOpenHashSet();
            target.shape(part.cells(), part.faces(), part.water(), part.lava(), part.plants(), part.fires(),
                    part.open());
            rooms.reindex(target, old);
            // Closed again after a breach, a room under water is pumped dry again; one on land, of what flooded it.
            if (!part.open() && wasBreached) {
                pump(target, target.underWater() ? target.cells() : target.flooded());
            }
            NeoForge.EVENT_BUS.post(target == room ? new RoomEvent.Changed(this.level, target)
                    : new RoomEvent.Formed(this.level, target));
        }
        if (room.cells().isEmpty() || rooms.room(room.id()) == null) {
            return;
        }
        if (leakedBefore <= 0 && leakArea(rooms, room) > 0 || !wasBreached && room.breached()) {
            NeoForge.EVENT_BUS.post(new RoomEvent.Leaking(this.level, room));
        }
    }

    /* How many blocks of a part neither the room held nor any other room. */
    private static int fresh(final RoomStore rooms, final RoomFinder.Part part, final LongOpenHashSet before) {
        int fresh = 0;
        final LongIterator each = part.cells().iterator();
        while (each.hasNext()) {
            final long cell = each.nextLong();
            if (!before.contains(cell) && rooms.roomAt(cell) == null) {
                fresh++;
            }
        }
        return fresh;
    }

    /* The other rooms whose blocks a part now holds. */
    private static List<Room> othersIn(final RoomStore rooms, final RoomFinder.Part part, final Room room) {
        final Map<Long, Room> found = new LinkedHashMap<>();
        final LongIterator each = part.cells().iterator();
        while (each.hasNext()) {
            final Room other = rooms.roomAt(each.nextLong());
            if (other != null && other != room) {
                found.putIfAbsent(other.id(), other);
            }
        }
        return new ArrayList<>(found.values());
    }

    /* How much of a room's faces gas crosses to the outside, through leaks worth the name. */
    static double leakArea(final RoomStore rooms, final Room room) {
        double area = 0;
        for (final RoomFace face : room.faces()) {
            if (face.area() >= GasMath.NOTICEABLE && rooms.roomAt(face.beyond().asLong()) == null) {
                area += face.area();
            }
        }
        return area;
    }

    /* Takes the water out of {@code cells} of a room: a room under water stays dry. */
    private void pump(final Room room, final LongSet cells) {
        final long[] wet = cells.toLongArray();
        for (final long cell : wet) {
            final BlockPos pos = BlockPos.of(cell);
            if (RoomLiving.water(this.level.getBlockState(pos))) {
                set(pos, Blocks.AIR.defaultBlockState());
                room.flood(cell, false);
            }
        }
    }

    /* Fills a new room with the air of the place it stands in. */
    private void fill(final Room room) {
        final Vec3 middle = room.bounds().getCenter();
        SealedRooms.fill(room, SealedRooms.placeClimate(this.level, middle));
    }

    /* Puts out the fires lit where there is too little oxygen for them, outside any room. */
    private void checkFires() {
        if (this.fireChecks.isEmpty()) {
            return;
        }
        final long[] lit = this.fireChecks.toLongArray();
        this.fireChecks.clear();
        if (!RoomBalance.fireNeedsOxygen()) {
            return;
        }
        for (final long cell : lit) {
            final BlockPos pos = BlockPos.of(cell);
            // A fire whose chunk is not loaded now is not read: that would load the chunk, or wait on it.
            if (!Loaded.at(this.level, pos)) {
                continue;
            }
            final BlockState state = this.level.getBlockState(pos);
            if (state.getBlock() instanceof BaseFireBlock && !GasMath.burns(SealedRooms.oxygenAt(this.level, pos))) {
                set(pos, Blocks.AIR.defaultBlockState());
                final RoomStore rooms = this.store;
                final Room room = rooms == null ? null : rooms.roomAt(cell);
                if (room != null) {
                    room.fire(cell, false);
                }
            }
        }
    }
}
