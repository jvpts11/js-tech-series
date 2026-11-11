/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.audio;

import dev.jstech.computers.MediaDevices;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.JukeboxSong;

/**
 * The records a Vinyl RW Drive plays and cuts (computers/docs/design/peripherals.md): any disc a jukebox plays, the
 * game's or another mod's, and the blank vinyl the drive cuts a recording onto, which becomes that disc.
 */
public final class VinylDiscs {

    /* The disc of each song, found once among every item there is. */
    private static final Map<ResourceLocation, Item> DISCS = new HashMap<>();

    private VinylDiscs() {
    }

    /** Whether {@code stack} goes on a drive's platter: a disc a jukebox plays, or a blank vinyl. */
    public static boolean playable(final ItemStack stack) {
        return stack.has(DataComponents.JUKEBOX_PLAYABLE) || blank(stack);
    }

    /** Whether {@code stack} is a blank vinyl record. */
    public static boolean blank(final ItemStack stack) {
        return stack.is(MediaDevices.BLANK_VINYL_RECORD.get());
    }

    /** The song the disc {@code stack} holds, or empty for anything a jukebox does not play. */
    public static Optional<Holder<JukeboxSong>> songOf(final ItemStack stack, final HolderLookup.Provider registries) {
        final JukeboxPlayable playable = stack.get(DataComponents.JUKEBOX_PLAYABLE);
        return playable == null ? Optional.empty() : playable.song().unwrap(registries);
    }

    /** The disc of the song {@code song}, as one comes from the game or its mod; empty when no item plays it. */
    public static ItemStack discOf(final ResourceLocation song) {
        synchronized (DISCS) {
            if (DISCS.isEmpty()) {
                for (final Item item : BuiltInRegistries.ITEM) {
                    final JukeboxPlayable playable = item.components().get(DataComponents.JUKEBOX_PLAYABLE);
                    if (playable != null && playable.song().key() != null) {
                        DISCS.putIfAbsent(playable.song().key().location(), item);
                    }
                }
            }
            final Item disc = DISCS.get(song);
            return disc == null ? ItemStack.EMPTY : new ItemStack(disc);
        }
    }
}
