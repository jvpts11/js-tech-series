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
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * How the Vinyl RW Drive on a computer's cable is, for Soundfoundry: what lies on its platter, what it is doing and how
 * far, where a recording goes, what stopped it, and the recordings in the system's Music/Vinyl folder with whether each
 * came from a disc. The answer to Record also asks the player's game to bring the disc's sound.
 *
 * @param hostPos    the computer
 * @param drive      where the drive is, or 0 when the computer has none on its cable
 * @param platter    what lies on its platter: {@link #EMPTY}, {@link #DISC} or {@link #BLANK}
 * @param disc       the song of the disc on its platter, as its label names it
 * @param work       what it does: 0 nothing, 1 recording, 2 cutting
 * @param elapsed    how far into its work it is, in ticks
 * @param length     how long its work lasts, in ticks
 * @param song       the song it records or cuts, as its label names it
 * @param songId     that song's id
 * @param upload     whether the player's game is to bring the disc's sound now
 * @param saving     where the recording goes, or the last one went
 * @param problem    what stopped it last, or nothing
 * @param recordings the recordings in the system's Music/Vinyl folder
 */
public record VinylStatePayload(BlockPos hostPos, long drive, int platter, String disc, int work, int elapsed,
                                int length, String song, String songId, boolean upload, String saving, Text problem,
                                List<Recording> recordings) implements CustomPacketPayload {

    public static final int EMPTY = 0;
    public static final int DISC = 1;
    public static final int BLANK = 2;
    /** The longest path, name or title that travels. */
    public static final int MOST_TEXT = 256;
    /** The most recordings listed. */
    public static final int MOST_RECORDINGS = 64;

    public static final CustomPacketPayload.Type<VinylStatePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "vinyl_state"));

    private static final StreamCodec<ByteBuf, String> TEXT = ByteBufCodecs.stringUtf8(MOST_TEXT);

    public static final StreamCodec<RegistryFriendlyByteBuf, VinylStatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, state) -> {
                BlockPos.STREAM_CODEC.encode(buf, state.hostPos());
                ByteBufCodecs.VAR_LONG.encode(buf, state.drive());
                ByteBufCodecs.VAR_INT.encode(buf, state.platter());
                TEXT.encode(buf, state.disc());
                ByteBufCodecs.VAR_INT.encode(buf, state.work());
                ByteBufCodecs.VAR_INT.encode(buf, state.elapsed());
                ByteBufCodecs.VAR_INT.encode(buf, state.length());
                TEXT.encode(buf, state.song());
                TEXT.encode(buf, state.songId());
                ByteBufCodecs.BOOL.encode(buf, state.upload());
                TEXT.encode(buf, state.saving());
                TextCodecs.STREAM_CODEC.encode(buf, state.problem());
                Recording.STREAM_CODEC.apply(ByteBufCodecs.list(MOST_RECORDINGS)).encode(buf, state.recordings());
            },
            buf -> new VinylStatePayload(BlockPos.STREAM_CODEC.decode(buf), ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf), TEXT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), TEXT.decode(buf),
                    TEXT.decode(buf), ByteBufCodecs.BOOL.decode(buf), TEXT.decode(buf),
                    TextCodecs.STREAM_CODEC.decode(buf),
                    Recording.STREAM_CODEC.apply(ByteBufCodecs.list(MOST_RECORDINGS)).decode(buf)));

    public VinylStatePayload {
        recordings = List.copyOf(recordings);
    }

    @Override
    public CustomPacketPayload.Type<VinylStatePayload> type() {
        return TYPE;
    }

    /**
     * A recording in the Music/Vinyl folder.
     *
     * @param path     its file
     * @param title    what it is called
     * @param fromDisc whether it was recorded from a disc, and so goes onto a vinyl
     * @param day      the world's day it was recorded on, or 0 when it came from no disc
     * @param millis   how long it plays
     */
    public record Recording(String path, String title, boolean fromDisc, long day, long millis) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Recording> STREAM_CODEC = StreamCodec.composite(
                TEXT, Recording::path,
                TEXT, Recording::title,
                ByteBufCodecs.BOOL, Recording::fromDisc,
                ByteBufCodecs.VAR_LONG, Recording::day,
                ByteBufCodecs.VAR_LONG, Recording::millis,
                Recording::new);
    }
}
