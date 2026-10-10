/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.audio.SoundfoundryCovers;
import dev.jstech.core.text.TextBounds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A cover the Standard Soundfoundry asked for, as the server made it: a small PNG, or nothing when there is none, in
 * which case the window makes one of the album's colours and initials.
 *
 * @param key   the cover, as a page named it
 * @param image the PNG, or empty for none
 */
public record SoundfoundryCoverPayload(String key, byte[] image) implements CustomPacketPayload {

    /** The longest a cover's key is. */
    public static final int MAX_KEY = 320;

    public static final CustomPacketPayload.Type<SoundfoundryCoverPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                    "soundfoundry_cover"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SoundfoundryCoverPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.stringUtf8(MAX_KEY), SoundfoundryCoverPayload::key,
                    ByteBufCodecs.byteArray(SoundfoundryCovers.MAX_BYTES), SoundfoundryCoverPayload::image,
                    SoundfoundryCoverPayload::new);

    /* Copied on the way in, and cut to what the wire takes, since a string past its cap throws as it is sent. */
    public SoundfoundryCoverPayload {
        key = TextBounds.clip(key, MAX_KEY);
        image = image.length <= SoundfoundryCovers.MAX_BYTES ? image.clone() : new byte[0];
    }

    @Override
    public CustomPacketPayload.Type<SoundfoundryCoverPayload> type() {
        return TYPE;
    }
}
