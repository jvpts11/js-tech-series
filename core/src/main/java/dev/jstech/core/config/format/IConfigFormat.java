/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config.format;

import java.util.Map;

/**
 * A way of writing a settings file down: how its bytes read into plain values, and how plain values are written
 * back, with comments above what they describe where the format has room for them.
 *
 * <p>Plain values are what every format shares: maps with text keys, kept in the order the file has them, lists,
 * text, numbers ({@link Integer}, {@link Long} or {@link Double}) and booleans. What a setting means is not the
 * format's business; it only carries the values between the file and the settings that read them.
 */
public interface IConfigFormat {

    /**
     * How deeply a file's maps and lists may sit inside each other. A settings file needs a handful of levels; a file
     * damaged, or built to choke, can open thousands, and a reader that follows each one a level down its stack runs
     * past the end of it.
     */
    int DEEPEST_NESTING = 32;

    /** The file's extension, without the dot. */
    String extension();

    /** Whether the format has room for comments, which a file of it then carries above what they describe. */
    boolean keepsComments();

    /**
     * Reads a whole file into plain values.
     *
     * @throws ConfigFormatException when the bytes are not a file of this format, saying where they stop being one
     */
    Map<String, Object> read(byte[] file) throws ConfigFormatException;

    /** Writes plain values down, with the comments {@code comments} gives for each place when the format keeps them. */
    byte[] write(Map<String, Object> values, IConfigComments comments);
}
