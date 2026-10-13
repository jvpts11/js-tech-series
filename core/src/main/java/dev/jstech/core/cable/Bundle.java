/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import dev.jstech.core.blockentity.IFieldPart;
import dev.jstech.core.persistence.SavedValue;
import java.util.ArrayList;
import java.util.Arrays;
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
 * The wires running through a cable block, each in its lane: the bundle a block of many cables holds, beside the parts
 * on its faces. A lane holds one wire, so a block holds one wire of each line, and a cable that never shares a block
 * holds it alone. The players who see the block are sent the whole bundle, which is small.
 */
public final class Bundle implements IFieldPart {

    private final Wire[] lanes = new Wire[Lane.COUNT];
    private final Runnable changed;

    /** Where the bundle is saved. */
    public static final String KEY = "Wires";
    private static final Codec<List<Wire>> CODEC = Wire.CODEC.listOf();
    private static final Logger LOGGER = LogUtils.getLogger();

    /** A bundle that runs {@code changed} each time a wire is laid, dyed or taken out. */
    public Bundle(final Runnable changed) {
        this.changed = Objects.requireNonNull(changed, "changed");
    }

    /** The wires, lane by lane. */
    public List<Wire> wires() {
        final List<Wire> out = new ArrayList<>(this.lanes.length);
        for (final Wire wire : this.lanes) {
            if (wire != null) {
                out.add(wire);
            }
        }
        return out;
    }

    /** The wire in {@code lane}, or null when the lane is empty. */
    public @Nullable Wire at(final Lane lane) {
        return this.lanes[lane.id()];
    }

    /** The wire of {@code type}, in whatever colour, or null when there is none. */
    public @Nullable Wire of(final CableType type) {
        final Wire wire = at(Wire.of(type).slot());
        return wire != null && wire.type() == type ? wire : null;
    }

    public boolean has(final CableType type) {
        return of(type) != null;
    }

    public int size() {
        int count = 0;
        for (final Wire wire : this.lanes) {
            if (wire != null) {
                count++;
            }
        }
        return count;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    /** Whether the bundle holds a cable that never shares a block. */
    public boolean holdsAlone() {
        final Wire middle = at(Lane.MIDDLE);
        return middle != null && middle.type().alone();
    }

    /**
     * Whether {@code wire} could be laid: its lane is free, and neither it nor what is here already is a cable that
     * never shares a block.
     */
    public boolean takes(final Wire wire) {
        if (wire.type().alone()) {
            return isEmpty();
        }
        return !holdsAlone() && at(wire.slot()) == null;
    }

    /** Lays {@code wire} in its lane; false when the bundle does not take it. */
    public boolean add(final Wire wire) {
        if (!takes(wire)) {
            return false;
        }
        this.lanes[wire.slot().id()] = wire;
        this.changed.run();
        return true;
    }

    /** Takes out the wire of {@code type}; false when there was none. */
    public boolean remove(final CableType type) {
        final Wire wire = of(type);
        if (wire == null) {
            return false;
        }
        this.lanes[wire.slot().id()] = null;
        this.changed.run();
        return true;
    }

    /** Puts {@code dyed} in place of the wire of its cable; false when there is none or it is already that colour. */
    public boolean dye(final Wire dyed) {
        final Wire wire = of(dyed.type());
        if (wire == null || wire.equals(dyed)) {
            return false;
        }
        this.lanes[dyed.slot().id()] = dyed;
        this.changed.run();
        return true;
    }

    @Override
    public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
        final List<Wire> wires = wires();
        if (wires.isEmpty()) {
            return;
        }
        SavedValue.written(CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, registries), wires), LOGGER,
                "the wires of a block").ifPresent(written -> tag.put(KEY, written));
    }

    @Override
    public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
        Arrays.fill(this.lanes, null);
        final Tag saved = tag.get(KEY);
        if (saved == null) {
            return;
        }
        SavedValue.read(CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, registries), saved), LOGGER,
                "the wires of a block").ifPresent(read -> {
                    for (final Wire wire : read) {
                        if (takes(wire)) {
                            this.lanes[wire.slot().id()] = wire;
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
