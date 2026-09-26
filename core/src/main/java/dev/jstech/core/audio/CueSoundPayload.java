/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import dev.jstech.core.JsCore;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A cue the server raises, sent to the players who hear it with the context its sound is picked by; each client picks
 * the sound from the bindings its own resource packs give.
 *
 * @param cue      the cue's id
 * @param onScreen whether it comes from the player's own screen rather than from the point it names
 * @param x        where it comes from, for a cue of the world
 * @param y        where it comes from, for a cue of the world
 * @param z        where it comes from, for a cue of the world
 * @param context  what its sound is picked by
 * @param volume   how loud, from 0 to 1
 * @param pitch    how high, 1 as recorded
 * @param side     which side of a stereo recording the speaker it comes out of plays
 * @param response what that speaker can reproduce
 */
public record CueSoundPayload(ResourceLocation cue, boolean onScreen, double x, double y, double z,
                              SoundContext context, float volume, float pitch, StereoSide side,
                              FrequencyResponse response) implements CustomPacketPayload {

    /** A cue played whole, both sides together and nothing taken away. */
    public CueSoundPayload(final ResourceLocation cue, final boolean onScreen, final double x, final double y,
                           final double z, final SoundContext context, final float volume, final float pitch) {
        this(cue, onScreen, x, y, z, context, volume, pitch, StereoSide.BOTH, FrequencyResponse.FULL);
    }

    public static final CustomPacketPayload.Type<CueSoundPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "cue_sound"));

    private static final StreamCodec<RegistryFriendlyByteBuf, Map<String, String>> VALUES = ByteBufCodecs.map(
            HashMap::new, ByteBufCodecs.stringUtf8(SoundContext.MAX_LENGTH),
            ByteBufCodecs.stringUtf8(SoundContext.MAX_LENGTH), SoundContext.MAX_DIMENSIONS);

    public static final StreamCodec<RegistryFriendlyByteBuf, CueSoundPayload> STREAM_CODEC =
            StreamCodec.ofMember(CueSoundPayload::write, CueSoundPayload::read);

    @Override
    public CustomPacketPayload.Type<CueSoundPayload> type() {
        return TYPE;
    }

    private void write(final RegistryFriendlyByteBuf buf) {
        ResourceLocation.STREAM_CODEC.encode(buf, cue);
        buf.writeBoolean(onScreen);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        VALUES.encode(buf, context.values());
        buf.writeFloat(volume);
        buf.writeFloat(pitch);
        buf.writeVarInt(side.id());
        buf.writeVarInt(response.maxSampleRate());
        buf.writeVarInt(response.bits());
        buf.writeVarInt(response.lowCutHz());
        buf.writeVarInt(response.highCutHz());
    }

    private static CueSoundPayload read(final RegistryFriendlyByteBuf buf) {
        return new CueSoundPayload(ResourceLocation.STREAM_CODEC.decode(buf), buf.readBoolean(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), new SoundContext(VALUES.decode(buf)), buf.readFloat(),
                buf.readFloat(), StereoSide.byId(buf.readVarInt()),
                new FrequencyResponse(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
    }
}
