/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.pcm;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class AudioDecodersTest {

    @Test
    void forFile_findsTheDecoderByItsExtensionInAnyCase() {
        assertInstanceOf(WavDecoder.class, AudioDecoders.forFile("Startup Chime.WAV"));
        assertInstanceOf(WavDecoder.class, AudioDecoders.find(".wav"));
        assertTrue(AudioDecoders.extensions().contains("wav"));
    }

    @Test
    void forFile_isNullForAKindNothingReads() {
        assertNull(AudioDecoders.forFile("readme"));
        assertNull(AudioDecoders.forFile("dot."));
        assertNull(AudioDecoders.forFile("tape.xyz"));
    }

    @Test
    void register_refusesASecondDecoderForTheSameExtension() {
        final IAudioDecoder first = encoded -> {
            throw new IOException("unused");
        };
        AudioDecoders.register("JunitTone", first);
        assertSame(first, AudioDecoders.find("junittone"));
        assertThrows(IllegalStateException.class, () -> AudioDecoders.register("junittone", first));
        assertThrows(IllegalArgumentException.class, () -> AudioDecoders.register(".", first));
    }

    @Test
    void open_refusesAKindNothingReadsAndLetsGoOfTheFile() {
        final boolean[] closed = {false};
        final ByteArrayInputStream file = new ByteArrayInputStream(new byte[4]) {
            @Override
            public void close() {
                closed[0] = true;
            }
        };
        assertThrows(IOException.class, () -> AudioDecoders.open("song.xyz", file));
        assertTrue(closed[0]);
    }
}
