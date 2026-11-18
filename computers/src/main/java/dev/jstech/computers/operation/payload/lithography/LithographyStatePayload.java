/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.lithography;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.lithography.LithographyLook;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: what the lithography program's window shows, after whatever it asked.
 *
 * @param hostPos the computer the program runs on
 * @param look    the station looked at
 * @param said    what came of what it asked, a mask refused and why; nothing for a look
 * @param warn    whether that is a refusal, which the window shows as one
 */
public record LithographyStatePayload(BlockPos hostPos, LithographyLook look, Text said, boolean warn)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<LithographyStatePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "lithography_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LithographyStatePayload> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, LithographyStatePayload::hostPos,
                    LithographyLook.STREAM_CODEC, LithographyStatePayload::look, TextCodecs.STREAM_CODEC,
                    LithographyStatePayload::said, ByteBufCodecs.BOOL, LithographyStatePayload::warn,
                    LithographyStatePayload::new);

    @Override
    public CustomPacketPayload.Type<LithographyStatePayload> type() {
        return TYPE;
    }
}
