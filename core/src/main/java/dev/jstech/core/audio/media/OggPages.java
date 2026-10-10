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
import java.util.ArrayList;
import java.util.List;

/**
 * Walks the pages of an Ogg file and hands back the first packets of its first logical stream, with the position the
 * last page of that stream ends on. Both the probe and the cover reader need exactly this, and the little-endian reads
 * it takes, so they share one reader and one set of bounds checks.
 *
 * <p>This class is pure and carries no Minecraft dependency.
 */
final class OggPages {

    private static final byte[] CAPTURE = {'O', 'g', 'g', 'S'};
    private static final int HEADER = 27;
    private static final int LACING_CONTINUES = 255;

    private OggPages() {
    }

    /**
     * Reads the file page by page.
     *
     * @param wanted          how many packets of the first stream to collect
     * @param scanAll         whether to keep walking the pages after the packets are in hand, for the last granule
     * @param mostPacketBytes the largest a packet may grow before the file is refused
     * @throws IOException when a page is not a page, or runs past the end of the file
     */
    static Result read(final byte[] content, final int wanted, final boolean scanAll, final int mostPacketBytes)
            throws IOException {
        final List<byte[]> packets = new ArrayList<>();
        final ByteArrayOutputStream packet = new ByteArrayOutputStream();
        long lastGranule = -1L;
        int serial = 0;
        boolean first = true;
        int at = 0;
        while (at + HEADER <= content.length) {
            if (!startsWith(content, at, CAPTURE)) {
                throw new IOException("not an Ogg page at byte " + at);
            }
            final long granule = le64(content, at + 6);
            final int pageSerial = (int) le32(content, at + 14);
            final int segments = content[at + 26] & 0xFF;
            int data = at + HEADER + segments;
            if (data > content.length) {
                throw new IOException("an Ogg page runs past the end of the file");
            }
            if (first) {
                serial = pageSerial;
                first = false;
            }
            final boolean ours = pageSerial == serial;
            for (int s = 0; s < segments; s++) {
                final int length = content[at + HEADER + s] & 0xFF;
                if (data + length > content.length) {
                    throw new IOException("an Ogg segment runs past the end of the file");
                }
                if (ours && packets.size() < wanted) {
                    packet.write(content, data, length);
                    if (packet.size() > mostPacketBytes) {
                        throw new IOException("an Ogg packet is larger than any this reads");
                    }
                    if (length < LACING_CONTINUES) {
                        packets.add(packet.toByteArray());
                        packet.reset();
                    }
                }
                data += length;
            }
            if (ours && granule >= 0) {
                lastGranule = granule;
            }
            if (!scanAll && packets.size() >= wanted) {
                break;
            }
            at = data;
        }
        return new Result(packets, lastGranule);
    }

    static boolean startsWith(final byte[] content, final int at, final byte[] prefix) {
        if (at < 0 || at + prefix.length > content.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (content[at + i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    /** Four little-endian bytes as an unsigned value, or zero when they are not all in the array. */
    static long le32(final byte[] b, final int at) {
        if (at < 0 || at + 4 > b.length) {
            return 0L;
        }
        return (b[at] & 0xFFL) | (b[at + 1] & 0xFFL) << 8 | (b[at + 2] & 0xFFL) << 16 | (b[at + 3] & 0xFFL) << 24;
    }

    static long le64(final byte[] b, final int at) {
        return le32(b, at) | le32(b, at + 4) << 32;
    }

    /**
     * What was read: the first packets of the first stream, and the last granule position that stream's pages
     * declared (negative when none did).
     */
    record Result(List<byte[]> packets, long lastGranule) {
    }
}
