/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDisks;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.os.fs.InstallerLayout;
import dev.jstech.computers.os.fs.ProgramFilesProjection;
import dev.jstech.computers.os.media.FormattedMediaItem;
import dev.jstech.computers.os.media.InstallerProjection;
import dev.jstech.computers.program.cli.CliTexts;
import dev.jstech.computers.program.cli.DosPath;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.program.cli.NetPath;
import dev.jstech.computers.storage.DriveVolumes;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
@TextHolder
public final class FileService {

    private final BlockEntity machine;
    private final ServerLevel level;
    /** Where a path on another machine of the network leads. */
    private final NetworkPathResolver network;
    /** The folder the caller stands in, asked for at the moment a path is read. */
    private final Supplier<DosPath.Location> where;

    private static final TextKey SHARED_READ_ONLY =
            TextKey.of("jsc.service.file.shared_read_only", "%s: %s is shared read-only");
    private static final TextKey DAT_READ_ONLY = TextKey.of("jsc.service.file.dat_read_only",
            "%s: .dat files are read-only (use the Network Interactor to access items)");
    private static final TextKey NOT_FOUND = TextKey.of("jsc.service.file.not_found", "%s: file not found");
    private static final TextKey NOT_EDITABLE =
            TextKey.of("jsc.service.file.not_editable", "%s: .%s files cannot be edited");
    private static final TextKey WROTE = TextKey.of("jsc.service.file.wrote", "wrote %s");
    private static final TextKey INVALID_NAME =
            TextKey.of("jsc.service.file.invalid_name", "%s: invalid file name for this filesystem");
    private static final TextKey DISK_FULL =
            TextKey.of("jsc.service.file.disk_full", "%s: not enough free space on the disk");
    private static final TextKey TYPE_READ_ONLY = TextKey.of("jsc.service.file.type_read_only", "%s: .%s is read-only");
    private static final TextKey DAT_UNDELETABLE = TextKey.of("jsc.service.file.dat_undeletable",
            "%s: .dat files cannot be deleted (use the Network Interactor)");
    private static final TextKey DELETED = TextKey.of("jsc.service.file.deleted", "deleted %s");
    private static final TextKey NO_FOLDERS =
            TextKey.of("jsc.service.file.no_folders", "Directories are not supported on this drive.");
    private static final TextKey ALREADY_EXISTS =
            TextKey.of("jsc.service.file.already_exists", "A subdirectory or file %s already exists.");
    private static final TextKey CANNOT_MAKE_FOLDER =
            TextKey.of("jsc.service.file.cannot_make_folder", "%s: unable to create directory");
    private static final TextKey FOLDER_IN_USE = TextKey.of("jsc.service.file.folder_in_use",
            "The process cannot access the directory because it is in use.");
    private static final TextKey FOLDER_NOT_EMPTY =
            TextKey.of("jsc.service.file.folder_not_empty", "The directory is not empty.");
    private static final TextKey COPIED = TextKey.of("jsc.service.file.copied", "        1 file(s) copied.");
    private static final TextKey MOVED = TextKey.of("jsc.service.file.moved", "        1 file(s) moved.");
    private static final TextKey CROSS_COPY_FILES_ONLY = TextKey.of("jsc.service.file.cross_copy_files_only",
            "%s: file not found (cross-drive copy supports files only)");
    private static final TextKey CROSS_MOVE_FILES_ONLY = TextKey.of("jsc.service.file.cross_move_files_only",
            "%s: file not found (cross-drive move supports files only)");
    private static final TextKey DESTINATION_READ_ONLY =
            TextKey.of("jsc.service.file.destination_read_only", "%s: the destination is read-only");
    private static final TextKey NAME_TAKEN =
            TextKey.of("jsc.service.file.name_taken", "%s: a file of that name is already there");
    private static final TextKey NO_NETWORK_MOVE = TextKey.of("jsc.service.file.no_network_move",
            "a file on another machine is copied, not moved: copy it and delete the original");
    private static final TextKey FORMAT_NOT_READY =
            TextKey.of("jsc.service.file.format_not_ready", "format: drive %s: drive not ready");
    private static final TextKey FORMAT_SYSTEM_DRIVE = TextKey.of("jsc.service.file.format_system_drive",
            "format: cannot format drive C: - the running system lives on it");
    private static final TextKey FORMATTED = TextKey.of("jsc.service.file.formatted",
            "Formatting drive %s: ... done\nAll data on the volume was erased.");
    private static final TextKey FORMAT_NO_DRIVE =
            TextKey.of("jsc.service.file.format_no_drive", "format: drive %s: not found");
    private static final TextKey NO_SUCH_FOLDER = TextKey.of("jsc.service.file.no_such_folder", "%s: no such folder");

    public FileService(final BlockEntity machine, final ServerLevel level, final NetworkPathResolver network,
                       final Supplier<DosPath.Location> where) {
        this.machine = machine;
        this.level = level;
        this.network = network;
        this.where = where;
    }

    /** A path read against the folder the caller stands in: its drive letter, that drive, and the path on it. */
    private record Resolved(char letter, DriveTable.Drive drive, String path) {
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

    /** What the drive a path lands on answers when it is not there or has nothing in it, or null when it is ready. */
    @Nullable
    private static ICliComputer.FsResult unready(final Resolved r) {
        if (r.drive() == null) {
            return DriveTable.missing(r.letter());
        }
        if (r.drive().disk().isEmpty()) {
            return DriveTable.notReady(r.letter());
        }
        return null;
    }

    /** What the share a network path lands on answers when it cannot be written to, or null when it can. */
    @Nullable
    private static ICliComputer.FsResult unwritable(final NetPath net, final NetworkPathResolver.Reached reached) {
        if (!reached.ok()) {
            return reached.error();
        }
        if (!reached.share().writable()) {
            return ICliComputer.FsResult.fail(SHARED_READ_ONLY.with(net.display(), net.share()));
        }
        return null;
    }

    /** What is in the folder at that path, on a drive of this machine or on a share of another. */
    public ICliComputer.FsResult listDisk(final String dir) {
        final NetPath net = NetPath.parse(dir);
        if (net != null) {
            return this.network.list(net);
        }
        final Resolved r = this.resolve(dir == null ? "" : dir);
        final ICliComputer.FsResult unready = unready(r);
        if (unready != null) {
            return unready;
        }
        final DriveTable.Drive drive = r.drive();
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
        final ICliComputer.FsResult unready = unready(r);
        if (unready != null) {
            return unready;
        }
        final DriveTable.Drive drive = r.drive();
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
                return ICliComputer.FsResult.fail(DAT_READ_ONLY.with(path));
            }
            return ICliComputer.FsResult.fail(NOT_FOUND.with(path));
        }
        return ICliComputer.FsResult.content(content.get());
    }

    /** Puts that text in the file at that path in place of what it held. */
    public ICliComputer.FsResult writeFile(final String path, final String content) {
        final NetPath net = NetPath.parse(path);
        if (net != null) {
            final NetworkPathResolver.Reached reached = this.network.reach(net);
            final ICliComputer.FsResult refused = unwritable(net, reached);
            return refused != null ? refused : reached.remote().writeFile(reached.path(), content);
        }
        final Resolved r = this.resolve(path);
        final ICliComputer.FsResult unready = unready(r);
        if (unready != null) {
            return unready;
        }
        final DriveTable.Drive drive = r.drive();
        final String real = r.path();
        // Any extension will do: a kind the machine does not know is kept as text under the name it was given.
        final FileType type = FileType.of(extensionOf(path));
        if (!type.userEditable()) {
            return ICliComputer.FsResult.fail(NOT_EDITABLE.with(path, type.extension()));
        }
        // Free space available, crediting back the file being overwritten so a same-size rewrite fits.
        final long oldWeight = DiskFilesystem.read(drive.disk(), real)
                .map(c -> FsPaths.sizeMbEq(c.getBytes(StandardCharsets.UTF_8).length,
                        DiskFilesystem.eraOf(drive.disk())))
                .orElse(0L);
        final DiskFilesystem.WriteResult result = DiskFilesystem.write(drive.disk(), real, type, content,
                DriveTable.freeWeightOf(drive.disk()) + oldWeight, drive.kind(), this.level.getGameTime());
        return written(result, drive, path, type);
    }

    /**
     * Adds that text at the end of the file at that path, making the file when there is none, without reading what it
     * already holds.
     */
    public ICliComputer.FsResult appendFile(final String path, final String content) {
        final NetPath net = NetPath.parse(path);
        if (net != null) {
            final NetworkPathResolver.Reached reached = this.network.reach(net);
            final ICliComputer.FsResult refused = unwritable(net, reached);
            return refused != null ? refused : reached.remote().appendFile(reached.path(), content);
        }
        final Resolved r = this.resolve(path);
        final ICliComputer.FsResult unready = unready(r);
        if (unready != null) {
            return unready;
        }
        final DriveTable.Drive drive = r.drive();
        final FileType type = FileType.of(extensionOf(path));
        if (!type.userEditable()) {
            return ICliComputer.FsResult.fail(NOT_EDITABLE.with(path, type.extension()));
        }
        final DiskFilesystem.WriteResult result = DiskFilesystem.append(drive.disk(), r.path(), type, content,
                DriveTable.freeWeightOf(drive.disk()), drive.kind(), this.level.getGameTime());
        return written(result, drive, path, type);
    }

    /** What a write or an addition answers, and what it keeps when it went through. */
    private static ICliComputer.FsResult written(final DiskFilesystem.WriteResult result, final DriveTable.Drive drive,
                                                 final String path, final FileType type) {
        return switch (result) {
            case OK -> {
                drive.commit().run();
                yield ICliComputer.FsResult.ok(WROTE.with(path));
            }
            case INVALID_PATH -> ICliComputer.FsResult.fail(INVALID_NAME.with(path));
            case DISK_FULL -> ICliComputer.FsResult.fail(DISK_FULL.with(path));
            case READ_ONLY -> ICliComputer.FsResult.fail(TYPE_READ_ONLY.with(path, type.extension()));
        };
    }

    /** Deletes the file at that path. */
    public ICliComputer.FsResult deleteFile(final String path) {
        final NetPath net = NetPath.parse(path);
        if (net != null) {
            final NetworkPathResolver.Reached reached = this.network.reach(net);
            final ICliComputer.FsResult refused = unwritable(net, reached);
            return refused != null ? refused : reached.remote().deleteFile(reached.path());
        }
        final Resolved r = this.resolve(path);
        final ICliComputer.FsResult unready = unready(r);
        if (unready != null) {
            return unready;
        }
        final DriveTable.Drive drive = r.drive();
        final String real = r.path();
        // Reject .dat entries before attempting deletion so we surface a clear message.
        final List<DiskFilesystem.FileEntry> all =
                DiskFilesystem.list(drive.disk(), FsPaths.parentDir(real), drive.kind());
        final boolean isDat = all.stream().anyMatch(e -> e.path().equals(real) && e.readOnly());
        if (isDat) {
            return ICliComputer.FsResult.fail(DAT_UNDELETABLE.with(path));
        }
        if (!DiskFilesystem.delete(drive.disk(), real)) {
            return ICliComputer.FsResult.fail(NOT_FOUND.with(path));
        }
        // The delete changed what the drive's stack carries; the owner keeps it.
        drive.commit().run();
        return ICliComputer.FsResult.ok(DELETED.with(path));
    }

    /** Makes a folder at that path. */
    public ICliComputer.FsResult makeDir(final String path) {
        if (path == null || path.isBlank()) {
            return ICliComputer.FsResult.fail(CliTexts.BAD_SYNTAX.text());
        }
        final NetPath net = NetPath.parse(path);
        if (net != null) {
            final NetworkPathResolver.Reached reached = this.network.reach(net);
            final ICliComputer.FsResult refused = unwritable(net, reached);
            return refused != null ? refused : reached.remote().makeDir(reached.path());
        }
        final Resolved r = this.resolve(path);
        final ICliComputer.FsResult unready = unready(r);
        if (unready != null) {
            return unready;
        }
        final DriveTable.Drive drive = r.drive();
        final String real = r.path();
        if (drive.kind() != FilesystemKind.HIERARCHICAL) {
            return ICliComputer.FsResult.fail(NO_FOLDERS.text());
        }
        if (real.isEmpty()) {
            return ICliComputer.FsResult.fail(CliTexts.BAD_SYNTAX.text());
        }
        if (DriveTable.dirExists(this.osHost(), drive, real) || DiskFilesystem.exists(drive.disk(), real)) {
            return ICliComputer.FsResult.fail(ALREADY_EXISTS.with(path));
        }
        if (DiskFilesystem.mkdir(drive.disk(), real, drive.kind())) {
            drive.commit().run();
            return ICliComputer.FsResult.ok("");
        }
        return ICliComputer.FsResult.fail(CANNOT_MAKE_FOLDER.with(path));
    }

    /** Removes the folder at that path, which has to be empty and cannot be the one the caller stands in. */
    public ICliComputer.FsResult removeDir(final String path) {
        final Resolved r = this.resolve(path);
        final ICliComputer.FsResult unready = unready(r);
        if (unready != null) {
            return unready;
        }
        final DriveTable.Drive drive = r.drive();
        final String real = r.path();
        if (drive.kind() != FilesystemKind.HIERARCHICAL) {
            return ICliComputer.FsResult.fail(NO_FOLDERS.text());
        }
        if (real.isEmpty()) {
            return ICliComputer.FsResult.fail(CliTexts.BAD_SYNTAX.text());
        }
        final DosPath.Location cwd = this.where.get();
        if (r.letter() == cwd.drive() && real.equals(cwd.storagePath())) {
            return ICliComputer.FsResult.fail(FOLDER_IN_USE.text());
        }
        if (!DriveTable.dirExists(this.osHost(), drive, real)) {
            return ICliComputer.FsResult.fail(CliTexts.PATH_NOT_FOUND.text());
        }
        // DOS 'rd' refuses a non-empty directory; there is no implicit recursive delete.
        final boolean hasChildren = !DiskFilesystem.listDirs(drive.disk(), real, drive.kind()).isEmpty()
                || !DiskFilesystem.list(drive.disk(), real, drive.kind()).isEmpty();
        if (hasChildren) {
            return ICliComputer.FsResult.fail(FOLDER_NOT_EMPTY.text());
        }
        if (DiskFilesystem.rmdir(drive.disk(), real, drive.kind())) {
            drive.commit().run();
            return ICliComputer.FsResult.ok("");
        }
        return ICliComputer.FsResult.fail(CliTexts.PATH_NOT_FOUND.text());
    }

    /** Copies a file, or a whole folder on one drive, to where it is told. */
    public ICliComputer.FsResult copyPath(final String src, final String dest) {
        final NetPath fromNet = NetPath.parse(src);
        final NetPath toNet = NetPath.parse(dest);
        if (fromNet != null || toNet != null) {
            return this.copyAcrossNetwork(src, fromNet, dest, toNet);
        }
        final Resolved s = this.resolve(src);
        final ICliComputer.FsResult sourceUnready = unready(s);
        if (sourceUnready != null) {
            return sourceUnready;
        }
        final Resolved d = this.resolve(dest);
        final ICliComputer.FsResult destUnready = unready(d);
        if (destUnready != null) {
            return destUnready;
        }
        // A destination that is an existing directory means "copy into it", keeping the source name.
        String realDest = d.path();
        if (DriveTable.dirExists(this.osHost(), d.drive(), realDest)) {
            realDest = FsPaths.join(realDest, FsPaths.fileName(s.path()));
        }
        if (s.letter() == d.letter()) {
            // Same drive: DiskFilesystem.copy handles both a single file and a whole directory subtree.
            if (DiskFilesystem.copy(s.drive().disk(), s.path(), realDest,
                    DriveTable.freeWeightOf(d.drive().disk()), s.drive().kind())) {
                s.drive().commit().run();
                return ICliComputer.FsResult.ok(COPIED.text());
            }
            return ICliComputer.FsResult.fail(CliTexts.FILE_NOT_FOUND.text());
        }
        // Cross-drive: copy a single file by reading the source and writing it to the destination drive.
        final Optional<String> content = DiskFilesystem.read(s.drive().disk(), s.path());
        if (content.isEmpty()) {
            return ICliComputer.FsResult.fail(CROSS_COPY_FILES_ONLY.with(src));
        }
        // Writing replaces what is there, so a file already of that name is refused as the same drive refuses it.
        if (DiskFilesystem.exists(d.drive().disk(), realDest)) {
            return alreadyThere(dest);
        }
        final FileType type = FileType.of(extensionOf(realDest));
        final DiskFilesystem.WriteResult wr = DiskFilesystem.write(d.drive().disk(), realDest, type, content.get(),
                DriveTable.freeWeightOf(d.drive().disk()), d.drive().kind(), this.level.getGameTime());
        return switch (wr) {
            case OK -> {
                d.drive().commit().run();
                yield ICliComputer.FsResult.ok(COPIED.text());
            }
            case DISK_FULL -> ICliComputer.FsResult.fail(DISK_FULL.with(dest));
            case INVALID_PATH -> ICliComputer.FsResult.fail(INVALID_NAME.with(dest));
            case READ_ONLY -> ICliComputer.FsResult.fail(DESTINATION_READ_ONLY.with(dest));
        };
    }

    /**
     * A copy with a shared folder at either end: the file is read where it is and written where it goes, through each
     * machine's own drives, so a read-only share refuses the write the same way its owner's prompt would. Files only;
     * a folder is copied one file at a time.
     */
    private ICliComputer.FsResult copyAcrossNetwork(final String src, final NetPath fromNet, final String dest,
                                                    final NetPath toNet) {
        final ICliComputer.FsResult content = this.readFile(src);
        if (!content.ok()) {
            return content;
        }
        String target = dest;
        final String name = fromNet != null ? fromNet.name() : FsPaths.fileName(this.resolve(src).path());
        if (toNet != null) {
            if (this.network.dirExists(toNet)) {
                target = toNet.display() + "\\" + name;
            }
        } else {
            final Resolved d = this.resolve(dest);
            if (d.drive() != null && !d.drive().disk().isEmpty()
                    && DriveTable.dirExists(this.osHost(), d.drive(), d.path())) {
                target = d.letter() + ":\\" + FsPaths.join(d.path(), name).replace('/', '\\');
            }
        }
        if (this.exists(target)) {
            return alreadyThere(target);
        }
        final ICliComputer.FsResult written = this.writeFile(target, content.message().english());
        return written.ok() ? ICliComputer.FsResult.ok(COPIED.text()) : written;
    }

    /**
     * What a copy or a move says when a file of that name is already where it would go. Copying on one drive has
     * always refused rather than write over it; across drives and across machines the copy is a write, which
     * replaces, so each of those asks first and says the same.
     */
    private static ICliComputer.FsResult alreadyThere(final String where) {
        return ICliComputer.FsResult.fail(NAME_TAKEN.with(where));
    }

    /** Moves a file, or a whole folder on one drive, into the folder it is told. */
    public ICliComputer.FsResult movePath(final String src, final String destDir) {
        if (NetPath.looksLike(src) || NetPath.looksLike(destDir)) {
            return ICliComputer.FsResult.fail(NO_NETWORK_MOVE.text());
        }
        final Resolved s = this.resolve(src);
        final ICliComputer.FsResult sourceUnready = unready(s);
        if (sourceUnready != null) {
            return sourceUnready;
        }
        final Resolved d = this.resolve(destDir);
        final ICliComputer.FsResult destUnready = unready(d);
        if (destUnready != null) {
            return destUnready;
        }
        if (!d.path().isEmpty() && !DriveTable.dirExists(this.osHost(), d.drive(), d.path())) {
            return ICliComputer.FsResult.fail(CliTexts.PATH_NOT_FOUND.text());
        }
        if (s.letter() == d.letter()) {
            if (DiskFilesystem.move(s.drive().disk(), s.path(), d.path(), s.drive().kind())) {
                s.drive().commit().run();
                return ICliComputer.FsResult.ok(MOVED.text());
            }
            return ICliComputer.FsResult.fail(CliTexts.FILE_NOT_FOUND.text());
        }
        // Cross-drive move = copy the file onto the destination drive, then delete the source.
        final Optional<String> content = DiskFilesystem.read(s.drive().disk(), s.path());
        if (content.isEmpty()) {
            return ICliComputer.FsResult.fail(CROSS_MOVE_FILES_ONLY.with(src));
        }
        final String destPath = FsPaths.join(d.path(), FsPaths.fileName(s.path()));
        if (DiskFilesystem.exists(d.drive().disk(), destPath)) {
            return alreadyThere(destDir);
        }
        final FileType type = FileType.of(extensionOf(destPath));
        final DiskFilesystem.WriteResult wr = DiskFilesystem.write(d.drive().disk(), destPath, type, content.get(),
                DriveTable.freeWeightOf(d.drive().disk()), d.drive().kind(), this.level.getGameTime());
        if (wr != DiskFilesystem.WriteResult.OK) {
            return switch (wr) {
                case DISK_FULL -> ICliComputer.FsResult.fail(DISK_FULL.with(destDir));
                case INVALID_PATH -> ICliComputer.FsResult.fail(INVALID_NAME.with(destDir));
                case READ_ONLY -> ICliComputer.FsResult.fail(DESTINATION_READ_ONLY.with(destDir));
                case OK -> ICliComputer.FsResult.ok("");
            };
        }
        DiskFilesystem.delete(s.drive().disk(), s.path());
        s.drive().commit().run();
        d.drive().commit().run();
        return ICliComputer.FsResult.ok(MOVED.text());
    }

    /** Renames a file or folder, which stays where it is. */
    public ICliComputer.FsResult renamePath(final String src, final String newName) {
        if (newName == null || newName.isBlank() || newName.contains("/") || newName.contains("\\")) {
            return ICliComputer.FsResult.fail(CliTexts.BAD_SYNTAX.text());
        }
        final Resolved s = this.resolve(src);
        final ICliComputer.FsResult unready = unready(s);
        if (unready != null) {
            return unready;
        }
        final DriveTable.Drive drive = s.drive();
        final String dest = FsPaths.join(FsPaths.parentDir(s.path()), newName);
        if (DiskFilesystem.rename(drive.disk(), s.path(), dest, drive.kind())) {
            drive.commit().run();
            return ICliComputer.FsResult.ok("");
        }
        return ICliComputer.FsResult.fail(CliTexts.FILE_NOT_FOUND.text());
    }

    /** Whether the drive with that letter carries an installed system, which formatting it would erase. */
    public boolean holdsSystem(final char letterRaw) {
        final char letter = Character.toUpperCase(letterRaw);
        for (final DriveTable.Drive drive : DriveTable.of(this.machine, this.level).all()) {
            if (drive.drive() == letter) {
                return !OsDisks.systemsOn(drive.disk()).ids().isEmpty();
            }
        }
        return false;
    }

    /**
     * Erases a drive: the system on it, its files, the items it stores and, on a medium, the installer it was stamped
     * as. The drive the running system lives on is refused.
     */
    public ICliComputer.OpResult formatDrive(final char letterRaw) {
        final char letter = Character.toUpperCase(letterRaw);
        for (final DriveTable.Drive drive : DriveTable.of(this.machine, this.level).all()) {
            if (drive.drive() != letter) {
                continue;
            }
            final ItemStack target = drive.disk();
            if (target.isEmpty()) {
                return ICliComputer.OpResult.fail(FORMAT_NOT_READY.with(String.valueOf(letter)));
            }
            final IOsHost host = this.osHost();
            if (letter == 'C' && host != null && host.hasOs()) {
                return ICliComputer.OpResult.fail(FORMAT_SYSTEM_DRIVE);
            }
            target.remove(ComputingComponents.DISK_SYSTEMS.get());
            target.remove(ComputingComponents.FILESYSTEM.get());
            DriveVolumes.erase(target);
            target.remove(ComputingComponents.DISK_PUBLIC_PERMILLE.get());
            target.remove(ComputingComponents.MEDIA_KIND.get());
            target.remove(ComputingComponents.MEDIA_PAYLOAD.get());
            target.remove(ComputingComponents.MEDIA_DATA.get());
            drive.commit().run();
            return ICliComputer.OpResult.ok(FORMATTED.with(String.valueOf(letter)));
        }
        return ICliComputer.OpResult.fail(FORMAT_NO_DRIVE.with(String.valueOf(letter)));
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
            return ICliComputer.FsResult.fail(CliTexts.PATH_NOT_FOUND.text());
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

    /**
     * A folder of this machine named the way a share names it, or the reason it cannot be.
     *
     * <p>The path is read against where the caller stands, so {@code pub} and {@code C:\pub} both work, and the
     * answer carries the DOS spelling because that is what a share is stored and shown as. A drive that is not
     * there, a drive with no disk and a folder that does not exist are three different answers, since they are
     * three different things to fix.
     *
     * @return the folder in DOS form, or a failure saying why it is not one
     */
    public ICliComputer.OpResult folderForShare(final String path) {
        final Resolved r = this.resolve(path);
        final ICliComputer.FsResult unready = unready(r);
        if (unready != null) {
            return ICliComputer.OpResult.fail(unready.message());
        }
        if (!r.path().isEmpty() && !DriveTable.dirExists(this.osHost(), r.drive(), r.path())) {
            return ICliComputer.OpResult.fail(NO_SUCH_FOLDER.with(path));
        }
        return ICliComputer.OpResult.ok(r.letter() + ":\\" + r.path().replace('/', '\\'));
    }

    /** Whether there is a file to read at that path. */
    public boolean exists(final String path) {
        return this.readFile(path).ok();
    }

    /** Whether there is a folder at that path on one of this machine's own drives. */
    public boolean folderExists(final String path) {
        final Resolved r = this.resolve(path);
        return unready(r) == null && DriveTable.dirExists(this.osHost(), r.drive(), r.path());
    }

    /** What the file at that path holds, or why it could not be read. */
    public ICliComputer.FsResult read(final String path) {
        return this.readFile(path);
    }

    /** Puts that text in the file at that path in place of what it held; false when it could not be written. */
    public boolean write(final String path, final String text) {
        return this.writeFile(path, text).ok();
    }

    /**
     * Adds that text at the end of the file at that path, making the file when there is none, without reading what it
     * already holds; false when it could not be written.
     */
    public boolean append(final String path, final String text) {
        return this.appendFile(path, text).ok();
    }

    /** Deletes the file at that path; false when it could not be deleted. */
    public boolean delete(final String path) {
        return this.deleteFile(path).ok();
    }

    /** Makes a folder at that path; false when it could not be made. */
    public boolean makeFolder(final String path) {
        return this.makeDir(path).ok();
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

    /** The lowercase extension of a file path (after the last dot), or {@code ""} when it has none. */
    private static String extensionOf(final String path) {
        final int dot = path.lastIndexOf('.');
        return dot >= 0 && dot < path.length() - 1 ? path.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
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
