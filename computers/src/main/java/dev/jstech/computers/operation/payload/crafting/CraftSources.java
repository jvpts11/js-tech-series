/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.crafting;

import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.operation.payload.CraftManagerStatePayload;
import dev.jstech.computers.operation.payload.CraftManagerStatePayload.WireSource;
import dev.jstech.computers.operation.payload.DiskFilesPayload;
import dev.jstech.computers.operation.payload.files.FilePayloads;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.os.media.FormattedMediaItem;
import dev.jstech.computers.os.media.IMediaDrive;
import dev.jstech.computers.os.media.MachineMedia;
import dev.jstech.core.text.Text;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static dev.jstech.computers.operation.payload.files.FileAccess.commitMedia;
import static dev.jstech.computers.operation.payload.files.FileAccess.filesystemKindOf;
import static dev.jstech.computers.operation.payload.files.FileAccess.mediaStackFor;

/**
 * The disks the Crafting Manager reads recipe files from and writes them to: the computer's own disk, when it has a
 * filesystem, and the writable medium in each linked drive; and what one folder of one of them holds.
 */
public final class CraftSources {

    private CraftSources() {
    }

    /**
     * One disk resolved.
     *
     * @param key  {@link CraftManagerStatePayload#SYSTEM_DISK}, or the medium's {@code media:} key
     * @param disk the stack that holds its files
     * @param kind the filesystem on it
     */
    public record Volume(String key, ItemStack disk, FilesystemKind kind) {

        /** Whether this is the computer's own disk. */
        public boolean system() {
            return key.equals(CraftManagerStatePayload.SYSTEM_DISK);
        }

        /** How much more it takes, in mB-equivalents. */
        public long free(final CraftingComputerBlockEntity cc) {
            return system() ? cc.systemDiskFreeWeight() : FormattedMediaItem.freeWeight(disk);
        }

        /** Saves it after its files changed: the computer keeps its own disk, a drive keeps its medium. */
        public void commit(final ServerLevel level, final CraftingComputerBlockEntity cc) {
            if (system()) {
                cc.setChanged();
            } else {
                commitMedia(level, cc, key);
            }
        }

        /** {@code folder} when it is a folder on this disk, else the disk's top. */
        public String folderOf(final String folder) {
            return kind == FilesystemKind.HIERARCHICAL && DiskFilesystem.isFolder(disk, folder) ? folder : "";
        }

        /** The names of the folders in {@code folder}, by name. */
        public List<String> folders(final String folder) {
            final List<String> names = new ArrayList<>();
            for (final String path : DiskFilesystem.listDirs(disk, folder, kind)) {
                names.add(nameOf(path));
            }
            return capped(names);
        }

        /** The names of the {@code .craft} files in {@code folder}, by name. */
        public List<String> craftFiles(final String folder) {
            final List<String> names = new ArrayList<>();
            for (final DiskFilesystem.FileEntry e : DiskFilesystem.list(disk, folder, kind)) {
                if (e.type() == FileType.CRAFT) {
                    names.add(nameOf(e.path()));
                }
            }
            return capped(names);
        }
    }

    /**
     * One folder of one disk, as the Manager lists it.
     *
     * @param sources every disk the computer reads
     * @param key     the disk listed, or {@code ""} when there is none
     * @param label   what that disk is called
     * @param folder  the folder listed, {@code ""} for its top
     * @param folders the folders in it
     * @param files   the {@code .craft} files in it
     */
    public record Listing(List<WireSource> sources, String key, Text label, String folder, List<String> folders,
                          List<String> files) {
    }

    /** The disk {@code key} names, or null when the computer reads no such disk now. */
    @Nullable
    public static Volume resolve(final ServerLevel level, final CraftingComputerBlockEntity cc, final String key) {
        if (key.equals(CraftManagerStatePayload.SYSTEM_DISK)) {
            final FilesystemKind kind = filesystemKindOf(cc);
            return cc.systemDisk().isEmpty() || kind == FilesystemKind.NONE ? null
                    : new Volume(key, cc.systemDisk(), kind);
        }
        if (!key.startsWith("media:")) {
            return null;
        }
        final ItemStack medium = mediaStackFor(level, cc, key);
        return usable(medium) ? new Volume(key, medium, FilesystemKind.HIERARCHICAL) : null;
    }

    /**
     * Lists {@code folder} of the disk {@code key} names; the top of the disk when that folder is not on it. A key
     * that names no disk the computer reads, or none, lists the one the computer shows first: the first linked drive
     * whose writable medium holds {@code .craft} files at its top, or else the first holding a writable medium, so a
     * download still has a target.
     */
    public static Listing listing(final ServerLevel level, final CraftingComputerBlockEntity cc, final String key,
                                  final String folder) {
        final List<WireSource> sources = sources(level, cc);
        final Volume asked = key.isEmpty() ? null : resolve(level, cc, key);
        final Volume volume = asked != null ? asked : first(level, cc);
        if (volume == null) {
            return new Listing(sources, "", Text.EMPTY, "", List.of(), List.of());
        }
        final String in = asked != null ? volume.folderOf(folder) : "";
        Text label = Text.EMPTY;
        for (final WireSource source : sources) {
            if (source.key().equals(volume.key())) {
                label = source.label();
            }
        }
        return new Listing(sources, volume.key(), label, in, volume.folders(in), volume.craftFiles(in));
    }

    /**
     * Every disk the computer reads, in the order its drive tree lists them, so drive letters count the same in every
     * window: its own disk, then each medium in a linked drive, a medium recipe files cannot go on among them.
     */
    public static List<WireSource> sources(final ServerLevel level, final CraftingComputerBlockEntity cc) {
        final List<WireSource> out = new ArrayList<>();
        for (final DiskFilesPayload.WireVolume volume : FilePayloads.driveVolumes(cc, level)) {
            final String key = volume.key().isEmpty() ? CraftManagerStatePayload.SYSTEM_DISK : volume.key();
            out.add(new WireSource(key, volume.label(), resolve(level, cc, key) != null));
        }
        return out;
    }

    /* The disk the computer shows first: a medium with recipes at its top, or else the first writable one. */
    @Nullable
    private static Volume first(final ServerLevel level, final CraftingComputerBlockEntity cc) {
        Volume writable = null;
        for (final IMediaDrive drive : MachineMedia.of(cc, level).drives()) {
            final ItemStack medium = drive.medium();
            if (!usable(medium)) {
                continue;
            }
            final Volume volume = new Volume(drive.volume().key(), medium, FilesystemKind.HIERARCHICAL);
            if (!volume.craftFiles("").isEmpty()) {
                return volume;
            }
            if (writable == null) {
                writable = volume;
            }
        }
        return writable;
    }

    private static boolean usable(final ItemStack medium) {
        return !medium.isEmpty() && medium.getItem() instanceof FormattedMediaItem fmt && fmt.writable();
    }

    private static String nameOf(final String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    /*
     * By name, and no more than a field of the state carries. A name the wire cannot carry would disconnect the player
     * on every listing; the filesystem's own name limit is the cap, so this only guards against a name it should never
     * hold.
     */
    private static List<String> capped(final List<String> names) {
        names.removeIf(name -> name.length() > FsPaths.MAX_NAME_LENGTH);
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return List.copyOf(names.subList(0, Math.min(CraftManagerStatePayload.MAX_MEDIA_FILES, names.size())));
    }
}
