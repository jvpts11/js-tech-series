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
 * Client to server: a terminal window on the desktop ran a command {@code line} against the computer
 * at {@code hostPos}. The server executes it through the same CLI as the Command Prompt and replies
 * with a {@link DesktopShellOutputPayload} to the same {@code session}.
 *
 * <p>The session is the window's own number: two terminals open on one desktop are two shells, each
 * with its own directory and its own replies, the way two terminal windows on a real machine are.
 * Zero is a caller with no window of its own.
 *
 * <p>{@code columns} is how wide the window's glass is. It travels with the line because the machine lays
 * its answer out in columns and has no glass of its own to measure: told nothing, it writes to the width a
 * monitor has, and a narrower window then folds every wide line in half.
 */
public record DesktopShellRunPayload(BlockPos hostPos, String line, int session, int columns)
        implements CustomPacketPayload {

    public static final int MAX_LEN = 512;

    /** The width the machine writes to when the one asking has not said, which is what a monitor holds. */
    public static final int DEFAULT_COLUMNS = 80;

    /** The narrowest glass the machine will lay a line out for; below this nothing would line up anyway. */
    public static final int LEAST_COLUMNS = 40;

    /** And the widest, since this arrives from a client and a width is a size the machine then writes. */
    public static final int MOST_COLUMNS = 300;

    public DesktopShellRunPayload {
        columns = columns <= 0 ? DEFAULT_COLUMNS : Math.clamp(columns, LEAST_COLUMNS, MOST_COLUMNS);
    }

    /** A line from a window that has not said how wide it is. */
    public DesktopShellRunPayload(final BlockPos hostPos, final String line, final int session) {
        this(hostPos, line, session, DEFAULT_COLUMNS);
    }

    /** A line from something that is not a terminal window, such as the Network Interactor's box. */
    public DesktopShellRunPayload(final BlockPos hostPos, final String line) {
        this(hostPos, line, 0, DEFAULT_COLUMNS);
    }

    public static final CustomPacketPayload.Type<DesktopShellRunPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "desktop_shell_run"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DesktopShellRunPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, DesktopShellRunPayload::hostPos,
                    ByteBufCodecs.stringUtf8(MAX_LEN), DesktopShellRunPayload::line,
                    ByteBufCodecs.VAR_INT, DesktopShellRunPayload::session,
                    ByteBufCodecs.VAR_INT, DesktopShellRunPayload::columns,
                    DesktopShellRunPayload::new);

    @Override
    public CustomPacketPayload.Type<DesktopShellRunPayload> type() {
        return TYPE;
    }
}
