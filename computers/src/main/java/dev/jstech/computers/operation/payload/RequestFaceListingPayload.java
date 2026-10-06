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
 * Client to server: a monitor's face in the world, showing its machine's desktop, wants what that desktop holds (the
 * files and folders on it, what is pinned to its panel, how it is set to look), so the face shows the same desktop a
 * player at the machine sees. The server answers with a {@link FaceListingPayload}, to a player near enough to see the
 * face, for whatever machine the monitor shows.
 *
 * @param monitor the monitor whose face asks
 */
public record RequestFaceListingPayload(BlockPos monitor) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RequestFaceListingPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "request_face_listing"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestFaceListingPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RequestFaceListingPayload::monitor,
                    RequestFaceListingPayload::new);

    @Override
    public CustomPacketPayload.Type<RequestFaceListingPayload> type() {
        return TYPE;
    }
}
