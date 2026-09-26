/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.media;

import dev.jstech.core.audio.FrequencyResponse;
import dev.jstech.core.audio.StereoSide;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * How the recordings' own records travel: their names and the places they are heard from. Kept apart from the records
 * themselves, which stay free of the game so they can be tested without it.
 */
public final class MediaCodecs {

    public static final StreamCodec<ByteBuf, MediaId> ID = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MediaId.HASH_DIGITS), MediaId::hash,
            ByteBufCodecs.stringUtf8(MediaId.MAX_FORMAT), MediaId::format,
            ByteBufCodecs.VAR_LONG, MediaId::bytes,
            MediaId::new);

    private static final StreamCodec<ByteBuf, StereoSide> SIDE =
            ByteBufCodecs.VAR_INT.map(StereoSide::byId, StereoSide::id);

    private static final StreamCodec<ByteBuf, FrequencyResponse> RESPONSE = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FrequencyResponse::maxSampleRate,
            ByteBufCodecs.VAR_INT, FrequencyResponse::bits,
            ByteBufCodecs.VAR_INT, FrequencyResponse::lowCutHz,
            ByteBufCodecs.VAR_INT, FrequencyResponse::highCutHz,
            FrequencyResponse::new);

    public static final StreamCodec<ByteBuf, MediaPlace> PLACE = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, MediaPlace::x,
            ByteBufCodecs.DOUBLE, MediaPlace::y,
            ByteBufCodecs.DOUBLE, MediaPlace::z,
            SIDE, MediaPlace::side,
            RESPONSE, MediaPlace::response,
            MediaPlace::new);

    private MediaCodecs() {
    }
}
