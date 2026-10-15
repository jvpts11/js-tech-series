/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.JsCore;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A player, sneaking with a wrench, turned the mouse wheel at a machine: the wrench shows the machine's next resource.
 *
 * @param pos the machine aimed at
 * @param up  whether the wheel turned up, to the next; down, to the one before
 */
public record WrenchResourcePayload(BlockPos pos, boolean up) implements CustomPacketPayload {

    public static final Type<WrenchResourcePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            JsCore.MODID, "wrench_resource"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WrenchResourcePayload> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, WrenchResourcePayload::pos, ByteBufCodecs.BOOL,
                    WrenchResourcePayload::up, WrenchResourcePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
