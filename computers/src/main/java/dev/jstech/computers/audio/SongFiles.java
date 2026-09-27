/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.audio;

import dev.jstech.computers.operation.payload.files.FileAccess;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.os.fs.RecordingFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The songs a computer can reach by path, the way its explorer names them: on its system disk, or on a medium in a
 * drive linked to it ({@code media:<drive>/...}).
 */
public final class SongFiles {

    private static final String MEDIA = "media:";

    private SongFiles() {
    }

    /** The song at that path, or null when there is no file there or it is not a song. */
    @Nullable
    public static RecordingFile read(final ServerLevel level, final IOsHost computer, final String path) {
        return content(level, computer, path).map(RecordingFile::read).orElse(null);
    }

    /** The text of the file at that path, or empty when it cannot be reached. */
    public static Optional<String> content(final ServerLevel level, final IOsHost computer, final String path) {
        final ItemStack volume = volumeOf(level, computer, path);
        return volume.isEmpty() ? Optional.empty() : DiskFilesystem.read(volume, inside(path));
    }

    /** Every song in that folder and the folders under it, by path, folders first by name and then their songs. */
    public static List<String> under(final ServerLevel level, final IOsHost computer, final String folder) {
        final List<String> found = new ArrayList<>();
        final ItemStack volume = volumeOf(level, computer, folder);
        if (!volume.isEmpty()) {
            collect(volume, inside(folder), driveOf(folder), found, 0);
        }
        return found;
    }

    /** The name a song goes by in a message: its file's, without the folders. */
    public static String nameOf(final String path) {
        return FsPaths.fileName(path);
    }

    private static void collect(final ItemStack volume, final String dir, final String prefix,
                                final List<String> found, final int depth) {
        // Deep enough for any real music folder, and a bound on a disk built to send this round for ever.
        if (depth > 8) {
            return;
        }
        final List<DiskFilesystem.FileEntry> files = new ArrayList<>(
                DiskFilesystem.list(volume, dir, FilesystemKind.HIERARCHICAL));
        files.sort((a, b) -> a.path().compareToIgnoreCase(b.path()));
        for (final DiskFilesystem.FileEntry file : files) {
            if (file.type().recording()) {
                found.add(prefix + file.path());
            }
        }
        final List<String> dirs = new ArrayList<>(DiskFilesystem.listDirs(volume, dir, FilesystemKind.HIERARCHICAL));
        dirs.sort(String::compareToIgnoreCase);
        for (final String sub : dirs) {
            if (sub.length() > dir.length()) {
                collect(volume, sub, prefix, found, depth + 1);
            }
        }
    }

    private static ItemStack volumeOf(final ServerLevel level, final IOsHost computer, final String path) {
        return path.startsWith(MEDIA) ? FileAccess.mediaStackFor(level, computer, path) : computer.systemDisk();
    }

    /* What a path on a medium starts with, the drive and its slash, which every song found there keeps. */
    private static String driveOf(final String path) {
        if (!path.startsWith(MEDIA)) {
            return "";
        }
        final int slash = path.indexOf('/');
        return slash < 0 ? path + "/" : path.substring(0, slash + 1);
    }

    private static String inside(final String path) {
        return path.startsWith(MEDIA) ? FileAccess.mediaSubPath(path) : path;
    }
}
