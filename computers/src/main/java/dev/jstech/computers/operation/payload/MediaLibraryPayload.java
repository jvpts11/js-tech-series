/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.audio.MediaLibrary;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: what the Media Player of a computer can play. Its songs, its image sequences, the sequence of the
 * folder it was asked about, and whether the computer has a sound card to play a recording on.
 *
 * @param hostPos   the computer
 * @param songs     the songs in its music folder
 * @param sequences its image sequences
 * @param asked     the pictures of the folder the player asked about, or none
 * @param sound     whether it plays recordings, which takes a sound card
 */
public record MediaLibraryPayload(BlockPos hostPos, List<MediaLibrary.Song> songs,
                                  List<MediaLibrary.Sequence> sequences, MediaLibrary.Sequence asked,
                                  boolean sound) implements CustomPacketPayload {

    /** The longest path or title that travels. */
    public static final int MOST_TEXT = 256;

    public static final CustomPacketPayload.Type<MediaLibraryPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "media_library"));

    private static final StreamCodec<RegistryFriendlyByteBuf, MediaLibrary.Song> SONG = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MOST_TEXT), MediaLibrary.Song::path,
            ByteBufCodecs.stringUtf8(MOST_TEXT), MediaLibrary.Song::title,
            ByteBufCodecs.VAR_LONG, MediaLibrary.Song::millis,
            MediaLibrary.Song::new);

    private static final StreamCodec<RegistryFriendlyByteBuf, MediaLibrary.Sequence> SEQUENCE =
            StreamCodec.composite(
                    ByteBufCodecs.stringUtf8(MOST_TEXT), MediaLibrary.Sequence::folder,
                    ByteBufCodecs.stringUtf8(MOST_TEXT).apply(ByteBufCodecs.list(MediaLibrary.MOST_FRAMES)),
                    MediaLibrary.Sequence::frames,
                    MediaLibrary.Sequence::new);

    public static final StreamCodec<RegistryFriendlyByteBuf, MediaLibraryPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, MediaLibraryPayload::hostPos,
                    SONG.apply(ByteBufCodecs.list(MediaLibrary.MOST_SONGS)), MediaLibraryPayload::songs,
                    SEQUENCE.apply(ByteBufCodecs.list(MediaLibrary.MOST_SEQUENCES)), MediaLibraryPayload::sequences,
                    SEQUENCE, MediaLibraryPayload::asked,
                    ByteBufCodecs.BOOL, MediaLibraryPayload::sound,
                    MediaLibraryPayload::new);

    public MediaLibraryPayload {
        songs = List.copyOf(songs);
        sequences = List.copyOf(sequences);
    }

    @Override
    public CustomPacketPayload.Type<MediaLibraryPayload> type() {
        return TYPE;
    }
}
