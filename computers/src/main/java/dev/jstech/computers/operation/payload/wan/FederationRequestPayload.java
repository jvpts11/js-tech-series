/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.wan;

import dev.jstech.computers.storage.StorageKey;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: the Federated tab asks what the networks this one is federated with hold, and with an action
 * takes items from one of them into this computer, sends the stack on the cursor to it, or has it craft something.
 * The server answers with the tab's state.
 *
 * @param hostPos    the computer asked through
 * @param monitorPos the monitor the player is at
 * @param network    the network shown or acted on, by its id; empty for the nearest
 * @param action     {@link #ASK}, {@link #TAKE}, {@link #SEND} or {@link #CRAFT}
 * @param key        what to take or craft
 * @param amount     how many
 */
public record FederationRequestPayload(BlockPos hostPos, BlockPos monitorPos, String network, byte action,
                                       Optional<StorageKey> key, long amount) implements CustomPacketPayload {

    /** Only asks. */
    public static final byte ASK = 0;
    /** Takes {@code amount} of {@code key} from that network into this computer. */
    public static final byte TAKE = 1;
    /** Sends the stack on the player's cursor to that network. */
    public static final byte SEND = 2;
    /** Has that network craft {@code amount} of {@code key}, which stays there. */
    public static final byte CRAFT = 3;
    public static final CustomPacketPayload.Type<FederationRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "federation_request"));
    private static final int ID = 40;

    public static final StreamCodec<RegistryFriendlyByteBuf, FederationRequestPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, FederationRequestPayload::hostPos,
                    BlockPos.STREAM_CODEC, FederationRequestPayload::monitorPos,
                    ByteBufCodecs.stringUtf8(ID), FederationRequestPayload::network,
                    ByteBufCodecs.BYTE, FederationRequestPayload::action,
                    ByteBufCodecs.optional(StorageKey.STREAM_CODEC), FederationRequestPayload::key,
                    ByteBufCodecs.VAR_LONG, FederationRequestPayload::amount,
                    FederationRequestPayload::new);

    /** Asks what {@code network} holds, and does nothing. */
    public static FederationRequestPayload ask(final BlockPos hostPos, final BlockPos monitorPos,
                                               final String network) {
        return new FederationRequestPayload(hostPos, monitorPos, network, ASK, Optional.empty(), 0L);
    }

    @Override
    public CustomPacketPayload.Type<FederationRequestPayload> type() {
        return TYPE;
    }
}
