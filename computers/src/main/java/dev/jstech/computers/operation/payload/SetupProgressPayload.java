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
 * What a machine says about the program it is setting up, to every desktop looking at it.
 *
 * <p>Sent while the job runs, and once more when it ends, with how it ended: done, refused with a
 * reason, or cancelled. A desktop opens its Setup window on the first of these and closes it after the
 * last, so the window is a view of the machine's job and never the other way round.
 *
 * @param hostPos   the machine
 * @param programId the program's registry path, which is also its icon
 * @param name      what to call it
 * @param house     who publishes it
 * @param sizeMb    what it takes on the disk
 * @param source    where it comes from, worded: "DVD", "the Mirror", read in the player's language
 * @param permille  how far along, in thousandths
 * @param phase     what is happening right now, read the same way
 * @param state     one of the {@code STATE_} values
 * @param message   why it was refused, or empty, read the same way
 * @param removing  taking the program off rather than putting it on
 */
public record SetupProgressPayload(BlockPos hostPos, String programId, String name, String house,
                                   int sizeMb, Text source, int permille, Text phase, int state,
                                   Text message, boolean removing) implements CustomPacketPayload {

    public static final int STATE_RUNNING = 0;
    public static final int STATE_DONE = 1;
    public static final int STATE_REFUSED = 2;
    public static final int STATE_CANCELLED = 3;

    private static final int MAX_ID = 64;
    private static final int MAX_NAME = 64;
    private static final int MAX_TEXT = 160;

    public static final CustomPacketPayload.Type<SetupProgressPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "setup_progress"));

    /*
     * Eleven fields is past what composite() takes, so the codec is written out. Every string is capped
     * on both sides, because a cap on a payload string does not cut what is too long, it fails to send.
     * The words a player reads travel as text: words that are data are cut to the same caps, and a declared
     * sentence is put together at the other end.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, SetupProgressPayload> STREAM_CODEC =
            StreamCodec.of((buf, p) -> {
                buf.writeBlockPos(p.hostPos());
                buf.writeUtf(clip(p.programId(), MAX_ID), MAX_ID);
                buf.writeUtf(clip(p.name(), MAX_NAME), MAX_NAME);
                buf.writeUtf(clip(p.house(), MAX_NAME), MAX_NAME);
                buf.writeVarInt(p.sizeMb());
                TextCodecs.STREAM_CODEC.encode(buf, clip(p.source(), MAX_NAME));
                buf.writeVarInt(p.permille());
                TextCodecs.STREAM_CODEC.encode(buf, clip(p.phase(), MAX_NAME));
                buf.writeVarInt(p.state());
                TextCodecs.STREAM_CODEC.encode(buf, clip(p.message(), MAX_TEXT));
                buf.writeBoolean(p.removing());
            }, buf -> new SetupProgressPayload(
                    buf.readBlockPos(), buf.readUtf(MAX_ID), buf.readUtf(MAX_NAME), buf.readUtf(MAX_NAME),
                    buf.readVarInt(), TextCodecs.STREAM_CODEC.decode(buf), buf.readVarInt(),
                    TextCodecs.STREAM_CODEC.decode(buf), buf.readVarInt(), TextCodecs.STREAM_CODEC.decode(buf),
                    buf.readBoolean()));

    /** A string never longer than the wire allows, so a long name is shortened rather than fatal. */
    private static String clip(final String text, final int max) {
        final String s = text == null ? "" : text;
        return s.length() <= max ? s : s.substring(0, max);
    }

    /** The same for words that are data; a declared sentence is left whole. */
    private static Text clip(final Text text, final int max) {
        if (text == null) {
            return Text.EMPTY;
        }
        return text instanceof Text.Literal literal ? Text.literal(clip(literal.value(), max)) : text;
    }

    /** Whether the job is over, however it ended. */
    public boolean over() {
        return this.state != STATE_RUNNING;
    }

    @Override
    public CustomPacketPayload.Type<SetupProgressPayload> type() {
        return TYPE;
    }
}
