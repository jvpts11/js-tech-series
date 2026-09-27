/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.program.SoundfoundryState;
import dev.jstech.core.audio.media.MediaTags;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextCodecs;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * How Soundfoundry stands on a machine, for the window showing it: the song it is on and where in it, how it plays,
 * and the playlist when it has changed since the window last had it.
 *
 * @param hostPos  the machine
 * @param revision which change of the playlist this is
 * @param songs    the playlist, only when it has changed since the revision the window asked with
 * @param current  the place of the song it is on, or -1
 * @param status   {@link #STOPPED}, {@link #PLAYING} or {@link #PAUSED}
 * @param position how far into its song it had got when this was sent, in milliseconds
 * @param playing  what the song it is on is
 * @param shuffle  whether it shuffles
 * @param repeat   whether it goes round the list again
 * @param volume   how loud, out of {@link SoundfoundryState#MAX_VOLUME}
 * @param balance  to which side, out of {@link SoundfoundryState#MAX_BALANCE} either way
 * @param device   whether the machine's sound plays recordings at all
 * @param output   where its sound comes out: {@link #OUT_NONE}, {@link #OUT_MONITOR}, {@link #OUT_SPEAKERS} or
 *                 {@link #OUT_BOTH}
 * @param trouble  why the last song could not play, or empty
 */
public record SoundfoundryStatePayload(BlockPos hostPos, int revision, Optional<List<Song>> songs, int current,
                                       int status, long position, Playing playing, boolean shuffle, boolean repeat,
                                       int volume, int balance, boolean device, int output, Text trouble)
        implements CustomPacketPayload {

    public static final int STOPPED = 0;
    public static final int PLAYING = 1;
    public static final int PAUSED = 2;

    public static final int OUT_NONE = 0;
    public static final int OUT_MONITOR = 1;
    public static final int OUT_SPEAKERS = 2;
    public static final int OUT_BOTH = 3;

    private static final int MAX_PATH = SoundfoundryActionPayload.MAX_PATH;

    public static final CustomPacketPayload.Type<SoundfoundryStatePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                    "soundfoundry_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SoundfoundryStatePayload> STREAM_CODEC =
            StreamCodec.of(SoundfoundryStatePayload::encode, SoundfoundryStatePayload::decode);

    /* Copied on the way in, so what the window is handed cannot change under it after it arrives. */
    public SoundfoundryStatePayload {
        songs = songs.map(List::copyOf);
    }

    /**
     * One song of the playlist, as the window lists it.
     *
     * @param path    where its file is
     * @param title   what it is called: its own title, or its file's name
     * @param artist  who made it, or empty
     * @param millis  how long it runs
     * @param present whether its file is still there to play
     */
    public record Song(String path, String title, String artist, long millis, boolean present) {
    }

    /**
     * What the song it is on is, as the display shows it.
     *
     * @param millis     how long it runs
     * @param kbps       the kilobits a second of it
     * @param sampleRate its samples a second
     * @param channels   1 for mono, 2 for stereo, 0 for no song
     */
    public record Playing(long millis, int kbps, int sampleRate, int channels) {

        /** No song. */
        public static final Playing NONE = new Playing(0L, 0, 0, 0);
    }

    @Override
    public CustomPacketPayload.Type<SoundfoundryStatePayload> type() {
        return TYPE;
    }

    private static void encode(final RegistryFriendlyByteBuf buf, final SoundfoundryStatePayload p) {
        BlockPos.STREAM_CODEC.encode(buf, p.hostPos);
        buf.writeVarInt(p.revision);
        buf.writeBoolean(p.songs.isPresent());
        if (p.songs.isPresent()) {
            final List<Song> songs = p.songs.get();
            final int count = Math.min(songs.size(), SoundfoundryState.MAX_SONGS);
            buf.writeVarInt(count);
            for (int i = 0; i < count; i++) {
                final Song song = songs.get(i);
                buf.writeUtf(clip(song.path(), MAX_PATH), MAX_PATH);
                buf.writeUtf(clip(song.title(), MediaTags.MAX_TEXT), MediaTags.MAX_TEXT);
                buf.writeUtf(clip(song.artist(), MediaTags.MAX_TEXT), MediaTags.MAX_TEXT);
                buf.writeVarLong(song.millis());
                buf.writeBoolean(song.present());
            }
        }
        buf.writeVarInt(p.current);
        buf.writeVarInt(p.status);
        buf.writeVarLong(p.position);
        buf.writeVarLong(p.playing.millis());
        buf.writeVarInt(p.playing.kbps());
        buf.writeVarInt(p.playing.sampleRate());
        buf.writeVarInt(p.playing.channels());
        buf.writeBoolean(p.shuffle);
        buf.writeBoolean(p.repeat);
        buf.writeVarInt(p.volume);
        buf.writeVarInt(p.balance);
        buf.writeBoolean(p.device);
        buf.writeVarInt(p.output);
        TextCodecs.STREAM_CODEC.encode(buf, p.trouble);
    }

    private static SoundfoundryStatePayload decode(final RegistryFriendlyByteBuf buf) {
        final BlockPos host = BlockPos.STREAM_CODEC.decode(buf);
        final int revision = buf.readVarInt();
        Optional<List<Song>> songs = Optional.empty();
        if (buf.readBoolean()) {
            final int count = Math.min(buf.readVarInt(), SoundfoundryState.MAX_SONGS);
            final List<Song> read = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                read.add(new Song(buf.readUtf(MAX_PATH), buf.readUtf(MediaTags.MAX_TEXT),
                        buf.readUtf(MediaTags.MAX_TEXT), buf.readVarLong(), buf.readBoolean()));
            }
            songs = Optional.of(read);
        }
        final int current = buf.readVarInt();
        final int status = buf.readVarInt();
        final long position = buf.readVarLong();
        final Playing playing = new Playing(buf.readVarLong(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
        final boolean shuffle = buf.readBoolean();
        final boolean repeat = buf.readBoolean();
        final int volume = buf.readVarInt();
        final int balance = buf.readVarInt();
        final boolean device = buf.readBoolean();
        final int output = buf.readVarInt();
        final Text trouble = TextCodecs.STREAM_CODEC.decode(buf);
        return new SoundfoundryStatePayload(host, revision, songs, current, status, position, playing, shuffle,
                repeat, volume, balance, device, output, trouble);
    }

    private static String clip(final String text, final int max) {
        return text.length() <= max ? text : text.substring(0, max);
    }
}
