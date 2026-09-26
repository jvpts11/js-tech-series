/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.media;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A recording the server keeps, named by what is in it: the SHA-256 of its bytes, the kind of file it is and how many
 * bytes it has. Two players who bring the same song bring the same recording, which is kept once; and a client that
 * already has a recording by that name has exactly those bytes, so it never asks for them again.
 *
 * @param hash   the SHA-256 of the file's bytes, as 64 lower-case hexadecimal digits
 * @param format the file's kind, as its extension is written without the dot ({@code ogg}, {@code wav})
 * @param bytes  how many bytes the file has
 */
public record MediaId(String hash, String format, long bytes) {

    /** How many hexadecimal digits a SHA-256 is written with. */
    public static final int HASH_DIGITS = 64;
    /** The longest a kind of file may be named. */
    public static final int MAX_FORMAT = 8;

    private static final Pattern HASH = Pattern.compile("[0-9a-f]{" + HASH_DIGITS + "}");
    private static final Pattern FORMAT = Pattern.compile("[a-z0-9]{1," + MAX_FORMAT + "}");

    public MediaId {
        if (hash == null || !HASH.matcher(hash).matches()) {
            throw new IllegalArgumentException("not a SHA-256 in lower-case hexadecimal: " + hash);
        }
        format = format == null ? "" : format.toLowerCase(Locale.ROOT);
        if (!FORMAT.matcher(format).matches()) {
            throw new IllegalArgumentException("not a kind of file: " + format);
        }
        if (bytes < 0) {
            throw new IllegalArgumentException("a file has no fewer than no bytes: " + bytes);
        }
    }

    /** The recording those bytes are, of that kind. */
    public static MediaId of(final byte[] content, final String format) {
        final MessageDigest digest = sha256();
        return new MediaId(HexFormat.of().formatHex(digest.digest(content)), format, content.length);
    }

    /** The digest a recording's name is made with, fresh for one file. */
    public static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (final NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("every Java runtime has SHA-256", unavailable);
        }
    }

    /** The name the recording's file is kept under: its hash and its kind. */
    public String fileName() {
        return hash + "." + format;
    }
}
