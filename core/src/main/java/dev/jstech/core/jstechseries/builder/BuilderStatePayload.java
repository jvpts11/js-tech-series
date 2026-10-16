/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.builder;

import dev.jstech.core.JsCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * How the player set the Multiblock Builder in a hand, for the server to keep on the builder.
 *
 * @param mainHand whether it is in the main hand, else the other
 * @param state    how it is set
 */
public record BuilderStatePayload(boolean mainHand, BuilderState state) implements CustomPacketPayload {

    public static final Type<BuilderStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            JsCore.MODID, "builder_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BuilderStatePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, BuilderStatePayload::mainHand,
                    BuilderState.STREAM_CODEC, BuilderStatePayload::state, BuilderStatePayload::new);

    @Override
    public Type<BuilderStatePayload> type() {
        return TYPE;
    }
}
