/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A round trip through the save, in the middle of a test: a block entity is saved as its chunk saves it, unloaded,
 * and read back as the chunk reads it when it loads again, a new block entity of its type standing in its place, so a
 * test shows what survives a save and goes on with the one read back.
 *
 * <pre>{@code
 * final SaveRoundTrip.Trip trip = SaveRoundTrip.of(helper, KILN);
 * trip.assertSame(helper, KILN);                 // every field read back as it was saved
 * final KilnBlockEntity kiln = trip.read(KilnBlockEntity.class);
 * }</pre>
 *
 * <p>The block entity read back is loaded as the game loads it, on the next tick: what it does as it loads (joining
 * a network, watching a structure) is done then.
 */
public final class SaveRoundTrip {

    private SaveRoundTrip() {
    }

    /** Saves and reads back the block entity at the arena's place {@code at}. */
    public static Trip of(final GameTestHelper helper, final BlockPos at) {
        final ServerLevel level = helper.getLevel();
        final BlockPos pos = helper.absolutePos(at);
        final BlockEntity saved = level.getBlockEntity(pos);
        if (saved == null) {
            throw Expect.failure(helper, at, "a round trip through the save: expected a block entity, found "
                    + level.getBlockState(pos));
        }
        return of(level, saved);
    }

    /** Saves {@code saved}, unloads it and reads it back in its place. */
    public static Trip of(final ServerLevel level, final BlockEntity saved) {
        final HolderLookup.Provider registries = level.registryAccess();
        final BlockPos pos = saved.getBlockPos();
        final BlockState state = saved.getBlockState();
        final CompoundTag before = saved.saveWithFullMetadata(registries);
        saved.onChunkUnloaded();
        final BlockEntity read = BlockEntity.loadStatic(pos, state, before.copy(), registries);
        if (read == null) {
            throw new IllegalStateException("the block entity at " + pos.toShortString() + " could not be read back"
                    + " from what it saved: " + before);
        }
        level.setBlockEntity(read);
        return new Trip(before, read.saveWithFullMetadata(registries), read);
    }

    /** Every field that differs between two saves, by its path, each with what it was and what it is. */
    public static List<String> differences(final CompoundTag before, final CompoundTag after) {
        final List<String> out = new ArrayList<>();
        compare("", before, after, out);
        return out;
    }

    private static void compare(final String path, final CompoundTag before, final CompoundTag after,
                                final List<String> out) {
        final TreeSet<String> keys = new TreeSet<>(before.getAllKeys());
        keys.addAll(after.getAllKeys());
        for (final String key : keys) {
            final Tag was = before.get(key);
            final Tag is = after.get(key);
            final String at = path.isEmpty() ? key : path + "." + key;
            if (was instanceof CompoundTag wasCompound && is instanceof CompoundTag isCompound) {
                compare(at, wasCompound, isCompound, out);
            } else if (!Objects.equals(was, is)) {
                out.add(at + ": " + (was == null ? "none" : was) + " became " + (is == null ? "none" : is));
            }
        }
    }

    /**
     * One round trip.
     *
     * @param before what the block entity saved
     * @param after  what the one read back saves at once
     * @param read   the one read back, standing in the world in the place of the other
     */
    public record Trip(CompoundTag before, CompoundTag after, BlockEntity read) {

        /** Every field that came back other than it was saved. */
        public List<String> changed() {
            return differences(before, after);
        }

        /** That every field came back as it was saved; else a failure listing the ones that did not. */
        public void assertSame(final GameTestHelper helper, final BlockPos at) {
            final List<String> changed = changed();
            if (!changed.isEmpty()) {
                throw Expect.failure(helper, at, "read back from the save, " + changed.size() + " fields differ: "
                        + String.join("; ", changed));
            }
        }

        /** The block entity read back, as a {@code type}. */
        public <T extends BlockEntity> T read(final Class<T> type) {
            if (!type.isInstance(read)) {
                throw new IllegalStateException("read back a " + read.getClass().getSimpleName() + ", not a "
                        + type.getSimpleName());
            }
            return type.cast(read);
        }
    }
}
