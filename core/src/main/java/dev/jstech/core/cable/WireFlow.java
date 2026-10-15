/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import java.util.Objects;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/**
 * What a wire carries, as the players who see it are told: what it is (a fluid's, a material's or an item's id), toward
 * which face of its block, how much a tick, and how full that is of what the wire carries, in steps: what a glass pipe
 * shows of its fluid and its level, a tube of its items, and a wrench of the way and the amount.
 *
 * @param what    the id of what it carries; empty for nothing
 * @param toward  the face of its block it goes toward
 * @param perTick how much a tick
 * @param level   how full the wire is, from 0 for empty to {@link #FULL}
 */
public record WireFlow(String what, Direction toward, long perTick, int level) {

    /** Nothing. */
    public static final WireFlow EMPTY = new WireFlow("", Direction.NORTH, 0L, 0);
    /** The level of a wire carrying all it can. */
    public static final int FULL = 4;

    private static final String WHAT = "What";
    private static final String TOWARD = "Toward";
    private static final String PER_TICK = "PerTick";
    private static final String LEVEL = "Level";

    public WireFlow {
        Objects.requireNonNull(what, "what");
        Objects.requireNonNull(toward, "toward");
        level = Math.max(0, Math.min(FULL, level));
    }

    /**
     * {@code perTick} of {@code what} toward {@code toward}, in a wire that carries {@code throughput} a tick: its level
     * a step at least, so anything that flows shows.
     */
    public static WireFlow of(final String what, final Direction toward, final long perTick, final long throughput) {
        final int level = throughput <= 0 ? FULL : (int) Math.ceil((double) Math.min(perTick, throughput) * FULL
                / throughput);
        return new WireFlow(what, toward, perTick, Math.max(1, level));
    }

    /** Whether it carries nothing. */
    public boolean isEmpty() {
        return this.what.isEmpty() || this.perTick <= 0;
    }

    /** Written for the players who see its block. */
    public CompoundTag write() {
        final CompoundTag tag = new CompoundTag();
        tag.putString(WHAT, this.what);
        tag.putString(TOWARD, this.toward.getSerializedName());
        tag.putLong(PER_TICK, this.perTick);
        tag.putInt(LEVEL, this.level);
        return tag;
    }

    /** Read back on the client; nothing for a tag that does not read. */
    public static WireFlow read(final @Nullable CompoundTag tag) {
        if (tag == null) {
            return EMPTY;
        }
        final Direction toward = Direction.byName(tag.getString(TOWARD));
        return toward == null ? EMPTY : new WireFlow(tag.getString(WHAT), toward, tag.getLong(PER_TICK),
                tag.getInt(LEVEL));
    }
}
