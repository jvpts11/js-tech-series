/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.fs;

import dev.jstech.core.audio.media.MediaId;
import dev.jstech.core.audio.media.MediaInfo;
import dev.jstech.core.audio.media.MediaTags;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * What a recording file on a disk holds: the name of the recording the server keeps, and what that recording is.
 *
 * <p>A song is megabytes, and a disk's files travel with the disk wherever it goes, to every client that looks at it.
 * So the bytes stay on the server, kept once however many disks hold the song, and the file on the disk holds a few
 * lines naming them. It still weighs on the disk what the song weighs, so a disk fills with music as it would.
 *
 * <p>What the recording says about itself is written into the file as well, so a player on the other side of the
 * network lists the songs on a disk, with their titles and lengths, from the disk alone.
 *
 * <p>This class is pure and carries no Minecraft dependency.
 *
 * @param media the recording the server keeps
 * @param info  what it is
 */
public record RecordingFile(MediaId media, MediaInfo info) {

    /** What every recording file starts with, so a file that is not one is recognised before it is read. */
    public static final String MAGIC = "JSREC1";

    /*
     * The longest a brought file's name is kept, leaving room within a name's limit for the dot, the longest kind of
     * recording and a number told apart by, such as "_12".
     */
    private static final int MAX_STEM = FsPaths.MAX_NAME_LENGTH - 1 - MediaId.MAX_FORMAT - 4;

    private static final char NEWLINE = '\n';
    private static final String MEDIA = "media";
    private static final String LENGTH = "length";
    private static final String RATE = "rate";
    private static final String CHANNELS = "channels";
    private static final String KBPS = "kbps";
    private static final String TITLE = "title";
    private static final String ARTIST = "artist";
    private static final String ALBUM = "album";
    private static final String YEAR = "year";
    private static final String TRACK = "track";

    /** The file's content: the magic line, then one line for each thing it says, each a key and its value. */
    public String write() {
        final MediaTags tags = info.tags();
        final StringBuilder out = new StringBuilder(MAGIC).append(NEWLINE);
        line(out, MEDIA, media.fileName() + " " + media.bytes());
        line(out, LENGTH, Long.toString(info.millis()));
        line(out, RATE, Integer.toString(info.sampleRate()));
        line(out, CHANNELS, Integer.toString(info.channels()));
        line(out, KBPS, Integer.toString(info.kbps()));
        line(out, TITLE, tags.title());
        line(out, ARTIST, tags.artist());
        line(out, ALBUM, tags.album());
        line(out, YEAR, tags.year());
        line(out, TRACK, Integer.toString(tags.track()));
        return out.toString();
    }

    /**
     * The recording a file's content names, or null when the content is not a recording file.
     *
     * <p>Only the recording's name is needed: a file whose other lines were lost still names its song, and plays.
     */
    @Nullable
    public static RecordingFile read(@Nullable final String content) {
        if (content == null || !content.startsWith(MAGIC + NEWLINE)) {
            return null;
        }
        final Map<String, String> values = new HashMap<>();
        for (final String line : content.substring(MAGIC.length() + 1).split("\n")) {
            final int space = line.indexOf(' ');
            if (space > 0) {
                values.putIfAbsent(line.substring(0, space), line.substring(space + 1));
            }
        }
        final MediaId media = mediaOf(values.get(MEDIA));
        if (media == null) {
            return null;
        }
        final MediaTags tags = new MediaTags(values.get(TITLE), values.get(ARTIST), values.get(ALBUM),
                values.get(YEAR), (int) number(values.get(TRACK)));
        return new RecordingFile(media, new MediaInfo(number(values.get(LENGTH)), (int) number(values.get(RATE)),
                (int) number(values.get(CHANNELS)), (int) number(values.get(KBPS)), tags));
    }

    /** How many bytes the recording is, which is what its file weighs on a disk. */
    public long bytes() {
        return media.bytes();
    }

    /**
     * The name, without its extension, a file brought from a player's own computer is kept under: its own name, with
     * whatever a disk's names may not hold taken out, and short enough to leave room for its extension and a number
     * that tells two of the same name apart. {@code fallback} when nothing of the name is left.
     */
    public static String stemOf(final String brought, final String fallback) {
        String name = brought == null ? "" : brought;
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        final int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        final StringBuilder kept = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            final char c = name.charAt(i);
            if (!Character.isISOControl(c)) {
                kept.append(c);
            }
        }
        String stem = kept.toString().strip();
        if (stem.length() > MAX_STEM) {
            stem = stem.substring(0, MAX_STEM).strip();
        }
        return stem.isEmpty() ? fallback : stem;
    }

    @Nullable
    private static MediaId mediaOf(@Nullable final String value) {
        if (value == null) {
            return null;
        }
        final int space = value.indexOf(' ');
        final int dot = value.indexOf('.');
        if (space < 0 || dot < 0 || dot > space) {
            return null;
        }
        try {
            final long bytes = Long.parseLong(value.substring(space + 1).strip());
            return new MediaId(value.substring(0, dot), value.substring(dot + 1, space), bytes);
        } catch (final IllegalArgumentException malformed) {
            return null;
        }
    }

    private static long number(@Nullable final String value) {
        if (value == null) {
            return 0L;
        }
        try {
            return Math.max(0L, Long.parseLong(value.strip()));
        } catch (final NumberFormatException malformed) {
            return 0L;
        }
    }

    /* A value is kept to one line, since a line is what ends it. */
    private static void line(final StringBuilder out, final String key, final String value) {
        out.append(key).append(' ').append(value.replace('\n', ' ').replace('\r', ' ')).append(NEWLINE);
    }
}
