/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.region;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.jstech.core.JsCore;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * Things a mod keeps in a dimension by where they are, each under a key, with the world: claimed areas, the beacons of
 * a network, the machines of a kind. It is a {@link SpatialIndex} per dimension, kept in that dimension's saved data,
 * so asking what is near a place reads only the regions round it. Declared once by the mod that keeps it:
 *
 * <pre>{@code
 * RegionIndex<Claim> CLAIMS = RegionIndex.declare(id("claims"), Claim.CODEC);
 * CLAIMS.put(level, "base", Box.around(x, y, z, 16), claim);
 * CLAIMS.nearest(level, pos, 64);
 * }</pre>
 *
 * @param <T> what each entry holds
 */
public final class RegionIndex<T> {

    private final ResourceLocation id;
    private final Codec<T> codec;
    private final SavedData.Factory<Save<T>> factory;

    /** A box as its two corners, six numbers. */
    private static final Codec<Box> BOX = Codec.INT.listOf().comapFlatMap(
            corners -> corners.size() == 6
                    ? DataResult.success(Box.between(corners.get(0), corners.get(1), corners.get(2),
                    corners.get(3), corners.get(4), corners.get(5)))
                    : DataResult.error(() -> "a box is six numbers, its two corners"),
            box -> List.of(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()));

    private RegionIndex(final ResourceLocation id, final Codec<T> codec) {
        this.id = id;
        this.codec = codec;
        this.factory = new SavedData.Factory<>(() -> new Save<>(codec), this::load);
    }

    /** Declares an index of entries holding {@code codec}'s values, kept under {@code id} in each dimension. */
    public static <T> RegionIndex<T> declare(final ResourceLocation id, final Codec<T> codec) {
        return new RegionIndex<>(Objects.requireNonNull(id, "id"), Objects.requireNonNull(codec, "codec"));
    }

    public ResourceLocation id() {
        return id;
    }

    /** Files {@code value} under {@code key} over {@code box} in {@code level}, in place of what the key held. */
    public void put(final ServerLevel level, final String key, final Box box, final T value) {
        final Save<T> save = save(level);
        save.index.put(key, box, value);
        save.setDirty();
    }

    /** Takes out what {@code key} held in {@code level}, and gives it back; null when it held nothing. */
    @Nullable
    public SpatialIndex.Entry<String, T> remove(final ServerLevel level, final String key) {
        final Save<T> save = save(level);
        final SpatialIndex.Entry<String, T> removed = save.index.remove(key);
        if (removed != null) {
            save.setDirty();
        }
        return removed;
    }

    @Nullable
    public SpatialIndex.Entry<String, T> get(final ServerLevel level, final String key) {
        return save(level).index.get(key);
    }

    /** Every entry of {@code level} whose box meets {@code area}. */
    public List<SpatialIndex.Entry<String, T>> within(final ServerLevel level, final Box area) {
        return save(level).index.within(area);
    }

    /** Every entry of {@code level} whose box holds {@code pos}. */
    public List<SpatialIndex.Entry<String, T>> containing(final ServerLevel level, final BlockPos pos) {
        return save(level).index.containing(pos.getX(), pos.getY(), pos.getZ());
    }

    /** The entry of {@code level} nearest {@code pos} within {@code radius} blocks, or null. */
    @Nullable
    public SpatialIndex.Entry<String, T> nearest(final ServerLevel level, final BlockPos pos, final int radius) {
        return save(level).index.nearest(pos.getX(), pos.getY(), pos.getZ(), radius);
    }

    /** How many entries {@code level} holds. */
    public int size(final ServerLevel level) {
        return save(level).index.size();
    }

    private Save<T> save(final ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(factory,
                "jstech_region_" + id.getNamespace() + "_" + id.getPath().replace('/', '_'));
    }

    private Save<T> load(final CompoundTag tag, final HolderLookup.Provider registries) {
        final Save<T> save = new Save<>(codec);
        for (final Tag one : tag.getList("entries", Tag.TAG_COMPOUND)) {
            final CompoundTag entry = (CompoundTag) one;
            final DataResult<Box> box = BOX.parse(NbtOps.INSTANCE, entry.get("box"));
            final DataResult<T> value = codec.parse(registries.createSerializationContext(NbtOps.INSTANCE),
                    entry.get("value"));
            if (box.isSuccess() && value.isSuccess()) {
                save.index.put(entry.getString("key"), box.getOrThrow(), value.getOrThrow());
            } else {
                JsCore.LOGGER.warn("The entry {} of the region index {} could not be read and is left out",
                        entry.getString("key"), id);
            }
        }
        return save;
    }

    /** The index of one dimension, as its saved data. */
    private static final class Save<V> extends SavedData {

        private final SpatialIndex<String, V> index = new SpatialIndex<>();
        private final Codec<V> codec;

        Save(final Codec<V> codec) {
            this.codec = codec;
        }

        @Override
        public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
            final ListTag entries = new ListTag();
            for (final SpatialIndex.Entry<String, V> entry : index.all()) {
                final CompoundTag one = new CompoundTag();
                one.putString("key", entry.key());
                one.put("box", BOX.encodeStart(NbtOps.INSTANCE, entry.box()).getOrThrow());
                one.put("value", codec.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE),
                        entry.value()).getOrThrow());
                entries.add(one);
            }
            tag.put("entries", entries);
            return tag;
        }
    }
}
