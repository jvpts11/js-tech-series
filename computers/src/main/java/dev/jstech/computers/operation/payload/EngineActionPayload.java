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
 * Client to server: the Network Manager's Services tab stops the network's engine, starts one, or replaces it with
 * another installed on the Mainframe at {@code hostPos}. The server answers with a fresh
 * {@link NetworkServicesPayload}.
 *
 * @param hostPos the Mainframe
 * @param action  {@link #STOP}, {@link #START}, {@link #REPLACE} or {@link #ROLE}
 * @param program the engine to start or to replace with, or for {@link #ROLE} the role's word; ignored by
 *                {@link #STOP}
 */
public record EngineActionPayload(BlockPos hostPos, byte action, String program) implements CustomPacketPayload {

    /** Stops the chosen engine, leaving the network with none. */
    public static final byte STOP = 0;
    /** Starts an installed engine at once, making it the chosen one. */
    public static final byte START = 1;
    /** Replaces the running engine with an installed one, through the steps a replacement takes. */
    public static final byte REPLACE = 2;
    /** Gives the Mainframe's engine a role, orchestrator or Subframe, named by its word. */
    public static final byte ROLE = 3;

    public static final CustomPacketPayload.Type<EngineActionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "engine_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EngineActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, EngineActionPayload::hostPos,
                    ByteBufCodecs.BYTE, EngineActionPayload::action,
                    ByteBufCodecs.stringUtf8(NetworkServicesPayload.MAX_NAME * 2), EngineActionPayload::program,
                    EngineActionPayload::new);

    @Override
    public CustomPacketPayload.Type<EngineActionPayload> type() {
        return TYPE;
    }
}
