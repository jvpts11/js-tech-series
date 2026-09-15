/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.os.fs.InstallerLayout;
import dev.jstech.computers.os.fs.ProgramFilesProjection;
import dev.jstech.computers.os.media.FormattedMediaItem;
import dev.jstech.computers.os.media.InstallerProjection;
import dev.jstech.computers.program.cli.DosPath;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.program.cli.ICliFiles;
import dev.jstech.computers.program.cli.NetPath;
import dev.jstech.computers.storage.StorageKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * A machine's drives, as the prompt and the programs on it reach them.
 *
 * <p>Both go through here, so a path means the same thing to a program as it does at the prompt, and a file written by
 * one is the file the other opens. A path is read against the folder the caller stands in, which the shell keeps for
 * its window, and a path on another machine of the network is followed on that machine.
 */
public final class FileService {

    private final BlockEntity machine;
    private final ServerLevel level;
    /** Where a path on another machine of the network leads. */
    private final NetworkPathResolver network;
    /** The folder the caller stands in, asked for at the moment a path is read. */
    private final Supplier<DosPath.Location> where;
    /** The shell's own door, for the writing side, which is still the shell's to answer. */
    private final ICliFiles door;

    public FileService(final BlockEntity machine, final ServerLevel level, final NetworkPathResolver network,
                       final Supplier<DosPath.Location> where, final ICliFiles door) {
        this.machine = machine;
        this.level = level;
        this.network = network;
        this.where = where;
        this.door = door;
    }

    /** A path read against the folder the caller stands in: its drive letter, that drive, and the path on it. */
    private record Resolved(char drive, DriveTable.Drive drive0, String path) {
    }

    private Resolved resolve(final String input) {
        final DosPath.Location loc = DosPath.resolve(this.where.get(), input == null ? "" : input);
        return new Resolved(loc.drive(), DriveTable.of(this.machine, this.level).find(loc.drive()), loc.storagePath());
    }

    /** The machine as the thing whose system and programs the system disk shows, or null when it is neither. */
    @Nullable
    private IOsHost osHost() {
        return this.machine instanceof IOsHost host ? host : null;
    }

    /** What is in the folder at that path, on a drive of this machine or on a share of another. */
    public ICliComputer.FsResult listDisk(final String dir) {
        final NetPath net = NetPath.parse(dir);
        if (net != null) {
            return this.network.list(net);
        }
        final Resolved r = this.resolve(dir == null ? "" : dir);
        if (r.drive0() == null) {
            return DriveTable.missing(r.drive());
        }
        if (r.drive0().disk().isEmpty()) {
            return DriveTable.notReady(r.drive());
        }
        final DriveTable.Drive drive = r.drive0();
        final String target = r.path();
        final List<ICliComputer.FsEntry> entries = new ArrayList<>();
        // Subdirectories first, then files, matching DOS DIR ordering.
        for (final String sub : DiskFilesystem.listDirs(drive.disk(), target, drive.kind())) {
            entries.add(new ICliComputer.FsEntry(FsPaths.fileName(sub), "", 0L, false, true, 0L));
        }
        for (final DiskFilesystem.FileEntry e : DiskFilesystem.list(drive.disk(), target, drive.kind())) {
            entries.add(new ICliComputer.FsEntry(FsPaths.fileName(e.path()), e.type().extension(),
                    e.weight(), e.readOnly(), false, e.modified()));
        }
        /*
         * An install disc stores nothing: its setup, readme and licence are generated from what it
         * installs, and the explorer has always shown them. The prompt showed an empty disc instead,
         * so the same projection is listed here, dirs with the dirs and files with the files.
         */
        for (final InstallerLayout.Entry e : InstallerProjection.list(drive.disk(), target)) {
            entries.add(new ICliComputer.FsEntry(FsPaths.fileName(e.path()),
                    e.directory() ? "" : e.type().extension(), 0L, true, e.directory(), 0L));
        }
        // The system's files and the installed programs' folders, generated the same way, on the system disk.
        final IOsHost host = this.osHost();
        if (host != null && drive.drive() == 'C') {
            final Set<String> seen = new HashSet<>();
            for (final ICliComputer.FsEntry entry : entries) {
                seen.add(entry.name());
            }
            for (final InstallerLayout.Entry e : ProgramFilesProjection.list(host, target)) {
                if (seen.add(FsPaths.fileName(e.path()))) {
                    entries.add(new ICliComputer.FsEntry(FsPaths.fileName(e.path()),
                            e.directory() ? "" : e.type().extension(), 0L, true, e.directory(), 0L));
                }
            }
        }
        return ICliComputer.FsResult.listing(entries);
    }

    /** What the file at that path holds, or why it could not be read. */
    public ICliComputer.FsResult readFile(final String path) {
        final NetPath net = NetPath.parse(path);
        if (net != null) {
            final NetworkPathResolver.Reached reached = this.network.reach(net);
            return reached.ok() ? reached.remote().readFile(reached.path()) : reached.error();
        }
        final Resolved r = this.resolve(path);
        if (r.drive0() == null) {
            return DriveTable.missing(r.drive());
        }
        if (r.drive0().disk().isEmpty()) {
            return DriveTable.notReady(r.drive());
        }
        final DriveTable.Drive drive = r.drive0();
        final String real = r.path();
        // A file on an install disc has no stored bytes: its text is generated from the disc's stamp.
        final Optional<String> projected = InstallerProjection.text(drive.disk(), real);
        if (projected.isPresent()) {
            return ICliComputer.FsResult.content(projected.get());
        }
        // So is a file of the system's own, or of an installed program's folder, on the system disk.
        final IOsHost host = this.osHost();
        if (host != null && drive.drive() == 'C') {
            final Optional<String> system = ProgramFilesProjection.text(host, real);
            if (system.isPresent()) {
                return ICliComputer.FsResult.content(system.get());
            }
        }
        final Optional<String> content = DiskFilesystem.read(drive.disk(), real);
        if (content.isEmpty()) {
            // Distinguish a .dat rejection from a plain missing file for a cleaner error.
            final List<DiskFilesystem.FileEntry> all =
                    DiskFilesystem.list(drive.disk(), FsPaths.parentDir(real), drive.kind());
            final boolean isDat = all.stream().anyMatch(e -> e.path().equals(real) && e.readOnly());
            if (isDat) {
                return ICliComputer.FsResult.fail(
                        path + ": .dat files are read-only (use the Network Interactor to access items)");
            }
            return ICliComputer.FsResult.fail(path + ": file not found");
        }
        return ICliComputer.FsResult.content(content.get());
    }

    /** Every drive the machine can see, the system disk first, as {@code df} shows them. */
    public List<ICliComputer.MountInfo> mounts() {
        final List<ICliComputer.MountInfo> out = new ArrayList<>();
        int index = 0;
        for (final DriveTable.Drive drive : DriveTable.of(this.machine, this.level).all()) {
            final ItemStack stack = drive.disk();
            final boolean ready = !stack.isEmpty();
            final long capacity;
            if (stack.getItem() instanceof DiskItem diskItem) {
                capacity = diskItem.spec().capacityItems() * StorageKey.MB_EQ_PER_ITEM;
            } else if (stack.getItem() instanceof FormattedMediaItem mediaItem) {
                capacity = mediaItem.format().capacityItems() * StorageKey.MB_EQ_PER_ITEM;
            } else {
                capacity = 0L;
            }
            final String device = drive.drive() == 'C' ? "sda1" : "sd" + (char) ('a' + index);
            out.add(new ICliComputer.MountInfo(drive.drive(), device, capacity,
                    ready ? DriveTable.freeWeightOf(stack) : 0L, ready));
            index++;
        }
        return out;
    }

    /**
     * Moves the caller to another folder: the path is read against where it stands, the folder has to be there, and
     * {@code moveTo} is handed where it landed, since where a caller stands is the caller's to keep.
     */
    public ICliComputer.FsResult changeDir(final String input, final Consumer<DosPath.Location> moveTo) {
        final DosPath.Location target = DosPath.resolve(this.where.get(), input);
        final DriveTable.Drive drive = DriveTable.of(this.machine, this.level).find(target.drive());
        if (drive == null) {
            return DriveTable.missing(target.drive());
        }
        if (drive.disk().isEmpty()) {
            return DriveTable.notReady(target.drive());
        }
        if (!DriveTable.dirExists(this.osHost(), drive, target.storagePath())) {
            return ICliComputer.FsResult.fail("The system cannot find the path specified.");
        }
        moveTo.accept(target);
        return ICliComputer.FsResult.ok("");
    }

    /**
     * Moves the caller to another drive; {@code moveTo} is handed the letter it landed on, as {@link #changeDir} is
     * handed the folder.
     */
    public ICliComputer.FsResult changeDrive(final char letter, final Consumer<Character> moveTo) {
        final DriveTable.Drive drive = DriveTable.of(this.machine, this.level).find(letter);
        if (drive == null) {
            return DriveTable.missing(letter);
        }
        if (drive.disk().isEmpty()) {
            return DriveTable.notReady(letter);
        }
        moveTo.accept(Character.toUpperCase(letter));
        return ICliComputer.FsResult.ok("");
    }

    /** Whether there is a file to read at that path. */
    public boolean exists(final String path) {
        return this.readFile(path).ok();
    }

    /** What the file at that path holds, or why it could not be read. */
    public ICliComputer.FsResult read(final String path) {
        return this.readFile(path);
    }

    /** Puts that text in the file at that path in place of what it held; false when it could not be written. */
    public boolean write(final String path, final String text) {
        return this.door.writeFile(path, text).ok();
    }

    /**
     * Adds that text at the end of the file at that path, making the file when there is none, without reading what it
     * already holds; false when it could not be written.
     */
    public boolean append(final String path, final String text) {
        return this.door.appendFile(path, text).ok();
    }

    /** Deletes the file at that path; false when it could not be deleted. */
    public boolean delete(final String path) {
        return this.door.deleteFile(path).ok();
    }

    /** Makes a folder at that path; false when it could not be made. */
    public boolean makeDir(final String path) {
        return this.door.makeDir(path).ok();
    }

    /**
     * The names in the folder at that path, or none when it cannot be read.
     *
     * <p>A name is the whole last segment of its path, extension included, so a name handed back is one that can be
     * opened as it is. A folder's ends in a slash, because whatever walks a tree has to tell which is which, and trying
     * to open each one to find out would be a poor answer.
     */
    public List<String> list(final String path) {
        final ICliComputer.FsResult listing = this.listDisk(path);
        final List<String> names = new ArrayList<>();
        if (listing.ok()) {
            for (final ICliComputer.FsEntry entry : listing.entries()) {
                names.add(entry.isDir() ? entry.name() + "/" : entry.name());
            }
        }
        return names;
    }

    /**
     * How many bytes a text takes on a disk, which is what reading and writing it are priced by: counted the way the
     * disk stores it, in UTF-8, without making the bytes to count them.
     */
    static long bytesOf(final String text) {
        long bytes = 0;
        final int length = text.length();
        for (int i = 0; i < length; i++) {
            final char c = text.charAt(i);
            if (c < 0x80) {
                bytes++;
            } else if (c < 0x800) {
                bytes += 2;
            } else if (!Character.isSurrogate(c)) {
                bytes += 3;
            } else if (Character.isHighSurrogate(c) && i + 1 < length && Character.isLowSurrogate(text.charAt(i + 1))) {
                bytes += 4;
                i++;
            } else {
                // A half of a pair without its other half is written as a single replacement byte.
                bytes++;
            }
        }
        return bytes;
    }
}
