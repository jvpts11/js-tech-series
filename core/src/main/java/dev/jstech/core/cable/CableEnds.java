/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import dev.jstech.core.blockentity.IFieldPart;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.phys.Vec3;

/**
 * How each wire of a cable block meets what lies beyond each of its faces: an end set apart from the default, kept by
 * lane and face, saved and sent to the players who see the block. An end set back to the default is forgotten.
 */
public final class CableEnds implements IFieldPart {

    private final Map<Integer, CableEnd> set = new TreeMap<>();
    private final Runnable changed;

    /** The key it is saved under. */
    public static final String KEY = "Ends";
    private static final String LANE = "Lane";
    private static final String FACE = "Face";
    private static final String END = "End";
    private static final int FACES = 6;
    private static final double CENTRE = 0.5;
    /* How far from the block's middle a point is still the wire's middle, in blocks: a quarter of a thick pipe. */
    private static final double MIDDLE = 0.25;

    /** The ends of one cable block, which tell {@code changed} when one is set. */
    public CableEnds(final Runnable changed) {
        this.changed = changed;
    }

    /**
     * Which end of a wire in the cable block at {@code pos} a player points at, hitting it at {@code hit} on the face
     * {@code clicked} of what they hit: the end whose arm the point lies along, or, at the wire's middle, the face they
     * hit, as a wrench picks the end it sets.
     */
    public static Direction facing(final BlockPos pos, final Vec3 hit, final Direction clicked) {
        final double x = hit.x - pos.getX() - CENTRE;
        final double y = hit.y - pos.getY() - CENTRE;
        final double z = hit.z - pos.getZ() - CENTRE;
        final double most = Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z)));
        if (most < MIDDLE) {
            return clicked;
        }
        if (most == Math.abs(x)) {
            return x > 0 ? Direction.EAST : Direction.WEST;
        }
        if (most == Math.abs(y)) {
            return y > 0 ? Direction.UP : Direction.DOWN;
        }
        return z > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    /** How the wire in {@code lane} meets what lies beyond {@code face}. */
    public CableEnd get(final Lane lane, final Direction face) {
        return this.set.getOrDefault(key(lane, face), CableEnd.DEFAULT);
    }

    /** Sets how the wire in {@code lane} meets what lies beyond {@code face}; whether that changed it. */
    public boolean set(final Lane lane, final Direction face, final CableEnd end) {
        final CableEnd before = get(lane, face);
        if (before.equals(end)) {
            return false;
        }
        if (end.isDefault()) {
            this.set.remove(key(lane, face));
        } else {
            this.set.put(key(lane, face), end);
        }
        this.changed.run();
        return true;
    }

    /** Forgets every end of the wire in {@code lane}, as the wire is taken out. */
    public void clear(final Lane lane) {
        boolean any = false;
        for (final Direction face : Direction.values()) {
            any |= this.set.remove(key(lane, face)) != null;
        }
        if (any) {
            this.changed.run();
        }
    }

    @Override
    public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
        if (this.set.isEmpty()) {
            return;
        }
        final RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        final ListTag list = new ListTag();
        for (final Map.Entry<Integer, CableEnd> entry : this.set.entrySet()) {
            CableEnd.CODEC.encodeStart(ops, entry.getValue()).result().ifPresent(saved -> {
                final CompoundTag one = new CompoundTag();
                one.putString(LANE, Lane.byId(entry.getKey() / FACES).serializedName());
                one.putString(FACE, Direction.from3DDataValue(entry.getKey() % FACES).getSerializedName());
                one.put(END, saved);
                list.add(one);
            });
        }
        tag.put(KEY, list);
    }

    @Override
    public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
        this.set.clear();
        final RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        for (final Tag element : tag.getList(KEY, Tag.TAG_COMPOUND)) {
            final CompoundTag one = (CompoundTag) element;
            final Lane lane = Lane.byName(one.getString(LANE));
            final Direction face = Direction.byName(one.getString(FACE));
            if (lane == null || face == null) {
                continue;
            }
            // An end that no longer reads is left at its default, so the cable keeps its wires.
            CableEnd.CODEC.parse(ops, one.get(END)).result().ifPresent(end -> this.set.put(key(lane, face), end));
        }
    }

    /* The players who see the block see how its ends are set, as the wrench shows them. */
    @Override
    public void writeClient(final CompoundTag tag, final HolderLookup.Provider registries) {
        save(tag, registries);
    }

    @Override
    public void readClient(final CompoundTag tag, final HolderLookup.Provider registries) {
        load(tag, registries);
    }

    private static int key(final Lane lane, final Direction face) {
        return lane.id() * FACES + face.get3DDataValue();
    }
}
