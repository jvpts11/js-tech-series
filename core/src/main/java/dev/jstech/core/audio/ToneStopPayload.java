/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import dev.jstech.core.JsCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The notes playing in a voice stop before their end: another sound took the voice, or the machine went off.
 *
 * @param voice the voice they play in, as {@link ToneSoundPayload#voice()} named it
 */
public record ToneStopPayload(String voice) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ToneStopPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "tone_stop"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToneStopPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(ToneSoundPayload.MAX_VOICE), ToneStopPayload::voice,
            ToneStopPayload::new);

    /* Cut to what the wire takes here, since a string past its cap throws as it is sent. */
    public ToneStopPayload {
        voice = voice.length() <= ToneSoundPayload.MAX_VOICE ? voice : voice.substring(0, ToneSoundPayload.MAX_VOICE);
    }

    @Override
    public CustomPacketPayload.Type<ToneStopPayload> type() {
        return TYPE;
    }
}
