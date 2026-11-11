/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.program.cli.man.CommandGuidePages;
import dev.jstech.core.guide.GuideCell;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.ModGuide;

/**
 * The pages on what a disk holds inside (computers/docs/design/storage-devices.md): its partition table and the file
 * system of each partition, the tools that change them on every system, two systems on one disk and how each starts,
 * btrfs and ZFS snapshots, ZFS mirrors, and the swap and the page file that stretch the memory.
 */
final class PartitionsPages {

    private PartitionsPages() {
    }

    /** Declares the section and its entries, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("disks_and_partitions").titled("Disks and partitions")
                .icon(HardwareItems.DISK_TRENCH_20M).in(chapter).register();
        partitions(guide, section);
        diskTools(guide, section);
        twoSystems(guide, section);
        snapshots(guide, section);
        zfsMirrors(guide, section);
        swap(guide, section);
    }

    private static void partitions(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("partitions", section).titled("Partitions and file systems").icon(HardwareItems.DISK_TRENCH_20M)
                .paragraph("A disk can be divided into partitions. Each partition is a part of the disk with a file"
                        + " system of its own, and each one that holds a file system is a volume of its own: Frames"
                        + " gives it a drive letter, Linux a name such as /dev/sdb1, and the network keeps items on"
                        + " it as on a disk.")
                .paragraph("A disk nobody has divided reads as one partition over all of it. Installing a system,"
                        + " or a disk tool, writes the table that lists the partitions.")
                .define("Partition table", "The list at the start of a disk that says where each partition begins"
                        + " and ends.")
                .table("The two kinds of table", "Table", "Written on", "What it holds")
                .row(GuideCell.words("Master Boot Record (MBR)"), GuideCell.words("Vintage to Transition"),
                        GuideCell.words("Four partitions at most, in the first 2 TB of the disk. One of them is"
                                + " active: the one the machine starts."))
                .row(GuideCell.words("GUID Partition Table (GPT)"), GuideCell.words("Standard and later"),
                        GuideCell.words("As many partitions as the disk has room for, over all of it."))
                .paragraph("A machine that starts with UEFI, from the Standard on, reads its systems from an EFI"
                        + " partition: 512 MB of FAT32 that every installer makes first on an empty GPT disk. It"
                        + " holds what starts the systems, and it is never a volume.")
                .define("EFI partition", "The small FAT32 partition a UEFI machine starts its systems from.")
                .subheading("File systems")
                .paragraph("Each partition is formatted with one file system. A system reads only the file"
                        + " systems it knows, and a machine makes only those of its era or older.")
                .table("The file systems", "File system", "From", "Read by", "Resizing")
                .row(GuideCell.code("FAT12, FAT16"), GuideCell.words("Vintage"), GuideCell.words("Every system"),
                        GuideCell.words("Unmounted only"))
                .row(GuideCell.code("FAT32"), GuideCell.words("Legacy"), GuideCell.words("Every system"),
                        GuideCell.words("Unmounted only"))
                .row(GuideCell.code("NTFS"), GuideCell.words("Legacy"),
                        GuideCell.words("Frames; Linux from the Transition"), GuideCell.words("Grows and shrinks"
                                + " while in use"))
                .row(GuideCell.code("exFAT"), GuideCell.words("Transition"),
                        GuideCell.words("Frames; Linux from the Advanced"), GuideCell.words("Unmounted only"))
                .row(GuideCell.code("ext2, ext3"), GuideCell.words("Legacy"), GuideCell.words("Linux"),
                        GuideCell.words("Grows in use; shrinks unmounted"))
                .row(GuideCell.code("ext4"), GuideCell.words("Transition"), GuideCell.words("Linux"),
                        GuideCell.words("Grows in use; shrinks unmounted"))
                .row(GuideCell.code("XFS"), GuideCell.words("Transition"), GuideCell.words("Linux"),
                        GuideCell.words("Only grows"))
                .row(GuideCell.code("btrfs"), GuideCell.words("Standard"), GuideCell.words("Linux"),
                        GuideCell.words("Grows and shrinks while in use"))
                .row(GuideCell.code("UFS"), GuideCell.words("Vintage"),
                        GuideCell.words("UNIX, FreeBSD, OpenBSD"), GuideCell.words("Unmounted only"))
                .row(GuideCell.code("ZFS"), GuideCell.words("Transition"), GuideCell.words("FreeBSD"),
                        GuideCell.words("Only grows"))
                .paragraph("A FAT16 partition holds 2 GB at most, and a file on FAT holds 4 GB at most. btrfs and"
                        + " ZFS keep snapshots ([](jsc:snapshots)), and ZFS mirrors a pool over two disks"
                        + " ([](jsc:zfs_mirrors)). A swap partition holds no file system: it is where the memory"
                        + " overflows to ([](jsc:swap)).")
                .ifSomethingGoesWrong("A partition does not show up as a drive.",
                        "The running system does not read its file system. Format it with one the system reads.",
                        "The tool cannot make the file system you picked.",
                        "It is of a later era than the machine.")
                .seeAlso("jsc:disks", "jsc:disk_tools_of_each_system", "jsc:local_storage")
                .register();
    }

    private static void diskTools(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("disk_tools_of_each_system", section).titled("The disk tools").icon(HardwareItems.DISK_TRENCH_20M)
                .paragraph("Every system has its own tools to make, format, resize and delete partitions. They all"
                        + " read the same disks, so they never disagree about them.")
                .table("The tools by system", "System", "In a window", "At the prompt")
                .row(GuideCell.words("MC-DOS"), GuideCell.words("-"), GuideCell.code("FDISK"))
                .row(GuideCell.words("Frames XP to 11"), GuideCell.words("Disk Management"),
                        GuideCell.code("diskpart"))
                .row(GuideCell.words("Linux with KDE"), GuideCell.words("KDE Partition Manager"),
                        GuideCell.code("fdisk -l, lsblk, mkfs"))
                .row(GuideCell.words("Other Linux desktops"), GuideCell.words("Disks"),
                        GuideCell.code("fdisk -l, lsblk, mkfs"))
                .row(GuideCell.words("UNIX System V"), GuideCell.words("-"), GuideCell.code("fdisk"))
                .row(GuideCell.words("FreeBSD"), GuideCell.words("-"), GuideCell.code("gpart show"))
                .row(GuideCell.words("OpenBSD"), GuideCell.words("-"), GuideCell.code("fdisk, disklabel -h"))
                .paragraph("Disk Management shows each volume in a list and each disk as a bar. Right-click a"
                        + " volume to format, shrink, extend or delete it, and free space to make a new one. On"
                        + " Frames XP it cannot resize a volume. The KDE Partition Manager keeps your changes as"
                        + " pending operations, and writes them only when you press Apply. Disks shows one disk at"
                        + " a time, with its volumes as a bar.")
                .subheading("What every tool refuses")
                .paragraph("The partition the running system runs from, and a swap it swaps to, are never deleted or"
                        + " formatted. A partition never shrinks below what it holds, and each file system resizes"
                        + " by its own rules, in the table on [](jsc:partitions): an ext4 partition in use only"
                        + " grows. A disk changes from MBR to GPT, or back, only when it is empty, and a new"
                        + " partition table erases the whole disk.")
                .steps("Open Disk Management, or the tool of your system.",
                        "Right-click the volume, and choose Shrink Volume.",
                        "Type how many megabytes to free, and press Shrink.",
                        "Right-click the free space, and make a new volume in it.")
                .subheading("At the prompt")
                .paragraph("FDISK on MC-DOS and fdisk on UNIX open a menu: make a partition, set the active one,"
                        + " delete one, or show them. diskpart on Frames takes commands at its own prompt: list disk,"
                        + " select disk 1, then create partition primary, format fs=ntfs quick, and exit. On Linux,"
                        + " mkfs.ext4 /dev/sdb1 formats one partition, and refuses the one the system runs from.")
                .session("jsc:guide/terminal/bash", "Ubuntu")
                .typed("player@pc:~$ ", "mkfs.ext4 /dev/sda1")
                .answer("/dev/sda1 is mounted; will not make a filesystem here!")
                .ifSomethingGoesWrong("The tool says the volume is in use.",
                        "It is the system's own partition, or its swap. Change it from a live medium or another"
                                + " system.",
                        "Convert to GPT is greyed out.",
                        "The disk still has partitions. Delete them first, or write a new table, which erases"
                                + " the disk.")
                .command(CommandGuidePages.id(Platform.FRAMES, "diskpart"))
                .command(CommandGuidePages.id(Platform.MC_DOS, "fdisk"))
                .command(CommandGuidePages.id(Platform.LINUX, "fdisk"))
                .command(CommandGuidePages.id(Platform.LINUX, "lsblk"))
                .command(CommandGuidePages.id(Platform.LINUX, "mkfs.ext4"))
                .command(CommandGuidePages.id(Platform.UNIX, "fdisk"))
                .command(CommandGuidePages.id(Platform.FREEBSD, "gpart"))
                .command(CommandGuidePages.id(Platform.OPENBSD, "disklabel"))
                .seeAlso("jsc:partitions", "jsc:two_systems_on_a_disk")
                .register();
    }

    private static void twoSystems(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("two_systems_on_a_disk", section).titled("Two systems on one disk")
                .icon(HardwareItems.DISK_TRENCH_20M)
                .paragraph("Each installer asks where the system goes, the way its real one did, and can put it"
                        + " beside another system on the same disk.")
                .table("Where each installer puts its system", "Installer", "What it asks")
                .row(GuideCell.words("MC-DOS, UNIX System V, Frames 95"),
                        GuideCell.words("Nothing: it takes the disk and formats it."))
                .row(GuideCell.words("Frames XP"), GuideCell.words("A list of the partitions and the free"
                        + " space, in text mode; then how to format the one you pick."))
                .row(GuideCell.words("Frames 7 to 11"), GuideCell.words("A table of the partitions, with New,"
                        + " Delete, Format and Extend under it."))
                .row(GuideCell.words("Ubuntu, Linux Mint"), GuideCell.words("Install alongside the other system:"
                        + " a divider shrinks its partition, and the arrow keys move it."))
                .row(GuideCell.words("Debian, Fedora"), GuideCell.words("Each disk with its partitions, and where"
                        + " the system will go."))
                .row(GuideCell.words("FreeBSD"), GuideCell.words("Auto (ZFS), with a mirror on a second disk if"
                        + " you want one, or Auto (UFS)."))
                .paragraph("On a UEFI machine the installer makes the EFI partition first, when the disk has none."
                        + " On Linux, the BSDs and UNIX it also makes a swap partition as big as the memory, when"
                        + " there is room for one ([](jsc:swap)).")
                .subheading("Choosing what starts")
                .paragraph("With two systems on a machine, something has to ask which one starts. On the Standard"
                        + " and later, the GRUB menu of a Linux system lists every other system, and the Frames"
                        + " Boot Manager of the EFI partition; the Midsoft Boot Manager lists each Frames with its"
                        + " partition. The firmware's boot menu, opened with F12 as the machine starts, has one"
                        + " line for every system on every partition.")
                .paragraph("On the Vintage, the disk starts its active partition, and nothing asks. To start the"
                        + " other system, make its partition the active one: option 2 of FDISK on MC-DOS, or of"
                        + " fdisk on UNIX.")
                .steps("Install the first system, and leave free space on the disk, or let the second installer"
                        + " shrink the first.",
                        "Start the second installer, and choose the free space, or Install alongside.",
                        "After the copy, restart. Pick the system in the boot menu, or set the active partition on"
                                + " the Vintage.")
                .ifSomethingGoesWrong("The installer says there is no room.",
                        "Free space on the disk first, by shrinking a volume with a disk tool.",
                        "An MBR disk refuses a new partition.",
                        "It already holds four. Delete one, or use a GPT disk.")
                .seeAlso("jsc:disk_tools_of_each_system", "jsc:partitions")
                .register();
    }

    private static void snapshots(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("snapshots", section).titled("Snapshots").icon(HardwareItems.DISK_TRENCH_20M)
                .paragraph("btrfs and ZFS can keep a snapshot of a partition: its items and its files exactly as"
                        + " they are. Rolling back returns the partition there, which undoes a corruption and"
                        + " anything you lost since.")
                .paragraph("A snapshot is taken at once and weighs nothing. As the partition changes, it weighs"
                        + " what changed since it was taken, and that takes room on the disk like any items do."
                        + " Delete a snapshot you no longer need, to give the room back.")
                .define("Snapshot", "A partition kept as it was at one moment, sharing everything that did not"
                        + " change since.")
                .subheading("btrfs, on Linux")
                .paragraph("btrfs subvolume snapshot takes one under a path you name. The rollback waits for the"
                        + " next start: set-default picks the snapshot, and the machine mounts it as / when it"
                        + " starts again.")
                .session("jsc:guide/terminal/bash", "Fedora")
                .typed("[player@fedora ~]$ ", "btrfs subvolume snapshot / /.snapshots/day4")
                .answer("Create a snapshot of '/' in '/.snapshots/day4'")
                .typed("[player@fedora ~]$ ", "btrfs subvolume list /")
                .answer("ID 256 gen 1842 top level 5 path root",
                        "ID 257 gen 1842 top level 5 path home",
                        "ID 258 gen 1842 top level 5 path .snapshots/day4")
                .typed("[player@fedora ~]$ ", "btrfs subvolume set-default 258 /")
                .paragraph("btrfs filesystem du -s shows what a snapshot keeps, and in Exclusive what only it"
                        + " keeps: the room that deleting it gives back. Another partition is named by its device,"
                        + " such as /dev/sdb1.")
                .subheading("ZFS, on FreeBSD")
                .paragraph("zfs snapshot takes one of a pool, named after an at sign, and zfs rollback returns to"
                        + " it at once. While later snapshots are kept, rollback refuses, and -r lets them go.")
                .session("jsc:guide/terminal/sh", "FreeBSD")
                .typed("root@freebsd:~ # ", "zfs snapshot zroot/usr/home@day4")
                .typed("root@freebsd:~ # ", "zfs list -t snapshot")
                .answer("NAME                    USED  AVAIL  REFER  MOUNTPOINT",
                        "zroot/usr/home@day4       0B      -  41.2G  -")
                .typed("root@freebsd:~ # ", "zfs rollback zroot/usr/home@day4")
                .paragraph("The system's own pool is zroot; another is named by its partition's label.")
                .ifSomethingGoesWrong("The disk fills up after a snapshot.",
                        "It keeps what you took out since. Delete the snapshots you do not need.",
                        "The command says the partition is not btrfs.",
                        "Only btrfs and ZFS keep snapshots. Name the partition by its device if it is not the"
                                + " root.")
                .command(CommandGuidePages.id(Platform.LINUX, "btrfs"))
                .command(CommandGuidePages.id(Platform.FREEBSD, "zfs"))
                .seeAlso("jsc:zfs_mirrors", "jsc:corruption_and_the_defrag")
                .register();
    }

    private static void zfsMirrors(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("zfs_mirrors", section).titled("ZFS mirrors").icon(HardwareItems.DISK_TRENCH_20M)
                .paragraph("A ZFS pool can live on two disks at once, each holding the same. When corruption"
                        + " damages part of the pool, ZFS repairs it from the other disk: as soon as that part is"
                        + " read, and when you scrub the pool, which reads all of it.")
                .define("Mirror", "A pool held whole on two disks, so either repairs the other.")
                .define("Scrub", "Reading a whole pool to find and repair what is damaged.")
                .paragraph("FreeBSD's installer makes a mirror: choose Auto (ZFS), then the second disk in the"
                        + " ZFS Configuration step. The second disk is no volume of its own; it is what the pool"
                        + " repairs from.")
                .session("jsc:guide/terminal/sh", "FreeBSD")
                .typed("root@freebsd:~ # ", "zpool scrub zroot")
                .typed("root@freebsd:~ # ", "zpool status zroot")
                .answer("  pool: zroot",
                        " state: ONLINE",
                        "  scan: scrub repaired 8M in 00:00:00 with 0 errors on Day 4 10:12",
                        "config:",
                        "",
                        "        NAME        STATE     READ WRITE CKSUM",
                        "        zroot       ONLINE       0     0     0",
                        "          mirror-0  ONLINE       0     0     0",
                        "            ada0p4  ONLINE       0     0     2  (repaired)",
                        "            ada1p4  ONLINE       0     0     0",
                        "",
                        "errors: No known data errors")
                .paragraph("CKSUM counts how many times the pool found damage and repaired it. The scan line says"
                        + " what the last scrub repaired, and when.")
                .paragraph("Without its second disk the pool is DEGRADED: it keeps working, finds the damage, and"
                        + " has nothing to repair it from. The Defrag is then the only cure, or a snapshot.")
                .ifSomethingGoesWrong("zpool status says DEGRADED.",
                        "The second disk is not in the machine, or it died. Put it back.",
                        "errors lists data errors after a scrub.",
                        "The pool has no second disk to repair from. Run the Defrag, or roll back to a snapshot.")
                .command(CommandGuidePages.id(Platform.FREEBSD, "zpool"))
                .seeAlso("jsc:snapshots", "jsc:corruption_and_the_defrag")
                .register();
    }

    private static void swap(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("swap", section).titled("Swap and the page file").icon(HardwareItems.DISK_TRENCH_20M)
                .paragraph("When the programs want more memory than the machine has, the system lends them disk"
                        + " space instead: the swap on Linux, the BSDs and UNIX, and the page file on Frames. A"
                        + " machine promises its memory and its swap together, so more programs fit.")
                .paragraph("The part of a program that sits in the swap runs slower, by the disk it is on.")
                .table("How fast the part in the swap runs", "Disk", "Speed")
                .row(GuideCell.words("Hard disk"), GuideCell.words("A quarter of its speed"))
                .row(GuideCell.words("SSD"), GuideCell.words("60% of its speed"))
                .row(GuideCell.words("NVMe"), GuideCell.words("90% of its speed"))
                .paragraph("Frames sizes its page file as it starts: one and a half times the memory on Frames 95"
                        + " and XP, and the same as the memory from Frames 7 on. The page file weighs on the"
                        + " system's disk like its files do. The other systems swap to a swap partition, which"
                        + " their installers make as big as the memory.")
                .define("Swap", "Disk space a system lends its programs when the memory runs out.")
                .define("Page file", "The swap of Frames: a file on the system's disk.")
                .subheading("Where to see it")
                .table("The swap on each system", "System", "Where")
                .row(GuideCell.words("Frames XP"), GuideCell.words("The Task Manager's Performance tab: PF Usage,"
                        + " and Commit in the status bar"))
                .row(GuideCell.words("Frames 10 and 11"), GuideCell.words("The Task Manager's Memory page, with what"
                        + " each program has in the page file"))
                .row(GuideCell.words("Linux desktops"), GuideCell.words("The System Monitor's swap line"))
                .row(GuideCell.words("Linux"), GuideCell.code("free -h, swapon --show, top"))
                .row(GuideCell.words("FreeBSD"), GuideCell.code("swapinfo, sysctl vm.swap_total"))
                .session("jsc:guide/terminal/bash", "Ubuntu")
                .typed("player@pc:~$ ", "swapon --show")
                .answer("NAME      TYPE      SIZE USED PRIO", "/dev/sda2 partition    4G   0B   -2")
                .ifSomethingGoesWrong("Programs run slowly when many are open.",
                        "Part of them is in the swap. Add memory, or put the swap on a faster disk.",
                        "A program is refused for want of memory.",
                        "The memory and the swap are both promised. Close a program, or add memory.")
                .command(CommandGuidePages.id(Platform.LINUX, "free"))
                .command(CommandGuidePages.id(Platform.LINUX, "swapon"))
                .command(CommandGuidePages.id(Platform.FREEBSD, "swapinfo"))
                .seeAlso("jsc:memory", "jsc:system_monitor_and_task_manager")
                .register();
    }
}
