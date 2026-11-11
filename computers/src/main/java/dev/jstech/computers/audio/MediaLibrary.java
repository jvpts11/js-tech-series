/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.audio;

import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.os.fs.PictureScaling;
import dev.jstech.computers.os.fs.PixImage;
import dev.jstech.computers.os.fs.RecordingFile;
import dev.jstech.computers.os.fs.SystemLayout;
import dev.jstech.computers.program.Programs;
import dev.jstech.core.audio.media.MediaStore;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * What the Media Player plays from a computer: the songs in its music folder, and its image sequences, each a folder of
 * pictures from Paint or a camera shown in the order of their names, the mod's video (computers/docs/design/
 * programs.md). The pictures travel to the player one at a time, brought down to the size it shows them at.
 */
public final class MediaLibrary {

    /** The most songs and sequences a library lists, and the most pictures a sequence plays. */
    public static final int MOST_SONGS = 64;
    public static final int MOST_SEQUENCES = 16;
    public static final int MOST_FRAMES = 240;
    /** The longest side a picture travels at: what the largest player window shows it at. */
    public static final int FRAME_SIDE = 192;
    /* A camera's picture larger than this is no picture anybody took, and is not read. */
    private static final long MOST_SOURCE_PIXELS = 4096L * 4096L;
    /* The most bytes of a camera's picture read, past which it is not one either. */
    private static final int MOST_SOURCE_BYTES = 64 * 1024 * 1024;

    private MediaLibrary() {
    }

    /**
     * A song of the library.
     *
     * @param path   where it is
     * @param title  what it is called, its tag's title or its file's name
     * @param millis how long it plays
     */
    public record Song(String path, String title, long millis) {
    }

    /**
     * An image sequence: a folder and its pictures, in the order of their names.
     *
     * @param folder the folder
     * @param frames its pictures, by path
     */
    public record Sequence(String folder, List<String> frames) {

        /** No sequence. */
        public static final Sequence NONE = new Sequence("", List.of());

        public Sequence {
            frames = List.copyOf(frames);
        }
    }

    /** Whether {@code computer} runs a system that comes with the Media Player: any desktop system. */
    public static boolean playerOn(final IOsHost computer) {
        final OsDef os = computer.installedOs();
        final ProgramSpec player = OsRegistry.getProgram(Programs.MEDIA_PLAYER);
        return os != null && player != null && player.platforms().contains(os.platform());
    }

    /** Whether a file of that kind is a picture a sequence plays: one from Paint, or one a camera took. */
    public static boolean picture(final FileType type) {
        return type == FileType.PIX || type == FileType.PNG;
    }

    /** The songs in the computer's music folder and the folders under it, in the order the folder lists them. */
    public static List<Song> songs(final ServerLevel level, final IOsHost computer) {
        final OsDef os = computer.installedOs();
        if (os == null) {
            return List.of();
        }
        final String folder = SystemLayout.musicDirFor(os, OsRegistry.getKernel(os.kernelId()));
        final List<Song> out = new ArrayList<>();
        for (final String path : SongFiles.under(level, computer, folder)) {
            if (out.size() == MOST_SONGS) {
                break;
            }
            final RecordingFile song = SongFiles.read(level, computer, path);
            if (song != null) {
                final String title = song.info().tags().title();
                out.add(new Song(path, title.isBlank() ? FsPaths.fileName(path) : title, song.info().millis()));
            }
        }
        return out;
    }

    /**
     * The computer's image sequences: its pictures folder when it holds pictures itself, and each folder in it that
     * does, by name.
     */
    public static List<Sequence> sequences(final ServerLevel level, final IOsHost computer) {
        final OsDef os = computer.installedOs();
        if (os == null) {
            return List.of();
        }
        final String pictures = SystemLayout.picturesDirFor(os, OsRegistry.getKernel(os.kernelId()));
        final List<Sequence> out = new ArrayList<>();
        final Sequence own = sequence(level, computer, pictures);
        if (!own.frames().isEmpty()) {
            out.add(own);
        }
        final ItemStack disk = computer.systemDisk();
        if (disk.isEmpty()) {
            return out;
        }
        final List<String> folders = new ArrayList<>(DiskFilesystem.listDirs(disk, pictures,
                FilesystemKind.HIERARCHICAL));
        folders.sort(String::compareToIgnoreCase);
        for (final String folder : folders) {
            if (out.size() == MOST_SEQUENCES) {
                break;
            }
            final Sequence one = folder.length() > pictures.length() ? sequence(level, computer, folder)
                    : Sequence.NONE;
            if (!one.frames().isEmpty()) {
                out.add(one);
            }
        }
        return out;
    }

    /** The pictures right in {@code folder}, in the order of their names, as a sequence plays them. */
    public static Sequence sequence(final ServerLevel level, final IOsHost computer, final String folder) {
        final ItemStack volume = SongFiles.volumeOf(level, computer, folder);
        if (volume.isEmpty()) {
            return new Sequence(folder, List.of());
        }
        final String prefix = SongFiles.driveOf(folder);
        final List<String> frames = new ArrayList<>();
        for (final DiskFilesystem.FileEntry file : DiskFilesystem.list(volume, SongFiles.inside(folder),
                FilesystemKind.HIERARCHICAL)) {
            if (picture(file.type())) {
                frames.add(prefix + file.path());
            }
        }
        frames.sort((a, b) -> FsPaths.fileName(a).compareToIgnoreCase(FsPaths.fileName(b)));
        return new Sequence(folder, frames.size() > MOST_FRAMES ? frames.subList(0, MOST_FRAMES) : frames);
    }

    /**
     * The picture at {@code path} as a PNG no larger than {@link #FRAME_SIDE} on its longer side, or null when it is
     * no picture this reads.
     */
    @Nullable
    public static byte[] frame(final ServerLevel level, final IOsHost computer, final String path) {
        final String content = SongFiles.content(level, computer, path).orElse(null);
        if (content == null) {
            return null;
        }
        try {
            final BufferedImage image = switch (FileType.ofPath(path)) {
                case PIX -> pixels(PixImage.decode(content));
                case PNG -> camera(RecordingFile.read(content));
                default -> null;
            };
            return image == null ? null : PictureScaling.png(fitted(image));
        } catch (final IOException unreadable) {
            return null;
        }
    }

    @Nullable
    private static BufferedImage pixels(@Nullable final PixImage picture) {
        if (picture == null) {
            return null;
        }
        final BufferedImage image = new BufferedImage(picture.width(), picture.height(),
                BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < picture.height(); y++) {
            for (int x = 0; x < picture.width(); x++) {
                final int index = picture.get(x, y);
                image.setRGB(x, y, PixImage.isTransparent(index) ? 0 : PixImage.colourOf(index));
            }
        }
        return image;
    }

    @Nullable
    private static BufferedImage camera(@Nullable final RecordingFile file) throws IOException {
        final MediaStore store = MediaStore.current().orElse(null);
        if (file == null || store == null || !store.has(file.media())) {
            return null;
        }
        try (InputStream in = store.open(file.media())) {
            final byte[] bytes = in.readNBytes(MOST_SOURCE_BYTES);
            return PictureScaling.decode(bytes, MOST_SOURCE_PIXELS);
        }
    }

    /* The picture brought down to the travelling size, its shape kept; a small one, as Paint's are, left as it is. */
    private static BufferedImage fitted(final BufferedImage image) {
        final int longer = Math.max(image.getWidth(), image.getHeight());
        if (longer <= FRAME_SIDE) {
            return image;
        }
        final int w = Math.max(1, image.getWidth() * FRAME_SIDE / longer);
        final int h = Math.max(1, image.getHeight() * FRAME_SIDE / longer);
        return PictureScaling.scale(image, 0, 0, image.getWidth(), image.getHeight(), w, h);
    }
}
