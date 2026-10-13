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
 * @param voice    the voice of a sound device it plays in, which the server can stop it by ({@link ToneStopPayload}),
 *                 or {@code ""} for notes nothing stops
 */
public record ToneSoundPayload(ResourceLocation sound, boolean onScreen, double x, double y, double z,
                               List<Tone> tones, String voice) implements CustomPacketPayload {

    /** The most notes one payload carries; a longer tune is sent in parts. */
    public static final int MAX_TONES = 1024;
    /** The longest a voice is named. */
    public static final int MAX_VOICE = 128;

    public static final CustomPacketPayload.Type<ToneSoundPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "tone_sound"));

    private static final StreamCodec<RegistryFriendlyByteBuf, Tone> TONE = StreamCodec.composite(
            StableCodecs.byId(Waveform.class, Waveform.SQUARE), Tone::wave,
            ByteBufCodecs.DOUBLE, Tone::frequency,
            ByteBufCodecs.VAR_INT, Tone::millis,
            ByteBufCodecs.FLOAT, Tone::volume,
            Tone::new);

    private static final StreamCodec<RegistryFriendlyByteBuf, List<Tone>> TONES =
            TONE.apply(ByteBufCodecs.list(MAX_TONES));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToneSoundPayload> STREAM_CODEC =
            StreamCodec.of(ToneSoundPayload::encode, ToneSoundPayload::decode);

    /* Cut to what the wire takes here, since a string past its cap throws as it is sent. */
    public ToneSoundPayload {
        tones = List.copyOf(tones);
        voice = voice.length() <= MAX_VOICE ? voice : voice.substring(0, MAX_VOICE);
    }

    /** Notes that nothing stops before they end. */
    public ToneSoundPayload(final ResourceLocation sound, final boolean onScreen, final double x, final double y,
                            final double z, final List<Tone> tones) {
        this(sound, onScreen, x, y, z, tones, "");
    }

    @Override
    public CustomPacketPayload.Type<ToneSoundPayload> type() {
        return TYPE;
    }

    private static void encode(final RegistryFriendlyByteBuf buf, final ToneSoundPayload payload) {
        ResourceLocation.STREAM_CODEC.encode(buf, payload.sound);
        buf.writeBoolean(payload.onScreen);
        buf.writeDouble(payload.x);
        buf.writeDouble(payload.y);
        buf.writeDouble(payload.z);
        TONES.encode(buf, payload.tones);
        buf.writeUtf(payload.voice, MAX_VOICE);
    }

    private static ToneSoundPayload decode(final RegistryFriendlyByteBuf buf) {
        return new ToneSoundPayload(ResourceLocation.STREAM_CODEC.decode(buf), buf.readBoolean(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), TONES.decode(buf), buf.readUtf(MAX_VOICE));
    }
}
