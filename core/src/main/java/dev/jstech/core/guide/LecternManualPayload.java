/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import dev.jstech.core.JsCore;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The player used a lectern holding a manual: their game opens the manual, read from that lectern.
 *
 * @param pos    the lectern
 * @param manual the id of the manual lying on it
 */
public record LecternManualPayload(BlockPos pos, String manual) implements CustomPacketPayload {

    public static final Type<LecternManualPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            JsCore.MODID, "lectern_manual"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LecternManualPayload> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, LecternManualPayload::pos,
                    ByteBufCodecs.stringUtf8(TornSheets.MOST_ID), LecternManualPayload::manual,
                    LecternManualPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
