/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.JsComputers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A page of the Standard Soundfoundry, asked for by its window: the machine answers with what the page lists.
 *
 * @param hostPos the machine
 * @param page    one of the pages of {@link SoundfoundryPagePayload}
 * @param arg     what the page is of: an album, a playlist's name or what is searched for; empty for the others
 */
public record SoundfoundryBrowsePayload(BlockPos hostPos, int page, String arg) implements CustomPacketPayload {

    /** The longest a page's argument is: an album's id, a playlist's name or a search. */
    public static final int MAX_ARG = 256;

    public static final CustomPacketPayload.Type<SoundfoundryBrowsePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                    "soundfoundry_browse"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SoundfoundryBrowsePayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SoundfoundryBrowsePayload::hostPos,
                    ByteBufCodecs.VAR_INT, SoundfoundryBrowsePayload::page,
                    ByteBufCodecs.stringUtf8(MAX_ARG), SoundfoundryBrowsePayload::arg,
                    SoundfoundryBrowsePayload::new);

    /* Cut to what the wire takes, since a string past its cap throws as it is sent. */
    public SoundfoundryBrowsePayload {
        arg = arg.length() <= MAX_ARG ? arg : arg.substring(0, MAX_ARG);
    }

    @Override
    public CustomPacketPayload.Type<SoundfoundryBrowsePayload> type() {
        return TYPE;
    }
}
