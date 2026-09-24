/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.pcm;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Reads WAV files: uncompressed samples of 8, 16 or 24 bits, mono or stereo, at any rate, the extensible form of the
 * header included. Whatever it reads it hands over as 16-bit samples; chunks it has no use for (a list of cues, the
 * artist's name) are skipped.
 */
public final class WavDecoder implements IAudioDecoder {

    private static final int FORMAT_PCM = 1;
    private static final int FORMAT_EXTENSIBLE = 0xFFFE;
    private static final int PLAIN_FORMAT_BYTES = 16;
    private static final int EXTENSION_BYTES = 10;
    /** A data chunk this long runs to the end of the file, which is how a recording still being made is written. */
    private static final long UNTIL_THE_END = 0xFFFFFFFFL;

    @Override
    public IPcmSource open(final InputStream encoded) throws IOException {
        final InputStream in = new BufferedInputStream(encoded);
        try {
            return readHeader(in);
        } catch (final IOException | RuntimeException failure) {
            in.close();
            throw failure;
        }
    }

    private static IPcmSource readHeader(final InputStream in) throws IOException {
        expect(in, "RIFF");
        readInt(in);
        expect(in, "WAVE");
        PcmFormat format = null;
        int bits = 0;
        while (true) {
            final String tag = readTag(in);
            final long size = readInt(in) & 0xFFFFFFFFL;
            if ("fmt ".equals(tag)) {
                int code = readShort(in);
                final int channels = readShort(in);
                final int rate = readInt(in);
                readInt(in);
                readShort(in);
                bits = readShort(in);
                long rest = size - PLAIN_FORMAT_BYTES;
                if (code == FORMAT_EXTENSIBLE && rest >= EXTENSION_BYTES) {
                    readShort(in);
                    readShort(in);
                    readInt(in);
                    code = readShort(in);
                    rest -= EXTENSION_BYTES;
                }
                skip(in, rest + (size & 1));
                format = formatOf(code, channels, rate, bits);
            } else if ("data".equals(tag)) {
                if (format == null) {
                    throw new IOException("the samples come before the format that says how to read them");
                }
                return new Source(in, format, bits / Byte.SIZE, size == UNTIL_THE_END ? Long.MAX_VALUE : size);
            } else {
                skip(in, size + (size & 1));
            }
        }
    }

    private static PcmFormat formatOf(final int code, final int channels, final int rate, final int bits)
            throws IOException {
        if (code != FORMAT_PCM) {
            throw new IOException("only uncompressed samples are read, and these are of format " + code);
        }
        if (bits != 8 && bits != 16 && bits != 24) {
            throw new IOException("samples of 8, 16 or 24 bits are read, and these have " + bits);
        }
        try {
            return new PcmFormat(rate, channels);
        } catch (final IllegalArgumentException unplayable) {
            throw new IOException(unplayable.getMessage(), unplayable);
        }
    }

    private static void expect(final InputStream in, final String tag) throws IOException {
        final String read = readTag(in);
        if (!tag.equals(read)) {
            throw new IOException("not a WAV file: " + tag + " was expected and " + read + " was found");
        }
    }

    private static String readTag(final InputStream in) throws IOException {
        return new String(in.readNBytes(4), StandardCharsets.US_ASCII);
    }

    private static int readShort(final InputStream in) throws IOException {
        return readByte(in) | readByte(in) << 8;
    }

    private static int readInt(final InputStream in) throws IOException {
        return readShort(in) | readShort(in) << 16;
    }

    private static int readByte(final InputStream in) throws IOException {
        final int value = in.read();
        if (value < 0) {
            throw new EOFException("the file ends inside its header");
        }
        return value;
    }

    private static void skip(final InputStream in, final long count) throws IOException {
        if (count < 0) {
            throw new IOException("a chunk is shorter than its own header says");
        }
        in.skipNBytes(count);
    }

    /** The samples of the data chunk, read and widened or narrowed to 16 bits as they are asked for. */
    private static final class Source implements IPcmSource {

        private final InputStream in;
        private final PcmFormat format;
        private final int bytesPerSample;
        private long remaining;
        private byte[] buffer = new byte[0];

        Source(final InputStream in, final PcmFormat format, final int bytesPerSample, final long bytes) {
            this.in = in;
            this.format = format;
            this.bytesPerSample = bytesPerSample;
            this.remaining = bytes;
        }

        @Override
        public PcmFormat format() {
            return format;
        }

        @Override
        public int read(final short[] into, final int offset, final int length) throws IOException {
            final int wanted = (int) Math.min(length, remaining / bytesPerSample);
            if (wanted <= 0) {
                remaining = 0;
                return -1;
            }
            final int bytes = wanted * bytesPerSample;
            if (buffer.length < bytes) {
                buffer = new byte[bytes];
            }
            final int got = in.readNBytes(buffer, 0, bytes);
            final int samples = got / bytesPerSample;
            if (samples == 0) {
                remaining = 0;
                return -1;
            }
            remaining = got < bytes ? 0 : remaining - bytes;
            for (int i = 0; i < samples; i++) {
                into[offset + i] = widen(i * bytesPerSample);
            }
            return samples;
        }

        @Override
        public void close() throws IOException {
            in.close();
        }

        /* 8-bit samples are unsigned around 128; wider ones are signed, little-endian, and 24 keeps its top 16. */
        private short widen(final int at) {
            return switch (bytesPerSample) {
                case 1 -> (short) (((buffer[at] & 0xFF) - 128) << 8);
                case 2 -> (short) ((buffer[at] & 0xFF) | buffer[at + 1] << 8);
                default -> (short) ((buffer[at + 1] & 0xFF) | buffer[at + 2] << 8);
            };
        }
    }
}
