/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.media;

import org.jetbrains.annotations.Nullable;

/**
 * A removable volume as the explorers address it: the medium in a reader ({@code media:<reader>}) or, on a Dock
 * Station, the disk in one of its trays ({@code media:<reader>.<tray>}), a path within it following a slash. One
 * place says how the key is written and read, so the server and every program agree on it.
 *
 * <p>Pure: no Minecraft types, so the keys can be tested without the game.
 *
 * @param readerPos the reader's packed position
 * @param bay       the dock's tray, or {@link #MEDIUM} for the reader's own medium
 */
public record MediaVolume(long readerPos, int bay) {

    /** The reader's own medium rather than a tray of a dock. */
    public static final int MEDIUM = -1;
    /** What every removable volume's key starts with. */
    public static final String PREFIX = "media:";

    /** The key of a reader's medium, or of a dock's tray. */
    public static String key(final long readerPos, final int bay) {
        return PREFIX + readerPos + (bay >= 0 ? "." + bay : "");
    }

    /** The volume a path is on, or null for a path on no removable volume or one that does not read. */
    @Nullable
    public static MediaVolume parse(final String path) {
        if (path == null || !path.startsWith(PREFIX)) {
            return null;
        }
        final String rest = path.substring(PREFIX.length());
        final int slash = rest.indexOf('/');
        final String token = slash < 0 ? rest : rest.substring(0, slash);
        final int dot = token.indexOf('.');
        try {
            final long reader = Long.parseLong(dot < 0 ? token : token.substring(0, dot));
            final int bay = dot < 0 ? MEDIUM : Integer.parseInt(token.substring(dot + 1));
            return bay < MEDIUM ? null : new MediaVolume(reader, bay);
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    /** The path within its volume, without the volume's key and its slash. */
    public static String subPath(final String path) {
        final String rest = path.substring(PREFIX.length());
        final int slash = rest.indexOf('/');
        return slash < 0 ? "" : rest.substring(slash + 1);
    }

    /** This volume's key. */
    public String key() {
        return key(readerPos, bay);
    }

    /** Whether it is a disk in a dock's tray rather than a reader's medium. */
    public boolean docked() {
        return bay >= 0;
    }
}
