/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.pcm;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class WavDecoderTest {

    private static final int PCM = 1;
    private static final int FLOAT = 3;

    @Test
    void open_readsSixteenBitStereoAsItIs() throws IOException {
        final byte[] data = le16(1000, -1000, 32767, -32768);
        final IPcmSource source = new WavDecoder().open(stream(wav(fmt(PCM, 2, 44_100, 16), chunk("data", data))));
        assertEquals(new PcmFormat(44_100, 2), source.format());
        assertArrayEquals(new short[] {1000, -1000, 32767, -32768}, readAll(source));
    }

    @Test
    void open_widensEightBitSamples() throws IOException {
        final byte[] data = {(byte) 128, (byte) 255, 0};
        final IPcmSource source = new WavDecoder().open(stream(wav(fmt(PCM, 1, 8_000, 8), chunk("data", data))));
        assertArrayEquals(new short[] {0, 127 << 8, -32768}, readAll(source));
    }

    @Test
    void open_keepsTheTopOfTwentyFourBitSamples() throws IOException {
        final byte[] data = {0x11, 0x34, 0x12, 0x00, 0x00, (byte) 0x80};
        final IPcmSource source = new WavDecoder().open(stream(wav(fmt(PCM, 1, 48_000, 24), chunk("data", data))));
        assertArrayEquals(new short[] {0x1234, -32768}, readAll(source));
    }

    @Test
    void open_skipsChunksItHasNoUseForAndTheirPadding() throws IOException {
        final byte[] wav = wav(chunk("LIST", new byte[] {1, 2, 3}), fmt(PCM, 1, 22_050, 16),
                chunk("cue ", new byte[5]), chunk("data", le16(7, -7)));
        assertArrayEquals(new short[] {7, -7}, readAll(new WavDecoder().open(stream(wav))));
    }

    @Test
    void open_readsTheExtensibleHeader() throws IOException {
        final ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.writeBytes(le16(0xFFFE, 1));
        body.writeBytes(le32(16_000));
        body.writeBytes(le32(32_000));
        body.writeBytes(le16(2, 16, 22, 16));
        body.writeBytes(le32(4));
        body.writeBytes(le16(PCM));
        body.writeBytes(new byte[14]);
        final byte[] wav = wav(chunk("fmt ", body.toByteArray()), chunk("data", le16(42)));
        final IPcmSource source = new WavDecoder().open(stream(wav));
        assertEquals(new PcmFormat(16_000, 1), source.format());
        assertArrayEquals(new short[] {42}, readAll(source));
    }

    @Test
    void read_stopsAtTheEndOfItsDataWhenTheFileGoesOn() throws IOException {
        final byte[] wav = wav(fmt(PCM, 1, 22_050, 16), chunk("data", le16(1, 2, 3)), chunk("LIST", new byte[8]));
        assertArrayEquals(new short[] {1, 2, 3}, readAll(new WavDecoder().open(stream(wav))));
    }

    @Test
    void read_endsWithTheWholeSamplesAFileCutShortStillHas() throws IOException {
        final byte[] whole = wav(fmt(PCM, 1, 22_050, 16), chunk("data", le16(5, 6, 7, 8)));
        final byte[] cut = Arrays.copyOf(whole, whole.length - 3);
        assertArrayEquals(new short[] {5, 6}, readAll(new WavDecoder().open(stream(cut))));
    }

    @Test
    void open_refusesWhatItDoesNotRead() {
        final WavDecoder decoder = new WavDecoder();
        assertThrows(IOException.class, () -> decoder.open(stream("OggS and then some".getBytes(
                StandardCharsets.US_ASCII))));
        assertThrows(IOException.class, () -> decoder.open(stream(wav(fmt(FLOAT, 1, 44_100, 32),
                chunk("data", new byte[4])))));
        assertThrows(IOException.class, () -> decoder.open(stream(wav(fmt(PCM, 6, 44_100, 16),
                chunk("data", new byte[12])))));
        assertThrows(IOException.class, () -> decoder.open(stream(wav(chunk("data", le16(1)),
                fmt(PCM, 1, 44_100, 16)))));
        assertThrows(IOException.class, () -> decoder.open(stream(wav(fmt(PCM, 1, 44_100, 16)))));
    }

    @Test
    void close_letsGoOfTheFile() throws IOException {
        final boolean[] closed = {false};
        final InputStream file = new ByteArrayInputStream(wav(fmt(PCM, 1, 22_050, 16), chunk("data", le16(1)))) {
            @Override
            public void close() {
                closed[0] = true;
            }
        };
        new WavDecoder().open(file).close();
        assertTrue(closed[0]);
    }

    private static short[] readAll(final IPcmSource source) throws IOException {
        short[] out = new short[0];
        final short[] chunk = new short[3];
        int read = source.read(chunk, 0, chunk.length);
        while (read > 0) {
            final int at = out.length;
            out = Arrays.copyOf(out, at + read);
            System.arraycopy(chunk, 0, out, at, read);
            read = source.read(chunk, 0, chunk.length);
        }
        assertEquals(-1, source.read(chunk, 0, chunk.length));
        return out;
    }

    private static InputStream stream(final byte[] bytes) {
        return new ByteArrayInputStream(bytes);
    }

    private static byte[] wav(final byte[]... chunks) {
        final ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.writeBytes("WAVE".getBytes(StandardCharsets.US_ASCII));
        for (final byte[] one : chunks) {
            body.writeBytes(one);
        }
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes("RIFF".getBytes(StandardCharsets.US_ASCII));
        out.writeBytes(le32(body.size()));
        out.writeBytes(body.toByteArray());
        return out.toByteArray();
    }

    private static byte[] fmt(final int code, final int channels, final int rate, final int bits) {
        final ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.writeBytes(le16(code, channels));
        body.writeBytes(le32(rate));
        body.writeBytes(le32(rate * channels * bits / 8));
        body.writeBytes(le16(channels * bits / 8, bits));
        return chunk("fmt ", body.toByteArray());
    }

    private static byte[] chunk(final String tag, final byte[] body) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(tag.getBytes(StandardCharsets.US_ASCII));
        out.writeBytes(le32(body.length));
        out.writeBytes(body);
        if (body.length % 2 == 1) {
            out.write(0);
        }
        return out.toByteArray();
    }

    private static byte[] le16(final int... values) {
        final byte[] out = new byte[values.length * 2];
        for (int i = 0; i < values.length; i++) {
            out[2 * i] = (byte) values[i];
            out[2 * i + 1] = (byte) (values[i] >> 8);
        }
        return out;
    }

    private static byte[] le32(final int value) {
        return new byte[] {(byte) value, (byte) (value >> 8), (byte) (value >> 16), (byte) (value >> 24)};
    }
}
