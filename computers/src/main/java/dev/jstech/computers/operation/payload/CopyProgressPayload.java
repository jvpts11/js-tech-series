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
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: a file copy on the machine at {@code hostPos} that takes time has started, or has ended. It says
 * what the copy windows show: the file, the folders it goes from and to, how much it weighs and how fast it goes, and
 * the game time it started at and ends at, so the player's game draws its progress smoothly without being told more.
 *
 * @param hostPos     the machine the copy runs on
 * @param job         which copy this is, for the end to find its start
 * @param kind        {@link #COPY}, {@link #MOVE} or {@link #DELETE}
 * @param name        the file's name
 * @param from        the folder it comes from, as a window names it
 * @param to          the folder it goes to, as a window names it; empty for a deletion
 * @param sizeMb      how much it weighs, in the megabytes its disk counts
 * @param mbPerSecond how fast it goes, in megabytes a second
 * @param startTick   the game time it started at
 * @param endTick     the game time it ends at
 * @param done        whether this says it has ended
 */
public record CopyProgressPayload(BlockPos hostPos, long job, byte kind, String name, String from, String to,
                                  long sizeMb, float mbPerSecond, long startTick, long endTick, boolean done)
        implements CustomPacketPayload {

    /** A copy: the file stays where it was. */
    public static final byte COPY = 0;
    /** A move to another volume: the file leaves where it was. */
    public static final byte MOVE = 1;
    /** A deletion that takes time, the file carried to the Recycle Bin of another volume. */
    public static final byte DELETE = 2;

    public static final CustomPacketPayload.Type<CopyProgressPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "copy_progress"));

    /* The longest file and folder names a copy window is sent. */
    private static final int NAME_MAX = 160;

    public static final StreamCodec<RegistryFriendlyByteBuf, CopyProgressPayload> STREAM_CODEC =
            StreamCodec.of(CopyProgressPayload::write, CopyProgressPayload::read);

    /** The same copy, ended. */
    public CopyProgressPayload ended() {
        return new CopyProgressPayload(hostPos, job, kind, name, from, to, sizeMb, mbPerSecond, startTick, endTick,
                true);
    }

    @Override
    public CustomPacketPayload.Type<CopyProgressPayload> type() {
        return TYPE;
    }

    private static void write(final RegistryFriendlyByteBuf buf, final CopyProgressPayload payload) {
        BlockPos.STREAM_CODEC.encode(buf, payload.hostPos());
        buf.writeVarLong(payload.job());
        buf.writeByte(payload.kind());
        buf.writeUtf(clip(payload.name()), NAME_MAX);
        buf.writeUtf(clip(payload.from()), NAME_MAX);
        buf.writeUtf(clip(payload.to()), NAME_MAX);
        buf.writeVarLong(payload.sizeMb());
        buf.writeFloat(payload.mbPerSecond());
        buf.writeVarLong(payload.startTick());
        buf.writeVarLong(payload.endTick());
        buf.writeBoolean(payload.done());
    }

    private static CopyProgressPayload read(final RegistryFriendlyByteBuf buf) {
        return new CopyProgressPayload(BlockPos.STREAM_CODEC.decode(buf), buf.readVarLong(), buf.readByte(),
                buf.readUtf(NAME_MAX), buf.readUtf(NAME_MAX), buf.readUtf(NAME_MAX), buf.readVarLong(),
                buf.readFloat(), buf.readVarLong(), buf.readVarLong(), buf.readBoolean());
    }

    private static String clip(final String text) {
        return text.length() > NAME_MAX ? text.substring(0, NAME_MAX) : text;
    }
}
