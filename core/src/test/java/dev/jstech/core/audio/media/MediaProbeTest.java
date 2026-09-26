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
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MediaProbeTest {

    private static final int SERIAL = 0x1234;
    private static final int RATE = 44_100;

    @Test
    void probe_oggReadsItsRateChannelsLengthAndTags() throws IOException {
        final byte[] file = concat(
                page(0, id(2, RATE, 192_000)),
                page(0, comments(List.of("TITLE=Copper Rain", "artist=Mira Vale", "ALBUM=Lanterns", "DATE=2004-05-01",
                        "TRACKNUMBER=3/12"))),
                page(RATE * 3L + RATE / 2, new byte[] {1, 2, 3}));
        final MediaInfo info = MediaProbe.probe("ogg", file);
        assertEquals(RATE, info.sampleRate());
        assertEquals(2, info.channels());
        assertEquals(3500, info.millis(), "the position the last page ends on, at the stream's rate");
        assertEquals(192, info.kbps());
        assertEquals(new MediaTags("Copper Rain", "Mira Vale", "Lanterns", "2004", 3), info.tags());
    }

    @Test
    void probe_oggReadsACommentPacketThatRunsOverTwoPages() throws IOException {
        final byte[] comments = comments(List.of("TITLE=" + "x".repeat(300)));
        final byte[] first = Arrays.copyOfRange(comments, 0, 255);
        final byte[] rest = Arrays.copyOfRange(comments, 255, comments.length);
        final byte[] file = concat(page(0, id(1, RATE, 0)), continuing(first), page(0, rest),
                page(RATE, new byte[] {9}));
        final MediaInfo info = MediaProbe.probe("ogg", file);
        assertEquals(MediaTags.MAX_TEXT, info.tags().title().length(), "the whole title is read, then cut");
        assertEquals(1000, info.millis());
    }

    @Test
    void probe_oggThatIsNotVorbisIsRefused() {
        final byte[] notVorbis = id(2, RATE, 0);
        notVorbis[1] = 'O';
        assertThrows(IOException.class, () -> MediaProbe.probe("ogg", concat(page(0, notVorbis))));
    }

    @Test
    void probe_wavReadsItsFormatLengthAndInfoList() throws IOException {
        final int channels = 2;
        final int bytesPerSample = 2;
        final int frames = RATE * 2;
        final byte[] file = wav(channels, bytesPerSample, frames, List.of(
                new String[] {"INAM", "Lava Light"}, new String[] {"IART", "Lantern Drive"},
                new String[] {"IPRD", "Glow"}, new String[] {"ICRD", "1999"}, new String[] {"ITRK", "6"}));
        final MediaInfo info = MediaProbe.probe("wav", file);
        assertEquals(2000, info.millis());
        assertEquals(channels, info.channels());
        assertEquals(RATE * channels * bytesPerSample * 8 / 1000, info.kbps());
        assertEquals(new MediaTags("Lava Light", "Lantern Drive", "Glow", "1999", 6), info.tags());
    }

    @Test
    void probe_wavWithoutTagsSaysNothingAboutItself() throws IOException {
        assertEquals(MediaTags.EMPTY, MediaProbe.probe("wav", wav(1, 2, 100, List.of())).tags());
    }

    @Test
    void probe_kindWithNoReaderIsRefused() {
        assertThrows(IOException.class, () -> MediaProbe.probe("mp3", new byte[16]));
    }

    /* A Vorbis identification packet. */
    private static byte[] id(final int channels, final int rate, final int nominal) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(1);
        out.writeBytes("vorbis".getBytes(StandardCharsets.US_ASCII));
        le32(out, 0);
        out.write(channels);
        le32(out, rate);
        le32(out, 0);
        le32(out, nominal);
        le32(out, 0);
        out.write(0xB8);
        out.write(1);
        return out.toByteArray();
    }

    /* A Vorbis comment packet with those comments. */
    private static byte[] comments(final List<String> comments) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(3);
        out.writeBytes("vorbis".getBytes(StandardCharsets.US_ASCII));
        final byte[] vendor = "test".getBytes(StandardCharsets.UTF_8);
        le32(out, vendor.length);
        out.writeBytes(vendor);
        le32(out, comments.size());
        for (final String comment : comments) {
            final byte[] bytes = comment.getBytes(StandardCharsets.UTF_8);
            le32(out, bytes.length);
            out.writeBytes(bytes);
        }
        out.write(1);
        return out.toByteArray();
    }

    /* One Ogg page holding one whole packet. */
    private static byte[] page(final long granule, final byte[] packet) {
        final int full = packet.length / 255;
        final byte[] lacing = new byte[full + 1];
        Arrays.fill(lacing, 0, full, (byte) 255);
        lacing[full] = (byte) (packet.length % 255);
        return rawPage(granule, lacing, packet);
    }

    /* One Ogg page holding the start of a packet the next page finishes. */
    private static byte[] continuing(final byte[] start) {
        final byte[] lacing = new byte[start.length / 255];
        Arrays.fill(lacing, (byte) 255);
        return rawPage(-1L, lacing, start);
    }

    private static byte[] rawPage(final long granule, final byte[] lacing, final byte[] body) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes("OggS".getBytes(StandardCharsets.US_ASCII));
        out.write(0);
        out.write(0);
        le32(out, (int) granule);
        le32(out, (int) (granule >> 32));
        le32(out, SERIAL);
        le32(out, 0);
        le32(out, 0);
        out.write(lacing.length);
        out.writeBytes(lacing);
        out.writeBytes(body);
        return out.toByteArray();
    }

    /* A WAV file of silence, with an INFO list of those tags. */
    private static byte[] wav(final int channels, final int bytesPerSample, final int frames,
                              final List<String[]> tags) {
        final ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.writeBytes("WAVE".getBytes(StandardCharsets.US_ASCII));
        body.writeBytes("fmt ".getBytes(StandardCharsets.US_ASCII));
        le32(body, 16);
        le16(body, 1);
        le16(body, channels);
        le32(body, RATE);
        le32(body, RATE * channels * bytesPerSample);
        le16(body, channels * bytesPerSample);
        le16(body, bytesPerSample * 8);
        if (!tags.isEmpty()) {
            final ByteArrayOutputStream info = new ByteArrayOutputStream();
            info.writeBytes("INFO".getBytes(StandardCharsets.US_ASCII));
            for (final String[] tag : tags) {
                final byte[] text = (tag[1] + "\0").getBytes(StandardCharsets.UTF_8);
                info.writeBytes(tag[0].getBytes(StandardCharsets.US_ASCII));
                le32(info, text.length);
                info.writeBytes(text);
                if ((text.length & 1) == 1) {
                    info.write(0);
                }
            }
            body.writeBytes("LIST".getBytes(StandardCharsets.US_ASCII));
            le32(body, info.size());
            body.writeBytes(info.toByteArray());
        }
        body.writeBytes("data".getBytes(StandardCharsets.US_ASCII));
        final int dataBytes = frames * channels * bytesPerSample;
        le32(body, dataBytes);
        body.writeBytes(new byte[dataBytes]);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes("RIFF".getBytes(StandardCharsets.US_ASCII));
        le32(out, body.size());
        out.writeBytes(body.toByteArray());
        return out.toByteArray();
    }

    private static byte[] concat(final byte[]... parts) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (final byte[] part : parts) {
            out.writeBytes(part);
        }
        return out.toByteArray();
    }

    private static void le16(final ByteArrayOutputStream out, final int value) {
        out.write(value & 0xFF);
        out.write(value >> 8 & 0xFF);
    }

    private static void le32(final ByteArrayOutputStream out, final int value) {
        le16(out, value & 0xFFFF);
        le16(out, value >>> 16);
    }
}
