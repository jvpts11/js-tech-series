/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What the team asking for a landing map knows of its ground: which of its pixels it has seen, which of those are too
 * steep to land on, how much of the whole world it has seen, and the whole world's cells seen, for the corner of the
 * map. Each set of pixels or cells is a bit set's bytes, a pixel's bit at its row times the width plus its column.
 *
 * @param known the map's pixels seen
 * @param steep the map's pixels seen too steep to land on
 * @param share how much of the whole world is seen, from 0 to 1
 * @param cover the whole world's cells seen, {@link #COVER_WIDE} round and {@link #COVER_TALL} from pole to pole
 */
public record LandingSight(byte[] known, byte[] steep, double share, byte[] cover) {

    /** How many cells round and from pole to pole the whole world's coverage is drawn in. */
    public static final int COVER_WIDE = 64;
    public static final int COVER_TALL = 32;
    /* The most bytes a set of a map's pixels holds: a bit for each of the largest map's. */
    private static final int MOST_BYTES = 512 * 512 / 8;
    public static final StreamCodec<ByteBuf, LandingSight> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.byteArray(MOST_BYTES), LandingSight::known,
            ByteBufCodecs.byteArray(MOST_BYTES), LandingSight::steep,
            ByteBufCodecs.DOUBLE, LandingSight::share,
            ByteBufCodecs.byteArray(MOST_BYTES), LandingSight::cover,
            LandingSight::new);

    public LandingSight {
        known = known.clone();
        steep = steep.clone();
        cover = cover.clone();
    }
}
