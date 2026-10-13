/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.media;

import dev.jstech.core.JsCore;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A recording playing near a player, from the point it has reached: the client plays it from each place it is heard
 * from, fetching it first when it does not have it, and starts that much further in for the time the fetching took.
 * A second one under the same key takes the place of the first.
 *
 * @param key          what is playing, so it can be replaced and stopped
 * @param sound        the declared sound it is played as, made as it plays, for its channel and its subtitle
 * @param media        the recording
 * @param places       where it is heard from
 * @param volume       how loud, as a share of the sound's own level
 * @param offsetMillis how far into it the playing had got when this was sent
 */
public record MediaPlayPayload(String key, ResourceLocation sound, MediaId media, List<MediaPlace> places,
                               float volume, long offsetMillis) implements CustomPacketPayload {

    public static final int MAX_KEY = 128;
    public static final int MAX_PLACES = 16;

    public static final CustomPacketPayload.Type<MediaPlayPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "media_play"));

    public static final StreamCodec<ByteBuf, MediaPlayPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_KEY), MediaPlayPayload::key,
            ResourceLocation.STREAM_CODEC, MediaPlayPayload::sound,
            MediaCodecs.ID, MediaPlayPayload::media,
            MediaCodecs.PLACE.apply(ByteBufCodecs.list(MAX_PLACES)), MediaPlayPayload::places,
            ByteBufCodecs.FLOAT, MediaPlayPayload::volume,
            ByteBufCodecs.VAR_LONG, MediaPlayPayload::offsetMillis,
            MediaPlayPayload::new);

    public MediaPlayPayload {
        places = List.copyOf(places.subList(0, Math.min(places.size(), MAX_PLACES)));
    }

    @Override
    public CustomPacketPayload.Type<MediaPlayPayload> type() {
        return TYPE;
    }
}
