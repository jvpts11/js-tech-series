/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.update;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Client to server: what the Update window shows for {@code item}, the item as the network holds it, asked by the
 * computer at {@code hostPos} through its monitor at {@code monitorPos}. {@code name} is what the window's name field
 * holds, which the anvil's result and price depend on; empty keeps the item's own.
 */
public record UpdatePreviewRequestPayload(BlockPos monitorPos, BlockPos hostPos, ItemStack item, String name)
        implements CustomPacketPayload {

    /** The longest name the field sends, a little over an anvil's own limit so a cut name still reads as cut. */
    public static final int MAX_NAME = 64;

    public static final CustomPacketPayload.Type<UpdatePreviewRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "update_preview_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdatePreviewRequestPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, UpdatePreviewRequestPayload::monitorPos,
                    BlockPos.STREAM_CODEC, UpdatePreviewRequestPayload::hostPos,
                    ItemStack.STREAM_CODEC, UpdatePreviewRequestPayload::item,
                    ByteBufCodecs.stringUtf8(MAX_NAME), UpdatePreviewRequestPayload::name,
                    UpdatePreviewRequestPayload::new);

    public UpdatePreviewRequestPayload {
        name = name == null ? "" : name.length() > MAX_NAME ? name.substring(0, MAX_NAME) : name;
    }

    @Override
    public CustomPacketPayload.Type<UpdatePreviewRequestPayload> type() {
        return TYPE;
    }
}
