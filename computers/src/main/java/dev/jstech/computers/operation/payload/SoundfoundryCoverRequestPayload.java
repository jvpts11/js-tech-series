/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.JsComputers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The Standard Soundfoundry asks for a cover a page named, which the server answers with a
 * {@link SoundfoundryCoverPayload}.
 *
 * @param hostPos the machine whose window asks, which the player has to be at
 * @param key     the cover, as a page named it
 */
public record SoundfoundryCoverRequestPayload(BlockPos hostPos, String key) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SoundfoundryCoverRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                    "soundfoundry_cover_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SoundfoundryCoverRequestPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SoundfoundryCoverRequestPayload::hostPos,
                    ByteBufCodecs.stringUtf8(SoundfoundryCoverPayload.MAX_KEY), SoundfoundryCoverRequestPayload::key,
                    SoundfoundryCoverRequestPayload::new);

    /* Cut to what the wire takes, since a string past its cap throws as it is sent. */
    public SoundfoundryCoverRequestPayload {
        key = key.length() <= SoundfoundryCoverPayload.MAX_KEY ? key
                : key.substring(0, SoundfoundryCoverPayload.MAX_KEY);
    }

    @Override
    public CustomPacketPayload.Type<SoundfoundryCoverRequestPayload> type() {
        return TYPE;
    }
}
