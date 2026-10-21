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
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The player pressed Take on the strip of a manual read from a lectern: the manual leaves the lectern for them.
 *
 * @param pos the lectern
 */
public record LecternTakePayload(BlockPos pos) implements CustomPacketPayload {

    public static final Type<LecternTakePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            JsCore.MODID, "lectern_take"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LecternTakePayload> STREAM_CODEC =
            BlockPos.STREAM_CODEC.<RegistryFriendlyByteBuf>cast().map(LecternTakePayload::new, LecternTakePayload::pos);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
