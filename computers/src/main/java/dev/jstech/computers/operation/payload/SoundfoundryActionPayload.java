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
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * What a player did in Soundfoundry, sent to the machine it runs on, which answers with how it now stands.
 *
 * @param hostPos the machine
 * @param action  one of the constants below
 * @param index   a place in the playlist, a volume, a balance or a switch, as the action takes; for {@link #LOOK}, the
 *                revision of the playlist the window already holds, so an unchanged one is not sent again
 * @param value   a point in a song, in milliseconds, for {@link #SEEK}
 * @param paths   the files or folder an action is about
 * @param indexes the places in the playlist an action is about
 */
public record SoundfoundryActionPayload(BlockPos hostPos, int action, int index, long value, List<String> paths,
                                        List<Integer> indexes) implements CustomPacketPayload {

    public static final int LOOK = 0;
    public static final int PLAY = 1;
    public static final int PAUSE = 2;
    public static final int STOP = 3;
    public static final int NEXT = 4;
    public static final int PREVIOUS = 5;
    public static final int SEEK = 6;
    public static final int VOLUME = 7;
    public static final int BALANCE = 8;
    public static final int SHUFFLE = 9;
    public static final int REPEAT = 10;
    public static final int ADD = 11;
    public static final int ADD_FOLDER = 12;
    public static final int REMOVE = 13;
    public static final int CROP = 14;
    public static final int CLEAR = 15;
    public static final int SORT_TITLE = 16;
    public static final int SORT_FILE = 17;
    public static final int REVERSE = 18;
    public static final int OPEN_LIST = 19;
    public static final int SAVE_LIST = 20;
    /**
     * Plays a page's songs in place of the list, from the one picked: {@code index} the page, {@code value} the
     * song's place among the section's, the first of {@code paths} what the page is of, the first of
     * {@code indexes} the section.
     */
    public static final int PLAY_PAGE = 21;
    /** Downloads the song the first of {@code paths} names. */
    public static final int DOWNLOAD = 22;
    /** Downloads every song of the catalogue's album the first of {@code paths} names. */
    public static final int DOWNLOAD_ALBUM = 23;
    /** Sends the machine's song at the first of {@code paths} to its Soundfoundry Server. */
    public static final int UPLOAD = 24;
    /** Streams from the Soundfoundry Server the first of {@code paths} names. */
    public static final int PICK_SERVER = 25;
    /** Puts the song the first of {@code paths} names in the liked songs, or takes it out if it is there. */
    public static final int LIKE = 26;
    /** Puts the song the second of {@code paths} names at the end of the playlist the first names. */
    public static final int PLAYLIST_ADD = 27;
    /** Takes the song at {@code index} out of the playlist the first of {@code paths} names. */
    public static final int PLAYLIST_REMOVE = 28;
    /** Makes a playlist of the name the first of {@code paths} is. */
    public static final int PLAYLIST_NEW = 29;
    /** Throws away the playlist the first of {@code paths} names. */
    public static final int PLAYLIST_DELETE = 30;

    /** Given as the index of {@link #ADD}: play the first of the songs added, as opening a song does. */
    public static final int AND_PLAY = 1;

    /** The most files one action names. */
    public static final int MAX_PATHS = 64;
    /** The longest path one names: a file's path, behind the drive a medium is in or what it is streamed from. */
    public static final int MAX_PATH = 256;

    public static final CustomPacketPayload.Type<SoundfoundryActionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                    "soundfoundry_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SoundfoundryActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SoundfoundryActionPayload::hostPos,
                    ByteBufCodecs.VAR_INT, SoundfoundryActionPayload::action,
                    ByteBufCodecs.VAR_INT, SoundfoundryActionPayload::index,
                    ByteBufCodecs.VAR_LONG, SoundfoundryActionPayload::value,
                    ByteBufCodecs.stringUtf8(MAX_PATH).apply(ByteBufCodecs.list(MAX_PATHS)),
                    SoundfoundryActionPayload::paths,
                    ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(SoundfoundryState.MAX_SONGS)),
                    SoundfoundryActionPayload::indexes,
                    SoundfoundryActionPayload::new);

    public SoundfoundryActionPayload {
        paths = List.copyOf(paths);
        indexes = List.copyOf(indexes);
    }

    /** An action about nothing but itself, or one place, volume or switch. */
    public static SoundfoundryActionPayload of(final BlockPos host, final int action, final int index) {
        return new SoundfoundryActionPayload(host, action, index, 0L, List.of(), List.of());
    }

    @Override
    public CustomPacketPayload.Type<SoundfoundryActionPayload> type() {
        return TYPE;
    }
}
