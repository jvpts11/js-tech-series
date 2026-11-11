/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.fs;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * What a backup file on a server's drive holds: the backup the world keeps, and what it weighs. A backup of a network
 * is its items and its computers' files, far too much to travel with a drive, so the world keeps it once and the file
 * names it, weighing on the drive what the backup weighs, as a song's file weighs its song
 * (computers/docs/design/programs.md).
 *
 * <p>This class is pure and carries no Minecraft dependency.
 *
 * @param backup the backup the world keeps
 * @param bytes  what it weighs
 */
public record BackupFile(UUID backup, long bytes) {

    /** What every backup file starts with, so a file that is not one is recognised before it is read. */
    public static final String MAGIC = "JSBAK1";
    private static final char NEWLINE = '\n';

    /** The file's content: the magic line, then the backup and what it weighs. */
    public String write() {
        return MAGIC + NEWLINE + backup + " " + bytes + NEWLINE;
    }

    /** The backup a file's content names, or null when the content is not a backup file. */
    @Nullable
    public static BackupFile read(@Nullable final String content) {
        if (content == null || !content.startsWith(MAGIC + NEWLINE)) {
            return null;
        }
        final String[] words = content.substring(MAGIC.length() + 1).strip().split(" ");
        if (words.length != 2) {
            return null;
        }
        try {
            return new BackupFile(UUID.fromString(words[0]), Math.max(0L, Long.parseLong(words[1])));
        } catch (final IllegalArgumentException notABackup) {
            return null;
        }
    }
}
