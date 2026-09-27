/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.audio;

import dev.jstech.computers.audio.MusicImports;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.core.client.audio.media.MediaUploader;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

/**
 * Brings songs from the player's own computer to a computer in the world: picked in the player's own system's file
 * dialog, or handed over by a program they were dropped on, and sent one after another.
 */
@TextHolder
public final class MusicImporter {

    static final TextKey PICK_TITLE = TextKey.of("jsc.music_import.pick_title", "Import from Your Computer");
    static final TextKey PICK_FILTER = TextKey.of("jsc.music_import.pick_filter", "Music (*.ogg, *.wav)");

    /* What the dialog shows: only the kinds a song is kept as. */
    private static final List<String> PATTERNS = List.of("*.ogg", "*.wav");

    private MusicImporter() {
    }

    /** Hears how bringing a set of songs goes. Every call comes on the game's own thread. */
    public interface IListener {

        /** One of the songs is on its way: which, of how many, and how much of it has gone. */
        void progress(int song, int songs, long sent, long total);

        /** One song has ended, kept or not, with what the player is told. */
        void finished(Path file, boolean ok, Text message);

        /** Every song has ended, or none was picked. */
        void done();
    }

    /**
     * Opens the player's own file dialog and brings what they pick to the computer at {@code host}.
     *
     * <p>The dialog belongs to the player's system and holds whoever opens it until it is closed, so it is opened on a
     * thread of its own and the game goes on drawing behind it.
     *
     * @param folder   the folder to put the songs in, or {@code ""} for the system's music folder
     * @param playlist whether they go at the end of Soundfoundry's playlist too
     */
    public static void pick(final BlockPos host, final String folder, final boolean playlist,
                            final IListener listener) {
        final String title = GameText.resolve(PICK_TITLE.text());
        final String filter = GameText.resolve(PICK_FILTER.text());
        final Thread picker = new Thread(() -> {
            final List<Path> picked = openDialog(title, filter);
            Minecraft.getInstance().execute(() -> bring(picked, host, folder, playlist, listener));
        }, "jsc-music-picker");
        picker.setDaemon(true);
        picker.start();
    }

    /**
     * Brings those files to the computer at {@code host}, one after another; a file that is not a song is passed over
     * with the reason.
     *
     * @param folder   the folder to put them in, or {@code ""} for the system's music folder
     * @param playlist whether they go at the end of Soundfoundry's playlist too
     */
    public static void bring(final List<Path> files, final BlockPos host, final String folder,
                             final boolean playlist, final IListener listener) {
        next(List.copyOf(files), 0, MusicImports.context(host, folder, playlist), listener);
    }

    /** Whether the file is of a kind a song is kept as, judged by its name. */
    public static boolean isMusic(final Path file) {
        final String name = file.getFileName() == null ? "" : file.getFileName().toString();
        final int dot = name.lastIndexOf('.');
        return dot > 0 && FileType.forRecording(name.substring(dot + 1).toLowerCase(Locale.ROOT)).isPresent();
    }

    private static void next(final List<Path> files, final int index, final String context,
                             final IListener listener) {
        if (index >= files.size()) {
            listener.done();
            return;
        }
        final Path file = files.get(index);
        if (!isMusic(file)) {
            listener.finished(file, false, MusicImports.NOT_MUSIC.text());
            next(files, index + 1, context, listener);
            return;
        }
        MediaUploader.upload(file, MusicImports.PURPOSE, context, new MediaUploader.IListener() {
            @Override
            public void progress(final long sent, final long total) {
                listener.progress(index + 1, files.size(), sent, total);
            }

            @Override
            public void finished(final boolean ok, final Text message) {
                listener.finished(file, ok, message);
                next(files, index + 1, context, listener);
            }
        });
    }

    /* The files the player picked, or none when they closed the dialog without picking. */
    private static List<Path> openDialog(final String title, final String filter) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final PointerBuffer patterns = stack.mallocPointer(PATTERNS.size());
            for (final String pattern : PATTERNS) {
                patterns.put(stack.UTF8(pattern));
            }
            patterns.flip();
            final String chosen = TinyFileDialogs.tinyfd_openFileDialog(title, "", patterns, filter, true);
            final List<Path> picked = new ArrayList<>();
            if (chosen != null) {
                // Several files come back as one line, their paths parted by a bar.
                for (final String one : chosen.split("\\|")) {
                    if (!one.isBlank()) {
                        picked.add(Path.of(one));
                    }
                }
            }
            return picked;
        }
    }
}
