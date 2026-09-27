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
 * A song Soundfoundry is fetching over the network: from the server's catalogue, or from the folder another computer
 * of the network shares. It comes in at the speed of the slowest cable on its way and is kept in the machine's music
 * folder once the last byte is in.
 *
 * <p>This class is pure and carries no Minecraft dependency.
 */
public final class SongDownload {

    private final String name;
    private final MediaId media;
    private final long source;
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
     * @param name     what the song's file is to be called once it is kept
     * @param media    the recording
     * @param source   where the sharing computer stands, or {@link #FROM_CATALOG}
     * @param path     where the song is on the sharing computer, or its album and file in the catalogue
     * @param from     what the sharing computer is called, or {@code ""} for the catalogue
     * @param playlist whether the song goes at the end of the playlist once it is kept
     */
    public SongDownload(final String name, final MediaId media, final long source, final String path,
                        final String from, final boolean playlist) {
        this.name = name;
        this.media = media;
        this.source = source;
        this.path = path;
        this.from = from;
        this.playlist = playlist;
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

    public long source() {
        return source;
    }

    /** Whether the song comes from the server's catalogue rather than from another computer. */
    public boolean fromCatalog() {
        return source == FROM_CATALOG;
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
