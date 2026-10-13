/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.engine.EngineSwap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * A replacement of the network's engine that is under way on a Mainframe.
 *
 * @param from      the engine being replaced, or {@code null} when the network had none chosen
 * @param to        the engine coming up
 * @param startedAt the game time the replacement began
 * @param ticks     how long it takes in all
 * @param inFlight  how many Operations were in flight when it began, which finish on the plans they had
 * @param itemTypes how many kinds of item the new engine has to index
 */
public record EngineReplacement(@Nullable ResourceLocation from, ResourceLocation to, long startedAt, int ticks,
                                int inFlight, int itemTypes) {

    /** A replacement beginning at {@code now} on a network holding {@code itemTypes} kinds of item. */
    public static EngineReplacement begin(@Nullable final ResourceLocation from, final ResourceLocation to,
                                          final long now, final int inFlight, final int itemTypes) {
        return new EngineReplacement(from, to, now, EngineSwap.ticksFor(itemTypes), inFlight, itemTypes);
    }

    /** Reads one back, or {@code null} when the tag holds none that can be read. */
    @Nullable
    public static EngineReplacement load(final CompoundTag tag) {
        final ResourceLocation to = ResourceLocation.tryParse(tag.getString("To"));
        if (to == null) {
            return null;
        }
        final ResourceLocation from = tag.contains("From") ? ResourceLocation.tryParse(tag.getString("From")) : null;
        return new EngineReplacement(from, to, tag.getLong("StartedAt"), tag.getInt("Ticks"), tag.getInt("InFlight"),
                tag.getInt("ItemTypes"));
    }

    /** How many of its ticks have gone by at {@code now}. */
    public long elapsed(final long now) {
        return Math.max(0L, Math.min(ticks, now - startedAt));
    }

    /** Whether it is over at {@code now}. */
    public boolean done(final long now) {
        return now - startedAt >= ticks;
    }

    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        if (from != null) {
            tag.putString("From", from.toString());
        }
        tag.putString("To", to.toString());
        tag.putLong("StartedAt", startedAt);
        tag.putInt("Ticks", ticks);
        tag.putInt("InFlight", inFlight);
        tag.putInt("ItemTypes", itemTypes);
        return tag;
    }
}
