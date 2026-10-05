/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: a file copy on the machine at {@code hostPos} that takes time has started, been paused or taken
 * up again, or has ended. It says what the copy windows show: the file, the folders it goes from and to, how much it
 * weighs and how fast it goes, and the game time it started at and ends at, so the player's game draws its progress
 * smoothly without being told more; a paused copy stands where it was at the game time it was paused.
 *
 * @param hostPos     the machine the copy runs on
 * @param job         which copy this is, for the end to find its start
 * @param kind        {@link #COPY}, {@link #MOVE} or {@link #DELETE}
 * @param name        the file's name
 * @param from        the folder it comes from, as a window names it: the volume's name for its root
 * @param to          the folder it goes to, named the same way; empty for a deletion
 * @param sizeMb      how much it weighs, in the megabytes its disk counts
 * @param mbPerSecond how fast it goes, in megabytes a second
 * @param startTick   the game time it started at
 * @param endTick     the game time it ends at
 * @param pausedTick  the game time its machine's copies were paused at, or {@link #RUNNING} while they run
 * @param done        whether this says it has ended
 */
public record CopyProgressPayload(BlockPos hostPos, long job, byte kind, String name, Text from, Text to,
                                  long sizeMb, float mbPerSecond, long startTick, long endTick, long pausedTick,
                                  boolean done)
        implements CustomPacketPayload {

    /** A copy: the file stays where it was. */
    public static final byte COPY = 0;
    /** A move to another volume: the file leaves where it was. */
    public static final byte MOVE = 1;
    /** A deletion that takes time, the file carried to the Recycle Bin of another volume. */
    public static final byte DELETE = 2;
    /** The {@code pausedTick} of a copy that is not paused. */
    public static final long RUNNING = -1L;

    public static final CustomPacketPayload.Type<CopyProgressPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "copy_progress"));

    /* The longest file name a copy window is sent. */
    private static final int NAME_MAX = 160;

    public static final StreamCodec<RegistryFriendlyByteBuf, CopyProgressPayload> STREAM_CODEC =
            StreamCodec.of(CopyProgressPayload::write, CopyProgressPayload::read);

    /** Whether its machine's copies are paused. */
    public boolean paused() {
        return pausedTick != RUNNING;
    }

    /** The same copy, ended. */
    public CopyProgressPayload ended() {
        return new CopyProgressPayload(hostPos, job, kind, name, from, to, sizeMb, mbPerSecond, startTick, endTick,
                pausedTick, true);
    }

    /** The same copy starting and ending {@code ticks} later, or sooner for a negative number. */
    public CopyProgressPayload shifted(final long ticks) {
        return new CopyProgressPayload(hostPos, job, kind, name, from, to, sizeMb, mbPerSecond, startTick + ticks,
                endTick + ticks, pausedTick, done);
    }

    /** The same copy, paused at the game time {@code tick}, or running again for {@link #RUNNING}. */
    public CopyProgressPayload pausedAt(final long tick) {
        return new CopyProgressPayload(hostPos, job, kind, name, from, to, sizeMb, mbPerSecond, startTick, endTick,
                tick, done);
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
        TextCodecs.STREAM_CODEC.encode(buf, payload.from());
        TextCodecs.STREAM_CODEC.encode(buf, payload.to());
        buf.writeVarLong(payload.sizeMb());
        buf.writeFloat(payload.mbPerSecond());
        buf.writeVarLong(payload.startTick());
        buf.writeVarLong(payload.endTick());
        buf.writeBoolean(payload.paused());
        if (payload.paused()) {
            buf.writeVarLong(payload.pausedTick());
        }
        buf.writeBoolean(payload.done());
    }

    private static CopyProgressPayload read(final RegistryFriendlyByteBuf buf) {
        final BlockPos host = BlockPos.STREAM_CODEC.decode(buf);
        final long job = buf.readVarLong();
        final byte kind = buf.readByte();
        final String name = buf.readUtf(NAME_MAX);
        final Text from = TextCodecs.STREAM_CODEC.decode(buf);
        final Text to = TextCodecs.STREAM_CODEC.decode(buf);
        final long size = buf.readVarLong();
        final float rate = buf.readFloat();
        final long start = buf.readVarLong();
        final long end = buf.readVarLong();
        final long paused = buf.readBoolean() ? buf.readVarLong() : RUNNING;
        return new CopyProgressPayload(host, job, kind, name, from, to, size, rate, start, end, paused,
                buf.readBoolean());
    }

    private static String clip(final String text) {
        return text.length() > NAME_MAX ? text.substring(0, NAME_MAX) : text;
    }
}
