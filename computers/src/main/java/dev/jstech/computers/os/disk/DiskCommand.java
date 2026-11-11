/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.disk;

import java.util.Arrays;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;

/**
 * What a disk tool asks of a machine's disks (computers/docs/design/storage-devices.md), written as the path a screen
 * asks a machine for a file by: {@code disks:look} for the report alone, and a change before it,
 * {@code disks:delete/0/2} deleting partition 2 of disk 0. The machine answers every one with its
 * {@link DiskReport}. Pure.
 *
 * @param verb   what is asked
 * @param disk   the disk it is about, by its place among the machine's disks, or -1
 * @param number the partition it is about, or where a new one starts, or -1
 * @param amount a size in megabytes, or 0
 * @param word   a file system, a scheme or a label, or empty
 */
public record DiskCommand(Verb verb, int disk, long number, long amount, String word) {

    /** Where a disk tool's paths start. */
    public static final String SCHEME = "disks:";

    /** The word a new table is asked for with when the disk is to have none. */
    public static final String NO_TABLE = "none";

    public DiskCommand {
        word = word == null ? "" : word;
    }

    /** Whether a path is one of these. */
    public static boolean names(final String path) {
        return path != null && path.startsWith(SCHEME);
    }

    /** Just the report. */
    public static String look() {
        return SCHEME + Verb.LOOK.word();
    }

    /** Deletes partition {@code number} of the disk. */
    public static String delete(final int disk, final int number) {
        return path(Verb.DELETE, disk, number, 0L, "");
    }

    /** Makes a partition of {@code sizeMb} at {@code startMb}, formatted with {@code fs} (empty for a raw one). */
    public static String create(final int disk, final long startMb, final long sizeMb, final String fs) {
        return path(Verb.CREATE, disk, startMb, sizeMb, fs);
    }

    /** Formats partition {@code number} with {@code fs}. */
    public static String format(final int disk, final int number, final String fs) {
        return path(Verb.FORMAT, disk, number, 0L, fs);
    }

    /** Resizes partition {@code number} to {@code sizeMb}. */
    public static String resize(final int disk, final int number, final long sizeMb) {
        return path(Verb.RESIZE, disk, number, sizeMb, "");
    }

    /** Makes partition {@code number} the one the Master Boot Record starts. */
    public static String active(final int disk, final int number) {
        return path(Verb.ACTIVE, disk, number, 0L, "");
    }

    /** Turns an empty disk's table into {@code scheme}. */
    public static String convert(final int disk, final PartitionScheme scheme) {
        return path(Verb.CONVERT, disk, -1L, 0L, scheme.serializedName());
    }

    /** Writes a new, empty table of {@code scheme} on the disk, erasing all it holds. */
    public static String table(final int disk, final PartitionScheme scheme) {
        return path(Verb.TABLE, disk, -1L, 0L, scheme.serializedName());
    }

    /** Erases the disk and leaves it with no table at all, one partition over it all as a disk is bought. */
    public static String erase(final int disk) {
        return path(Verb.TABLE, disk, -1L, 0L, NO_TABLE);
    }

    /** Names partition {@code number}'s volume {@code label}. */
    public static String label(final int disk, final int number, final String label) {
        // A label is typed by a player: the separator never reaches the path.
        return path(Verb.LABEL, disk, number, 0L, label.replace('/', ' '));
    }

    /** Takes a snapshot of partition {@code number} named {@code name}, which may hold slashes. */
    public static String snapshot(final int disk, final int number, final String name) {
        return path(Verb.SNAPSHOT, disk, number, 0L, name);
    }

    /** Rolls partition {@code number} back to snapshot {@code name}, letting later ones go when {@code letNewerGo}. */
    public static String rollBack(final int disk, final int number, final String name, final boolean letNewerGo) {
        return path(Verb.ROLLBACK, disk, number, letNewerGo ? 1L : 0L, name);
    }

    /** Lets snapshot {@code name} of partition {@code number} go. */
    public static String destroy(final int disk, final int number, final String name) {
        return path(Verb.DESTROY, disk, number, 0L, name);
    }

    /** Sets the snapshot numbered {@code snapshot} as the one partition {@code number} starts from next. */
    public static String startFrom(final int disk, final int number, final int snapshot) {
        return path(Verb.DEFAULT, disk, number, snapshot, "");
    }

    /** Scrubs the ZFS pool on partition {@code number}. */
    public static String scrub(final int disk, final int number) {
        return path(Verb.SCRUB, disk, number, 0L, "");
    }

    /** What a path asks, or null when it is not one of these or cannot be read. */
    @Nullable
    public static DiskCommand parse(final String path) {
        if (!names(path)) {
            return null;
        }
        final String[] parts = path.substring(SCHEME.length()).split("/", -1);
        final Verb verb = Verb.byWord(parts[0]);
        if (verb == null) {
            return null;
        }
        try {
            final int disk = parts.length > 1 ? Integer.parseInt(parts[1]) : -1;
            final long number = parts.length > 2 ? Long.parseLong(parts[2]) : -1L;
            final long amount = parts.length > 3 ? Long.parseLong(parts[3]) : 0L;
            // The word is the rest of the path: a snapshot's name keeps its slashes.
            final String word = parts.length > 4 ? String.join("/", Arrays.asList(parts).subList(4, parts.length))
                    : "";
            return new DiskCommand(verb, disk, number, amount, word);
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    private static String path(final Verb verb, final int disk, final long number, final long amount,
                               final String word) {
        return SCHEME + verb.word() + "/" + disk + "/" + number + "/" + amount + "/" + word;
    }

    /** What a disk tool asks. */
    public enum Verb {
        LOOK, DELETE, CREATE, FORMAT, RESIZE, ACTIVE, CONVERT, TABLE, LABEL, SNAPSHOT, ROLLBACK, DESTROY, DEFAULT,
        SCRUB;

        /** Its word in a path. */
        public String word() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        @Nullable
        static Verb byWord(final String word) {
            for (final Verb verb : values()) {
                if (verb.word().equals(word)) {
                    return verb;
                }
            }
            return null;
        }
    }
}
