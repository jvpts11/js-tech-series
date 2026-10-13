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
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: what the desktop on a monitor's face holds, the same listing a desktop opened at the machine is
 * handed, addressed to the face of that monitor rather than to any screen the player has open.
 *
 * @param monitor the monitor whose face it is for
 * @param listing the desktop's listing
 */
public record FaceListingPayload(BlockPos monitor, DesktopFilesPayload listing) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<FaceListingPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "face_listing"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FaceListingPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, FaceListingPayload::monitor,
                    DesktopFilesPayload.STREAM_CODEC, FaceListingPayload::listing,
                    FaceListingPayload::new);

    @Override
    public CustomPacketPayload.Type<FaceListingPayload> type() {
        return TYPE;
    }
}
