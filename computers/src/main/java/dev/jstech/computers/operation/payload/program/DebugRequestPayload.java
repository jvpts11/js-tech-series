/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.program;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * What a studio's debugger asks of the machine it runs on: to start a program under the debugger, to go on from where
 * it stopped, to stop it, or to know what its breakpoints and watches are.
 *
 * @param action  one of the actions below
 * @param listing the listing to debug, for {@link #START}; empty otherwise
 * @param lines   the breakpoints, each {@code file:line}, for {@link #START} and {@link #BREAKPOINTS}; the names to
 *                watch for {@link #WATCHES}; the typed line for {@link #INPUT}
 */
public record DebugRequestPayload(BlockPos hostPos, int action, String listing, List<String> lines)
        implements CustomPacketPayload {

    public static final int START = 0;
    public static final int CONTINUE = 1;
    public static final int STEP_INTO = 2;
    public static final int STEP_OVER = 3;
    public static final int STEP_OUT = 4;
    public static final int PAUSE = 5;
    public static final int STOP = 6;
    public static final int BREAKPOINTS = 7;
    public static final int WATCHES = 8;
    public static final int INPUT = 9;

    /** The longest path a listing is asked for by, and the longest breakpoint, name or typed line. */
    public static final int MOST_PATH = 1024;
    public static final int MOST_LINE = 4096;
    /** The most breakpoints or watches one request carries. */
    public static final int MOST_LINES = 256;

    public static final CustomPacketPayload.Type<DebugRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "debug_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DebugRequestPayload> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, DebugRequestPayload::hostPos,
                    ByteBufCodecs.VAR_INT, DebugRequestPayload::action,
                    ByteBufCodecs.stringUtf8(MOST_PATH), DebugRequestPayload::listing,
                    ByteBufCodecs.stringUtf8(MOST_LINE).apply(ByteBufCodecs.list(MOST_LINES)),
                    DebugRequestPayload::lines, DebugRequestPayload::new);

    public DebugRequestPayload {
        lines = List.copyOf(lines);
    }

    /** One of the requests that carries nothing but what it is. */
    public static DebugRequestPayload of(final BlockPos hostPos, final int action) {
        return new DebugRequestPayload(hostPos, action, "", List.of());
    }

    @Override
    public CustomPacketPayload.Type<DebugRequestPayload> type() {
        return TYPE;
    }
}
