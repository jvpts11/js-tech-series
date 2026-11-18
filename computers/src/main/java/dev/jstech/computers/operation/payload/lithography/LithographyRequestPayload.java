/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.lithography;

import dev.jstech.computers.JsComputers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * What the lithography program's window asks of its computer: a look at one of the stations linked to it, or a mask
 * written on that station's blank. Every answer is the program's whole state ({@link LithographyStatePayload}).
 *
 * @param hostPos the computer the program runs on
 * @param action  what is asked: {@link #LOOK} or {@link #WRITE}
 * @param station which of the computer's stations, from nought
 * @param mask    the mask to write, by its item's id; any for a look
 */
public record LithographyRequestPayload(BlockPos hostPos, int action, int station, ResourceLocation mask)
        implements CustomPacketPayload {

    public static final int LOOK = 0;
    public static final int WRITE = 1;

    public static final CustomPacketPayload.Type<LithographyRequestPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "lithography_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LithographyRequestPayload> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, LithographyRequestPayload::hostPos, ByteBufCodecs.VAR_INT,
                    LithographyRequestPayload::action, ByteBufCodecs.VAR_INT, LithographyRequestPayload::station,
                    ResourceLocation.STREAM_CODEC, LithographyRequestPayload::mask, LithographyRequestPayload::new);

    @Override
    public CustomPacketPayload.Type<LithographyRequestPayload> type() {
        return TYPE;
    }
}
