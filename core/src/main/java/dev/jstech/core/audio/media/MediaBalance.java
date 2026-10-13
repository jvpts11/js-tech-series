/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.media;

/**
 * How much of the real connection recordings may take, and how big a recording a player may bring. The server's
 * owner sets them in the server config, which overwrites these when it loads and on a reload; they are the owner's
 * to set because a server with many players and a thin connection wants them lower than one with a few friends.
 *
 * <p>This is the game's own connection, not the machines' network inside the game: how fast a download goes between
 * two computers in the world is the business of whoever makes the computers, and it is slower than this.
 */
public final class MediaBalance {

    /** Kilobytes a second the server sends each player of the recordings they are about to hear. */
    public static final int DEFAULT_DOWNLOAD_KILOBYTES_PER_SECOND = 1024;
    /** Kilobytes a second a player sends the server of a recording they bring. */
    public static final int DEFAULT_UPLOAD_KILOBYTES_PER_SECOND = 512;
    /** The biggest recording a player may bring, in megabytes; 0 takes none. */
    public static final int DEFAULT_MAX_FILE_MEGABYTES = 32;
    /** How much of the store the recordings one player brought may take, in megabytes; 0 sets no limit. */
    public static final int DEFAULT_PLAYER_QUOTA_MEGABYTES = 512;

    private static final int TICKS_PER_SECOND = 20;
    private static final int KILOBYTE = 1024;

    private static volatile int downloadKilobytesPerSecond = DEFAULT_DOWNLOAD_KILOBYTES_PER_SECOND;
    private static volatile int uploadKilobytesPerSecond = DEFAULT_UPLOAD_KILOBYTES_PER_SECOND;
    private static volatile int maxFileMegabytes = DEFAULT_MAX_FILE_MEGABYTES;
    private static volatile int playerQuotaMegabytes = DEFAULT_PLAYER_QUOTA_MEGABYTES;

    private MediaBalance() {
    }

    /** The bytes a tick the server sends one player of the recordings they are about to hear. */
    public static int downloadBytesPerTick() {
        return Math.max(1, downloadKilobytesPerSecond * KILOBYTE / TICKS_PER_SECOND);
    }

    public static void setDownloadKilobytesPerSecond(final int kilobytes) {
        downloadKilobytesPerSecond = Math.max(1, kilobytes);
    }

    /** The bytes a tick a player sends of a recording they bring. */
    public static int uploadBytesPerTick() {
        return Math.max(1, uploadKilobytesPerSecond * KILOBYTE / TICKS_PER_SECOND);
    }

    public static void setUploadKilobytesPerSecond(final int kilobytes) {
        uploadKilobytesPerSecond = Math.max(1, kilobytes);
    }

    /** The biggest recording a player may bring, in bytes; 0 when players may bring none. */
    public static long maxFileBytes() {
        return (long) maxFileMegabytes * KILOBYTE * KILOBYTE;
    }

    /** The same, in megabytes, as the owner wrote it. */
    public static int maxFileMegabytes() {
        return maxFileMegabytes;
    }

    public static void setMaxFileMegabytes(final int megabytes) {
        maxFileMegabytes = Math.max(0, megabytes);
    }

    /** How much of the store the recordings one player brought may take, in bytes; 0 when there is no limit. */
    public static long playerQuotaBytes() {
        return (long) playerQuotaMegabytes * KILOBYTE * KILOBYTE;
    }

    /** The same, in megabytes, as the owner wrote it. */
    public static int playerQuotaMegabytes() {
        return playerQuotaMegabytes;
    }

    public static void setPlayerQuotaMegabytes(final int megabytes) {
        playerQuotaMegabytes = Math.max(0, megabytes);
    }

    /** Back to the design estimates, for tests. */
    public static void reset() {
        setDownloadKilobytesPerSecond(DEFAULT_DOWNLOAD_KILOBYTES_PER_SECOND);
        setUploadKilobytesPerSecond(DEFAULT_UPLOAD_KILOBYTES_PER_SECOND);
        setMaxFileMegabytes(DEFAULT_MAX_FILE_MEGABYTES);
        setPlayerQuotaMegabytes(DEFAULT_PLAYER_QUOTA_MEGABYTES);
    }
}
