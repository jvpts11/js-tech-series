/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.network;

import dev.jstech.space.JsSpace;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * What a Small Shipyard said as its builder's screen asked it to let its ship go, or to take the ship in its bay back:
 * that it does, its {@code going}, or why it cannot; the screen shows it over its keys.
 */
public record ShipyardSaidPayload(Component words, boolean going) implements CustomPacketPayload {

    public static final Type<ShipyardSaidPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            JsSpace.MODID, "shipyard_said"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShipyardSaidPayload> STREAM_CODEC =
            StreamCodec.composite(ComponentSerialization.TRUSTED_STREAM_CODEC, ShipyardSaidPayload::words,
                    ByteBufCodecs.BOOL, ShipyardSaidPayload::going, ShipyardSaidPayload::new);

    @Override
    public Type<ShipyardSaidPayload> type() {
        return TYPE;
    }
}
