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
 * Client to server: the Crafting Manager's Interfaces tab sets one thing of a Crafting Interface the computer drives:
 * paused or running, exclusive or not, or the most jobs it runs at once.
 *
 * @param hostPos the position of the Crafting Computer
 * @param place   the interface, as the state numbers its place
 * @param setting {@link #PAUSED}, {@link #EXCLUSIVE} or {@link #MAX_JOBS}
 * @param value   1 or 0 for the first two, the number of jobs for the last (0 for as many as come)
 */
public record SetCraftInterfacePayload(BlockPos hostPos, int place, int setting, int value)
        implements CustomPacketPayload {

    public static final int PAUSED = 0;
    public static final int EXCLUSIVE = 1;
    public static final int MAX_JOBS = 2;

    public static final CustomPacketPayload.Type<SetCraftInterfacePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "set_craft_interface"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetCraftInterfacePayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SetCraftInterfacePayload::hostPos,
                    ByteBufCodecs.VAR_INT, SetCraftInterfacePayload::place,
                    ByteBufCodecs.VAR_INT, SetCraftInterfacePayload::setting,
                    ByteBufCodecs.VAR_INT, SetCraftInterfacePayload::value,
                    SetCraftInterfacePayload::new);

    @Override
    public CustomPacketPayload.Type<SetCraftInterfacePayload> type() {
        return TYPE;
    }
}
