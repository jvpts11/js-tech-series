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
 * A removable volume as the explorers address it: the medium in a reader ({@code media:<reader>}), on a Dock Station
 * the disk in one of its trays ({@code media:<reader>.<tray>}), or on a computer's own front the medium in a drive in
 * one of its bays or the stick in one of its ports ({@code media:<computer>.f<slot>}), a path within it following a
 * slash. One place says how the key is written and read, so the server and every program agree on it.
 *
 * <p>Pure: no Minecraft types, so the keys can be tested without the game.
 *
 * @param readerPos the reader's packed position, or the computer's for a slot on its front
 * @param bay       the dock's tray, {@link #MEDIUM} for the reader's own medium, or below that a slot on the front
 */
public record MediaVolume(long readerPos, int bay) {

    /** The reader's own medium rather than a tray of a dock. */
    public static final int MEDIUM = -1;
    /** What every removable volume's key starts with. */
    public static final String PREFIX = "media:";
    /* What marks a slot on a computer's own front in a key. */
    private static final String FRONT = "f";
    /* The bay numbers below MEDIUM count a computer's front slots down from here: the first slot, then the next. */
    private static final int FIRST_FRONT = -2;

    /** The medium in slot {@code slot} of the front of the computer at {@code computerPos}: a bay's, then a port's. */
    public static MediaVolume front(final long computerPos, final int slot) {
        return new MediaVolume(computerPos, FIRST_FRONT - slot);
    }

    /** The key of a reader's medium, of a dock's tray, or of a slot on a computer's front. */
    public static String key(final long readerPos, final int bay) {
        if (bay >= 0) {
            return PREFIX + readerPos + "." + bay;
        }
        return bay == MEDIUM ? PREFIX + readerPos : PREFIX + readerPos + "." + FRONT + (FIRST_FRONT - bay);
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
            if (dot < 0) {
                return new MediaVolume(reader, MEDIUM);
            }
            final String place = token.substring(dot + 1);
            if (place.startsWith(FRONT)) {
                final int slot = Integer.parseInt(place.substring(FRONT.length()));
                return slot < 0 ? null : front(reader, slot);
            }
            final int bay = Integer.parseInt(place);
            return bay < MEDIUM ? null : new MediaVolume(reader, bay);
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    /** The path within its volume, without the volume's key and its slash; empty for a path on no volume. */
    public static String subPath(final String path) {
        if (!path.startsWith(PREFIX)) {
            return "";
        }
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

    /** Whether it is a slot on a computer's own front: a drive in one of its bays, or one of its ports. */
    public boolean onFront() {
        return bay <= FIRST_FRONT;
    }

    /** The slot on the computer's front, for a volume {@link #onFront() on its front}. */
    public int frontSlot() {
        return FIRST_FRONT - bay;
    }
}
