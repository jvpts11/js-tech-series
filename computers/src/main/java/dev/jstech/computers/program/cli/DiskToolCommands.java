/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.os.FileSystemType;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.disk.DiskCommand;
import dev.jstech.computers.os.disk.DiskLayout;
import dev.jstech.computers.os.disk.DiskReport;
import dev.jstech.computers.os.disk.DiskTexts;
import dev.jstech.computers.os.disk.PartitionRole;
import dev.jstech.computers.os.disk.PartitionScheme;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Each system's disk tools at its prompt (computers/docs/design/storage-devices.md): diskpart on Frames, FDISK on
 * MC-DOS, fdisk on UNIX System V, {@code fdisk -l} and {@code lsblk} on Linux, {@code gpart show} on FreeBSD, and
 * OpenBSD's {@code fdisk} and {@code disklabel}. They all read the machine's disks from the same report its windows
 * read, and the three that change them talk to the machine the same way Disk Management does.
 */
public final class DiskToolCommands {

    private DiskToolCommands() {
    }

    /** The disk tools of the Unix family's prompts. */
    static List<ICliCommand> posix() {
        return List.of(new LinuxFdisk(), new Lsblk(), new Gpart(), new OpenBsdFdisk(), new Disklabel(),
                new UnixFdisk(), new Mkfs("mkfs.ext4", FileSystemType.EXT4, List.of("mkfs")),
                new Mkfs("mkfs.ext3", FileSystemType.EXT3, List.of()), new Mkfs("mkfs.ext2", FileSystemType.EXT2,
                        List.of()), new Mkfs("mkfs.xfs", FileSystemType.XFS, List.of()),
                new Mkfs("mkfs.btrfs", FileSystemType.BTRFS, List.of()),
                new Mkfs("mkfs.vfat", FileSystemType.FAT32, List.of("mkfs.fat")),
                new Mkfs("mkfs.exfat", FileSystemType.EXFAT, List.of()),
                new Mkfs("mkfs.ntfs", FileSystemType.NTFS, List.of()));
    }

    /** The disk tools of the DOS family's prompts: Frames' diskpart and MC-DOS's FDISK. */
    static List<ICliCommand> dos() {
        return List.of(new Diskpart(), new DosFdisk());
    }

    @TextHolder
    static final class LinuxFdisk implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.disk.fdisk.summary",
                "list the partition tables of the disks");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.disk.fdisk.about", "With -l, prints each disk's"
                + " partition table, every partition with where it starts and ends, its size and its type. To change"
                + " a table, use the desktop's disk tool: the KDE Partition Manager or Disks.");
        private static final TextKey USAGE = TextKey.of("jsc.cli.disk.fdisk.usage", "usage: fdisk -l [device]");
        private static final TextKey DISK_LINE = TextKey.of("jsc.cli.disk.fdisk.disk_line",
                "Disk %s: %s, %s bytes, %s sectors");
        private static final TextKey MODEL = TextKey.of("jsc.cli.disk.fdisk.model", "Disk model: %s");
        private static final TextKey UNITS = TextKey.of("jsc.cli.disk.fdisk.units",
                "Units: sectors of 1 * 512 = 512 bytes");
        private static final TextKey LABEL_TYPE = TextKey.of("jsc.cli.disk.fdisk.label_type", "Disklabel type: %s");
        private static final TextKey HEADER_GPT = TextKey.of("jsc.cli.disk.fdisk.header_gpt",
                "Device          Start        End    Sectors  Size Type");
        private static final TextKey HEADER_DOS = TextKey.of("jsc.cli.disk.fdisk.header_dos",
                "Device     Boot      Start        End    Sectors  Size Id Type");
        private static final TextKey NO_DEVICE = TextKey.of("jsc.cli.disk.fdisk.no_device",
                "fdisk: cannot open %s: No such file or directory");

        @Override public CommandScope scope() {
            return CommandScope.on(Platform.LINUX);
        }

        @Override public String name() {
            return "fdisk";
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public List<Text> description() {
            return List.of(ABOUT.text());
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs() || !"-l".equals(ctx.arg(0))) {
                ctx.out().line(USAGE);
                return;
            }
            final DiskReport report = DiskToolLines.report(ctx.computer());
            final int only = ctx.argCount() > 1 ? DiskToolLines.linuxDiskOf(ctx.arg(1)) : -1;
            if (ctx.argCount() > 1 && report.disk(only) == null) {
                ctx.out().error(NO_DEVICE.with(ctx.arg(1)));
                return;
            }
            for (final DiskReport.Disk disk : report.disks()) {
                if (only >= 0 && disk.index() != only) {
                    continue;
                }
                print(ctx, disk);
            }
        }

        private static void print(final CliContext ctx, final DiskReport.Disk disk) {
            final String name = DiskToolLines.linuxDisk(disk.index());
            final boolean gpt = disk.scheme() == PartitionScheme.GPT;
            ctx.out().line(DISK_LINE.with(name, DiskToolLines.iec(disk.sizeMb()),
                    Long.toString(disk.sizeMb() * 1_048_576L), Long.toString(disk.sizeMb()
                            * DiskToolLines.SECTORS_PER_MB)));
            ctx.out().line(MODEL.with(disk.model()));
            ctx.out().line(UNITS);
            ctx.out().line(LABEL_TYPE.with(gpt ? "gpt" : "dos"));
            ctx.out().line("");
            ctx.out().line(gpt ? HEADER_GPT : HEADER_DOS);
            for (final DiskReport.Part part : disk.parts()) {
                final long start = DiskToolLines.startSector(part.startMb());
                final long sectors = part.sizeMb() * DiskToolLines.SECTORS_PER_MB;
                final String device = name + part.number();
                ctx.out().line(gpt
                        ? String.format(Locale.ROOT, "%-10s %10d %10d %10d %5s %s", device, start,
                        start + sectors - 1, sectors, DiskToolLines.human(part.sizeMb()), DiskToolLines.gptType(part))
                        : String.format(Locale.ROOT, "%-10s %4s %10d %10d %10d %5s %s", device,
                        part.active() ? "*" : "", start, start + sectors - 1, sectors,
                        DiskToolLines.human(part.sizeMb()), DiskToolLines.dosType(part)));
            }
            ctx.out().line("");
        }
    }

    @TextHolder
    static final class Lsblk implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.disk.lsblk.summary",
                "list the disks and their partitions");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.disk.lsblk.about", "Lists every disk and each of"
                + " its partitions as a tree, with its size and where it is mounted. With -f, each partition's file"
                + " system and label instead of its size.");
        private static final TextKey HEADER = TextKey.of("jsc.cli.disk.lsblk.header", "NAME     SIZE TYPE MOUNTPOINTS");
        private static final TextKey HEADER_FS = TextKey.of("jsc.cli.disk.lsblk.header_fs",
                "NAME   FSTYPE LABEL  MOUNTPOINTS");

        @Override public CommandScope scope() {
            return CommandScope.on(Platform.LINUX);
        }

        @Override public String name() {
            return "lsblk";
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public List<Text> description() {
            return List.of(ABOUT.text());
        }

        @Override public void run(final CliContext ctx) {
            boolean fs = false;
            int only = -1;
            for (final String arg : ctx.args()) {
                if ("-f".equals(arg)) {
                    fs = true;
                } else if (!arg.startsWith("-")) {
                    only = DiskToolLines.linuxDiskOf(arg);
                }
            }
            ctx.out().line(fs ? HEADER_FS : HEADER);
            for (final DiskReport.Disk disk : DiskToolLines.report(ctx.computer()).disks()) {
                if (only >= 0 && disk.index() != only) {
                    continue;
                }
                final String name = DiskToolLines.linuxDisk(disk.index()).substring(5);
                ctx.out().line(fs ? name : String.format(Locale.ROOT, "%-6s %6s disk", name,
                        DiskToolLines.human(disk.sizeMb())));
                for (int i = 0; i < disk.parts().size(); i++) {
                    final DiskReport.Part part = disk.parts().get(i);
                    final String branch = (i == disk.parts().size() - 1 ? "`-" : "|-") + name + part.number();
                    final String mount = mountOf(part, name, ctx.computer().era());
                    ctx.out().line(fs ? String.format(Locale.ROOT, "%-6s %-6s %-6s %s", branch,
                            DiskToolLines.lsblkType(part), part.label(), mount)
                            : String.format(Locale.ROOT, "%-6s %6s part %s", branch,
                            DiskToolLines.human(part.sizeMb()), mount));
                }
            }
        }

        /* Where Linux has a partition mounted: its root, the EFI one, its swap, and the others it reads. */
        private static String mountOf(final DiskReport.Part part, final String disk, final Era era) {
            final FileSystemType fs = part.fileSystem();
            if (part.mounted() && part.swap()) {
                return "[SWAP]";
            }
            if (part.mounted()) {
                return "/";
            }
            if (part.role() == PartitionRole.EFI) {
                return "/boot/efi";
            }
            if (fs == null || !fs.holdsData() || !fs.readBy(Platform.LINUX, era)) {
                return "";
            }
            return "/media/" + (part.label().isEmpty() ? disk + part.number()
                    : part.label().toLowerCase(Locale.ROOT));
        }
    }

    @TextHolder
    static final class Gpart implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.disk.gpart.summary",
                "show the partition tables of the disks");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.disk.gpart.about", "gpart show prints each disk's"
                + " partition table: where each partition starts and how long it is in sectors, its index, its type"
                + " and its size, the free space between them marked as free.");
        private static final TextKey USAGE = TextKey.of("jsc.cli.disk.gpart.usage", "usage: gpart show [geom]");

        @Override public CommandScope scope() {
            return CommandScope.on(Platform.FREEBSD);
        }

        @Override public String name() {
            return "gpart";
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public List<Text> description() {
            return List.of(ABOUT.text());
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs() || !"show".equals(ctx.arg(0))) {
                ctx.out().line(USAGE);
                return;
            }
            final int only = ctx.argCount() > 1 ? DiskToolLines.bsdDiskOf(ctx.arg(1)) : -1;
            for (final DiskReport.Disk disk : DiskToolLines.report(ctx.computer()).disks()) {
                if (only >= 0 && disk.index() != only) {
                    continue;
                }
                final long first = disk.scheme() == PartitionScheme.GPT ? 40L : 63L;
                final long sectors = disk.sizeMb() * DiskToolLines.SECTORS_PER_MB;
                ctx.out().line(String.format(Locale.ROOT, "=>%11d %11d  %s  %s  (%s)", first, sectors - first,
                        DiskToolLines.bsdDisk(disk.index()), disk.scheme().name(), DiskToolLines.human(disk.sizeMb())));
                // In the order they lie: each partition, and the free space between them as gpart's token marks it.
                final List<long[]> rows = new ArrayList<>();
                for (final DiskReport.Part part : disk.parts()) {
                    rows.add(new long[]{part.startMb(), part.sizeMb(), part.number()});
                }
                for (final DiskLayout.FreeSpace gap : disk.layout().free()) {
                    rows.add(new long[]{gap.startMb(), gap.sizeMb(), -1L});
                }
                rows.sort((a, b) -> Long.compare(a[0], b[0]));
                for (final long[] row : rows) {
                    final DiskReport.Part part = row[2] < 0 ? null : disk.part((int) row[2]);
                    ctx.out().line(String.format(Locale.ROOT, "%13d %11d %5s  %s  (%s)",
                            DiskToolLines.startSector(row[0]), row[1] * DiskToolLines.SECTORS_PER_MB,
                            part == null ? "" : Long.toString(row[2]),
                            part == null ? "- free -" : DiskToolLines.gpartType(part), DiskToolLines.human(row[1])));
                }
                ctx.out().line("");
            }
        }
    }

    @TextHolder
    static final class OpenBsdFdisk implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.disk.obsd_fdisk.summary",
                "show a disk's Master Boot Record");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.disk.obsd_fdisk.about", "Prints the disk's"
                + " Master Boot Record: its four entries, the one marked with a star the one that starts, and the"
                + " OpenBSD one, inside which disklabel keeps the system's own partitions.");
        private static final TextKey USAGE = TextKey.of("jsc.cli.disk.obsd_fdisk.usage", "usage: fdisk disk");
        private static final TextKey HEADER = TextKey.of("jsc.cli.disk.obsd_fdisk.header",
                "  #: id         LBA start          size ]");

        @Override public CommandScope scope() {
            return CommandScope.on(Platform.OPENBSD);
        }

        @Override public String name() {
            return "fdisk";
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public List<Text> description() {
            return List.of(ABOUT.text());
        }

        @Override public void run(final CliContext ctx) {
            final DiskReport.Disk disk = ctx.hasArgs()
                    ? DiskToolLines.report(ctx.computer()).disk(DiskToolLines.bsdDiskOf(ctx.arg(0))) : null;
            if (disk == null) {
                ctx.out().line(USAGE);
                return;
            }
            final long sectors = disk.sizeMb() * DiskToolLines.SECTORS_PER_MB;
            ctx.out().line(String.format(Locale.ROOT, "Disk: %s       geometry: %d/255/63 [%d Sectors]",
                    ctx.arg(0), sectors / (255L * 63L), sectors));
            ctx.out().line("Offset: 0       Signature: 0xAA55");
            ctx.out().line(HEADER);
            ctx.out().line("-".repeat(46));
            for (int entry = 0; entry < PartitionScheme.MBR.mostPartitions(); entry++) {
                final DiskReport.Part part = entry < disk.parts().size() ? disk.parts().get(entry) : null;
                // An entry nothing uses is fdisk's own token, Unused, which no language translates.
                if (part == null) {
                    ctx.out().line(String.format(Locale.ROOT, " %d: 00 %17d %13d ] Unused", entry, 0, 0));
                    continue;
                }
                final boolean openBsd = part.fileSystem() == FileSystemType.UFS
                        || part.fileSystem() == FileSystemType.SWAP;
                final String type = DiskToolLines.dosType(part);
                ctx.out().line(String.format(Locale.ROOT, "%s%d: %s %17d %13d ] %s", part.active() ? "*" : " ",
                        entry, openBsd ? "A6" : String.format(Locale.ROOT, "%2s", type.substring(0, 2).strip())
                                .replace(' ', '0').toUpperCase(Locale.ROOT),
                        DiskToolLines.startSector(part.startMb()), part.sizeMb() * DiskToolLines.SECTORS_PER_MB,
                        openBsd ? "OpenBSD" : type.substring(3)));
            }
        }
    }

    @TextHolder
    static final class Disklabel implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.disk.disklabel.summary",
                "show OpenBSD's own partitions on a disk");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.disk.disklabel.about", "With -h, prints the"
                + " disk's label in sizes a person reads: a: the root, b: the swap, c: the whole disk, and any other"
                + " partition with its file system.");
        private static final TextKey USAGE = TextKey.of("jsc.cli.disk.disklabel.usage", "usage: disklabel -h disk");
        private static final TextKey PARTITIONS = TextKey.of("jsc.cli.disk.disklabel.partitions", "16 partitions:");
        private static final TextKey HEADER = TextKey.of("jsc.cli.disk.disklabel.header",
                "#                size           offset  fstype");

        @Override public CommandScope scope() {
            return CommandScope.on(Platform.OPENBSD);
        }

        @Override public String name() {
            return "disklabel";
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public List<Text> description() {
            return List.of(ABOUT.text());
        }

        @Override public void run(final CliContext ctx) {
            final String named = ctx.args().stream().filter(arg -> !arg.startsWith("-")).findFirst().orElse("");
            final DiskReport.Disk disk = DiskToolLines.report(ctx.computer()).disk(DiskToolLines.bsdDiskOf(named));
            if (disk == null) {
                ctx.out().line(USAGE);
                return;
            }
            final long sectors = disk.sizeMb() * DiskToolLines.SECTORS_PER_MB;
            ctx.out().line("# /dev/r" + named + "c:");
            ctx.out().line("type: ESDI");
            ctx.out().line("disk: ESDI/IDE disk");
            ctx.out().line("label: " + disk.model());
            ctx.out().line("total sectors: " + sectors);
            ctx.out().line("");
            ctx.out().line(PARTITIONS);
            ctx.out().line(HEADER);
            // a: is the root, b: the swap, c: the whole disk; every other partition takes the next letter from d:.
            final List<String> rows = new ArrayList<>();
            char next = 'd';
            for (final DiskReport.Part part : disk.parts()) {
                final FileSystemType fs = part.fileSystem();
                final char letter = fs == FileSystemType.UFS && !part.system().isEmpty() ? 'a'
                        : fs == FileSystemType.SWAP ? 'b' : next++;
                rows.add(String.format(Locale.ROOT, "  %c: %16s %16d  %s%s", letter,
                        DiskToolLines.human(part.sizeMb()), DiskToolLines.startSector(part.startMb()),
                        fs == FileSystemType.UFS ? "4.2BSD" : fs == FileSystemType.SWAP ? "swap"
                                : fs == null ? "unknown" : fs.displayName(), letter == 'a' ? " # /" : ""));
            }
            rows.add(String.format(Locale.ROOT, "  c: %16s %16d  unused", DiskToolLines.human(disk.sizeMb()), 0));
            rows.sort(String::compareTo);
            rows.forEach(ctx.out()::line);
        }
    }

    /**
     * Linux's mkfs, one command for each file system it makes: the partition named is formatted with it, emptied of
     * what it held; the partition the system runs from, or a swap in use, is refused as the real one refuses it.
     */
    @TextHolder
    static final class Mkfs implements ICliCommand {

        private final String name;
        private final FileSystemType fs;
        private final List<String> aliases;

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.disk.mkfs.summary",
                "make a file system on a partition (erases it)");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.disk.mkfs.about", "Formats the partition named,"
                + " /dev/sdb1 for the first of the second disk, with the file system the command is named for:"
                + " everything on it is lost. A partition the running system uses is refused. lsblk lists them.");
        private static final TextKey USAGE = TextKey.of("jsc.cli.disk.mkfs.usage", "usage: %s /dev/<partition>");
        private static final TextKey NO_DEVICE = TextKey.of("jsc.cli.disk.mkfs.no_device",
                "The file %s does not exist and no size was specified.");
        private static final TextKey MOUNTED = TextKey.of("jsc.cli.disk.mkfs.mounted",
                "%s is mounted; will not make a filesystem here!");
        private static final TextKey MADE = TextKey.of("jsc.cli.disk.mkfs.made",
                "Creating %s filesystem on %s ... done");

        Mkfs(final String name, final FileSystemType fs, final List<String> aliases) {
            this.name = name;
            this.fs = fs;
            this.aliases = List.copyOf(aliases);
        }

        @Override public CommandScope scope() {
            return CommandScope.on(Platform.LINUX);
        }

        @Override public String name() {
            return this.name;
        }

        @Override public List<String> aliases() {
            return this.aliases;
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public List<Text> description() {
            return List.of(ABOUT.text());
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().line(USAGE.with(this.name));
                return;
            }
            final String device = ctx.arg(0);
            final int disk = DiskToolLines.linuxDiskOf(device);
            final int number = DiskToolLines.linuxPartitionOf(device);
            final DiskReport.Disk found = DiskToolLines.report(ctx.computer()).disk(disk);
            final DiskReport.Part part = found == null ? null : found.part(number);
            // The tool's own banner, a name and a version, which read the same in every language.
            ctx.out().dim(this.fs.name().startsWith("EXT") ? "mke2fs 1.47 (JSC)" : this.name + " (JSC)");
            if (part == null) {
                ctx.out().error(NO_DEVICE.with(device));
                return;
            }
            if (part.mounted()) {
                ctx.out().error(MOUNTED.with(device));
                return;
            }
            final DiskReport.Refusal refusal = DiskToolLines.ask(ctx.computer(),
                    DiskCommand.format(disk, number, this.fs.serializedName())).refusal();
            if (refusal != null) {
                ctx.out().error(CliTexts.SAID_BY.with(this.name, DiskTexts.reason(refusal)));
                return;
            }
            ctx.out().ok(MADE.with(this.fs.displayName(), device));
        }
    }

    @TextHolder
    static final class Diskpart implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.disk.diskpart.summary",
                "change the disks' partitions at the prompt");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.disk.diskpart.about", "Opens DiskPart, which"
                + " asks for its commands at DISKPART>: list disk, select disk, list partition, select partition,"
                + " create partition primary, delete partition, format, extend, shrink, active and convert. exit"
                + " leaves it.");

        @Override public CommandScope scope() {
            return CommandScope.on(Platform.FRAMES);
        }

        @Override public String name() {
            return "diskpart";
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public List<Text> description() {
            return List.of(ABOUT.text());
        }

        @Override public void run(final CliContext ctx) {
            ctx.out().start(new DiskpartProcess(ctx.computer()));
        }
    }

    @TextHolder
    static final class DosFdisk implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.disk.dos_fdisk.summary",
                "set up the fixed disk's partitions");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.disk.dos_fdisk.about", "Opens the Fixed Disk"
                + " Setup Program: create a DOS partition, set the active partition, the one the machine starts,"
                + " delete a partition, or display them.");

        @Override public CommandScope scope() {
            return CommandScope.on(Platform.MC_DOS);
        }

        @Override public String name() {
            return "fdisk";
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public List<Text> description() {
            return List.of(ABOUT.text());
        }

        @Override public void run(final CliContext ctx) {
            ctx.out().start(new DosFdiskProcess(ctx.computer()));
        }
    }

    @TextHolder
    static final class UnixFdisk implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.disk.unix_fdisk.summary",
                "set up the disk's partitions and the one that boots");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.disk.unix_fdisk.about", "Prints the disk's"
                + " partitions and offers to create one, change the active one, the one the machine boots from, or"
                + " delete one. Nothing is written until Exit; Cancel leaves the disk as it was.");

        @Override public CommandScope scope() {
            return CommandScope.on(Platform.UNIX);
        }

        @Override public String name() {
            return "fdisk";
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public List<Text> description() {
            return List.of(ABOUT.text());
        }

        @Override public void run(final CliContext ctx) {
            ctx.out().start(new UnixFdiskProcess(ctx.computer()));
        }
    }
}
