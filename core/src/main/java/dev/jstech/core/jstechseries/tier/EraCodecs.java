/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.tier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

/**
 * How an {@link Era} is saved and sent. A save and a data file keep its id, so an era an add-on puts between two moves
 * no other; a data file may also name one of the series' seven bare ({@code "legacy"}). A packet sends its place in
 * the order, which is the same on both sides, since both load the same mods.
 */
public final class EraCodecs {

    /** An era by its id; a bare name is one of the series' seven. */
    public static final Codec<Era> CODEC = Codec.STRING.comapFlatMap(EraCodecs::read, Era::id);
    /** An era by its place in the order. */
    public static final StreamCodec<ByteBuf, Era> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(Eras::at, Era::level);
    /** An era or none (null), by its place in the order, with -1 for none. */
    public static final StreamCodec<ByteBuf, Era> OPTIONAL_STREAM_CODEC = ByteBufCodecs.VAR_INT.map(
            EraCodecs::orNone, EraCodecs::placeOf);

    private EraCodecs() {
    }

    /** The era saved as {@code id}, or null for none: what a save that may hold no era reads back. */
    public static @Nullable Era find(final @Nullable String id) {
        return id == null || id.isEmpty() ? null : Eras.byName(id).orElse(null);
    }

    /** The era sent as {@code place} in the order, or null for -1, the place of none. */
    public static @Nullable Era orNone(final int place) {
        return place < 0 ? null : Eras.at(place);
    }

    /** The place {@code era} is sent as, -1 for none. */
    public static int placeOf(final @Nullable Era era) {
        return era == null ? -1 : era.level();
    }

    private static DataResult<Era> read(final String id) {
        return Eras.byName(id).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "no era " + id));
    }
}
