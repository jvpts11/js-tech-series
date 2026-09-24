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
 * Server to client: play a sound of the interface for this player alone, from their own screen and nowhere in the
 * world, as a program the player is using answers them.
 *
 * @param sound  the declared sound's id
 * @param volume how loud, as a share of the sound's own level
 * @param pitch  how high, 1 being the sound as recorded
 */
public record ScreenSoundPayload(ResourceLocation sound, float volume, float pitch) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ScreenSoundPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "screen_sound"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ScreenSoundPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, ScreenSoundPayload::sound,
            ByteBufCodecs.FLOAT, ScreenSoundPayload::volume,
            ByteBufCodecs.FLOAT, ScreenSoundPayload::pitch,
            ScreenSoundPayload::new);

    @Override
    public CustomPacketPayload.Type<ScreenSoundPayload> type() {
        return TYPE;
    }
}
