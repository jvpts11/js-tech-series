/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.audio.SoundfoundryShare;
import dev.jstech.computers.program.SongDownload;
import dev.jstech.computers.program.SoundfoundryState;
import dev.jstech.core.audio.media.MediaTags;
import dev.jstech.core.id.StableNames;
import dev.jstech.core.network.DataLink;
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
import org.jetbrains.annotations.Nullable;

/**
 * How Soundfoundry's sharing stands on a machine, for the window showing it: the songs it is fetching, how many it
 * shares and how many computers it reaches, what a search found when it is the answer to one, and the songs it shares
 * when the window shows them.
 *
 * @param hostPos      the machine
 * @param found        what a search found, only in the answer to one
 * @param downloads    the songs it is fetching and those it fetched, oldest first
 * @param shared       the names of the songs it shares, only when the window asked for them
 * @param sharedFolder where it keeps the songs it shares, written the way its system writes a path
 * @param sharing      how many songs it shares
 * @param computers    how many other computers of its network run Soundfoundry
 * @param store        whether the server's catalogue reaches it
 * @param ownLink      the cable it is plugged into, as {@link #linkOf} names it
 * @param trouble      what the player is told about the last thing they did, or empty
 */
public record SoundfoundryShareStatePayload(BlockPos hostPos, Optional<List<Found>> found, List<Fetch> downloads,
                                            Optional<List<String>> shared, String sharedFolder, int sharing,
                                            int computers, boolean store, String ownLink, Text trouble)
        implements CustomPacketPayload {

    /** The most shared songs named at once. */
    public static final int MAX_SHARED = 500;
    private static final int MAX_PATH = SoundfoundryActionPayload.MAX_PATH;
    /** The longest name of a cable or of a download's status. */
    private static final int MAX_NAME = 32;
    private static final StableNames<SongDownload.Status> STATUSES = StableNames.of(SongDownload.Status.class);

    public static final CustomPacketPayload.Type<SoundfoundryShareStatePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                    "soundfoundry_share_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SoundfoundryShareStatePayload> STREAM_CODEC =
            StreamCodec.of(SoundfoundryShareStatePayload::encode, SoundfoundryShareStatePayload::decode);

    /* Copied on the way in, so what the window is handed cannot change under it after it arrives. */
    public SoundfoundryShareStatePayload {
        found = found.map(List::copyOf);
        downloads = List.copyOf(downloads);
        shared = shared.map(List::copyOf);
    }

    /**
     * A song a search found, as the window lists it.
     *
     * @param title  a catalogue song's title, or a shared song's file name
     * @param artist who a catalogue song is by, or empty
     * @param bytes  how big it is
     * @param from   what the computer sharing it is called, or {@code ""} for the catalogue
     * @param source where that computer stands, or the catalogue's mark
     * @param path   where the song is, which is what asking for it names
     * @param link   the slowest cable on the way to that computer, as {@link #linkOf} names it; none for the catalogue
     */
    public record Found(String title, String artist, long bytes, String from, long source, String path, String link) {
    }

    /**
     * A song on its way, or one that came or never will.
     *
     * @param name       what its file is called
     * @param bytes      how big it is
     * @param done       how much of it is in
     * @param from       what the computer it comes from is called, or {@code ""} for the catalogue
     * @param link       the slowest cable on its way, as {@link #linkOf} names it; none while none is known
     * @param status     how it stands, by its status's name
     * @param millisLeft how long the rest takes, or -1 while it is not coming in
     * @param trouble    why it will not come, or empty
     */
    public record Fetch(String name, long bytes, long done, String from, String link, String status, long millisLeft,
                        Text trouble) {

        /** How it stands; a status this side does not know reads as waiting. */
        public SongDownload.Status statusValue() {
            return STATUSES.byName(status, SongDownload.Status.WAITING);
        }
    }

    /** A cable as it travels: by its name, or {@code ""} for none. */
    public static String linkOf(@Nullable final DataLink link) {
        return link == null ? "" : link.serializedName();
    }

    /** A cable as it came, or null for none. */
    @Nullable
    public static DataLink linkNamed(final String link) {
        return DataLink.byName(link);
    }

    @Override
    public CustomPacketPayload.Type<SoundfoundryShareStatePayload> type() {
        return TYPE;
    }

    private static void encode(final RegistryFriendlyByteBuf buf, final SoundfoundryShareStatePayload p) {
        BlockPos.STREAM_CODEC.encode(buf, p.hostPos);
        buf.writeBoolean(p.found.isPresent());
        if (p.found.isPresent()) {
            final List<Found> found = p.found.get();
            final int count = Math.min(found.size(), SoundfoundryShare.MAX_FOUND);
            buf.writeVarInt(count);
            for (int i = 0; i < count; i++) {
                final Found one = found.get(i);
                buf.writeUtf(clip(one.title(), MAX_PATH), MAX_PATH);
                buf.writeUtf(clip(one.artist(), MediaTags.MAX_TEXT), MediaTags.MAX_TEXT);
                buf.writeVarLong(one.bytes());
                buf.writeUtf(clip(one.from(), MediaTags.MAX_TEXT), MediaTags.MAX_TEXT);
                buf.writeLong(one.source());
                buf.writeUtf(clip(one.path(), MAX_PATH), MAX_PATH);
                buf.writeUtf(clip(one.link(), MAX_NAME), MAX_NAME);
            }
        }
        final int downloads = Math.min(p.downloads.size(), SoundfoundryState.MAX_DOWNLOADS);
        buf.writeVarInt(downloads);
        for (int i = 0; i < downloads; i++) {
            final Fetch one = p.downloads.get(i);
            buf.writeUtf(clip(one.name(), MAX_PATH), MAX_PATH);
            buf.writeVarLong(one.bytes());
            buf.writeVarLong(one.done());
            buf.writeUtf(clip(one.from(), MediaTags.MAX_TEXT), MediaTags.MAX_TEXT);
            buf.writeUtf(clip(one.link(), MAX_NAME), MAX_NAME);
            buf.writeUtf(clip(one.status(), MAX_NAME), MAX_NAME);
            buf.writeVarLong(one.millisLeft() + 1L);
            TextCodecs.STREAM_CODEC.encode(buf, one.trouble());
        }
        buf.writeBoolean(p.shared.isPresent());
        if (p.shared.isPresent()) {
            final List<String> shared = p.shared.get();
            final int count = Math.min(shared.size(), MAX_SHARED);
            buf.writeVarInt(count);
            for (int i = 0; i < count; i++) {
                buf.writeUtf(clip(shared.get(i), MAX_PATH), MAX_PATH);
            }
        }
        buf.writeUtf(clip(p.sharedFolder, MAX_PATH), MAX_PATH);
        buf.writeVarInt(p.sharing);
        buf.writeVarInt(p.computers);
        buf.writeBoolean(p.store);
        buf.writeUtf(clip(p.ownLink, MAX_NAME), MAX_NAME);
        TextCodecs.STREAM_CODEC.encode(buf, p.trouble);
    }

    private static SoundfoundryShareStatePayload decode(final RegistryFriendlyByteBuf buf) {
        final BlockPos host = BlockPos.STREAM_CODEC.decode(buf);
        Optional<List<Found>> found = Optional.empty();
        if (buf.readBoolean()) {
            final int count = Math.min(buf.readVarInt(), SoundfoundryShare.MAX_FOUND);
            final List<Found> read = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                read.add(new Found(buf.readUtf(MAX_PATH), buf.readUtf(MediaTags.MAX_TEXT), buf.readVarLong(),
                        buf.readUtf(MediaTags.MAX_TEXT), buf.readLong(), buf.readUtf(MAX_PATH),
                        buf.readUtf(MAX_NAME)));
            }
            found = Optional.of(read);
        }
        final int downloadCount = Math.min(buf.readVarInt(), SoundfoundryState.MAX_DOWNLOADS);
        final List<Fetch> downloads = new ArrayList<>(downloadCount);
        for (int i = 0; i < downloadCount; i++) {
            downloads.add(new Fetch(buf.readUtf(MAX_PATH), buf.readVarLong(), buf.readVarLong(),
                    buf.readUtf(MediaTags.MAX_TEXT), buf.readUtf(MAX_NAME), buf.readUtf(MAX_NAME),
                    buf.readVarLong() - 1L, TextCodecs.STREAM_CODEC.decode(buf)));
        }
        Optional<List<String>> shared = Optional.empty();
        if (buf.readBoolean()) {
            final int count = Math.min(buf.readVarInt(), MAX_SHARED);
            final List<String> read = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                read.add(buf.readUtf(MAX_PATH));
            }
            shared = Optional.of(read);
        }
        final String sharedFolder = buf.readUtf(MAX_PATH);
        final int sharing = buf.readVarInt();
        final int computers = buf.readVarInt();
        final boolean store = buf.readBoolean();
        final String ownLink = buf.readUtf(MAX_NAME);
        final Text trouble = TextCodecs.STREAM_CODEC.decode(buf);
        return new SoundfoundryShareStatePayload(host, found, downloads, shared, sharedFolder, sharing, computers,
                store, ownLink, trouble);
    }

    private static String clip(final String text, final int max) {
        return text.length() <= max ? text : text.substring(0, max);
    }
}
