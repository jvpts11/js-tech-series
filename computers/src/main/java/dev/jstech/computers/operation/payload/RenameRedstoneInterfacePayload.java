/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.blockentity.RedstoneInterfaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The name typed into a Redstone Interface's screen, sent as it is typed.
 *
 * @param sensorPos the interface being named
 * @param name      the name asked for, empty to give it back the word for it
 */
public record RenameRedstoneInterfacePayload(BlockPos sensorPos, String name) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RenameRedstoneInterfacePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "rename_redstone_interface"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RenameRedstoneInterfacePayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RenameRedstoneInterfacePayload::sensorPos,
                    ByteBufCodecs.stringUtf8(RedstoneInterfaceBlockEntity.MAX_NAME),
                    RenameRedstoneInterfacePayload::name,
                    RenameRedstoneInterfacePayload::new);

    @Override
    public CustomPacketPayload.Type<RenameRedstoneInterfacePayload> type() {
        return TYPE;
    }
}
