/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.physics;

import dev.jstech.core.JsCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The laws a player carries with them, sent to their game, which moves them: a player's fall is worked out on their own
 * game, so it must know how they are pulled.
 *
 * @param laws the laws they carry; none of their own for none
 */
public record OwnLawsPayload(Laws laws) implements CustomPacketPayload {

    public static final Type<OwnLawsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "own_laws"));
    public static final StreamCodec<ByteBuf, OwnLawsPayload> STREAM_CODEC = ByteBufCodecs.fromCodec(Laws.CODEC)
            .map(OwnLawsPayload::new, OwnLawsPayload::laws);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
