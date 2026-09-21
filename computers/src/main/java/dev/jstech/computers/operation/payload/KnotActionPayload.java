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
 * Client to server: what a Knot window asks of the network's repository.
 *
 * <p>Pushing reads the file off this machine's own disk rather than sending its text, so what is kept is
 * what the machine really holds and not what a client said it holds. Pulling writes it back the same way.
 */
public record KnotActionPayload(BlockPos hostPos, BlockPos monitorPos, int action,
                                String file, String message, int revision) implements CustomPacketPayload {

    /** Ask for the state, and for the comparison of whichever revision is named. */
    public static final int LOOK = 0;

    /** Save this machine's copy of a file as a new revision. */
    public static final int PUSH = 1;

    /** Write a revision back onto this machine's disk. */
    public static final int PULL = 2;

    /** The longest path a push may name, which is also what the answer carries back. */
    public static final int MAX_PATH = 160;

    /** The longest thing that may be said about a revision. */
    public static final int MAX_MESSAGE = 128;

    public static final CustomPacketPayload.Type<KnotActionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "knot_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, KnotActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, KnotActionPayload::hostPos,
                    BlockPos.STREAM_CODEC, KnotActionPayload::monitorPos,
                    ByteBufCodecs.VAR_INT, KnotActionPayload::action,
                    ByteBufCodecs.stringUtf8(MAX_PATH), KnotActionPayload::file,
                    ByteBufCodecs.stringUtf8(MAX_MESSAGE), KnotActionPayload::message,
                    ByteBufCodecs.VAR_INT, KnotActionPayload::revision,
                    KnotActionPayload::new);

    @Override
    public CustomPacketPayload.Type<KnotActionPayload> type() {
        return TYPE;
    }
}
