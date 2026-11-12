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
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * How Easy Disc Creator's computer stands: its drives, each lettered with what it holds, and the copy under way.
 *
 * @param hostPos the computer
 * @param places  the drives
 * @param copy    the copy under way, or {@link Copy#NONE}
 * @param answer  what the last action came to when it did not go, or nothing
 */
public record DiscCopyStatePayload(BlockPos hostPos, List<Place> places, Copy copy, Text answer)
        implements CustomPacketPayload {

    /** The most drives listed. */
    public static final int MOST_PLACES = 16;

    public static final CustomPacketPayload.Type<DiscCopyStatePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "disc_copy_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DiscCopyStatePayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, DiscCopyStatePayload::hostPos,
                    Place.STREAM_CODEC.apply(ByteBufCodecs.list(MOST_PLACES)), DiscCopyStatePayload::places,
                    Copy.STREAM_CODEC, DiscCopyStatePayload::copy,
                    TextCodecs.STREAM_CODEC, DiscCopyStatePayload::answer,
                    DiscCopyStatePayload::new);

    public DiscCopyStatePayload {
        places = List.copyOf(places);
    }

    @Override
    public CustomPacketPayload.Type<DiscCopyStatePayload> type() {
        return TYPE;
    }

    /**
     * A drive and what it holds, as data: the program words it in the player's own language.
     *
     * @param pos     where it is
     * @param letter  the letter the program names it by
     * @param drive   what kind of drive it is
     * @param item    the item in it, or the air when it holds none
     * @param name    the name the player gave the medium in it, or nothing
     * @param blank   whether it holds a blank to write on
     * @param vinyl   whether it is a Vinyl RW Drive
     * @param storage whether the medium in it holds the network's storage
     * @param bytes   how many bytes the medium in it holds
     * @param song    the song of a music disc in it, by its id, or nothing
     * @param seconds how long that song is, or 0
     */
    public record Place(long pos, String letter, Text drive, ResourceLocation item, String name, boolean blank,
                        boolean vinyl, boolean storage, long bytes, String song, int seconds) {

        /** The longest name of a medium or a song sent; a longer one is cut to it. */
        public static final int MOST_NAME = 64;

        public static final StreamCodec<RegistryFriendlyByteBuf, Place> STREAM_CODEC = StreamCodec.of(
                (buf, place) -> {
                    ByteBufCodecs.VAR_LONG.encode(buf, place.pos());
                    ByteBufCodecs.stringUtf8(2).encode(buf, place.letter());
                    TextCodecs.STREAM_CODEC.encode(buf, place.drive());
                    ResourceLocation.STREAM_CODEC.encode(buf, place.item());
                    ByteBufCodecs.stringUtf8(MOST_NAME).encode(buf, place.name());
                    ByteBufCodecs.BOOL.encode(buf, place.blank());
                    ByteBufCodecs.BOOL.encode(buf, place.vinyl());
                    ByteBufCodecs.BOOL.encode(buf, place.storage());
                    ByteBufCodecs.VAR_LONG.encode(buf, place.bytes());
                    ByteBufCodecs.stringUtf8(MOST_NAME).encode(buf, place.song());
                    ByteBufCodecs.VAR_INT.encode(buf, place.seconds());
                },
                buf -> new Place(ByteBufCodecs.VAR_LONG.decode(buf), ByteBufCodecs.stringUtf8(2).decode(buf),
                        TextCodecs.STREAM_CODEC.decode(buf), ResourceLocation.STREAM_CODEC.decode(buf),
                        ByteBufCodecs.stringUtf8(MOST_NAME).decode(buf), ByteBufCodecs.BOOL.decode(buf),
                        ByteBufCodecs.BOOL.decode(buf), ByteBufCodecs.BOOL.decode(buf),
                        ByteBufCodecs.VAR_LONG.decode(buf), ByteBufCodecs.stringUtf8(MOST_NAME).decode(buf),
                        ByteBufCodecs.VAR_INT.decode(buf)));
    }

    /**
     * The copy under way.
     *
     * @param phase  its step, by {@code DiscCopies.Phase} order, or -1 for none
     * @param source the source drive's letter
     * @param target the target drive's letter
     * @param done   how far through its step it is, in thousandths
     * @param bytes  how many bytes it moves
     * @param left   the ticks its step still takes
     * @param ask    what it asks of the player now, or nothing
     * @param note   what it came to, once over
     */
    public record Copy(int phase, String source, String target, int done, long bytes, int left, Text ask, Text note) {

        public static final Copy NONE = new Copy(-1, "", "", 0, 0L, 0, Text.EMPTY, Text.EMPTY);

        public static final StreamCodec<RegistryFriendlyByteBuf, Copy> STREAM_CODEC = StreamCodec.of(
                (buf, copy) -> {
                    ByteBufCodecs.VAR_INT.encode(buf, copy.phase());
                    ByteBufCodecs.stringUtf8(2).encode(buf, copy.source());
                    ByteBufCodecs.stringUtf8(2).encode(buf, copy.target());
                    ByteBufCodecs.VAR_INT.encode(buf, copy.done());
                    ByteBufCodecs.VAR_LONG.encode(buf, copy.bytes());
                    ByteBufCodecs.VAR_INT.encode(buf, copy.left());
                    TextCodecs.STREAM_CODEC.encode(buf, copy.ask());
                    TextCodecs.STREAM_CODEC.encode(buf, copy.note());
                },
                buf -> new Copy(ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.stringUtf8(2).decode(buf),
                        ByteBufCodecs.stringUtf8(2).decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                        ByteBufCodecs.VAR_LONG.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                        TextCodecs.STREAM_CODEC.decode(buf), TextCodecs.STREAM_CODEC.decode(buf)));
    }
}
