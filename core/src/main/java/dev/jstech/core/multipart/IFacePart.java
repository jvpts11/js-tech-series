/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * A thin device mounted on one face of a block, which it shares with the block's other parts and with what runs
 * through its middle: a bus on a cable, a cover, a sensor. A part works on the block its face points at.
 */
public interface IFacePart {

    /** The kind of part this is. */
    PartType<?> type();

    /** The part was mounted on {@code face} of {@code host}, or read back there from a save. */
    void attach(IPartHost host, Direction face);

    /** Runs once a tick on the server while the block is loaded. */
    default void serverTick() {
    }

    /** Writes what the part keeps into a save. */
    default void save(final CompoundTag tag, final HolderLookup.Provider registries) {
    }

    /** Reads what the part keeps back from a save. */
    default void load(final CompoundTag tag, final HolderLookup.Provider registries) {
    }

    /** The item a player gets back for the part. */
    ItemStack partItem();

    /** Drops whatever the part holds besides itself, as the part or its block goes. */
    default void dropContents(final ServerLevel level) {
    }

    /**
     * A player used the part, on the server: it opens its screen or does what it does when used.
     *
     * @return whether it did anything
     */
    default boolean use(final ServerPlayer player) {
        return false;
    }
}
