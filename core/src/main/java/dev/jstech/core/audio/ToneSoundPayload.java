/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import dev.jstech.core.JsCore;
import dev.jstech.core.audio.pcm.Tone;
import dev.jstech.core.audio.pcm.Waveform;
import dev.jstech.core.id.StableCodecs;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A synthesised sound the server sends to the players who hear it: the notes themselves, which the client makes into
 * samples, rather than a recording, so a program's tune costs a few bytes a note on the wire.
 *
 * @param sound    the declared sound it is played as, made as it plays, for its channel and its subtitle
 * @param onScreen whether it comes from the player's own screen rather than from the point it names
 * @param x        where it comes from, for a sound of the world
 * @param y        where it comes from, for a sound of the world
 * @param z        where it comes from, for a sound of the world
 * @param tones    the notes, in order
 */
public record ToneSoundPayload(ResourceLocation sound, boolean onScreen, double x, double y, double z,
                               List<Tone> tones) implements CustomPacketPayload {

    /** The most notes one payload carries; a longer tune is sent in parts. */
    public static final int MAX_TONES = 1024;

    public static final CustomPacketPayload.Type<ToneSoundPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "tone_sound"));

    private static final StreamCodec<RegistryFriendlyByteBuf, Tone> TONE = StreamCodec.composite(
            StableCodecs.byId(Waveform.class, Waveform.SQUARE), Tone::wave,
            ByteBufCodecs.DOUBLE, Tone::frequency,
            ByteBufCodecs.VAR_INT, Tone::millis,
            ByteBufCodecs.FLOAT, Tone::volume,
            Tone::new);

    public static final StreamCodec<RegistryFriendlyByteBuf, ToneSoundPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, ToneSoundPayload::sound,
            ByteBufCodecs.BOOL, ToneSoundPayload::onScreen,
            ByteBufCodecs.DOUBLE, ToneSoundPayload::x,
            ByteBufCodecs.DOUBLE, ToneSoundPayload::y,
            ByteBufCodecs.DOUBLE, ToneSoundPayload::z,
            TONE.apply(ByteBufCodecs.list(MAX_TONES)), ToneSoundPayload::tones,
            ToneSoundPayload::new);

    public ToneSoundPayload {
        tones = List.copyOf(tones);
    }

    @Override
    public CustomPacketPayload.Type<ToneSoundPayload> type() {
        return TYPE;
    }
}
