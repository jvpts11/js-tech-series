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
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A page of the Standard Soundfoundry as the machine answers it: the sidebar every page shows (the Soundfoundry
 * Servers the machine reaches, the one it picked, its playlists) and what the page itself lists, its albums and its
 * songs.
 *
 * @param hostPos the machine
 * @param page    which page: {@link #HOME}, {@link #LIBRARY}, {@link #ALBUM}, {@link #PLAYLIST}, {@link #SEARCH} or
 *                {@link #CATALOG}
 * @param arg     what the page is of, as it was asked for
 * @param sidebar what the sidebar shows
 * @param albums  the albums the page lists: the catalogue's, or the one album the page is
 * @param rows    the songs the page lists, each in its section
 */
public record SoundfoundryPagePayload(BlockPos hostPos, int page, String arg, Sidebar sidebar, List<Album> albums,
                                      List<Row> rows) implements CustomPacketPayload {

    public static final int HOME = 0;
    public static final int LIBRARY = 1;
    public static final int ALBUM = 2;
    public static final int PLAYLIST = 3;
    public static final int SEARCH = 4;
    public static final int CATALOG = 5;

    /** A song of the page's own list: an album's, a playlist's, a search's, the machine's own files. */
    public static final int TRACKS = 0;
    /** A song of the network's library, on the home page. */
    public static final int NETWORK = 1;
    /** A song the machine has downloaded, on the home page. */
    public static final int DOWNLOADED = 2;

    /** The song only plays through a Soundfoundry Server. */
    public static final int STREAM = 0;
    /** The same recording is on the machine's disk, so it plays without a server. */
    public static final int ON_DISK = 1;
    /** It is being downloaded. */
    public static final int COMING = 2;
    /** It is one of the machine's own files. */
    public static final int LOCAL = 3;
    /** It is one of the machine's own files, being sent to a Soundfoundry Server. */
    public static final int SENDING = 4;

    /** The most albums a page lists. */
    public static final int MAX_ALBUMS = 200;
    /** The most songs a page lists. */
    public static final int MAX_ROWS = SoundfoundryState.MAX_SONGS;
    /** The most servers the sidebar lists. */
    public static final int MAX_SERVERS = 16;
    /** The most playlists the sidebar lists. */
    public static final int MAX_PLAYLISTS = 64;
    /** The longest a song's reference is: its path, behind what it is streamed from. */
    public static final int MAX_REF = SoundfoundryActionPayload.MAX_PATH;
    /*
     * A title or a name as a page shows it: half of what a tag keeps, which is wider than any row draws, so a page
     * of the most songs stays well under what one packet carries.
     */
    private static final int MAX_TEXT = MediaTags.MAX_TEXT / 2;
    private static final int MAX_ID = SoundfoundryBrowsePayload.MAX_ARG;

    public static final CustomPacketPayload.Type<SoundfoundryPagePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                    "soundfoundry_page"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SoundfoundryPagePayload> STREAM_CODEC =
            StreamCodec.of(SoundfoundryPagePayload::encode, SoundfoundryPagePayload::decode);

    /* Copied on the way in, so what the window is handed cannot change under it after it arrives. */
    public SoundfoundryPagePayload {
        albums = List.copyOf(albums.size() > MAX_ALBUMS ? albums.subList(0, MAX_ALBUMS) : albums);
        rows = List.copyOf(rows.size() > MAX_ROWS ? rows.subList(0, MAX_ROWS) : rows);
    }

    /**
     * What the sidebar shows.
     *
     * @param servers   the Soundfoundry Servers the machine reaches
     * @param chosen    the id of the one it streams from, or empty when it reaches none
     * @param playlists its playlists, the liked songs first
     * @param catalog   whether the server's catalogue reaches it
     */
    public record Sidebar(List<Server> servers, String chosen, List<String> playlists, boolean catalog) {

        /** A machine that reaches nothing. */
        public static final Sidebar NONE = new Sidebar(List.of(), "", List.of(), false);

        public Sidebar {
            servers = List.copyOf(servers.size() > MAX_SERVERS ? servers.subList(0, MAX_SERVERS) : servers);
            playlists = List.copyOf(playlists.size() > MAX_PLAYLISTS ? playlists.subList(0, MAX_PLAYLISTS)
                    : playlists);
        }
    }

    /**
     * A Soundfoundry Server.
     *
     * @param id        how the network knows it
     * @param name      what it goes by
     * @param songs     how many songs its library holds
     * @param listeners how many computers are listening to it
     */
    public record Server(String id, String name, int songs, int listeners) {
    }

    /**
     * An album of the catalogue.
     *
     * @param downloaded how many of its songs the machine has on its disk
     */
    public record Album(String id, String title, String artist, String year, int songs, long millis,
                        int downloaded) {
    }

    /**
     * A song as a page lists it.
     *
     * @param ref     how the song is named on a list, which playing it puts on the machine's list
     * @param album   the album it is on, or empty
     * @param from    the computer it came from, for a song of the network's library; empty otherwise
     * @param state   {@link #STREAM}, {@link #ON_DISK}, {@link #COMING}, {@link #LOCAL} or {@link #SENDING}
     * @param section {@link #TRACKS}, {@link #NETWORK} or {@link #DOWNLOADED}
     * @param liked   whether the song is in the liked songs
     */
    public record Row(String ref, String title, String artist, String album, long millis, String from, int state,
                      int section, boolean liked) {
    }

    /** The songs of one section, in the order the page lists them. */
    public List<Row> section(final int section) {
        final List<Row> out = new ArrayList<>();
        for (final Row row : rows) {
            if (row.section() == section) {
                out.add(row);
            }
        }
        return out;
    }

    @Override
    public CustomPacketPayload.Type<SoundfoundryPagePayload> type() {
        return TYPE;
    }

    private static void encode(final RegistryFriendlyByteBuf buf, final SoundfoundryPagePayload p) {
        BlockPos.STREAM_CODEC.encode(buf, p.hostPos);
        buf.writeVarInt(p.page);
        buf.writeUtf(clip(p.arg, MAX_ID), MAX_ID);
        buf.writeVarInt(p.sidebar.servers().size());
        for (final Server server : p.sidebar.servers()) {
            buf.writeUtf(clip(server.id(), MAX_ID), MAX_ID);
            buf.writeUtf(clip(server.name(), MAX_TEXT), MAX_TEXT);
            buf.writeVarInt(server.songs());
            buf.writeVarInt(server.listeners());
        }
        buf.writeUtf(clip(p.sidebar.chosen(), MAX_ID), MAX_ID);
        buf.writeVarInt(p.sidebar.playlists().size());
        for (final String playlist : p.sidebar.playlists()) {
            buf.writeUtf(clip(playlist, MAX_TEXT), MAX_TEXT);
        }
        buf.writeBoolean(p.sidebar.catalog());
        buf.writeVarInt(p.albums.size());
        for (final Album album : p.albums) {
            buf.writeUtf(clip(album.id(), MAX_ID), MAX_ID);
            buf.writeUtf(clip(album.title(), MAX_TEXT), MAX_TEXT);
            buf.writeUtf(clip(album.artist(), MAX_TEXT), MAX_TEXT);
            buf.writeUtf(clip(album.year(), MAX_TEXT), MAX_TEXT);
            buf.writeVarInt(album.songs());
            buf.writeVarLong(album.millis());
            buf.writeVarInt(album.downloaded());
        }
        buf.writeVarInt(p.rows.size());
        for (final Row row : p.rows) {
            buf.writeUtf(clip(row.ref(), MAX_REF), MAX_REF);
            buf.writeUtf(clip(row.title(), MAX_TEXT), MAX_TEXT);
            buf.writeUtf(clip(row.artist(), MAX_TEXT), MAX_TEXT);
            buf.writeUtf(clip(row.album(), MAX_TEXT), MAX_TEXT);
            buf.writeVarLong(row.millis());
            buf.writeUtf(clip(row.from(), MAX_TEXT), MAX_TEXT);
            buf.writeVarInt(row.state());
            buf.writeVarInt(row.section());
            buf.writeBoolean(row.liked());
        }
    }

    private static SoundfoundryPagePayload decode(final RegistryFriendlyByteBuf buf) {
        final BlockPos host = BlockPos.STREAM_CODEC.decode(buf);
        final int page = buf.readVarInt();
        final String arg = buf.readUtf(MAX_ID);
        final int serverCount = Math.min(buf.readVarInt(), MAX_SERVERS);
        final List<Server> servers = new ArrayList<>(serverCount);
        for (int i = 0; i < serverCount; i++) {
            servers.add(new Server(buf.readUtf(MAX_ID), buf.readUtf(MAX_TEXT), buf.readVarInt(), buf.readVarInt()));
        }
        final String chosen = buf.readUtf(MAX_ID);
        final int playlistCount = Math.min(buf.readVarInt(), MAX_PLAYLISTS);
        final List<String> playlists = new ArrayList<>(playlistCount);
        for (int i = 0; i < playlistCount; i++) {
            playlists.add(buf.readUtf(MAX_TEXT));
        }
        final Sidebar sidebar = new Sidebar(servers, chosen, playlists, buf.readBoolean());
        final int albumCount = Math.min(buf.readVarInt(), MAX_ALBUMS);
        final List<Album> albums = new ArrayList<>(albumCount);
        for (int i = 0; i < albumCount; i++) {
            albums.add(new Album(buf.readUtf(MAX_ID), buf.readUtf(MAX_TEXT), buf.readUtf(MAX_TEXT),
                    buf.readUtf(MAX_TEXT), buf.readVarInt(), buf.readVarLong(), buf.readVarInt()));
        }
        final int rowCount = Math.min(buf.readVarInt(), MAX_ROWS);
        final List<Row> rows = new ArrayList<>(rowCount);
        for (int i = 0; i < rowCount; i++) {
            rows.add(new Row(buf.readUtf(MAX_REF), buf.readUtf(MAX_TEXT), buf.readUtf(MAX_TEXT),
                    buf.readUtf(MAX_TEXT), buf.readVarLong(), buf.readUtf(MAX_TEXT), buf.readVarInt(),
                    buf.readVarInt(), buf.readBoolean()));
        }
        return new SoundfoundryPagePayload(host, page, arg, sidebar, albums, rows);
    }

    private static String clip(final String text, final int max) {
        return text.length() <= max ? text : text.substring(0, max);
    }
}
