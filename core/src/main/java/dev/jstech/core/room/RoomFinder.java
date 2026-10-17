/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.room;

import dev.jstech.core.air.AirMix;
import dev.jstech.core.dimension.Climate;
import dev.jstech.core.dimension.DimensionRules;
import dev.jstech.core.dimension.DimensionRulesData;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

/**
 * Works out the shape of a room a little at a time, a number of blocks read each tick: first the blocks of air that
 * join up from where it starts, each part apart, up to the largest room; then, for each part, what bounds it face by
 * face and what lies beyond. A part larger than the largest room is open to the place round it. It reads only chunks
 * already loaded whole, and waits, to be started again, at one that is not: a read of another would load it, or make
 * it, in the middle of the tick.
 */
final class RoomFinder {

    private final ServerLevel level;
    private final long room;
    private final LongArrayList starts;
    private final LongOpenHashSet previous;
    private final int largest;
    private final List<LongOpenHashSet> parts = new ArrayList<>();
    private final List<Boolean> open = new ArrayList<>();
    private final List<Part> found = new ArrayList<>();
    private final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
    private final LongArrayFIFOQueue frontier = new LongArrayFIFOQueue();
    private int nextStart;
    private Stage stage = Stage.FILL;
    private @Nullable LongOpenHashSet part;
    /* Where the faces are being worked out: which part, and how far through its blocks. */
    private int facePart;
    private LongOpenHashSet faceSet;
    private long[] faceCells;
    private int faceCell;
    private List<RoomFace> faces;
    private LongOpenHashSet fires;
    private LongOpenHashSet plantWalls;
    private int water;
    private int lava;
    private int plants;
    /* The chunk last read, kept, as most reads fall in the one before. */
    private @Nullable LevelChunk chunk;
    private int chunkX = Integer.MIN_VALUE;
    private int chunkZ = Integer.MIN_VALUE;
    /* The chunk it met that is not loaded whole, as {@link ChunkPos#asLong}. */
    private long waitedOn;

    /* How far up a column of water beyond a wall is read for its depth. */
    private static final int DEEPEST = 256;

    /**
     * A search from {@code starts}, for the room {@code room} as it was ({@code previous}, its blocks), or for none
     * ({@code room} -1).
     */
    RoomFinder(final ServerLevel level, final long room, final LongArrayList starts, final LongOpenHashSet previous,
               final int largest) {
        this.level = level;
        this.room = room;
        this.starts = starts;
        this.previous = previous;
        this.largest = largest;
    }

    /** The room it works out again, or -1 for a search for a new one. */
    long room() {
        return this.room;
    }

    /** Whether it is done. */
    boolean done() {
        return this.stage == Stage.DONE;
    }

    /** Whether it met a chunk that is not loaded, and must start again later. */
    boolean waiting() {
        return this.stage == Stage.WAITING;
    }

    /** The chunk it waits for, once it is {@linkplain #waiting() waiting}, as {@link ChunkPos#asLong}. */
    long waitedOn() {
        return this.waitedOn;
    }

    /** The parts it found, once it is done. */
    List<Part> parts() {
        return this.found;
    }

    /** Works on, reading at most {@code budget} blocks; how many it read. */
    int work(final int budget) {
        int read = 0;
        while (read < budget && this.stage != Stage.DONE && this.stage != Stage.WAITING) {
            read += this.stage == Stage.FILL ? fill(budget - read) : face(budget - read);
        }
        return read;
    }

    /* Grows the part being filled, or starts the next; when every start is done, turns to the faces. */
    private int fill(final int budget) {
        int read = 0;
        if (this.part == null) {
            while (this.nextStart < this.starts.size()) {
                final long start = this.starts.getLong(this.nextStart++);
                if (inAnyPart(start)) {
                    continue;
                }
                final BlockState state = stateAt(start);
                if (state == null) {
                    this.stage = Stage.WAITING;
                    return read;
                }
                read++;
                if (Seals.of(this.level, this.at, state).wall()) {
                    continue;
                }
                this.part = new LongOpenHashSet();
                this.part.add(start);
                this.frontier.clear();
                this.frontier.enqueue(start);
                break;
            }
            if (this.part == null) {
                this.stage = Stage.FACES;
                return Math.max(read, 1);
            }
        }
        final LongOpenHashSet filling = this.part;
        boolean opened = false;
        while (!this.frontier.isEmpty() && read < budget) {
            final long cell = this.frontier.dequeueLong();
            for (final Direction side : Direction.values()) {
                final long next = BlockPos.offset(cell, side);
                if (filling.contains(next)) {
                    continue;
                }
                final BlockState state = stateAt(next);
                if (state == null) {
                    this.stage = Stage.WAITING;
                    return read;
                }
                read++;
                if (Seals.of(this.level, this.at, state).wall()) {
                    continue;
                }
                filling.add(next);
                this.frontier.enqueue(next);
                if (filling.size() > this.largest) {
                    opened = true;
                    this.frontier.clear();
                    break;
                }
            }
        }
        if (this.frontier.isEmpty()) {
            this.parts.add(filling);
            this.open.add(opened || filling.size() > this.largest);
            this.part = null;
        }
        return Math.max(read, 1);
    }

    /* Works out the faces of the part being looked at, block by block. */
    private int face(final int budget) {
        if (this.faceCells == null) {
            if (this.facePart >= this.parts.size()) {
                this.stage = Stage.DONE;
                return 1;
            }
            final LongOpenHashSet cells = cellsOf(this.facePart);
            if (cells.isEmpty()) {
                this.facePart++;
                return 1;
            }
            this.faceSet = cells;
            this.faceCells = cells.toLongArray();
            this.faceCell = 0;
            this.faces = new ArrayList<>();
            this.fires = new LongOpenHashSet();
            this.plantWalls = new LongOpenHashSet();
            this.water = 0;
            this.lava = 0;
            this.plants = 0;
        }
        final LongOpenHashSet cells = this.faceSet;
        final DimensionRules rules = DimensionRulesData.of(this.level);
        int read = 0;
        while (this.faceCell < this.faceCells.length && read < budget) {
            final long cell = this.faceCells[this.faceCell];
            final BlockState inside = stateAt(cell);
            if (inside == null) {
                this.stage = Stage.WAITING;
                return read;
            }
            read++;
            if (inside.getFluidState().is(FluidTags.WATER)) {
                this.water++;
            } else if (inside.getFluidState().is(FluidTags.LAVA)) {
                this.lava++;
            }
            if (inside.getBlock() instanceof BaseFireBlock) {
                this.fires.add(cell);
            }
            if (RoomLiving.plant(inside)) {
                this.plants++;
            }
            for (final Direction side : Direction.values()) {
                final long next = BlockPos.offset(cell, side);
                if (cells.contains(next)) {
                    continue;
                }
                final BlockState bounding = stateAt(next);
                if (bounding == null) {
                    this.stage = Stage.WAITING;
                    return read;
                }
                read++;
                final RoomFace face = bound(rules, cell, side, BlockPos.of(next), bounding);
                this.faces.add(face);
                // Leaves are whole blocks, so they bound a room's air rather than fill it; they still breathe for it.
                if (face.kind() != RoomFace.Kind.HOLE && RoomLiving.plant(bounding)) {
                    this.plantWalls.add(next);
                }
            }
            this.faceCell++;
        }
        if (this.faceCell >= this.faceCells.length) {
            this.found.add(new Part(cells, this.open.get(this.facePart), List.copyOf(this.faces), this.water,
                    this.lava, this.plants + this.plantWalls.size(), this.fires));
            this.faceCells = null;
            this.facePart++;
        }
        return Math.max(read, 1);
    }

    /* The face of {@code cell} towards {@code block}, of {@code state}: what bounds it there, and what lies beyond. */
    private RoomFace bound(final DimensionRules rules, final long cell, final Direction side, final BlockPos block,
                           final BlockState state) {
        final Seal seal = Seals.of(this.level, block, state);
        final RoomFace.Kind kind;
        final BlockPos beyond;
        if (!seal.wall()) {
            kind = RoomFace.Kind.HOLE;
            beyond = block;
        } else {
            beyond = block.relative(side);
            kind = seal.door() ? RoomFace.Kind.DOOR : seal.leak() > 0 ? RoomFace.Kind.CRACK : RoomFace.Kind.WALL;
        }
        final double area = Seals.leak(seal, state);
        final double rating = seal.wall() ? Seals.rating(seal, state) : 0;
        final BlockState past = stateAt(beyond.asLong());
        final boolean water = past != null && past.getFluidState().is(FluidTags.WATER);
        final double outside;
        final AirMix air;
        if (water) {
            final int depth = depth(beyond);
            outside = GasMath.underWater(rules.at(beyond.getY() + depth).pressure(), depth);
            air = AirMix.VACUUM;
        } else {
            final Climate climate = rules.at(beyond.getY());
            outside = climate.pressure();
            air = SealedRooms.airOf(climate);
        }
        return new RoomFace(BlockPos.of(cell), side, block, kind, kind == RoomFace.Kind.WALL ? 0 : area, rating,
                beyond, outside, air, water);
    }

    /* How deep {@code pos} lies under water: the water above it, up to its top, in the same column. */
    private int depth(final BlockPos pos) {
        int depth = 0;
        long up = pos.asLong();
        while (depth < DEEPEST) {
            up = BlockPos.offset(up, Direction.UP);
            final BlockState state = stateAt(up);
            if (state == null || !state.getFluidState().is(FluidTags.WATER)) {
                break;
            }
            depth++;
        }
        return depth + 1;
    }

    /*
     * The block at {@code pos}, read from its chunk if that chunk is loaded whole, else none; {@link #at} is left at
     * {@code pos}.
     */
    private @Nullable BlockState stateAt(final long pos) {
        this.at.set(pos);
        final int x = this.at.getX() >> 4;
        final int z = this.at.getZ() >> 4;
        if (x != this.chunkX || z != this.chunkZ) {
            this.chunk = this.level.getChunkSource().getChunkNow(x, z);
            this.chunkX = x;
            this.chunkZ = z;
        }
        final LevelChunk loaded = this.chunk;
        if (loaded == null) {
            this.waitedOn = ChunkPos.asLong(x, z);
            return null;
        }
        return loaded.getBlockState(this.at);
    }

    /*
     * The blocks of a part the room holds: the whole part when it is closed; when it is open, only the room's own
     * blocks in it, a room opened keeping its shape.
     */
    private LongOpenHashSet cellsOf(final int index) {
        final LongOpenHashSet cells = this.parts.get(index);
        if (!this.open.get(index)) {
            return cells;
        }
        final LongOpenHashSet own = new LongOpenHashSet();
        if (this.room < 0) {
            return own;
        }
        final LongIterator each = this.previous.iterator();
        while (each.hasNext()) {
            final long cell = each.nextLong();
            if (cells.contains(cell)) {
                own.add(cell);
            }
        }
        return own;
    }

    private boolean inAnyPart(final long cell) {
        for (final LongOpenHashSet done : this.parts) {
            if (done.contains(cell)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A part of a room found.
     *
     * @param cells  its blocks of air
     * @param open   whether it is open to the place round it
     * @param faces  what bounds it
     * @param water  how many of its blocks hold water
     * @param lava   how many of its blocks hold lava
     * @param plants how many plants grow in it
     * @param fires  the fires burning in it
     */
    record Part(LongOpenHashSet cells, boolean open, List<RoomFace> faces, int water, int lava, int plants,
                LongOpenHashSet fires) {
    }

    private enum Stage {
        FILL,
        FACES,
        WAITING,
        DONE
    }
}
