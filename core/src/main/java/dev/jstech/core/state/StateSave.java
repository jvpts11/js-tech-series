/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.state;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import dev.jstech.core.persistence.SaveLayout;
import dev.jstech.core.persistence.SavedValue;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * The file a {@link CoreState} is kept in: its value, written by the state's codec beside the version of its layout,
 * and read back through the layout's steps.
 *
 * <p>A value that cannot be read whole keeps what can be read of it, and the log says what could not. A value that
 * cannot be written keeps the last that could, so a save never empties the file. A file in an older layout is written
 * again in today's at the next save; one a newer mod wrote is read as it is, and its state keeps a copy of it.
 *
 * @param <V> the value the file holds: a state's value, or each player's or team's values by their key
 */
public final class StateSave<V> extends SavedData {

    private final String name;
    private final Codec<V> codec;
    private final SaveLayout layout;
    private V value;
    /** The version a newer mod wrote the file in, until its state has kept the copy; 0 when none did. */
    private int newer;
    /** Whether the value in the file could not be read at all, until its state has kept a copy of the file. */
    private boolean unreadable;
    /** The value as last written or read, written again should the value ever fail to encode. */
    private @Nullable Tag lastWritten;

    private static final Logger LOGGER = LogUtils.getLogger();

    private StateSave(final String name, final Codec<V> codec, final SaveLayout layout, final V value) {
        this.name = name;
        this.codec = codec;
        this.layout = layout;
        this.value = value;
    }

    /**
     * How the file is made new and read back.
     *
     * @param name   what the file holds, in words, for the log
     * @param codec  what writes the value
     * @param layout the value's layout
     * @param fresh  the value of a file that holds nothing yet
     * @param adopt  what the file keeps of a value it read, such as a map it can change
     */
    public static <V> SavedData.Factory<StateSave<V>> factory(final String name, final Codec<V> codec,
                                                              final SaveLayout layout, final Supplier<V> fresh,
                                                              final UnaryOperator<V> adopt) {
        return new SavedData.Factory<>(() -> new StateSave<>(name, codec, layout, fresh.get()),
                (tag, registries) -> load(name, codec, layout, fresh, adopt, tag, registries), null);
    }

    /** The value the file holds. */
    public V value() {
        return this.value;
    }

    @Override
    public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
        final DynamicOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries);
        final Tag written = SavedValue.written(this.codec.encodeStart(ops, this.value), LOGGER, this.name)
                .orElse(this.lastWritten);
        if (written == null) {
            return tag;
        }
        this.lastWritten = written;
        return this.layout.wrap(written);
    }

    /** Whether the file held a value that could not be read at all, once; false when it was already asked. */
    boolean takeUnreadable() {
        final boolean found = this.unreadable;
        this.unreadable = false;
        return found;
    }

    /** Puts {@code next} in the file, to be written at the next save. */
    void replace(final V next) {
        this.value = next;
        setDirty();
    }

    /** The value changed where it is held, and is to be written at the next save. */
    void changed() {
        setDirty();
    }

    /** The version a newer mod wrote the file in, once; 0 when none did, or when it was already asked. */
    int takeNewer() {
        final int found = this.newer;
        this.newer = 0;
        return found;
    }

    private static <V> StateSave<V> load(final String name, final Codec<V> codec, final SaveLayout layout,
                                         final Supplier<V> fresh, final UnaryOperator<V> adopt,
                                         final CompoundTag tag, final HolderLookup.Provider registries) {
        final SaveLayout.Found found = layout.unwrap(tag);
        V value = fresh.get();
        boolean unreadable = false;
        if (found.value() != null) {
            final DynamicOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries);
            final Optional<V> read = SavedValue.read(codec.parse(ops, found.value()), LOGGER, name).map(adopt);
            unreadable = read.isEmpty() && !layout.isNewer(found.version());
            value = read.orElse(value);
        }
        final StateSave<V> save = new StateSave<>(name, codec, layout, value);
        save.unreadable = unreadable;
        if (layout.isNewer(found.version())) {
            save.newer = found.version();
        } else {
            save.lastWritten = found.value();
            if (found.version() < layout.version()) {
                save.setDirty();
            }
        }
        return save;
    }
}
