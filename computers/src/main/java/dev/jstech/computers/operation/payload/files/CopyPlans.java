/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.files;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.machine.FileCopyJobs;
import dev.jstech.computers.machine.RemoteComputerService;
import dev.jstech.computers.operation.payload.CopyProgressPayload;
import dev.jstech.computers.operation.payload.DiskFilesPayload;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.VolumeLabel;
import dev.jstech.computers.os.fs.CopyTiming;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.os.media.InstallerProjection;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.network.DataLink;
import dev.jstech.core.text.Text;
import dev.jstech.core.tier.HardwareEra;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import static dev.jstech.computers.operation.payload.files.FileAccess.NET_ROOT;
import static dev.jstech.computers.operation.payload.files.FileAccess.mediaStackFor;
import static dev.jstech.computers.operation.payload.files.FileAccess.mediaSubPath;
import static dev.jstech.computers.operation.payload.files.FileAccess.netDos;
import static dev.jstech.computers.operation.payload.files.FileAccess.netShell;

/**
 * What a copy weighs and how fast it goes, worked out when it starts: the file's size on the volume it comes from,
 * and the slowest of the volumes it goes between and, to or from another machine, the slowest cable on the way.
 * The names a copy window shows come from here too.
 */
final class CopyPlans {

    private CopyPlans() {
    }

    /** The copy of {@code kind} from {@code src} into {@code destDir} on that machine, as its windows show it. */
    static FileCopyJobs.Copy plan(final ServerLevel level, final IOsHost computer, final byte kind, final String src,
                                  final String destDir) {
        final String name = FsPaths.fileName(pathPart(src));
        final Text from = folderName(level, computer, src, parent(pathPart(src)));
        final Text to = folderName(level, computer, destDir, pathPart(destDir));
        final long size = sizeOf(level, computer, src);
        double rate = CopyTiming.slowest(rateOf(level, computer, src), rateOf(level, computer, destDir));
        if (src.startsWith(NET_ROOT) || destDir.startsWith(NET_ROOT)) {
            final double cable = cableRate(level, computer, src.startsWith(NET_ROOT) ? src : destDir);
            rate = CopyTiming.slowest(rate, cable);
        }
        return new FileCopyJobs.Copy(kind, name, from, to, size, rate);
    }

    /** What a file in the trash weighs and how fast its disk lets it go, for a deletion that takes time. */
    static FileCopyJobs.Copy deletion(final IOsHost computer, final String path) {
        final ItemStack disk = computer.systemDisk();
        final String folder = parent(path);
        return new FileCopyJobs.Copy(CopyProgressPayload.DELETE, FsPaths.fileName(path),
                folder.isEmpty() ? systemDiskName(computer) : Text.literal(FsPaths.fileName(folder)), Text.EMPTY,
                DiskFilesystem.weightOf(disk, path), volumeRate(disk));
    }

    /*
     * A folder as a copy window names it: its own name, or for the root of a volume the volume's name, the way the
     * explorer's drive tree names it.
     */
    private static Text folderName(final ServerLevel level, final IOsHost computer, final String path,
                                   final String folder) {
        if (!folder.isEmpty()) {
            return Text.literal(FsPaths.fileName(folder));
        }
        if (path.startsWith("media:")) {
            final ItemStack medium = mediaStackFor(level, computer, path);
            return VolumeLabel.of(medium, InstallerProjection.facts(medium)
                    .map(f -> DiskFilesPayload.SETUP.with(f.name()))
                    .orElse(DiskFilesPayload.REMOVABLE_DRIVE.text()));
        }
        if (path.startsWith(NET_ROOT)) {
            return DiskFilesPayload.NETWORK.text();
        }
        return systemDiskName(computer);
    }

    /*
     * The system disk's root as the explorer names it: the root itself on a UNIX-like system, where everything hangs
     * from it, and elsewhere the player's label for the disk, or a local disk.
     */
    private static Text systemDiskName(final IOsHost computer) {
        final OsDef os = computer.installedOs();
        if (os != null && os.platform().unixLike()) {
            return Text.literal("/");
        }
        return VolumeLabel.of(computer.systemDisk(), DiskFilesPayload.LOCAL_DISK.text());
    }

    /* The file's size, read on the volume it is on; over the network it is read through the shell. */
    private static long sizeOf(final ServerLevel level, final IOsHost computer, final String src) {
        if (src.startsWith(NET_ROOT)) {
            final ServerCliComputer shell = netShell(level, computer);
            if (shell == null) {
                return 0L;
            }
            final ICliComputer.FsResult read = shell.readFile(netDos(src));
            final HardwareEra era = DiskFilesystem.eraOf(computer.systemDisk());
            // A file read over the network comes back as its words, whose length is what it weighs.
            return read.ok() && read.message() instanceof Text.Literal words
                    ? FsPaths.sizeMbEq(words.value().length(), era) : 0L;
        }
        final ItemStack volume = src.startsWith("media:") ? mediaStackFor(level, computer, src) : computer.systemDisk();
        return volume.isEmpty() ? 0L : DiskFilesystem.weightOf(volume, src.startsWith("media:") ? mediaSubPath(src)
                : src);
    }

    /* How fast the volume a path is on reads and writes; a path on the network stands for the machine's own disk. */
    private static double rateOf(final ServerLevel level, final IOsHost computer, final String path) {
        if (path.startsWith("media:")) {
            return volumeRate(mediaStackFor(level, computer, path));
        }
        return volumeRate(computer.systemDisk());
    }

    private static double volumeRate(final ItemStack volume) {
        if (volume.getItem() instanceof DiskItem disk) {
            return CopyTiming.diskRate(disk.spec().tier().speedMultiplier());
        }
        return volume.isEmpty() ? CopyTiming.diskRate(1) : CopyTiming.mediaRate(InstallerProjection.formatOf(volume));
    }

    /*
     * The slowest cable between this machine and the one a network path names, which is as fast as the copy can go
     * between them; as fast as the machine's own disk when the two cannot be found.
     */
    private static double cableRate(final ServerLevel level, final IOsHost computer, final String netPath) {
        if (!(computer instanceof IComputerTerminalHost terminal) || !(computer instanceof AbstractComputerBlockEntity
                here)) {
            return Double.MAX_VALUE;
        }
        final String rest = netPath.substring(NET_ROOT.length());
        final int slash = rest.indexOf('/');
        final String host = slash < 0 ? rest : rest.substring(0, slash);
        final Map<String, BlockEntity> matches = new RemoteComputerService(terminal, level).matching(host);
        if (matches.size() != 1
                || !(matches.values().iterator().next() instanceof AbstractComputerBlockEntity there)) {
            return Double.MAX_VALUE;
        }
        final Optional<DataLink> slowest = here.slowestCableTo(level, there);
        return slowest.map(link -> CopyTiming.cableRate(link.throughput())).orElse(Double.MAX_VALUE);
    }

    /* The part of an explorer path that is a path on its volume, past "media:<drive>" or "net:". */
    private static String pathPart(final String path) {
        if (path.startsWith("media:")) {
            return mediaSubPath(path);
        }
        return path.startsWith(NET_ROOT) ? path.substring(NET_ROOT.length()) : path;
    }

    private static String parent(final String path) {
        final int slash = path.lastIndexOf('/');
        return slash < 0 ? "" : path.substring(0, slash);
    }
}
