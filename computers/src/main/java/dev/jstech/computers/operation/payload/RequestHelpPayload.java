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
 * Client to server: the Help window asking for what this computer can do, and for one page of it.
 *
 * <p>The window knows nothing of its own: which commands a machine has is the machine's answer, and the page
 * is written by the command. So it asks for both, and asks again whenever the player opens another page.
 *
 * @param name the command whose page is wanted, or empty for the list alone and the page it opens on
 */
public record RequestHelpPayload(BlockPos hostPos, String name) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RequestHelpPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "request_help"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestHelpPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RequestHelpPayload::hostPos,
                    ByteBufCodecs.STRING_UTF8, RequestHelpPayload::name,
                    RequestHelpPayload::new);

    @Override
    public CustomPacketPayload.Type<RequestHelpPayload> type() {
        return TYPE;
    }
}
