/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import dev.jstech.core.audio.media.MediaId;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.network.DataTier;
import dev.jstech.core.text.Text;
import org.jetbrains.annotations.Nullable;

/**
 * A song Soundfoundry is moving over the network: fetching it from the server's catalogue, from the folder another
 * computer of the network shares or from a Soundfoundry Server's library, or sending one of its own to a Soundfoundry
 * Server. It goes at the speed of the slowest cable on its way and is kept, once the last byte is in, in the music
 * folder of the machine it was going to.
 *
 * <p>This class is pure and carries no Minecraft dependency.
 */
public final class SongDownload {

    private final Kind kind;
    private final String name;
    private final MediaId media;
    private final long source;
    private final String server;
    private final String path;
    private final String from;
    private final boolean playlist;
    private long done;
    private Status status = Status.RUNNING;
    /** The slowest cable on the way the last time it was looked at, or null while none is known. */
    @Nullable
    private DataTier link;
    private Text trouble = Text.EMPTY;

    /** What {@link #source()} is for a song from the server's catalogue. */
    public static final long FROM_CATALOG = Long.MIN_VALUE;

    /**
     * A song fetched from the catalogue or from another computer's shared folder.
     *
     * @param name     what the song's file is to be called once it is kept
     * @param media    the recording
     * @param source   where the sharing computer stands, or {@link #FROM_CATALOG}
     * @param path     where the song is on the sharing computer, or its album and file in the catalogue
     * @param from     what the sharing computer is called, or {@code ""} for the catalogue
     * @param playlist whether the song goes at the end of the playlist once it is kept
     */
    public SongDownload(final String name, final MediaId media, final long source, final String path,
                        final String from, final boolean playlist) {
        this(source == FROM_CATALOG ? Kind.CATALOG : Kind.PEER, name, media, source, "", path, from, playlist);
    }

    private SongDownload(final Kind kind, final String name, final MediaId media, final long source,
                         final String server, final String path, final String from, final boolean playlist) {
        this.kind = kind;
        this.name = name;
        this.media = media;
        this.source = source;
        this.server = server;
        this.path = path;
        this.from = from;
        this.playlist = playlist;
    }

    /**
     * A song fetched from a Soundfoundry Server's library.
     *
     * @param server the server, as the network knows it
     * @param path   where the song is on the server
     * @param from   what the server is called
     */
    public static SongDownload fromServer(final String name, final MediaId media, final String server,
                                          final String path, final String from, final boolean playlist) {
        return new SongDownload(Kind.SERVER, name, media, 0L, server, path, from, playlist);
    }

    /**
     * One of the machine's own songs sent to a Soundfoundry Server, for the rest of the network to hear.
     *
     * @param path where the song is on this machine
     * @param to   what the server is called
     */
    public static SongDownload toServer(final String name, final MediaId media, final String server,
                                        final String path, final String to) {
        return new SongDownload(Kind.UPLOAD, name, media, 0L, server, path, to, false);
    }

    /** Puts a song back as a machine's save kept it. */
    public static SongDownload restored(final Kind kind, final String name, final MediaId media, final long source,
                                        final String server, final String path, final String from,
                                        final boolean playlist) {
        return new SongDownload(kind, name, media, source, server, path, from, playlist);
    }

    /** Which way a song goes, and from where. */
    public enum Kind implements IStableName {
        /** From another computer's shared folder. */
        PEER("peer"),
        /** From the server's catalogue. */
        CATALOG("catalog"),
        /** From a Soundfoundry Server's library. */
        SERVER("server"),
        /** From this machine to a Soundfoundry Server. */
        UPLOAD("upload");

        private final String serializedName;

        Kind(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return serializedName;
        }
    }

    /** What happens to a song on its way. */
    public enum Status implements IStableName {
        /** Coming in. */
        RUNNING("running"),
        /** Nothing is coming in: the computer sharing it, or the network, cannot be reached for now. */
        WAITING("waiting"),
        /** In, and kept. */
        DONE("done"),
        /** It will not come in; {@link #trouble()} says why. */
        FAILED("failed");

        private final String serializedName;

        Status(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return serializedName;
        }

        /** Whether it is over, done or given up on. */
        public boolean finished() {
            return this == DONE || this == FAILED;
        }
    }

    public String name() {
        return name;
    }

    public MediaId media() {
        return media;
    }

    public Kind kind() {
        return kind;
    }

    public long source() {
        return source;
    }

    /** The Soundfoundry Server it comes from or goes to, as the network knows it; empty for the other kinds. */
    public String server() {
        return server;
    }

    /** Whether the song comes from the server's catalogue rather than from another computer. */
    public boolean fromCatalog() {
        return kind == Kind.CATALOG;
    }

    /** Whether the song goes from this machine to a Soundfoundry Server rather than coming to it. */
    public boolean upload() {
        return kind == Kind.UPLOAD;
    }

    public String path() {
        return path;
    }

    public String from() {
        return from;
    }

    public boolean playlist() {
        return playlist;
    }

    /** How many bytes of it are in. */
    public long done() {
        return done;
    }

    /** How many bytes it has. */
    public long bytes() {
        return media.bytes();
    }

    public Status status() {
        return status;
    }

    @Nullable
    public DataTier link() {
        return link;
    }

    /** Why it will not come in, or empty. */
    public Text trouble() {
        return trouble;
    }

    /** Whether it is still to come in, running or waiting. */
    public boolean active() {
        return status == Status.RUNNING || status == Status.WAITING;
    }

    /** Whether every byte is in. */
    public boolean complete() {
        return done >= bytes();
    }

    /**
     * Brings in what arrives in that long at that speed, never past the end.
     *
     * @param bytesPerSecond how fast it comes in
     * @param millis         for how long
     */
    public void advance(final long bytesPerSecond, final long millis) {
        if (status != Status.RUNNING || bytesPerSecond <= 0 || millis <= 0) {
            return;
        }
        // At least a byte, so a crawl still gets somewhere.
        final long arrived = Math.max(1L, bytesPerSecond * millis / 1000L);
        done = Math.min(bytes(), done + arrived);
    }

    /** How long what is left takes at that speed, in milliseconds; -1 when it is not coming in. */
    public long millisLeft(final long bytesPerSecond) {
        if (status != Status.RUNNING || bytesPerSecond <= 0) {
            return -1L;
        }
        final long left = bytes() - done;
        return left <= 0 ? 0L : (left * 1000L + bytesPerSecond - 1L) / bytesPerSecond;
    }

    /** It can be reached, over a way whose slowest cable is {@code value}; a waiting song comes in again. */
    public void reached(@Nullable final DataTier value) {
        link = value;
        if (status == Status.WAITING) {
            status = Status.RUNNING;
        }
    }

    /** It cannot be reached for now. */
    public void unreachable() {
        if (status == Status.RUNNING) {
            status = Status.WAITING;
        }
    }

    /** Every byte is in and the song is kept. */
    public void kept() {
        done = bytes();
        status = Status.DONE;
        trouble = Text.EMPTY;
    }

    /** It will not come in, for that reason. */
    public void failed(final Text why) {
        status = Status.FAILED;
        trouble = why;
    }

    /** Puts back how it stood when the machine was saved. */
    public void restore(final long doneBytes, final Status value, @Nullable final DataTier lastLink,
                        final Text why) {
        done = Math.clamp(doneBytes, 0L, bytes());
        status = value;
        link = lastLink;
        trouble = why;
    }
}
