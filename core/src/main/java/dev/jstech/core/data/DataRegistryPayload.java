/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.data;

import dev.jstech.core.JsCore;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Some of the values of a synced {@link DataRegistry} the server read, sent to a player's game: a registry bigger than
 * one message's tag may be goes in parts, in order, and the player's game takes the values once the last has come.
 *
 * @param registry the registry's id
 * @param entries  each value of this part by its file id, as the registry's codec writes it
 * @param part     which part this is, from nought
 * @param parts    how many parts the registry's values go in
 */
public record DataRegistryPayload(ResourceLocation registry, CompoundTag entries, int part, int parts)
        implements CustomPacketPayload {

    public static final Type<DataRegistryPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "data_registry"));
    public static final StreamCodec<ByteBuf, DataRegistryPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, DataRegistryPayload::registry,
            ByteBufCodecs.COMPOUND_TAG, DataRegistryPayload::entries,
            ByteBufCodecs.VAR_INT, DataRegistryPayload::part,
            ByteBufCodecs.VAR_INT, DataRegistryPayload::parts,
            DataRegistryPayload::new);
    /*
     * The most a message's tag may be as a player's game reads it, by the game's own count of a tag's size; and the
     * most a part holds, half of it, so a part is always read whatever its values' own overhead.
     */
    private static final long MOST_READ = 2_097_152;
    private static final long MOST_PART = MOST_READ / 2;
    /* What the game counts a tag holding values, and each value beside its own size and its key, as costing. */
    private static final long TAG_COST = 48;
    private static final long VALUE_COST = 64;

    /** All of a registry's values in one part, as before they could go in parts. */
    public DataRegistryPayload(final ResourceLocation registry, final CompoundTag entries) {
        this(registry, entries, 0, 1);
    }

    /**
     * The values {@code entries} of {@code registry} as the messages that carry them: as few as keep each part under
     * what one message's tag may be, in the order of the values' ids. A value too big to go in any message stays
     * behind, told in the log, as it would drop every joining player.
     */
    public static List<DataRegistryPayload> parts(final ResourceLocation registry, final CompoundTag entries) {
        final List<CompoundTag> parts = new ArrayList<>();
        CompoundTag part = new CompoundTag();
        long size = TAG_COST;
        for (final String key : new TreeSet<>(entries.getAllKeys())) {
            final Tag value = entries.get(key);
            final long cost = VALUE_COST + 2L * key.length() + value.sizeInBytes();
            if (TAG_COST + cost > MOST_READ) {
                JsCore.LOGGER.warn("The {} value {} is too big to send to the players and is left out", registry, key);
                continue;
            }
            if (size + cost > MOST_PART && !part.isEmpty()) {
                parts.add(part);
                part = new CompoundTag();
                size = TAG_COST;
            }
            part.put(key, value);
            size += cost;
        }
        parts.add(part);
        final List<DataRegistryPayload> out = new ArrayList<>(parts.size());
        for (int each = 0; each < parts.size(); each++) {
            out.add(new DataRegistryPayload(registry, parts.get(each), each, parts.size()));
        }
        return List.copyOf(out);
    }

    /** Whether this is the last part of the registry's values. */
    public boolean last() {
        return this.part + 1 >= this.parts;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
