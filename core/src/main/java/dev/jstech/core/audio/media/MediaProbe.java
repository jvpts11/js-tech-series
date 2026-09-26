/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.media;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Reads what a recording is from its file, without decoding a sample of it: how long it runs, its rate, its channels,
 * its bit rate, and the tags it carries. The server reads it where it has no decoder at all, when a player brings a
 * song or an album is put in its catalogue.
 *
 * <p>Two kinds are read. An Ogg Vorbis file says its rate and channels in its first packet and its tags in its
 * second, and how long it runs is the position its last page ends on. A WAV file says its rate and channels in its
 * format chunk, how long it runs by the size of its data, and its tags in an INFO list, when it has one.
 */
public final class MediaProbe {

    private static final byte[] OGG_CAPTURE = {'O', 'g', 'g', 'S'};
    private static final int OGG_HEADER = 27;
    private static final int VORBIS_ID_PACKET = 1;
    private static final int VORBIS_COMMENT_PACKET = 3;
    private static final int VORBIS_SIGNATURE = 7;
    private static final int LACING_CONTINUES = 255;
    private static final int WAV_PCM = 1;
    private static final int WAV_EXTENSIBLE = 0xFFFE;
    private static final int YEAR_DIGITS = 4;

    private MediaProbe() {
    }

    /**
     * What the recording in {@code content} is.
     *
     * @param format the kind of file it is, by its extension
     * @throws IOException when it is not a file of that kind this can read
     */
    public static MediaInfo probe(final String format, final byte[] content) throws IOException {
        return switch (format.toLowerCase(Locale.ROOT)) {
            case "ogg" -> ogg(content);
            case "wav" -> wav(content);
            default -> throw new IOException("no reader for ." + format + " files");
        };
    }

    /* Ogg Vorbis: the identification and comment packets of the first stream, and the end of its last page. */
    private static MediaInfo ogg(final byte[] content) throws IOException {
        int at = 0;
        int serial = 0;
        boolean first = true;
        long lastGranule = -1L;
        int packets = 0;
        int channels = 0;
        int rate = 0;
        int nominal = 0;
        MediaTags tags = MediaTags.EMPTY;
        final ByteArrayOutputStream packet = new ByteArrayOutputStream();
        while (at + OGG_HEADER <= content.length) {
            if (!startsWith(content, at, OGG_CAPTURE)) {
                throw new IOException("not an Ogg page at byte " + at);
            }
            final long granule = le64(content, at + 6);
            final int pageSerial = (int) le32(content, at + 14);
            final int segments = content[at + 26] & 0xFF;
            int data = at + OGG_HEADER + segments;
            if (data > content.length) {
                throw new IOException("an Ogg page runs past the end of the file");
            }
            if (first) {
                serial = pageSerial;
                first = false;
            }
            final boolean ours = pageSerial == serial;
            for (int s = 0; s < segments; s++) {
                final int length = content[at + OGG_HEADER + s] & 0xFF;
                if (data + length > content.length) {
                    throw new IOException("an Ogg segment runs past the end of the file");
                }
                if (ours && packets < 2) {
                    packet.write(content, data, length);
                    if (length < LACING_CONTINUES) {
                        final byte[] whole = packet.toByteArray();
                        packet.reset();
                        if (packets == 0) {
                            requireVorbis(whole, VORBIS_ID_PACKET);
                            channels = whole[11] & 0xFF;
                            rate = (int) le32(whole, 12);
                            nominal = (int) le32(whole, 20);
                        } else {
                            requireVorbis(whole, VORBIS_COMMENT_PACKET);
                            tags = vorbisTags(whole);
                        }
                        packets++;
                    }
                }
                data += length;
            }
            if (ours && granule >= 0) {
                lastGranule = granule;
            }
            at = data;
        }
        if (packets < 2 || rate <= 0 || channels <= 0) {
            throw new IOException("not an Ogg Vorbis recording");
        }
        final long millis = lastGranule <= 0 ? 0L : lastGranule * 1000L / rate;
        final int kbps = nominal > 0 ? nominal / 1000 : kbpsOf(content.length, millis);
        return new MediaInfo(millis, rate, channels, kbps, tags);
    }

    private static void requireVorbis(final byte[] packet, final int type) throws IOException {
        if (packet.length < VORBIS_SIGNATURE || (packet[0] & 0xFF) != type
                || !"vorbis".equals(new String(packet, 1, 6, StandardCharsets.US_ASCII))) {
            throw new IOException("not an Ogg Vorbis recording");
        }
    }

    /* The comments of a Vorbis comment packet: a vendor string, then KEY=value pairs. */
    private static MediaTags vorbisTags(final byte[] packet) throws IOException {
        int at = VORBIS_SIGNATURE;
        final long vendor = le32(packet, at);
        at += 4 + (int) vendor;
        final long count = le32(packet, at);
        at += 4;
        final Map<String, String> found = new HashMap<>();
        for (long i = 0; i < count; i++) {
            final long length = le32(packet, at);
            at += 4;
            if (length < 0 || at + length > packet.length) {
                throw new IOException("a Vorbis comment runs past its packet");
            }
            final String comment = new String(packet, at, (int) length, StandardCharsets.UTF_8);
            at += (int) length;
            final int equals = comment.indexOf('=');
            if (equals > 0) {
                found.putIfAbsent(comment.substring(0, equals).toUpperCase(Locale.ROOT), comment.substring(equals + 1));
            }
        }
        final String date = found.getOrDefault("DATE", found.getOrDefault("YEAR", ""));
        return new MediaTags(found.get("TITLE"), found.get("ARTIST"), found.get("ALBUM"), yearOf(date),
                trackOf(found.getOrDefault("TRACKNUMBER", "")));
    }

    /* WAV: the format chunk, the size of the data chunk, and an INFO list when there is one. */
    private static MediaInfo wav(final byte[] content) throws IOException {
        if (content.length < 12 || !startsWith(content, 0, new byte[] {'R', 'I', 'F', 'F'})
                || !startsWith(content, 8, new byte[] {'W', 'A', 'V', 'E'})) {
            throw new IOException("not a WAV file");
        }
        int at = 12;
        int channels = 0;
        int rate = 0;
        int byteRate = 0;
        int blockAlign = 0;
        long dataBytes = -1L;
        MediaTags tags = MediaTags.EMPTY;
        while (at + 8 <= content.length) {
            final String id = new String(content, at, 4, StandardCharsets.US_ASCII);
            final long size = le32(content, at + 4);
            final int body = at + 8;
            final long end = Math.min(content.length, body + size);
            switch (id) {
                case "fmt " -> {
                    final int tag = le16(content, body);
                    if (tag != WAV_PCM && tag != WAV_EXTENSIBLE) {
                        throw new IOException("a WAV file of samples other than plain PCM");
                    }
                    channels = le16(content, body + 2);
                    rate = (int) le32(content, body + 4);
                    byteRate = (int) le32(content, body + 8);
                    blockAlign = le16(content, body + 12);
                }
                case "data" -> dataBytes = size == 0xFFFFFFFFL ? content.length - body : end - body;
                case "LIST" -> {
                    if (end - body >= 4 && startsWith(content, body, new byte[] {'I', 'N', 'F', 'O'})) {
                        tags = wavTags(content, body + 4, (int) end);
                    }
                }
                default -> {
                    // Any other chunk says nothing a player reads.
                }
            }
            // Chunks are padded to an even length.
            at = (int) Math.min(content.length, body + size + (size & 1L));
        }
        if (rate <= 0 || channels <= 0 || blockAlign <= 0 || dataBytes < 0) {
            throw new IOException("a WAV file without its format or its data");
        }
        final long millis = dataBytes / blockAlign * 1000L / rate;
        return new MediaInfo(millis, rate, channels, byteRate * 8 / 1000, tags);
    }

    /* The sub-chunks of an INFO list: each a zero-terminated string. */
    private static MediaTags wavTags(final byte[] content, final int from, final int to) {
        final Map<String, String> found = new HashMap<>();
        int at = from;
        while (at + 8 <= to) {
            final String id = new String(content, at, 4, StandardCharsets.US_ASCII);
            final int size = (int) le32(content, at + 4);
            final int body = at + 8;
            final int end = Math.min(to, body + size);
            int stop = body;
            while (stop < end && content[stop] != 0) {
                stop++;
            }
            found.put(id, new String(content, body, stop - body, StandardCharsets.UTF_8));
            at = body + size + (size & 1);
        }
        return new MediaTags(found.get("INAM"), found.get("IART"), found.get("IPRD"),
                yearOf(found.getOrDefault("ICRD", "")), trackOf(found.getOrDefault("ITRK", "")));
    }

    /* The year a date starts with, as four digits, or nothing when it starts with none. */
    private static String yearOf(final String date) {
        final String trimmed = date.strip();
        if (trimmed.length() < YEAR_DIGITS) {
            return "";
        }
        for (int i = 0; i < YEAR_DIGITS; i++) {
            if (!Character.isDigit(trimmed.charAt(i))) {
                return "";
            }
        }
        return trimmed.substring(0, YEAR_DIGITS);
    }

    /* The number a track is written with, before any "of so many": "3/12" is the third. */
    private static int trackOf(final String track) {
        int value = 0;
        for (final char c : track.strip().toCharArray()) {
            if (!Character.isDigit(c)) {
                break;
            }
            value = value * 10 + (c - '0');
            if (value > Short.MAX_VALUE) {
                return 0;
            }
        }
        return value;
    }

    private static int kbpsOf(final long bytes, final long millis) {
        return millis <= 0 ? 0 : (int) (bytes * 8L / millis);
    }

    private static boolean startsWith(final byte[] content, final int at, final byte[] prefix) {
        if (at + prefix.length > content.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (content[at + i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static int le16(final byte[] b, final int at) {
        return at + 2 > b.length ? 0 : (b[at] & 0xFF) | (b[at + 1] & 0xFF) << 8;
    }

    private static long le32(final byte[] b, final int at) {
        if (at + 4 > b.length) {
            return 0L;
        }
        return (b[at] & 0xFFL) | (b[at + 1] & 0xFFL) << 8 | (b[at + 2] & 0xFFL) << 16 | (b[at + 3] & 0xFFL) << 24;
    }

    private static long le64(final byte[] b, final int at) {
        return le32(b, at) | le32(b, at + 4) << 32;
    }
}
