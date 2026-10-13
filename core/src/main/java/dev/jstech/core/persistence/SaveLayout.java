/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.persistence;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * The layout one kind of saved thing is written in: its version, and the steps that bring something written in an
 * older version up to today's. A block entity, an attachment, a world's state: each kind declares its layout once,
 * and every one of it saved carries the version it was written in.
 *
 * <p>Three ways in, one for each shape a saved thing has. A compound that is the thing itself, such as a block
 * entity's tag, carries its version under {@value #VERSION_KEY} beside its own entries ({@link #stamp}, {@link
 * #read(CompoundTag)}). A value saved whole, such as a state's file, is written as a compound with its version and the
 * value under {@value #VALUE_KEY} ({@link #wrap}, {@link #unwrap}). A value written by a codec, such as an attachment,
 * takes the same compound through {@link #codec}.
 *
 * <p>Whatever was saved before it carried a version is at version 0: a compound with no version, or a value that is
 * not wrapped. A layout that is still at its first version needs no steps for it, since what was saved then is what
 * is saved now. Something saved by a newer version of its mod is read as far as it can be, as it is, and the log says
 * so once.
 */
public final class SaveLayout {

    private final String name;
    private final UpgradeChain<Tag> chain;

    /** Where a saved compound keeps the version of the layout it was written in. */
    public static final String VERSION_KEY = "SaveVersion";
    /** Where a value saved whole sits beside its version. */
    public static final String VALUE_KEY = "Value";
    private static final Logger LOGGER = LogUtils.getLogger();

    /** The layouts the log has already said were written by a newer mod, so it says so once and not every read. */
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private SaveLayout(final String name, final UpgradeChain<Tag> chain) {
        this.name = name;
        this.chain = chain;
    }

    /** The layout of {@code name}, at its first version and with no steps. */
    public static SaveLayout of(final String name) {
        return builder(name).build();
    }

    /** Starts declaring the layout of {@code name}, which names it in the log: a registry id is a good name. */
    public static Builder builder(final String name) {
        return new Builder(name);
    }

    /** The version a saved compound says it was written in; 0 when it says none. */
    public static int versionOf(final CompoundTag tag) {
        return tag.get(VERSION_KEY) instanceof NumericTag number ? number.getAsInt() : 0;
    }

    public String name() {
        return this.name;
    }

    /** Today's version. */
    public int version() {
        return this.chain.version();
    }

    /** Whether {@code found} is a version a newer version of the mod wrote. */
    public boolean isNewer(final int found) {
        return this.chain.isNewer(found);
    }

    /** Writes today's version into a compound being saved, and gives the compound back. */
    public CompoundTag stamp(final CompoundTag tag) {
        tag.putInt(VERSION_KEY, this.chain.version());
        return tag;
    }

    /**
     * A saved compound in today's layout. When steps have to run they run on a copy, so the compound handed in is
     * left as it was; one already of today's version is handed back as it is.
     */
    public CompoundTag read(final CompoundTag tag) {
        final int found = versionOf(tag);
        warnIfNewer(found);
        if (!this.chain.needsSteps(found)) {
            return tag;
        }
        final Tag upgraded = this.chain.upgrade(tag.copy(), found);
        if (upgraded instanceof CompoundTag compound) {
            return compound;
        }
        LOGGER.error("A step of {} turned a compound into something else; it is read as it was saved", this.name);
        return tag;
    }

    /** A value saved whole: a compound holding today's version and the value. */
    public CompoundTag wrap(final Tag value) {
        final CompoundTag out = new CompoundTag();
        out.putInt(VERSION_KEY, this.chain.version());
        out.put(VALUE_KEY, value);
        return out;
    }

    /**
     * What {@link #wrap} wrote, in today's layout, with the version it was found in. A compound with no version is
     * taken whole as the value of version 0, which is what a file from before versions holds.
     */
    public Found unwrap(final CompoundTag saved) {
        final boolean versioned = saved.contains(VERSION_KEY);
        final int found = versioned ? versionOf(saved) : 0;
        final Tag value = versioned ? saved.get(VALUE_KEY) : saved;
        warnIfNewer(found);
        if (value == null) {
            return new Found(found, null);
        }
        return new Found(found, this.chain.needsSteps(found) ? this.chain.upgrade(value.copy(), found) : value);
    }

    /**
     * {@code inner} with its value wrapped as {@link #wrap} does: it writes today's version beside the value, and
     * reads an older value through the steps first. A value it finds unwrapped is one saved before the layout had
     * versions, at version 0, so an attachment that gains a layout reads what it saved before.
     */
    public <T> Codec<T> codec(final Codec<T> inner) {
        Objects.requireNonNull(inner, "inner");
        return new Codec<>() {
            @Override
            public <O> DataResult<Pair<T, O>> decode(final DynamicOps<O> ops, final O input) {
                int found = 0;
                O value = input;
                final Optional<MapLike<O>> map = ops.getMap(input).result();
                if (map.isPresent() && map.get().get(VERSION_KEY) != null && map.get().get(VALUE_KEY) != null) {
                    found = ops.getNumberValue(map.get().get(VERSION_KEY)).result().map(Number::intValue).orElse(0);
                    value = map.get().get(VALUE_KEY);
                }
                warnIfNewer(found);
                if (SaveLayout.this.chain.needsSteps(found)) {
                    value = upgraded(ops, value, found);
                }
                return inner.decode(ops, value);
            }

            @Override
            public <O> DataResult<O> encode(final T input, final DynamicOps<O> ops, final O prefix) {
                return inner.encodeStart(ops, input).flatMap(encoded -> ops.mapBuilder()
                        .add(VERSION_KEY, ops.createInt(SaveLayout.this.chain.version()))
                        .add(VALUE_KEY, encoded)
                        .build(prefix));
            }

            @Override
            public String toString() {
                return "SaveLayout[" + SaveLayout.this.name + "] " + inner;
            }
        };
    }

    /*
     * A value a codec reads, through the steps. What is already saved as tags is handed to them as it is, since a
     * conversion would reshape it (a list of ints comes back as an int array); anything else is converted to tags
     * for the steps and back. The caller's own ops read the result, so registries stay in reach.
     */
    @SuppressWarnings("unchecked")
    private <O> O upgraded(final DynamicOps<O> ops, final O value, final int found) {
        if (value instanceof Tag saved) {
            return (O) this.chain.upgrade(saved.copy(), found);
        }
        return NbtOps.INSTANCE.convertTo(ops, this.chain.upgrade(ops.convertTo(NbtOps.INSTANCE, value), found));
    }

    private void warnIfNewer(final int found) {
        if (this.chain.isNewer(found) && WARNED.add(this.name)) {
            LOGGER.warn("{} was saved by a newer version of its mod (layout {}, this one knows {}); what can be read "
                    + "is read as it is", this.name, found, this.chain.version());
        }
    }

    /**
     * A value read back, in today's layout.
     *
     * @param version the version it was saved in
     * @param value   the value, or null when the save held none
     */
    public record Found(int version, @Nullable Tag value) {
    }

    /** Declares a layout, its version and its steps; {@link #build()} checks every step starts below the version. */
    public static final class Builder {

        private final String name;
        private final UpgradeChain.Builder<Tag> chain;

        private Builder(final String name) {
            this.name = Objects.requireNonNull(name, "name");
            this.chain = UpgradeChain.builder(name);
        }

        /** Today's version, counted from 1; raised when something saved before needs a step to be read. */
        public Builder version(final int layout) {
            this.chain.version(layout);
            return this;
        }

        /** The step that takes something saved in version {@code from} to version {@code from + 1}. */
        public Builder upgrade(final int from, final ISaveUpgrade step) {
            Objects.requireNonNull(step, "step");
            this.chain.step(from, step::upgrade);
            return this;
        }

        public SaveLayout build() {
            return new SaveLayout(this.name, this.chain.build());
        }
    }
}
