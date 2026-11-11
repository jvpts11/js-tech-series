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
 * What Easy Disc Creator asks of its computer: how its drives and its copy stand, to start a copy from one drive onto
 * another, to go on once the blank asked for is in, or to stop.
 *
 * @param hostPos the computer
 * @param action  one of the actions below
 * @param source  the drive to copy from, by its place
 * @param target  the drive to write on, by its place
 * @param copies  how many copies
 */
public record DiscCopyActionPayload(BlockPos hostPos, int action, long source, long target, int copies)
        implements CustomPacketPayload {

    public static final int LOOK = 0;
    public static final int START = 1;
    public static final int GO_ON = 2;
    public static final int STOP = 3;

    public static final CustomPacketPayload.Type<DiscCopyActionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "disc_copy_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DiscCopyActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, DiscCopyActionPayload::hostPos,
                    ByteBufCodecs.VAR_INT, DiscCopyActionPayload::action,
                    ByteBufCodecs.VAR_LONG, DiscCopyActionPayload::source,
                    ByteBufCodecs.VAR_LONG, DiscCopyActionPayload::target,
                    ByteBufCodecs.VAR_INT, DiscCopyActionPayload::copies,
                    DiscCopyActionPayload::new);

    @Override
    public CustomPacketPayload.Type<DiscCopyActionPayload> type() {
        return TYPE;
    }
}
