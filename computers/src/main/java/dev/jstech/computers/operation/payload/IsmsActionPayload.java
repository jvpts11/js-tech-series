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
 * Client to server: something the IQL Server Management Studio window {@code window} on the computer at
 * {@code hostPos} asks of the network that is not a statement: a fresh Object Explorer, the Midsoft IQL Server
 * started, stopped or restarted, a job started, paused or deleted, an item let go, an Operation stopped, a craft's
 * estimated plan, the Activity Monitor's Operations, or a trace begun or ended.
 *
 * @param action what is asked, one of the constants here
 * @param target what it is asked of: a job's name, an item's id, an Operation's short id; empty where nothing is
 * @param arg    a number it takes: how many to plan, which events to trace; zero where it takes none
 */
public record IsmsActionPayload(BlockPos monitorPos, BlockPos hostPos, int window, int action, String target,
                                int arg) implements CustomPacketPayload {

    public static final int REFRESH = 0;
    public static final int ENGINE_START = 1;
    public static final int ENGINE_STOP = 2;
    public static final int ENGINE_RESTART = 3;
    public static final int JOB_START = 4;
    public static final int JOB_PAUSE = 5;
    public static final int JOB_DELETE = 6;
    public static final int UNLOCK = 7;
    public static final int CANCEL = 8;
    public static final int PLAN = 9;
    public static final int ACTIVITY = 10;
    public static final int TRACE_START = 11;
    public static final int TRACE_STOP = 12;
    /** The tab an action's answer names: none, so the studio tells it in the tab in front. */
    public static final int NO_TAB = -1;

    private static final int MAX_TARGET = 64;

    public static final CustomPacketPayload.Type<IsmsActionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "isms_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, IsmsActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, IsmsActionPayload::monitorPos,
                    BlockPos.STREAM_CODEC, IsmsActionPayload::hostPos,
                    ByteBufCodecs.VAR_INT, IsmsActionPayload::window,
                    ByteBufCodecs.VAR_INT, IsmsActionPayload::action,
                    ByteBufCodecs.stringUtf8(MAX_TARGET), IsmsActionPayload::target,
                    ByteBufCodecs.VAR_INT, IsmsActionPayload::arg,
                    IsmsActionPayload::new);

    public IsmsActionPayload {
        target = target == null ? "" : target.length() > MAX_TARGET ? target.substring(0, MAX_TARGET) : target;
    }

    @Override
    public CustomPacketPayload.Type<IsmsActionPayload> type() {
        return TYPE;
    }
}
