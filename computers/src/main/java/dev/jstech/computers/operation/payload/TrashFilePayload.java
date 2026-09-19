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
 * Client to server: a desktop is deleting {@code path} from the system disk of the computer at {@code hostPos}, which
 * puts it in that desktop's trash. What is already in the trash is deleted for good instead.
 */
public record TrashFilePayload(BlockPos hostPos, String path) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TrashFilePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "trash_file"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TrashFilePayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, TrashFilePayload::hostPos,
                    ByteBufCodecs.stringUtf8(160), TrashFilePayload::path,
                    TrashFilePayload::new);

    @Override
    public CustomPacketPayload.Type<TrashFilePayload> type() {
        return TYPE;
    }
}
