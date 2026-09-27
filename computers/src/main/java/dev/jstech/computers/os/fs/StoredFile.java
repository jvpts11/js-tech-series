/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.fs;

import dev.jstech.core.tier.HardwareEra;

import java.nio.charset.StandardCharsets;

/**
 * An immutable value representing a single file stored on a disk.
 *
 * <p>{@code content} is the serialised file payload as a UTF-8 string: plain text for text
 * types ({@link FileType#IQL}, {@link FileType#TXT}, etc.) and a serialised representation
 * for binary-ish types such as {@link FileType#CRAFT}.
 *
 * <p>This record is pure and carries no Minecraft dependency.
 *
 * @param path    the full path of the file within the volume (e.g. {@code "script.iql"}
 *                for FLAT, or {@code "scripts/daily.iql"} for HIERARCHICAL)
 * @param type    the file type
 * @param content the file's text content encoded as a UTF-8 string
 * @param modified the world game time (total ticks) the file was last written; {@code 0} means unknown
 */
public record StoredFile(String path, FileType type, String content, long modified) {

    /** A file with an unknown modification time (0). */
    public StoredFile(final String path, final FileType type, final String content) {
        this(path, type, content, 0L);
    }

    /**
     * What a file of that type holding that content weighs, in bytes: its text, except that a recording weighs the
     * recording it names and an archive adds the recordings packed into it.
     */
    public static long bytesOf(final FileType type, final String content) {
        final long text = content.getBytes(StandardCharsets.UTF_8).length;
        if (type.recording()) {
            final RecordingFile recording = RecordingFile.read(content);
            return recording == null ? text : recording.bytes();
        }
        return type == FileType.ARK ? text + Archive.recordingBytes(content) : text;
    }

    /** The file's size in bytes, as {@link #bytesOf} weighs it. */
    public long byteSize() {
        return bytesOf(type, content);
    }

    /**
     * Returns the disk-space cost in mB-equivalents on a disk of {@code era}, rounded up to the nearest
     * block ({@link HardwareEra#bytesPerMbEq()} bytes there).
     */
    public long weight(final HardwareEra era) {
        return FsPaths.sizeMbEq(byteSize(), era);
    }
}
