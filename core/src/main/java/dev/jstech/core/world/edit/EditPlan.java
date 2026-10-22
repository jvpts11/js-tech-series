/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.world.edit;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The blocks a large edit sets, planned away from the server's thread and set on it a few thousand at a time: each
 * place and what goes there, ordered chunk by chunk so each chunk is reached once. By default the blocks are set as
 * structures are, without waking their neighbours; {@link Builder#updates()} wakes them, as a player's placing does.
 * A block may carry what its block entity holds (a chest's contents, a machine's settings), loaded into it once it
 * is set; and a plan made {@link Builder#undoable()} keeps what it replaces, so its edit can be undone
 * ({@link WorldEdit#undo()}).
 */
public final class EditPlan {

    private final long[] places;
    private final BlockState[] blocks;
    private final @Nullable CompoundTag[] data;
    private final int flags;
    private final boolean undoable;

    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private EditPlan(final long[] places, final BlockState[] blocks, final @Nullable CompoundTag[] data,
                     final int flags, final boolean undoable) {
        this.places = places;
        this.blocks = blocks;
        this.data = data;
        this.flags = flags;
        this.undoable = undoable;
    }

    /** Starts a plan. */
    public static Builder builder() {
        return new Builder();
    }

    /** How many blocks it sets. */
    public int size() {
        return this.places.length;
    }

    /** The place of its {@code index}th block, packed as {@link BlockPos#asLong()}. */
    public long place(final int index) {
        return this.places[index];
    }

    /** What its {@code index}th block is set to. */
    public BlockState block(final int index) {
        return this.blocks[index];
    }

    /** What its {@code index}th block's block entity is given once it is set, or null for nothing. */
    public @Nullable CompoundTag data(final int index) {
        return this.data[index];
    }

    /** How the blocks are set, as the game's {@code Block.UPDATE_*} flags. */
    public int flags() {
        return this.flags;
    }

    /** Whether its edit keeps what it replaces, to be undone. */
    public boolean undoable() {
        return this.undoable;
    }

    /** A plan being made; on any thread, one at a time. */
    public static final class Builder {

        private final LongArrayList places = new LongArrayList();
        private final List<BlockState> blocks = new ArrayList<>();
        private final List<CompoundTag> data = new ArrayList<>();
        private int flags = QUIET;
        private boolean undoable;

        private Builder() {
        }

        /** Sets {@code x, y, z} to {@code block}; set twice, the later wins. */
        public Builder set(final int x, final int y, final int z, final BlockState block) {
            return set(x, y, z, block, null);
        }

        public Builder set(final BlockPos pos, final BlockState block) {
            return set(pos.getX(), pos.getY(), pos.getZ(), block);
        }

        /** Sets {@code x, y, z} to {@code block}, its block entity given {@code held} once it is set. */
        public Builder set(final int x, final int y, final int z, final BlockState block,
                           final @Nullable CompoundTag held) {
            this.places.add(BlockPos.asLong(x, y, z));
            this.blocks.add(block);
            this.data.add(held);
            return this;
        }

        /** The blocks set wake their neighbours, as a player's placing does: water flows, sand falls. */
        public Builder updates() {
            this.flags = Block.UPDATE_ALL;
            return this;
        }

        /** Sets the blocks with the game's {@code Block.UPDATE_*} {@code flags}, as another plan did. */
        public Builder flags(final int how) {
            this.flags = how;
            return this;
        }

        /**
         * Its edit keeps each block it replaces, with what the block's entity held, so it can be undone: at the cost
         * of keeping them while the edit is the last its player may undo.
         */
        public Builder undoable() {
            this.undoable = true;
            return this;
        }

        /** The plan, ordered chunk by chunk, each place in the order it was set within its chunk. */
        public EditPlan build() {
            final Integer[] order = new Integer[this.places.size()];
            Arrays.setAll(order, index -> index);
            Arrays.sort(order, Comparator.comparingLong(index -> chunkOf(this.places.getLong(index))));
            final long[] sorted = new long[order.length];
            final BlockState[] states = new BlockState[order.length];
            final CompoundTag[] held = new CompoundTag[order.length];
            for (int i = 0; i < order.length; i++) {
                sorted[i] = this.places.getLong(order[i]);
                states[i] = this.blocks.get(order[i]);
                held[i] = this.data.get(order[i]);
            }
            return new EditPlan(sorted, states, held, this.flags, this.undoable);
        }

        /* The chunk of a packed place, as one number that keeps chunks apart and neighbours near. */
        private static long chunkOf(final long place) {
            return (long) (BlockPos.getX(place) >> 4) << Integer.SIZE
                    | Integer.toUnsignedLong(BlockPos.getZ(place) >> 4);
        }
    }
}
