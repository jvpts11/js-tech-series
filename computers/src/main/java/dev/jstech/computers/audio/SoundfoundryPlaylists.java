/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.audio;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.operation.payload.files.FileAccess;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.program.SoundfoundryState;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * The playlists a Standard Soundfoundry keeps: {@code .m3u} files in a {@code Playlists} folder of the machine's music
 * folder, one song to a line after a line naming it, the way the players of the time wrote them. A line can name a
 * song streamed from a Soundfoundry Server as well as a file, so a playlist mixes the two. The songs the player likes
 * are a playlist like any other, called {@value #LIKED}, which is always listed first even before it holds a song.
 */
public final class SoundfoundryPlaylists {

    /** The folder of the music folder the playlists are kept in. */
    public static final String FOLDER = "Playlists";
    /** The playlist of the songs the player likes. */
    public static final String LIKED = "Liked Songs";
    /** The longest a playlist's name is. */
    public static final int MAX_NAME = 32;
    /** A playlist's first line, which is how the players of the time recognised one. */
    public static final String HEADER = "#EXTM3U";
    /** What starts the line naming a song, before its length and what it is listed as. */
    public static final String ENTRY = "#EXTINF:";
    private static final String EXTENSION = ".m3u";

    private SoundfoundryPlaylists() {
    }

    /** One song of a playlist as its file writes it: what it is, how long in seconds and how it is listed. */
    public record Line(String ref, long seconds, String shown) {
    }

    /** The folder the machine keeps its playlists in. */
    public static String folderOf(final AbstractComputerBlockEntity computer) {
        return FsPaths.join(MusicImports.musicFolderOf(computer), FOLDER);
    }

    /** The playlists the machine keeps, the liked songs first and the others by name. */
    public static List<String> names(final AbstractComputerBlockEntity computer) {
        final List<String> names = new ArrayList<>();
        names.add(LIKED);
        final ItemStack disk = computer.systemDisk();
        if (disk.isEmpty()) {
            return names;
        }
        final List<String> others = new ArrayList<>();
        for (final DiskFilesystem.FileEntry file : DiskFilesystem.list(disk, folderOf(computer),
                FilesystemKind.HIERARCHICAL)) {
            final String name = FsPaths.fileName(file.path());
            if (name.toLowerCase(Locale.ROOT).endsWith(EXTENSION)) {
                final String stem = name.substring(0, name.length() - EXTENSION.length());
                if (!stem.equalsIgnoreCase(LIKED)) {
                    others.add(stem);
                }
            }
        }
        others.sort(String::compareToIgnoreCase);
        names.addAll(others);
        return names;
    }

    /** The songs of that playlist, each as the list names it; none when it has no file. */
    public static List<String> read(final ServerLevel level, final AbstractComputerBlockEntity computer,
                                    final String name) {
        return SongFiles.content(level, computer, pathOf(computer, name)).map(SoundfoundryPlaylists::refsOf)
                .orElseGet(ArrayList::new);
    }

    /**
     * Writes that playlist with those songs, making its folder when it has none.
     *
     * @return whether it is written
     */
    public static boolean write(final ServerLevel level, final AbstractComputerBlockEntity computer,
                                final String name, final List<String> refs) {
        final ItemStack disk = computer.systemDisk();
        final FilesystemKind kind = FileAccess.filesystemKindOf(computer);
        if (disk.isEmpty() || !DiskFilesystem.mkdirs(disk, folderOf(computer), kind)) {
            return false;
        }
        final List<Line> lines = new ArrayList<>(refs.size());
        for (final String ref : refs) {
            final SongSources.Described song = SongSources.describe(level, computer, ref);
            lines.add(song == null ? new Line(ref, 0L, SongFiles.nameOf(ref))
                    : new Line(ref, song.info().millis() / 1000L, shownAs(song.title(), song.artist())));
        }
        final String path = pathOf(computer, name);
        final boolean written = DiskFilesystem.write(disk, path, FileType.of("m3u"), contentOf(lines),
                computer.systemDiskFreeWeight() + DiskFilesystem.weightOf(disk, path), kind, level.getGameTime())
                == DiskFilesystem.WriteResult.OK;
        if (written) {
            computer.setChanged();
        }
        return written;
    }

    /** Throws a playlist away; the liked songs are only emptied, since they are always there. */
    public static boolean delete(final ServerLevel level, final AbstractComputerBlockEntity computer,
                                 final String name) {
        if (name.equalsIgnoreCase(LIKED)) {
            return write(level, computer, LIKED, List.of());
        }
        final boolean gone = DiskFilesystem.delete(computer.systemDisk(), pathOf(computer, name));
        if (gone) {
            computer.setChanged();
        }
        return gone;
    }

    /** Whether a name can be a playlist's: a file's name, short enough, and not the liked songs'. */
    public static boolean validName(final String name) {
        return !name.isBlank() && name.length() <= MAX_NAME && FsPaths.isValidName(name + EXTENSION)
                && !name.equalsIgnoreCase(LIKED);
    }

    /** A playlist's text, from its songs. */
    public static String contentOf(final List<Line> lines) {
        final StringBuilder out = new StringBuilder(HEADER).append('\n');
        for (final Line line : lines) {
            out.append(ENTRY).append(line.seconds()).append(',').append(line.shown()).append('\n')
                    .append(line.ref()).append('\n');
        }
        return out.toString();
    }

    /** The songs a playlist's text names, as it names them; nothing for a text that is no playlist. */
    public static List<String> refsOf(final String content) {
        final List<String> refs = new ArrayList<>();
        if (!content.startsWith(HEADER)) {
            return refs;
        }
        for (final String line : content.split("\n")) {
            final String entry = line.strip();
            if (!entry.isEmpty() && !entry.startsWith("#") && refs.size() < SoundfoundryState.MAX_SONGS) {
                refs.add(entry);
            }
        }
        return refs;
    }

    /** How a song is listed in a playlist's file: its artist and its title, or its title alone. */
    public static String shownAs(final String title, final String artist) {
        // A line break inside a tag would start a second line in the file, which reads back as a song.
        final String text = artist.isEmpty() ? title : artist + " - " + title;
        return text.replace('\r', ' ').replace('\n', ' ');
    }

    private static String pathOf(final AbstractComputerBlockEntity computer, final String name) {
        return FsPaths.join(folderOf(computer), name + EXTENSION);
    }
}
