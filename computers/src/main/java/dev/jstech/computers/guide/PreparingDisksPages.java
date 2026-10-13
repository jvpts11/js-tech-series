/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import dev.jstech.core.guide.GuideCell;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.ModGuide;

/**
 * The pages of the Linux by Hand chapter on preparing the disks: looking at them with lsblk, dividing them with
 * fdisk, giving the partitions their file systems, mounting them, and what each disk error means.
 */
final class PreparingDisksPages {

    private PreparingDisksPages() {
    }

    /** Declares the sections and entries of the disk preparation chapter, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("disk_tools").titled("Preparing the disks")
                .in(chapter).register();
        seeingTheDisks(guide, section);
        partitioningWithFdisk(guide, section);
        filesystemsAndMkfs(guide, section);
        mounting(guide, section);
        whenAToolRefuses(guide, section);
    }

    private static void seeingTheDisks(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("seeing_the_disks", section).titled("Seeing the disks (lsblk)")
                .paragraph("Before you change anything on a disk, look at it. The command lsblk lists every disk"
                        + " the computer has, and every partition on those disks, in one table. It answers at once"
                        + " and changes nothing.")
                .paragraph("You use it at three points: first to find your disk, then after you have divided it,"
                        + " and then after you have made the file systems, to check that each step did what you"
                        + " meant. lsblk needs no words after its name.")
                .table("The columns of lsblk", "Column", "What it tells you")
                .row(GuideCell.code("NAME"), GuideCell.words("The name of the device, such as sda for a disk or"
                        + " sda2 for its second partition."))
                .row(GuideCell.code("MAJ:MIN"), GuideCell.words("Two numbers the kernel uses to tell devices apart,"
                        + " which you can ignore."))
                .row(GuideCell.code("RM"), GuideCell.words("Whether the device can be removed; every row here"
                        + " shows 0."))
                .row(GuideCell.code("SIZE"), GuideCell.words("How big the device is: 512M is 512 megabytes, 1G is"
                        + " 1 gigabyte."))
                .row(GuideCell.code("RO"), GuideCell.words("1 when the device is read-only: the live medium is, and"
                        + " your disks are not."))
                .row(GuideCell.code("TYPE"), GuideCell.words("disk for a whole disk, part for a partition, and loop"
                        + " for the live medium."))
                .row(GuideCell.code("MOUNTPOINTS"), GuideCell.words("Where the device is mounted, or none when it"
                        + " is not."))
                .paragraph("Sizes are written with M for megabytes, G for gigabytes and T for terabytes. A decimal"
                        + " place appears only when it is needed: 512M, 1G, 1.5G, 4T.")
                .subheading("Reading the names")
                .paragraph("A disk takes its name from the slot it sits in, not from how many disks you have. The"
                        + " first slot is sda, the second sdb, and so on. An empty slot still takes its letter, so a"
                        + " disk alone in the third slot is sdc. The computer's own slots come first, then the bay"
                        + " on the front of the case ([](jsc:case_front)).")
                .paragraph("Read the name from lsblk and use that exact name in every later command. Your disk may"
                        + " not be sda.")
                .subheading("A disk with partitions")
                .paragraph("A partition is a part of a disk that holds its own file system. lsblk lists each"
                        + " partition under its disk. A vertical bar and a dash mark every partition except the last,"
                        + " and the last one has a back-quote and a dash instead. A disk nobody has divided shows no"
                        + " partition rows at all.")
                .session("jsc:guide/terminal/bash", "Arch")
                .typed("root@archiso ~ # ", "lsblk")
                .answer("NAME      MAJ:MIN RM   SIZE RO TYPE MOUNTPOINTS",
                        "loop0     7:0      0  1126M  1 loop /run/archiso/airootfs",
                        "sda       8:0      0     1G  0 disk",
                        "|-sda1    8:1      0   512M  0 part /mnt/boot",
                        "`-sda2    8:2      0   512M  0 part /mnt")
                .paragraph("Here your disk is sda, 1G in size, and it is divided into two partitions. You made them"
                        + " with fdisk in the next entry. The MOUNTPOINTS column shows where the new system is"
                        + " attached: sda2 is the root filesystem, mounted at /mnt, and sda1 is mounted at"
                        + " /mnt/boot. The loop0 row is the live medium you are working from, read-only, and it is"
                        + " not a disk you install onto.")
                .define("Device", "A disk or a partition the system can read and write, named as lsblk prints it.")
                .define("Partition table", "The list kept at the start of a disk that says where each partition"
                        + " begins and ends.")
                .define("File system", "The way a partition stores its files: a table of names, folders and data."
                        + " Each partition holds one, and a tool named mkfs makes an empty one.")
                .define("Mount", "To attach a file system to a folder, so that its files appear inside that"
                        + " folder.")
                .define("Mount point", "The folder that a file system is attached to.")
                .define("Root filesystem", "The file system the whole new system lives in. The system sees it as"
                        + " its top folder, /.")
                .ifSomethingGoesWrong("Your disk is not in the lsblk table.",
                        "Only a disk in one of the computer's disk slots, or in its hot-swap bay, is a device."
                                + " Put the disk there and run lsblk again.",
                        "The disk has a different name from the one you expected.",
                        "The letter follows the slot the disk sits in. Use the name lsblk prints for it in every"
                                + " later command.")
                .seeAlso("jsc:disks", "jsc:case_front", "jsc:server_racks")
                .register();
    }

    private static void partitioningWithFdisk(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("partitioning_with_fdisk", section).titled("Dividing a disk (fdisk)")
                .paragraph("fdisk is the editor that writes a disk's partition table: the list of where each"
                        + " partition begins and ends. You open it on a whole disk, and it asks you what to do next.")
                .paragraph("The editor works in memory. Nothing reaches the disk until you type w, so you can try"
                        + " things and throw them away. Leaving with q, or with [[Ctrl]]+[[C]], drops everything you"
                        + " typed since the editor opened.")
                .warning("Writing with w replaces the disk's partition table, and the partitions it held before are"
                        + " gone.")
                .table("The commands of fdisk", "Letter", "What it does")
                .row(GuideCell.code("m"), GuideCell.words("Prints the menu of commands; help does the same."))
                .row(GuideCell.code("g"), GuideCell.words("Makes a new, empty GUID Partition Table (GPT), and"
                        + " removes the partitions shown."))
                .row(GuideCell.code("n"), GuideCell.words("Adds a partition, by a size or by three questions."))
                .row(GuideCell.code("t"), GuideCell.words("Changes the type of a partition."))
                .row(GuideCell.code("d"), GuideCell.words("Deletes a partition."))
                .row(GuideCell.code("p"), GuideCell.words("Prints the table: the disk, its label and its"
                        + " partitions."))
                .row(GuideCell.code("w"), GuideCell.words("Writes the table to the disk and leaves the editor."))
                .row(GuideCell.code("q"), GuideCell.words("Leaves the editor without writing anything."))
                .subheading("Making a new table (g)")
                .paragraph("A new disk has no partition table, and fdisk says so when you open it. Type g to make a"
                        + " GPT, the modern kind that UEFI (Unified Extensible Firmware Interface) machines read."
                        + " The old kind, which p prints as dos, is not one this editor can make.")
                .paragraph("The line that g prints shows the disk's GUID, a long identifier that stays the same for"
                        + " that disk. Only g makes a new GPT, although a disk that already has partitions may be"
                        + " GPT as well.")
                .subheading("Making a partition (n)")
                .paragraph("With a size on the line, n makes the partition at once. Type n +512M for 512 megabytes,"
                        + " or n +1G for a gigabyte. A plain number, such as \"n 1024\", also means megabytes.")
                .paragraph("Typed alone, n asks three questions in turn. For the first two, press [[Enter]]: the"
                        + " editor picks the number and the starting sector for you. The third asks where the"
                        + " partition ends, and the question shows the range. On a 1G disk it reads:")
                .paragraph("\"Last sector, +/-sectors or +/-size{K,M,G,T,P} (2048-2097118, default 2095103):\"")
                .paragraph("Press [[Enter]] to take the rest of the disk, or type a size such as +512M. A sector is"
                        + " 512 bytes, and a megabyte is 2,048 sectors, so the question also takes a sector number.")
                .paragraph("When the partition is made, the editor says so: \"Created a new partition 1 of type"
                        + " 'Linux filesystem' and of size 512 MiB.\" Every new partition starts as a Linux filesystem."
                        + " You change it with t, when it is to be the EFI (Extensible Firmware Interface) system"
                        + " partition.")
                .subheading("Choosing the type (t)")
                .table("The words that set a type", "Type", "Words you can type")
                .row(GuideCell.words("EFI System"), GuideCell.code("1, uefi, efi, ef"))
                .row(GuideCell.words("Linux filesystem"), GuideCell.code("20, linux, 83"))
                .paragraph("Type L to see the types the editor knows. It prints two rows, EFI System and Linux"
                        + " filesystem, and then asks again. With one partition on the disk, t selects it and asks"
                        + " for the type. With several, it asks which partition first, and Enter picks the last one.")
                .paragraph("When the change works, the editor prints the old and new type, exactly as it writes"
                        + " them: \"Changed type of partition 'Linux filesystem' to 'EFI System'.\"")
                .subheading("Printing, deleting and writing (p, d, w)")
                .paragraph("p prints the table as the editor sees it, before anything is written. It starts with"
                        + " the disk's size, and then one row for each partition. The rows show Device, Start, End,"
                        + " Sectors, Size and Type, in that order.")
                .paragraph("d deletes a partition and renumbers the rest from partition 1. Then the editor prints"
                        + " \"Partition 2 has been deleted.\", with the number of the one you chose.")
                .paragraph("w stores the table on the disk. The editor then says that the partition table has been"
                        + " altered, and that it is syncing the disks. After that the partitions show in lsblk, and"
                        + " mkfs can use them.")
                .subheading("The steps for a UEFI disk")
                .steps("Type fdisk /dev/sda and press [[Enter]].",
                        "Type g and press [[Enter]], to make a GPT.",
                        "Type n +512M and press [[Enter]], for the first partition.",
                        "Type t and press [[Enter]], then type 1 for the EFI system partition.",
                        "Type n and press [[Enter]] three times, to make the second partition from the rest of the"
                                + " disk.",
                        "Type w and press [[Enter]], to write the table and leave.")
                .define("GPT", "The modern kind of partition table, which UEFI machines read.")
                .define("GUID", "A globally unique identifier: a long code, which looks random, that a partition"
                        + " table or a partition is given so that it can be told apart from others.")
                .define("Sector", "The smallest piece of a disk that the table counts in: 512 bytes.")
                .define("EFI system partition", "A small partition that the firmware reads to find the boot"
                        + " loader. Only UEFI machines need one.")
                .ifSomethingGoesWrong("fdisk: bad usage",
                        "You did not name a disk. Type fdisk /dev/sda, with the name lsblk gave your disk.",
                        "fdisk: cannot open /dev/sda2: No such file or directory",
                        "fdisk opens a whole disk only, not a partition. Use the disk's name, such as sda.",
                        "Device does not contain a recognized partition table.",
                        "This is information, not a refusal: the disk has no partitions yet. Type g to make a GPT,"
                                + " then n.",
                        "No partition is defined yet!",
                        "t and d need a partition to act on. Make one with n first.",
                        "Value out of range.",
                        "The size or number is too big or too small for this disk. Use a smaller size, or a number"
                                + " from the list p prints.",
                        "No free sectors available.",
                        "The disk is full, or an earlier partition took the rest of it, or there are 128 partitions."
                                + " Delete the last partition with d. A partition that took the rest has to be deleted"
                                + " and made again with a size.",
                        "Failed to parse partition type 'swap'.",
                        "Type 1 or uefi for an EFI system partition, 20 or linux for a Linux filesystem, or L to see"
                                + " the list.",
                        "x: unknown command",
                        "The letter x is not a command of the editor. Type m to see the commands it has.",
                        "mkfs.fat: sda1 is not an EFI System partition.",
                        "The partition's type was never set to EFI. Run fdisk /dev/sda, then type t, then type 1 for"
                                + " EFI System, then type w.",
                        "You quit with q or [[Ctrl]]+[[C]], and your changes are gone.",
                        "Both leave without writing. Only w keeps the changes, so type w before you leave.")
                .seeAlso("jsc:firmware", "jsc:eras", "jsc:arch_bootloader")
                .register();
    }

    private static void filesystemsAndMkfs(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("filesystems_and_mkfs", section).titled("Giving a partition a file system")
                .paragraph("A partition is an empty space until it holds a file system, which decides how its"
                        + " files are stored. The new system's root goes on an ext4 file system (the fourth extended"
                        + " file system), and the EFI system partition gets a FAT (File Allocation Table) file"
                        + " system.")
                .paragraph("Each command below makes one file system, and it erases what was on the partition"
                        + " before. Make a file system only on the partition you have divided for it.")
                .subheading("The root: mkfs.ext4")
                .paragraph("ext4 is the file system the Linux systems keep their root on. The command mkfs.ext4"
                        + " makes it, with the name of the partition after it: mkfs.ext4 /dev/sda2.")
                .paragraph("The partition you name becomes the root. There is only one root at a time: a second"
                        + " mkfs.ext4 on another partition makes that one the root instead. A root that was mounted"
                        + " is unmounted by the new format.")
                .paragraph("Making the file system takes time, which depends on the disk. A hard disk takes 140"
                        + " ticks, which is 7 seconds. A solid-state drive (SSD) takes 35 ticks, and an NVMe"
                        + " (non-volatile memory express) disk takes 24 ticks, the least it can take. 20 ticks make"
                        + " one second ([](jsc:units_and_ticks)).")
                .paragraph("The tool prints these lines as it works, in this order:")
                .table("What mkfs.ext4 prints", "Line", "What it tells you")
                .row(GuideCell.code("mke2fs 1.47.1 (20-May-2024)"), GuideCell.words("The version of mke2fs, the"
                        + " program behind mkfs.ext4 that makes ext4."))
                .row(GuideCell.code("Creating filesystem with N 4k blocks and M inodes"), GuideCell.words("How many"
                        + " blocks of 4 kibibytes (KiB) and how many inodes the file system has, which depend on its"
                        + " size."))
                .row(GuideCell.code("Filesystem UUID: followed by an identifier"), GuideCell.words("The identifier"
                        + " of this file system, a universally unique identifier (UUID), the same every time for"
                        + " that disk."))
                .row(GuideCell.code("Superblock backups stored on blocks:"), GuideCell.words("Printed only for a file"
                        + " system bigger than 128 megabytes (MB): the numbers of the blocks that hold spare copies"
                        + " of its header."))
                .row(GuideCell.code("Allocating group tables:"), GuideCell.words("A counting bar, which ends with"
                        + " done."))
                .row(GuideCell.code("Writing inode tables:"), GuideCell.words("A counting bar, which ends with"
                        + " done."))
                .row(GuideCell.code("Creating journal (N blocks):"), GuideCell.words("Printed when the file system"
                        + " has a journal, and then done."))
                .row(GuideCell.code("Writing superblocks and filesystem accounting information:"),
                        GuideCell.words("The last step, a counting bar, which ends with done."))
                .paragraph("On a whole disk that has no partition table, the command also works. This is the way of"
                        + " the older machines, which start from the whole disk and have no EFI system partition"
                        + " ([](jsc:eras)).")
                .subheading("The EFI system partition: mkfs.fat")
                .paragraph("Type mkfs.fat -F 32 /dev/sda1, with the name of the EFI system partition. The tool does"
                        + " not check the -F 32, so type it anyway, as shown.")
                .paragraph("The tool waits a short while, then prints one line and nothing else: \"mkfs.fat 4.2"
                        + " (2021-01-31)\". It takes 17 ticks on a hard disk and 8 ticks on an SSD or NVMe disk. The"
                        + " partition is formatted and not mounted, so you mount it yourself later.")
                .define("ext4", "The Linux file system that the root filesystem uses. The program mke2fs makes it.")
                .define("FAT", "The simple file system that the firmware reads from the EFI system partition. It is"
                        + " also called FAT32 or vfat.")
                .define("Block", "The unit that ext4 stores data in: 4 KiB, which is 4,096 bytes.")
                .define("Inode", "One record for each file or folder, which says what it is and where its data is."
                        + " Its number is fixed when the file system is made.")
                .define("Superblock", "The header of a file system, with its size and layout. Backups are spare"
                        + " copies of it.")
                .define("Journal", "A small area where ext4 writes each change before it makes the change final, so"
                        + " that a crash does not damage the file system.")
                .define("UUID", "A universally unique identifier, a long number that names one file system. The new"
                        + " system's file table, /etc/fstab, can name a file system by its UUID, so the device name"
                        + " of the disk does not matter.")
                .ifSomethingGoesWrong("Usage: mkfs.ext4 /dev/<device>",
                        "You did not name a partition. Type mkfs.ext4 /dev/sda2, with the name lsblk gave it.",
                        "mke2fs: No such file or directory while trying to determine filesystem size",
                        "That partition does not exist yet. Make it with fdisk first, or check its number in lsblk.",
                        "mke2fs: sda1 is the EFI System partition.",
                        "That partition holds the boot loader, so it is not for ext4. Format it with mkfs.fat -F 32"
                                + " /dev/sda1, and give mkfs.ext4 the other partition.",
                        "/dev/sda contains a gpt partition table.",
                        "You named the whole disk, and it has a partition table. Use one of its partitions, such as"
                                + " /dev/sda2.",
                        "Usage: mkfs.fat -F 32 /dev/<partition>",
                        "You did not name a partition. Type mkfs.fat -F 32 /dev/sda1, with the name lsblk gave it.",
                        "mkfs.fat: unable to open sda1: No such file or directory",
                        "That partition does not exist. Make it with fdisk first, or check its number in lsblk.",
                        "mkfs.fat: sda1 is not an EFI System partition.",
                        "Set its type first: fdisk /dev/sda, then type t, then type 1 for EFI System, then type w."
                                + " Then run mkfs.fat again.")
                .seeAlso("jsc:firmware", "jsc:disks", "jsc:units_and_ticks")
                .register();
    }

    private static void mounting(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("mounting", section).titled("Mounting the disks")
                .paragraph("You cannot reach a file system's files until it is mounted: attached to a folder, so that"
                        + " you can reach its files through that folder. The new system is built inside /mnt on Arch,"
                        + " or inside /mnt/gentoo on Gentoo, and the disks are mounted there.")
                .paragraph("Mount the root first. A mount that works prints nothing at all, which is how the tool"
                        + " says yes. Its form is mount, the partition, then the folder: the /dev/ in front of the"
                        + " name is optional.")
                .table("The places a mount can go", "Partition", "Mount point, on Arch", "Mount point, on Gentoo")
                .row(GuideCell.words("The root"), GuideCell.code("/mnt"), GuideCell.code("/mnt/gentoo"))
                .row(GuideCell.words("The EFI system partition"), GuideCell.code("/mnt/boot"),
                        GuideCell.code("/mnt/gentoo/efi"))
                .paragraph("The tool accepts the root folder, and the boot or efi folder under the root, written"
                        + " exactly as shown, with no slash at the end. The EFI system partition has to be mounted"
                        + " after the root.")
                .subheading("The steps on Arch")
                .steps("Type mount /dev/sda2 /mnt and press [[Enter]], to mount the root.",
                        "Type mount --mkdir /dev/sda1 /mnt/boot and press [[Enter]], to mount the EFI system"
                                + " partition.")
                .subheading("The steps on Gentoo")
                .steps("Type mount /dev/sda2 /mnt/gentoo and press [[Enter]], to mount the root.",
                        "Type mount /dev/sda1 /mnt/gentoo/efi and press [[Enter]], to mount the EFI system"
                                + " partition.")
                .paragraph("The tool makes the folder of the EFI system partition anyway, so you can leave out"
                        + " --mkdir. It ignores that flag if you type it, and it ignores the option word after -o,"
                        + " such as noatime.")
                .subheading("Binding the medium's own folders")
                .paragraph("Gentoo's checklist asks for four folders of the live medium to be bound into the new"
                        + " system before you step into it: /proc, /sys, /dev and /run. The new system needs them"
                        + " while it is set up. You bind them with the type proc, or with the flag --rbind or --bind.")
                .paragraph("A bind goes after the root is mounted, and it names the folder of the medium first, then"
                        + " the place in the new system. For example, mount --types proc /proc /mnt/gentoo/proc binds"
                        + " /proc. Each bind is silent when it works.")
                .define("Bind mount", "A folder of the running medium that appears again inside the new system, with"
                        + " the same contents.")
                .ifSomethingGoesWrong("mount: bad usage",
                        "You gave fewer than two words. Type mount /dev/sda2 /mnt, with the partition and the folder.",
                        "mount: /mnt/boot: mount point does not exist.",
                        "The root is not mounted yet, and the EFI system partition comes after it. Mount the root"
                                + " first with mount /dev/sda2 /mnt.",
                        "mount: /mnt/boot: wrong fs type on /dev/sda1.",
                        "That partition is not the FAT partition the tool expects. Run mkfs.fat -F 32 /dev/sda1 first,"
                                + " and check the name.",
                        "mount: /mnt: wrong fs type, bad option, bad superblock on /dev/sda2.",
                        "The root is not formatted as ext4 yet. Run mkfs.ext4 /dev/sda2 first.",
                        "mount: /mnt: mount point does not exist.",
                        "You tried to bind a folder before mounting the root. Mount the root first, then bind.",
                        "mount: /mnt/data: mount point does not exist.",
                        "That folder is not /mnt, /mnt/gentoo, or their boot or efi folder. Use one of those.")
                .seeAlso("jsc:arch_chroot", "jsc:arch_fstab", "jsc:when_a_tool_refuses")
                .register();
    }

    private static void whenAToolRefuses(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("when_a_tool_refuses", section).titled("What each disk error means")
                .paragraph("Most messages start with the tool's name, such as fdisk, mkfs.ext4 or"
                        + " mount. When a tool stops you, find its message in the tables below. A"
                        + " message in a plain sentence, such as Device does not contain a recognized partition table,"
                        + " is information rather than a refusal.")
                .paragraph("lsblk never refuses anything. It always prints its table.")
                .table("The messages of fdisk", "Message", "What it means, and the fix")
                .row(GuideCell.code("fdisk: bad usage"), GuideCell.words("No disk was named: type fdisk /dev/sda."))
                .row(GuideCell.code("fdisk: cannot open /dev/sda2: No such file or directory"),
                        GuideCell.words("No disk of that name is in the computer: use a disk name from lsblk, not a"
                                + " partition name."))
                .row(GuideCell.code("Device does not contain a recognized partition table."),
                        GuideCell.words("Information: the disk has no partitions yet, so type g for a GPT."))
                .row(GuideCell.code("No partition is defined yet!"), GuideCell.words("t or d needs a partition:"
                        + " make one with n first."))
                .row(GuideCell.code("No free sectors available."), GuideCell.words("The disk is full, an earlier"
                        + " partition took the rest, or there are 128 partitions: delete the last one with d."))
                .row(GuideCell.code("Value out of range."), GuideCell.words("The size or number is outside what the"
                        + " disk has: use a smaller size or a number from the list."))
                .row(GuideCell.code("Failed to parse partition type 'swap'."), GuideCell.words("The type word is not"
                        + " known: type 1 for EFI System, 20 for Linux filesystem, or L for the list."))
                .row(GuideCell.code("x: unknown command"), GuideCell.words("The letter x is not a command of the"
                        + " editor: type m for the list."))
                .row(GuideCell.code("Changed type of partition 'Linux filesystem' to 'EFI System'."),
                        GuideCell.words("Success, with nothing to fix."))
                .table("The messages of mkfs.ext4", "Message", "What it means, and the fix")
                .row(GuideCell.code("Usage: mkfs.ext4 /dev/<device>"), GuideCell.words("No partition was named: give"
                        + " one, such as /dev/sda2."))
                .row(GuideCell.code("mke2fs: No such file or directory while trying to determine filesystem size"),
                        GuideCell.words("The partition does not exist yet: make it with fdisk, or check its number."))
                .row(GuideCell.code("mke2fs: sda1 is the EFI System partition."), GuideCell.words("The EFI system"
                        + " partition was named: use mkfs.fat -F 32 on it, and mkfs.ext4 on another partition."))
                .row(GuideCell.code("/dev/sda contains a gpt partition table."), GuideCell.words("The whole disk was"
                        + " named, and it has a partition table: use one partition, such as sda2."))
                .table("The messages of mkfs.fat", "Message", "What it means, and the fix")
                .row(GuideCell.code("Usage: mkfs.fat -F 32 /dev/<partition>"), GuideCell.words("No partition was"
                        + " named: give one, such as /dev/sda1."))
                .row(GuideCell.code("mkfs.fat: unable to open sda1: No such file or directory"),
                        GuideCell.words("The partition does not exist: make it with fdisk, or check its number."))
                .row(GuideCell.code("mkfs.fat: sda1 is not an EFI System partition."), GuideCell.words("Its type is not"
                        + " EFI System yet: run fdisk, then t, then 1, then w, and try again."))
                .table("The messages of mount", "Message", "What it means, and the fix")
                .row(GuideCell.code("mount: bad usage"), GuideCell.words("Fewer than two words were given: give a"
                        + " partition and a folder."))
                .row(GuideCell.code("mount: /mnt/boot: mount point does not exist."), GuideCell.words("The root is not"
                        + " mounted yet, so mount it first."))
                .row(GuideCell.code("mount: /mnt/data: mount point does not exist."),
                        GuideCell.words("The folder is not a place that can be mounted: use /mnt, or the boot or efi"
                                + " folder under it."))
                .row(GuideCell.code("mount: /mnt/boot: wrong fs type on /dev/sda1."), GuideCell.words("The EFI system"
                        + " partition is not formatted as FAT, so run mkfs.fat -F 32 on it first."))
                .row(GuideCell.code("mount: /mnt: wrong fs type, bad option, bad superblock on /dev/sda2."),
                        GuideCell.words("The partition is not the one mkfs.ext4 last formatted, so run mkfs.ext4 on it"
                                + " first."))
                .paragraph("A mount or a bind that works prints nothing at all, so silence is its success.")
                .ifSomethingGoesWrong("The same message comes back after you fix something.",
                        "Run lsblk and compare the name you typed with the name it prints. The name follows the slot"
                                + " the disk is in.",
                        "The message is not in these tables.",
                        "Find the tool's name at the start of the message, read that tool's table, and check the"
                                + " name with lsblk.")
                .seeAlso("jsc:seeing_the_disks", "jsc:partitioning_with_fdisk", "jsc:mounting")
                .register();
    }
}
