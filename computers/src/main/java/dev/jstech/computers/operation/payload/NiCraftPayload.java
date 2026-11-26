/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.storage.StorageKey;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: the Network Interactor desktop window requested a craft. The same network CRAFT
 * operation the terminal submits, addressed to the host by position so it works without a menu.
 *
 * @param host       the computer the desktop is bound to
 * @param monitorPos the monitor used, validated against the player's reach
 * @param result     the product to craft: an item, or a liquid or gas a machine makes
 * @param amount     how much to craft, in the product's own units
 */
public record NiCraftPayload(BlockPos host, BlockPos monitorPos, StorageKey result,
                             long amount) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<NiCraftPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "ni_craft"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NiCraftPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, NiCraftPayload::host,
                    BlockPos.STREAM_CODEC, NiCraftPayload::monitorPos,
                    StorageKey.STREAM_CODEC, NiCraftPayload::result,
                    ByteBufCodecs.VAR_LONG, NiCraftPayload::amount,
                    NiCraftPayload::new);

    @Override
    public CustomPacketPayload.Type<NiCraftPayload> type() {
        return TYPE;
    }
}
