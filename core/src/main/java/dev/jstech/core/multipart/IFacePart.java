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
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

/**
 * A thin device mounted on one face of a block, which it shares with the block's other parts and with what runs
 * through its middle: a bus on a cable, a cover, a sensor. A part works on the block its face points at.
 */
public interface IFacePart {

    /** The kind of part this is. */
    PartType<?> type();

    /** The part was mounted on {@code face} of {@code host}, or read back there from a save. */
    void attach(IPartHost host, Direction face);

    /**
     * Whether the part is at work now, on the server; its kind's busy model shows it on the players' games. A part
     * that can be busy calls {@link IPartHost#partChanged} when this changes, which sends it to the players.
     */
    default boolean busy() {
        return false;
    }

    /** Runs once a tick on the server while the block is loaded, for a part that {@link #ticks()}. */
    default void serverTick() {
    }

    /**
     * Whether the part needs its tick: a space none of whose parts needs one is never ticked. A part whose answer
     * changes calls {@link IPartHost#partChanged}.
     */
    default boolean ticks() {
        return true;
    }

    /**
     * The redstone strength the part gives out of its face, 0 to 15: to the block there and to wire beside it. A part
     * whose signal changes calls {@link IPartHost#partSignalChanged}.
     */
    default int signal() {
        return 0;
    }

    /** The strength it gives into the block on its face, which then powers its own neighbours, as a lever does. */
    default int directSignal() {
        return 0;
    }

    /**
     * What a comparator reading the part's face gives, 0 to 15. A part whose reading changes calls
     * {@link IPartHost#partSignalChanged}.
     */
    default int comparator() {
        return 0;
    }

    /** The light the part gives, 0 to 15. A part whose light changes calls {@link IPartHost#partChanged}. */
    default int light() {
        return 0;
    }

    /** The block on the part's face, or the redstone reaching it, changed; on the server. */
    default void neighbourChanged() {
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

    /**
     * What the part offers on its own face as {@code capability}: a bus the inventory it reaches, a tank its fluid. The
     * block the part sits on answers with it on that face, for the game's items, fluids and energy and for every
     * capability declared with the Core; nothing, unless the part says.
     */
    default <T> @Nullable T capability(final BlockCapability<T, @Nullable Direction> capability) {
        return null;
    }
}
