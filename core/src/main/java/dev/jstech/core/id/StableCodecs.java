/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.id;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Codecs that carry an enum constant by what it declares itself as: its {@link IStableName name} in data files and
 * item components, its {@link IStableId id} on the wire. Kept apart from {@link StableIds} and {@link StableNames} so
 * those stay free of Minecraft types.
 */
public final class StableCodecs {

    private StableCodecs() {
    }

    /** A constant as its name; a name no constant declares is a decoding error that lists the names there are. */
    public static <E extends Enum<E> & IStableName> Codec<E> byName(final Class<E> type) {
        final StableNames<E> names = StableNames.of(type);
        return Codec.STRING.comapFlatMap(name -> {
            final E found = names.find(name);
            return found != null ? DataResult.success(found) : DataResult.error(() ->
                    "Unknown " + names.typeName() + " '" + name + "', expected one of " + names.names());
        }, IStableName::serializedName);
    }

    /** A constant as its id in one byte; an id no constant declares reads as {@code fallback}. */
    public static <E extends Enum<E> & IStableId> StreamCodec<ByteBuf, E> byId(final Class<E> type,
                                                                                final E fallback) {
        final StableIds<E> ids = StableIds.of(type);
        return ByteBufCodecs.BYTE.map(id -> ids.byId(id, fallback), constant -> (byte) constant.id());
    }
}
