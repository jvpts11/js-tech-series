/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import dev.jstech.core.blockentity.IFieldPart;
import dev.jstech.core.persistence.SavedValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * The wires running through the middle of a block, side by side, a lane each, in the order they were laid: the bundle
 * a block of many cables holds, beside the parts on its faces. A bundle holds each wire once and at most
 * {@link #LANES} of them. The players who see the block are sent the whole bundle, which is small.
 */
public final class Bundle implements IFieldPart {

    private final List<Wire> wires = new ArrayList<>();
    private final Runnable changed;

    /** How many wires a bundle holds: three rows of three lanes. */
    public static final int LANES = 9;
    /** Where the bundle is saved. */
    public static final String KEY = "Wires";
    private static final Codec<List<Wire>> CODEC = Wire.CODEC.listOf();
    private static final Logger LOGGER = LogUtils.getLogger();

    /** A bundle that runs {@code changed} each time a wire is laid or taken out. */
    public Bundle(final Runnable changed) {
        this.changed = Objects.requireNonNull(changed, "changed");
    }

    /** The wires, lane by lane. */
    public List<Wire> wires() {
        return Collections.unmodifiableList(this.wires);
    }

    /** The wire in {@code lane}, or null when the lane is empty. */
    public @Nullable Wire at(final int lane) {
        return lane >= 0 && lane < this.wires.size() ? this.wires.get(lane) : null;
    }

    public boolean has(final Wire wire) {
        return this.wires.contains(wire);
    }

    public int size() {
        return this.wires.size();
    }

    public boolean isEmpty() {
        return this.wires.isEmpty();
    }

    /** Lays {@code wire} in the next lane; false when it is already here or every lane is taken. */
    public boolean add(final Wire wire) {
        if (has(wire) || this.wires.size() >= LANES) {
            return false;
        }
        this.wires.add(wire);
        this.changed.run();
        return true;
    }

    /** Takes {@code wire} out, the wires after it moving up a lane; false when it was not here. */
    public boolean remove(final Wire wire) {
        if (!this.wires.remove(wire)) {
            return false;
        }
        this.changed.run();
        return true;
    }

    @Override
    public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
        if (this.wires.isEmpty()) {
            return;
        }
        SavedValue.written(CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, registries), this.wires), LOGGER,
                "the wires of a block").ifPresent(written -> tag.put(KEY, written));
    }

    @Override
    public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
        this.wires.clear();
        final Tag saved = tag.get(KEY);
        if (saved == null) {
            return;
        }
        SavedValue.read(CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, registries), saved), LOGGER,
                "the wires of a block").ifPresent(read -> {
                    for (final Wire wire : read) {
                        if (!this.wires.contains(wire) && this.wires.size() < LANES) {
                            this.wires.add(wire);
                        }
                    }
                });
    }

    @Override
    public void writeClient(final CompoundTag tag, final HolderLookup.Provider registries) {
        save(tag, registries);
    }

    @Override
    public void readClient(final CompoundTag tag, final HolderLookup.Provider registries) {
        load(tag, registries);
    }
}
