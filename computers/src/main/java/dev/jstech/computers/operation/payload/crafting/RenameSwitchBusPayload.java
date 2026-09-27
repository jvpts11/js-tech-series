/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: the name typed for a machine a Crafting Switch reaches over a crafting bus, on the switch's own
 * screen. The bus is not the block the player has open, so the name is only applied when the switch's own survey
 * currently lists that bus among the machines it found.
 *
 * @param switchPos the Crafting Switch the player has open
 * @param cablePos  the data cable the bus is mounted on
 * @param busFace   the {@link Direction#get3DDataValue()} of the mounted bus
 * @param name      the new name for the machine the bus reaches
 */
public record RenameSwitchBusPayload(BlockPos switchPos, BlockPos cablePos, int busFace, String name)
        implements CustomPacketPayload {

    /** Wide enough for the name box this is typed from (48 characters); the server clamps it further. */
    public static final int MAX_LEN = 64;

    public static final CustomPacketPayload.Type<RenameSwitchBusPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "rename_switch_bus"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RenameSwitchBusPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RenameSwitchBusPayload::switchPos,
                    BlockPos.STREAM_CODEC, RenameSwitchBusPayload::cablePos,
                    ByteBufCodecs.VAR_INT, RenameSwitchBusPayload::busFace,
                    ByteBufCodecs.stringUtf8(MAX_LEN), RenameSwitchBusPayload::name,
                    RenameSwitchBusPayload::new);

    @Override
    public CustomPacketPayload.Type<RenameSwitchBusPayload> type() {
        return TYPE;
    }
}
