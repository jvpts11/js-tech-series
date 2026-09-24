/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.pcm;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.Nullable;

/**
 * The kinds of sound file the series can play that are not in its own assets, a recording a player keeps on a disk
 * among them, each read by the decoder registered for its extension. WAV is read here; Ogg Vorbis is registered by
 * the client, which carries the game's own decoder; a mod or an addon registers any other kind.
 */
public final class AudioDecoders {

    private static final Map<String, IAudioDecoder> BY_EXTENSION = new ConcurrentHashMap<>();

    static {
        register("wav", new WavDecoder());
    }

    private AudioDecoders() {
    }

    /**
     * Registers the decoder for files with that extension, written without its dot.
     *
     * @throws IllegalStateException when that extension already has a decoder, so two mods never fight over one
     */
    public static void register(final String extension, final IAudioDecoder decoder) {
        final String key = normalise(extension);
        if (BY_EXTENSION.putIfAbsent(key, decoder) != null) {
            throw new IllegalStateException("." + key + " files already have a decoder");
        }
    }

    /** The decoder for files with that extension, or null when none reads them. */
    @Nullable
    public static IAudioDecoder find(final String extension) {
        return BY_EXTENSION.get(normalise(extension));
    }

    /** The decoder for a file of that name, by the extension after its last dot, or null when none reads it. */
    @Nullable
    public static IAudioDecoder forFile(final String name) {
        final int dot = name.lastIndexOf('.');
        return dot < 0 || dot == name.length() - 1 ? null : find(name.substring(dot + 1));
    }

    /** Every extension that has a decoder, in alphabetical order. */
    public static Set<String> extensions() {
        return new TreeSet<>(BY_EXTENSION.keySet());
    }

    /**
     * Opens the samples of the file of that name.
     *
     * @throws IOException when no decoder reads its kind, or its decoder cannot read it
     */
    public static IPcmSource open(final String name, final InputStream encoded) throws IOException {
        final IAudioDecoder decoder = forFile(name);
        if (decoder == null) {
            encoded.close();
            throw new IOException("no decoder reads " + name);
        }
        return decoder.open(encoded);
    }

    private static String normalise(final String extension) {
        final String bare = extension.startsWith(".") ? extension.substring(1) : extension;
        if (bare.isEmpty()) {
            throw new IllegalArgumentException("an extension has at least one character");
        }
        return bare.toLowerCase(Locale.ROOT);
    }
}
