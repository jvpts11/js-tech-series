/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * The places block entities keep, carried with the blocks they name as a structure is made, set down or taken to
 * another dimension, for every mod's block entities alike. One that does not look after its places itself
 * ({@link IMovesWithStructures}) has them put right in what it saved: each place written as the game writes places,
 * three whole numbers in a row, an {@code X}, a {@code Y} and a {@code Z} side by side under one name, or a packed
 * place under a name holding "pos", that names a block which moved with it, is changed to where that block went,
 * turned as it was. A place naming a block that did not move, on the land below, is left as it is.
 *
 * <p>A mod whose block entities keep their places some other way says how to put them right, by the block entity's
 * type, from its own code or from another mod's that knows it:
 *
 * <pre>{@code
 * StructurePlaces.fixer(ResourceLocation.parse("othermod:relay"), (saved, move) -> {
 *     final CompoundTag fixed = saved.copy();
 *     fixed.putLong("Remote", move.where(BlockPos.of(saved.getLong("Remote"))).asLong());
 *     return fixed;
 * });
 * StructurePlaces.leaveAlone(ResourceLocation.parse("othermod:counter"));   // its numbers are no places
 * }</pre>
 */
public final class StructurePlaces {

    /* How each block entity type, by its id, has its places put right, where its mod said. */
    private static final Map<ResourceLocation, IPlaceFixer> FIXERS = new ConcurrentHashMap<>();
    /* The block entity types, by their id, whose saved numbers are never places. */
    private static final Set<ResourceLocation> LEFT_ALONE = ConcurrentHashMap.newKeySet();
    /* The name a place packed into one number is kept under holds this. */
    private static final String PACKED = "pos";
    private static final int PLACE_LENGTH = 3;

    private StructurePlaces() {
    }

    /** Puts right the places a block entity of the type {@code type} saved with {@code fixer}, rather than read them. */
    public static void fixer(final ResourceLocation type, final IPlaceFixer fixer) {
        FIXERS.put(type, fixer);
    }

    /** Leaves what a block entity of the type {@code type} saved as it is: none of its numbers are places. */
    public static void leaveAlone(final ResourceLocation type) {
        LEFT_ALONE.add(type);
    }

    /**
     * What {@code saved}, saved by a block entity of the type {@code type}, says once its places follow the blocks of
     * {@code move}; none when nothing in it changes. {@code saved} itself is never changed.
     */
    public static @Nullable CompoundTag moved(final ResourceLocation type, final CompoundTag saved,
                                              final BlockMove move) {
        final IPlaceFixer fixer = FIXERS.get(type);
        if (fixer != null) {
            return fixer.fix(saved, move);
        }
        if (LEFT_ALONE.contains(type) || move.moved().isEmpty()) {
            return null;
        }
        final CompoundTag fixed = saved.copy();
        return follow(fixed, move.moved()) ? fixed : null;
    }

    /* Moves each place written in {@code tag} that names a block of {@code moved}; whether any was. */
    private static boolean follow(final CompoundTag tag, final Map<BlockPos, BlockPos> moved) {
        boolean changed = false;
        for (final String key : List.copyOf(tag.getAllKeys())) {
            final Tag value = tag.get(key);
            if (value instanceof IntArrayTag array && array.size() == PLACE_LENGTH) {
                final int[] at = array.getAsIntArray();
                final BlockPos went = moved.get(new BlockPos(at[0], at[1], at[2]));
                if (went != null) {
                    tag.putIntArray(key, new int[] {went.getX(), went.getY(), went.getZ()});
                    changed = true;
                }
            } else if (value instanceof LongTag packed && key.toLowerCase(Locale.ROOT).contains(PACKED)) {
                final BlockPos went = moved.get(BlockPos.of(packed.getAsLong()));
                if (went != null) {
                    tag.putLong(key, went.asLong());
                    changed = true;
                }
            } else if (value instanceof IntTag) {
                changed |= followSideBySide(tag, key, moved);
            } else if (value instanceof CompoundTag inner) {
                changed |= follow(inner, moved);
            } else if (value instanceof ListTag list) {
                changed |= follow(list, moved);
            }
        }
        return changed;
    }

    /* Moves each place written in the list {@code list}, of places or of what holds them; whether any was. */
    private static boolean follow(final ListTag list, final Map<BlockPos, BlockPos> moved) {
        boolean changed = false;
        for (int i = 0; i < list.size(); i++) {
            final Tag value = list.get(i);
            if (value instanceof IntArrayTag array && array.size() == PLACE_LENGTH) {
                final int[] at = array.getAsIntArray();
                final BlockPos went = moved.get(new BlockPos(at[0], at[1], at[2]));
                if (went != null) {
                    list.set(i, new IntArrayTag(new int[] {went.getX(), went.getY(), went.getZ()}));
                    changed = true;
                }
            } else if (value instanceof CompoundTag inner) {
                changed |= follow(inner, moved);
            } else if (value instanceof ListTag inner) {
                changed |= follow(inner, moved);
            }
        }
        return changed;
    }

    /*
     * Moves the place written side by side under {@code key}, when it is a place's X: the same name with a Y and with
     * a Z there, whole numbers too, naming a block of {@code moved}; whether it was.
     */
    private static boolean followSideBySide(final CompoundTag tag, final String key,
                                            final Map<BlockPos, BlockPos> moved) {
        for (int i = 0; i < key.length(); i++) {
            final char letter = key.charAt(i);
            if (letter != 'X' && letter != 'x') {
                continue;
            }
            final boolean upper = letter == 'X';
            final String yKey = key.substring(0, i) + (upper ? 'Y' : 'y') + key.substring(i + 1);
            final String zKey = key.substring(0, i) + (upper ? 'Z' : 'z') + key.substring(i + 1);
            if (!(tag.get(yKey) instanceof IntTag) || !(tag.get(zKey) instanceof IntTag)) {
                continue;
            }
            final BlockPos went = moved.get(new BlockPos(tag.getInt(key), tag.getInt(yKey), tag.getInt(zKey)));
            if (went != null) {
                tag.putInt(key, went.getX());
                tag.putInt(yKey, went.getY());
                tag.putInt(zKey, went.getZ());
                return true;
            }
        }
        return false;
    }

    /** How a block entity type's places are put right, as its blocks move with a structure. */
    @FunctionalInterface
    public interface IPlaceFixer {

        /**
         * What {@code saved}, as a block entity of the type saved it, says once its places follow the blocks of
         * {@code move}; none when nothing in it changes. {@code saved} is not to be changed: change a copy.
         */
        @Nullable CompoundTag fix(CompoundTag saved, BlockMove move);
    }
}
