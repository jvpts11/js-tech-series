/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * What Soundfoundry asks of the Vinyl RW Drive on its computer's cable: how it is, to record the disc on its platter,
 * to stop, to cut a recording onto the blank on it, or to eject the record.
 *
 * @param hostPos the computer
 * @param action  one of the actions below
 * @param path    the recording to cut, for {@link #CUT}
 */
public record VinylActionPayload(BlockPos hostPos, int action, String path) implements CustomPacketPayload {

    public static final int LOOK = 0;
    public static final int RECORD = 1;
    public static final int STOP = 2;
    public static final int CUT = 3;
    public static final int EJECT = 4;

    public static final CustomPacketPayload.Type<VinylActionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "vinyl_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VinylActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, VinylActionPayload::hostPos,
                    ByteBufCodecs.VAR_INT, VinylActionPayload::action,
                    ByteBufCodecs.stringUtf8(VinylStatePayload.MOST_TEXT), VinylActionPayload::path,
                    VinylActionPayload::new);

    @Override
    public CustomPacketPayload.Type<VinylActionPayload> type() {
        return TYPE;
    }
}
