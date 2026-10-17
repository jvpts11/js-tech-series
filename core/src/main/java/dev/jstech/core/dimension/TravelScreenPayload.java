/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.dimension;

import dev.jstech.core.JsCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: the passage this player is about to make to another dimension is shown through the screen
 * declared as {@code screen}, in place of the game's own while the land is loaded.
 *
 * @param screen the declared screen's id
 */
public record TravelScreenPayload(ResourceLocation screen) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TravelScreenPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "travel_screen"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TravelScreenPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, TravelScreenPayload::screen,
            TravelScreenPayload::new);

    @Override
    public CustomPacketPayload.Type<TravelScreenPayload> type() {
        return TYPE;
    }
}
