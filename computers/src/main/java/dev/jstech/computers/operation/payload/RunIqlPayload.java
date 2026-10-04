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
 * Client to server: one IQL statement of a studio's query tab, for the computer at {@code hostPos}, to be parsed and
 * run against the network. A script is sent a statement at a time, each no longer than {@link #MAX_LEN}. The server
 * answers with an {@link IqlResultPayload} carrying the same {@code window}, {@code tab} and {@code seq}, so the
 * answer reaches the tab that asked, in the window that asked, even with two studios open.
 *
 * @param window the studio window that asked, as it numbers itself
 * @param tab    the query tab that asked
 * @param seq    which statement of the run this is, from zero
 */
public record RunIqlPayload(BlockPos monitorPos, BlockPos hostPos, int window, int tab, int seq, String statement)
        implements CustomPacketPayload {

    public static final int MAX_LEN = 512;

    public static final CustomPacketPayload.Type<RunIqlPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "run_iql"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RunIqlPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RunIqlPayload::monitorPos,
                    BlockPos.STREAM_CODEC, RunIqlPayload::hostPos,
                    ByteBufCodecs.VAR_INT, RunIqlPayload::window,
                    ByteBufCodecs.VAR_INT, RunIqlPayload::tab,
                    ByteBufCodecs.VAR_INT, RunIqlPayload::seq,
                    ByteBufCodecs.stringUtf8(MAX_LEN), RunIqlPayload::statement,
                    RunIqlPayload::new);

    public RunIqlPayload {
        statement = statement == null ? "" : statement.length() > MAX_LEN ? statement.substring(0, MAX_LEN)
                : statement;
    }

    @Override
    public CustomPacketPayload.Type<RunIqlPayload> type() {
        return TYPE;
    }
}
