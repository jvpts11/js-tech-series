/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.media;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

/**
 * The picture a recording carries of itself, its cover, as the file holds it: in an Ogg Vorbis file a
 * {@code METADATA_BLOCK_PICTURE} comment (a FLAC picture block in base 64), the front cover first; in a Wave file the
 * attached picture of an ID3 tag in an {@code id3 } chunk. What comes back are the bytes of the picture's own file, a
 * PNG or a JPEG as it was put there, for whoever shows it to read.
 *
 * <p>This class is pure and carries no Minecraft dependency.
 */
public final class MediaPicture {

    /** The largest picture looked for: far past any cover, well short of what memory holds. */
    public static final int MOST_BYTES = 8 * 1024 * 1024;

    private static final int OGG_HEADER = 27;
    private static final int LACING_CONTINUES = 255;
    private static final int VORBIS_COMMENT_PACKET = 3;
    private static final int VORBIS_SIGNATURE = 7;
    private static final String PICTURE_TAG = "METADATA_BLOCK_PICTURE=";
    /* The picture type of a front cover, in FLAC's picture block and in ID3's attached picture alike. */
    private static final int FRONT_COVER = 3;
    private static final int ID3_HEADER = 10;
    private static final int ID3_FRAME_HEADER = 10;

    private MediaPicture() {
    }

    /**
     * The cover the recording in {@code content} carries, or empty when it carries none or cannot be read.
     *
     * @param format the kind of file it is, by its extension
     */
    public static Optional<byte[]> find(final String format, final byte[] content) {
        try {
            return switch (format.toLowerCase(Locale.ROOT)) {
                case "ogg" -> ogg(content);
                case "wav" -> wav(content);
                default -> Optional.empty();
            };
        } catch (final IndexOutOfBoundsException | IllegalArgumentException malformed) {
            // A file that claims more than it holds carries no cover worth showing.
            return Optional.empty();
        }
    }

    /* The comment packet of the first stream, and the picture it names. */
    private static Optional<byte[]> ogg(final byte[] content) {
        final byte[] comments = secondPacket(content);
        if (comments == null || comments.length < VORBIS_SIGNATURE
                || (comments[0] & 0xFF) != VORBIS_COMMENT_PACKET) {
            return Optional.empty();
        }
        int at = VORBIS_SIGNATURE;
        at += 4 + (int) le32(comments, at);
        final long count = le32(comments, at);
        at += 4;
        byte[] found = null;
        for (long i = 0; i < count && at + 4 <= comments.length; i++) {
            final int length = (int) le32(comments, at);
            at += 4;
            if (length < 0 || at + length > comments.length) {
                return Optional.ofNullable(found);
            }
            if (length > PICTURE_TAG.length() && startsWithIgnoringCase(comments, at, PICTURE_TAG)) {
                final String encoded = new String(comments, at + PICTURE_TAG.length(),
                        length - PICTURE_TAG.length(), StandardCharsets.US_ASCII);
                final byte[] block = Base64.getMimeDecoder().decode(encoded);
                final int type = (int) be32(block, 0);
                final byte[] picture = flacPicture(block);
                if (picture != null && (found == null || type == FRONT_COVER)) {
                    found = picture;
                    if (type == FRONT_COVER) {
                        return Optional.of(found);
                    }
                }
            }
            at += length;
        }
        return Optional.ofNullable(found);
    }

    /* A FLAC picture block: its type, the picture's kind and description, its size, then the picture itself. */
    private static byte[] flacPicture(final byte[] block) {
        int at = 4;
        at += 4 + (int) be32(block, at);
        at += 4 + (int) be32(block, at);
        at += 16;
        final int length = (int) be32(block, at);
        at += 4;
        if (length <= 0 || length > MOST_BYTES || at + length > block.length) {
            return null;
        }
        final byte[] picture = new byte[length];
        System.arraycopy(block, at, picture, 0, length);
        return picture;
    }

    /* The Wave file's ID3 tag, when it has one, and the picture attached to it. */
    private static Optional<byte[]> wav(final byte[] content) {
        if (content.length < 12 || !ascii(content, 0, "RIFF") || !ascii(content, 8, "WAVE")) {
            return Optional.empty();
        }
        int at = 12;
        while (at + 8 <= content.length) {
            final String id = new String(content, at, 4, StandardCharsets.US_ASCII);
            final long size = le32(content, at + 4);
            final int body = at + 8;
            if (id.equalsIgnoreCase("id3 ")) {
                return id3(content, body, (int) Math.min(content.length, body + size));
            }
            at = (int) Math.min(content.length, body + size + (size & 1L));
        }
        return Optional.empty();
    }

    /* An ID3 tag of version 2.3 or 2.4, and the first front cover it attaches, else the first picture. */
    private static Optional<byte[]> id3(final byte[] content, final int from, final int to) {
        if (to - from < ID3_HEADER || !ascii(content, from, "ID3")) {
            return Optional.empty();
        }
        final int version = content[from + 3] & 0xFF;
        if (version != 3 && version != 4) {
            return Optional.empty();
        }
        final int end = Math.min(to, from + ID3_HEADER + syncsafe(content, from + 6));
        int at = from + ID3_HEADER;
        byte[] found = null;
        while (at + ID3_FRAME_HEADER <= end && content[at] != 0) {
            final String frame = new String(content, at, 4, StandardCharsets.US_ASCII);
            final int size = version == 4 ? syncsafe(content, at + 4) : (int) be32(content, at + 4);
            final int body = at + ID3_FRAME_HEADER;
            if (size <= 0 || body + size > end) {
                break;
            }
            if (frame.equals("APIC")) {
                final int type = pictureType(content, body, body + size);
                final byte[] picture = attached(content, body, body + size);
                if (picture != null && (found == null || type == FRONT_COVER)) {
                    found = picture;
                    if (type == FRONT_COVER) {
                        break;
                    }
                }
            }
            at = body + size;
        }
        return Optional.ofNullable(found);
    }

    /* An attached picture: its text's encoding, its kind, its type, its description, then the picture itself. */
    private static byte[] attached(final byte[] content, final int from, final int to) {
        final int encoding = content[from] & 0xFF;
        int at = zeroAfter(content, from + 1, to, 1) + 1;
        at += 1;
        final int width = encoding == 1 || encoding == 2 ? 2 : 1;
        at = zeroAfter(content, at, to, width) + width;
        final int length = to - at;
        if (at > to || length <= 0 || length > MOST_BYTES) {
            return null;
        }
        final byte[] picture = new byte[length];
        System.arraycopy(content, at, picture, 0, length);
        return picture;
    }

    private static int pictureType(final byte[] content, final int from, final int to) {
        final int at = zeroAfter(content, from + 1, to, 1) + 1;
        return at < to ? content[at] & 0xFF : -1;
    }

    /* Where the string starting at {@code from} ends: its terminator, one or two bytes of zero. */
    private static int zeroAfter(final byte[] content, final int from, final int to, final int width) {
        int at = from;
        while (at + width <= to) {
            if (content[at] == 0 && (width == 1 || content[at + 1] == 0)) {
                return at;
            }
            at += width;
        }
        return to;
    }

    /* The second packet of the first logical stream: in Vorbis, the comments. */
    private static byte[] secondPacket(final byte[] content) {
        int at = 0;
        int serial = 0;
        boolean first = true;
        int packets = 0;
        final ByteArrayOutputStream packet = new ByteArrayOutputStream();
        while (at + OGG_HEADER <= content.length && ascii(content, at, "OggS")) {
            final int pageSerial = (int) le32(content, at + 14);
            final int segments = content[at + 26] & 0xFF;
            int data = at + OGG_HEADER + segments;
            if (first) {
                serial = pageSerial;
                first = false;
            }
            for (int s = 0; s < segments && data <= content.length; s++) {
                final int length = content[at + OGG_HEADER + s] & 0xFF;
                if (data + length > content.length) {
                    return null;
                }
                if (pageSerial == serial) {
                    packet.write(content, data, length);
                    if (packet.size() > MOST_BYTES * 2) {
                        return null;
                    }
                    if (length < LACING_CONTINUES) {
                        if (++packets == 2) {
                            return packet.toByteArray();
                        }
                        packet.reset();
                    }
                }
                data += length;
            }
            at = data;
        }
        return null;
    }

    private static boolean ascii(final byte[] content, final int at, final String text) {
        if (at + text.length() > content.length) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (content[at + i] != text.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static boolean startsWithIgnoringCase(final byte[] content, final int at, final String text) {
        return at + text.length() <= content.length && new String(content, at, text.length(),
                StandardCharsets.US_ASCII).equalsIgnoreCase(text);
    }

    private static long le32(final byte[] b, final int at) {
        return (b[at] & 0xFFL) | (b[at + 1] & 0xFFL) << 8 | (b[at + 2] & 0xFFL) << 16 | (b[at + 3] & 0xFFL) << 24;
    }

    private static long be32(final byte[] b, final int at) {
        return (b[at] & 0xFFL) << 24 | (b[at + 1] & 0xFFL) << 16 | (b[at + 2] & 0xFFL) << 8 | (b[at + 3] & 0xFFL);
    }

    /* A size written seven bits to a byte, the way ID3 keeps its sizes free of false frame starts. */
    private static int syncsafe(final byte[] b, final int at) {
        return (b[at] & 0x7F) << 21 | (b[at + 1] & 0x7F) << 14 | (b[at + 2] & 0x7F) << 7 | (b[at + 3] & 0x7F);
    }
}
