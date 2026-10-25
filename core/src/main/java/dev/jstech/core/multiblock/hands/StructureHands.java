/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.hands;

import dev.jstech.core.blockentity.IFieldPart;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/**
 * The state of the parts of a formed structure's model a player works by hand, kept by its terminal: whether each door
 * is open and when it last moved, saved, and sent to the players who see it, who draw it turning.
 */
public final class StructureHands implements IFieldPart {

    private final InteractiveModel model;
    private final List<Hinge> hinges = new ArrayList<>();
    private final boolean[] open;
    private final long[] moved;
    private @Nullable Runnable synced;

    private static final String KEY = "Hands";
    private static final String OPEN_KEY = "Open";
    private static final String MOVED_KEY = "Moved";

    /** The state of the parts {@code model} declares, every door shut. */
    public StructureHands(final InteractiveModel model) {
        this.model = model;
        for (final IHandPart part : model.parts()) {
            if (part instanceof IHandPart.Door door) {
                this.hinges.add(door.hinge());
            }
        }
        this.open = new boolean[this.hinges.size()];
        this.moved = new long[this.hinges.size()];
    }

    /** What of the model is worked by hand. */
    public InteractiveModel model() {
        return this.model;
    }

    /** Whether the door of number {@code door} is open, or opening. */
    public boolean open(final int door) {
        return this.open[door];
    }

    /** Opens or shuts the door of number {@code door}, at the game's tick {@code now}, for the players to see. */
    public void setOpen(final int door, final boolean to, final long now) {
        if (this.open[door] == to) {
            return;
        }
        this.open[door] = to;
        this.moved[door] = now;
        if (this.synced != null) {
            this.synced.run();
        }
    }

    /**
     * How far each door is open at {@code time}, in the game's ticks and a part of one, by its number: from 0 shut to
     * 1 open, a door that moved less than its hinge's time ago part of the way.
     */
    public float[] openness(final double time) {
        final float[] out = new float[this.open.length];
        for (int door = 0; door < out.length; door++) {
            final int ticks = this.hinges.get(door).ticks();
            final float done = ticks == 0 ? 1 : (float) Math.max(0, Math.min(1, (time - this.moved[door]) / ticks));
            out[door] = this.open[door] ? done : 1 - done;
        }
        return out;
    }

    @Override
    public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
        write(tag);
    }

    @Override
    public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
        read(tag);
    }

    @Override
    public void writeClient(final CompoundTag tag, final HolderLookup.Provider registries) {
        write(tag);
    }

    @Override
    public void readClient(final CompoundTag tag, final HolderLookup.Provider registries) {
        read(tag);
    }

    /** Tells {@code changed} each time a door opens or shuts, so the terminal sends it. */
    public void on(final Runnable changed) {
        this.synced = changed;
    }

    private void write(final CompoundTag tag) {
        final CompoundTag own = new CompoundTag();
        final byte[] flags = new byte[this.open.length];
        for (int door = 0; door < flags.length; door++) {
            flags[door] = (byte) (this.open[door] ? 1 : 0);
        }
        own.putByteArray(OPEN_KEY, flags);
        own.putLongArray(MOVED_KEY, this.moved);
        tag.put(KEY, own);
    }

    private void read(final CompoundTag tag) {
        final CompoundTag own = tag.getCompound(KEY);
        final byte[] flags = own.getByteArray(OPEN_KEY);
        final long[] times = own.getLongArray(MOVED_KEY);
        // A model that gained or lost a door since keeps those it still has.
        for (int door = 0; door < this.open.length; door++) {
            this.open[door] = door < flags.length && flags[door] != 0;
            this.moved[door] = door < times.length ? times[door] : 0;
        }
    }
}
