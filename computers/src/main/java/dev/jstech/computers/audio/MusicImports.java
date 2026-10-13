/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.audio;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.files.FileAccess;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.os.fs.RecordingFile;
import dev.jstech.computers.os.fs.SystemLayout;
import dev.jstech.core.audio.media.IMediaUploadHandler;
import dev.jstech.core.audio.media.MediaId;
import dev.jstech.core.audio.media.MediaInfo;
import dev.jstech.core.audio.media.MediaReceipt;
import dev.jstech.core.audio.media.MediaStore;
import dev.jstech.core.audio.media.MediaUploads;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Songs a player brings from their own computer to a computer in the world, kept on its system disk as files naming
 * the recordings the server keeps.
 *
 * <p>A program asks for it by sending the song for {@link #PURPOSE}, with {@link #context} saying which computer and
 * which folder. Only a player at that computer's screen may, and a song the disk has no room for is refused before a
 * byte of it is sent. It lands in the folder asked for, or in the system's music folder, made the first time.
 */
@TextHolder
public final class MusicImports implements IMediaUploadHandler {

    /** What a program on a computer brings songs for. */
    public static final String PURPOSE = JsComputers.MODID + ":music";

    static final TextKey NOT_AT_IT = TextKey.of("jsc.music_import.not_at_it",
            "Music can only be brought to a computer from its own screen");
    static final TextKey NO_COMPUTER = TextKey.of("jsc.music_import.no_computer", "That computer is not there");
    static final TextKey NO_DISK = TextKey.of("jsc.music_import.no_disk",
            "The computer has no system disk to keep music on");
    static final TextKey NO_FOLDERS = TextKey.of("jsc.music_import.no_folders",
            "This system keeps no folders to put music in");
    public static final TextKey NOT_MUSIC = TextKey.of("jsc.music_import.not_music",
            "Only Ogg Vorbis and Wave files can be kept as music");
    static final TextKey NO_ROOM = TextKey.of("jsc.music_import.no_room", "There is no room on the disk for %s");
    static final TextKey NOT_WRITTEN = TextKey.of("jsc.music_import.not_written", "%s could not be put in %s");
    static final TextKey KEPT = TextKey.of("jsc.music_import.kept", "%s is in %s");

    /* The name a song is kept under when nothing of the name it was brought with is left. */
    private static final String FALLBACK_NAME = "Track";
    /* The context's parts, the computer, whether the playlist takes it and the folder, on a line each. */
    private static final char SEPARATOR = '\n';
    private static final String PLAYLIST = "playlist";

    private MusicImports() {
    }

    /** Takes the songs brought for {@link #PURPOSE}; called once as the mod loads. */
    public static void register() {
        MediaUploads.handle(PURPOSE, new MusicImports());
    }

    /**
     * What a program sends with a song it brings.
     *
     * @param folder   the folder to put it in, or {@code ""} for the system's music folder
     * @param playlist whether to put it at the end of Soundfoundry's playlist too
     */
    public static String context(final BlockPos host, final String folder, final boolean playlist) {
        return Long.toString(host.asLong()) + SEPARATOR + (playlist ? PLAYLIST : "") + SEPARATOR + folder;
    }

    /**
     * Why a song cannot be kept on that computer, or null when it can.
     *
     * @param name what the song's file was called, which the player is told
     */
    @Nullable
    public static Text whyNot(final ServerLevel level, final BlockPos host, final String name, final MediaId media) {
        return level.getBlockEntity(host) instanceof IOsHost computer ? whyNot(computer, name, media)
                : NO_COMPUTER.text();
    }

    /** The same, for a machine already in hand, such as one server of a rack. */
    @Nullable
    public static Text whyNot(final IOsHost computer, final String name, final MediaId media) {
        final ItemStack disk = computer.systemDisk();
        if (disk.isEmpty()) {
            return NO_DISK.text();
        }
        if (FileAccess.filesystemKindOf(computer) != FilesystemKind.HIERARCHICAL) {
            return NO_FOLDERS.text();
        }
        if (FileType.forRecording(media.format()).isEmpty()) {
            return NOT_MUSIC.text();
        }
        if (FsPaths.sizeMbEq(media.bytes(), DiskFilesystem.eraOf(disk)) > computer.systemDiskFreeWeight()) {
            return NO_ROOM.with(name);
        }
        return null;
    }

    /**
     * Puts a file naming the recording on the computer's system disk, in {@code folder}, or the system's music folder
     * when that is empty, under the name the song was brought with; a second song of the same name gets a number.
     *
     * @return whether the song is on the disk, and what the player is told
     */
    public static MediaReceipt keep(final ServerLevel level, final BlockPos host, final String folder,
                                    final String name, final MediaId media, final MediaInfo info) {
        return keep(level, host, folder, false, name, media, info);
    }

    /** The same, putting the song at the end of Soundfoundry's playlist too when {@code playlist}. */
    public static MediaReceipt keep(final ServerLevel level, final BlockPos host, final String folder,
                                    final boolean playlist, final String name, final MediaId media,
                                    final MediaInfo info) {
        return level.getBlockEntity(host) instanceof IOsHost computer
                ? keep(level, computer, folder, playlist, name, media, info)
                : MediaReceipt.refused(NO_COMPUTER.text());
    }

    /** The same, for a machine already in hand, such as one server of a rack. */
    public static MediaReceipt keep(final ServerLevel level, final IOsHost computer, final String folder,
                                    final boolean playlist, final String name, final MediaId media,
                                    final MediaInfo info) {
        final Text why = whyNot(computer, name, media);
        if (why != null) {
            return MediaReceipt.refused(why);
        }
        final ItemStack disk = computer.systemDisk();
        final FilesystemKind kind = FileAccess.filesystemKindOf(computer);
        final FileType type = FileType.forRecording(media.format()).orElseThrow();
        final String dir = folder.isEmpty() ? musicFolderOf(computer) : folder;
        if (!DiskFilesystem.mkdirs(disk, dir, kind)) {
            return MediaReceipt.refused(NOT_WRITTEN.with(name, dir));
        }
        final String content = new RecordingFile(media, info).write();
        final String path = DiskFilesystem.uniquePath(disk,
                FsPaths.join(dir, RecordingFile.stemOf(name, FALLBACK_NAME)), "." + type.extension(), content);
        // The same song brought again to the same folder lands on itself, so what it held is room it hands back.
        final DiskFilesystem.WriteResult result = DiskFilesystem.write(disk, path, type, content,
                computer.systemDiskFreeWeight() + DiskFilesystem.weightOf(disk, path), kind, level.getGameTime());
        if (result == DiskFilesystem.WriteResult.OK) {
            MediaStore.current().ifPresent(store -> store.used(media));
        }
        if (result == DiskFilesystem.WriteResult.OK && playlist
                && computer instanceof AbstractComputerBlockEntity machine) {
            machine.console().soundfoundry().add(List.of(path));
        }
        computer.setChanged();
        return switch (result) {
            case OK -> MediaReceipt.accepted(KEPT.with(FsPaths.fileName(path), dir));
            case DISK_FULL -> MediaReceipt.refused(NO_ROOM.with(name));
            case INVALID_PATH, READ_ONLY, TOO_LARGE -> MediaReceipt.refused(NOT_WRITTEN.with(name, dir));
        };
    }

    /** The folder a system keeps its songs in. */
    public static String musicFolderOf(final IOsHost computer) {
        final OsDef os = computer.installedOs();
        return SystemLayout.musicDirFor(os, os == null ? null : OsRegistry.getKernel(os.kernelId()));
    }

    @Override
    @Nullable
    public Text refuse(final ServerPlayer player, final String context, final String name, final MediaId media) {
        final Target target = Target.parse(context).orElse(null);
        if (target == null || !ComputerAccess.machine(Target::host).admits(player, target)) {
            return NOT_AT_IT.text();
        }
        return whyNot(player.serverLevel(), target.host(), name, media);
    }

    /*
     * Not asked again whether the player is still at the screen: they may close it while a long song is on its way,
     * and it was theirs to bring when they started. Whether the disk still has room is asked again, by keeping it.
     */
    @Override
    public MediaReceipt received(final ServerPlayer player, final String context, final String name,
                                 final MediaId media, final MediaInfo info) {
        return Target.parse(context)
                .map(target -> keep(player.serverLevel(), target.host(), target.folder(), target.playlist(), name,
                        media, info))
                .orElseGet(() -> MediaReceipt.refused(NO_COMPUTER.text()));
    }

    /** The computer a song is for, whether the playlist takes it, and the folder it goes in, as a program wrote them. */
    private record Target(BlockPos host, boolean playlist, String folder) {

        static Optional<Target> parse(final String context) {
            final int first = context.indexOf(SEPARATOR);
            final int second = first < 0 ? -1 : context.indexOf(SEPARATOR, first + 1);
            if (second < 0) {
                return Optional.empty();
            }
            try {
                return Optional.of(new Target(BlockPos.of(Long.parseLong(context.substring(0, first))),
                        PLAYLIST.equals(context.substring(first + 1, second)), context.substring(second + 1)));
            } catch (final NumberFormatException malformed) {
                return Optional.empty();
            }
        }
    }
}
