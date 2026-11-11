/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.os.FileSystemType;
import dev.jstech.computers.os.disk.DiskCommand;
import dev.jstech.computers.os.disk.DiskReport;
import dev.jstech.computers.os.disk.PartitionRole;
import java.util.Locale;

/**
 * What the disk tools at the prompt share (computers/docs/design/storage-devices.md): the machine's disks read from it,
 * a change asked of them, the names each family gives a disk and a partition, and the sizes and the type names its
 * tools print. The names and numbers are data, the same in every language.
 */
final class DiskToolLines {

    /* A megabyte is two thousand and forty-eight sectors of five hundred and twelve bytes. */
    static final long SECTORS_PER_MB = 2_048L;
    /* Where the first partition starts on a disk the tools align: a megabyte in. */
    static final long FIRST_SECTOR = 2_048L;
    private static final long MB_PER_GB = 1_024L;
    private static final long MB_PER_TB = 1_048_576L;

    private DiskToolLines() {
    }

    /** The machine's disks as they are. */
    static DiskReport report(final ICliComputer computer) {
        return DiskReport.read(computer.disks(DiskCommand.look()));
    }

    /** The machine's disks after a change, with why it was refused, if it was. */
    static DiskReport ask(final ICliComputer computer, final String path) {
        return DiskReport.read(computer.disks(path));
    }

    /** A disk as Linux names it: {@code /dev/sda}. */
    static String linuxDisk(final int disk) {
        return "/dev/sd" + (char) ('a' + disk);
    }

    /** The disk named, by Linux's name for it or one of its partitions; -1 when it names none. */
    static int linuxDiskOf(final String device) {
        final String name = device.startsWith("/dev/") ? device.substring(5) : device;
        return name.length() >= 3 && name.startsWith("sd") ? name.charAt(2) - 'a' : -1;
    }

    /** The partition named by Linux's name for it, {@code sda4} or {@code /dev/sda4}; -1 for a whole disk. */
    static int linuxPartitionOf(final String device) {
        final String name = device.startsWith("/dev/") ? device.substring(5) : device;
        if (name.length() <= 3) {
            return -1;
        }
        try {
            return Integer.parseInt(name.substring(3));
        } catch (final NumberFormatException e) {
            return -1;
        }
    }

    /** A disk as FreeBSD names it: {@code ada0}. */
    static String bsdDisk(final int disk) {
        return "ada" + disk;
    }

    /** The disk named by FreeBSD's or OpenBSD's name for it ({@code ada0}, {@code wd0}, {@code sd1}); -1. */
    static int bsdDiskOf(final String device) {
        final String name = device.startsWith("/dev/") ? device.substring(5) : device;
        final String digits = name.replaceAll("^[a-z]+", "").replaceAll("[^0-9].*$", "");
        try {
            return digits.isEmpty() ? -1 : Integer.parseInt(digits);
        } catch (final NumberFormatException e) {
            return -1;
        }
    }

    /** A size as the Unix tools print it read by a person: 512M, 1.5G, 2.0T. */
    static String human(final long mb) {
        if (mb >= MB_PER_TB) {
            return String.format(Locale.ROOT, "%.1fT", mb / (double) MB_PER_TB);
        }
        if (mb >= MB_PER_GB) {
            return String.format(Locale.ROOT, "%.1fG", mb / (double) MB_PER_GB);
        }
        return mb + "M";
    }

    /** A whole disk's size as fdisk prints it: {@code 2 TiB}, {@code 40 GiB}. */
    static String iec(final long mb) {
        if (mb >= MB_PER_TB) {
            return trim(mb / (double) MB_PER_TB) + " TiB";
        }
        if (mb >= MB_PER_GB) {
            return trim(mb / (double) MB_PER_GB) + " GiB";
        }
        return mb + " MiB";
    }

    /** The sector a partition starts on. */
    static long startSector(final long startMb) {
        return FIRST_SECTOR + startMb * SECTORS_PER_MB;
    }

    /** A partition's type as fdisk names it on a GUID Partition Table. */
    static String gptType(final DiskReport.Part part) {
        final FileSystemType fs = part.fileSystem();
        if (part.role() == PartitionRole.EFI) {
            return "EFI System";
        }
        if (fs == null) {
            return "Linux filesystem";
        }
        return switch (fs) {
            case SWAP -> "Linux swap";
            case EXT2, EXT3, EXT4, XFS, BTRFS -> "Linux filesystem";
            case UFS -> "FreeBSD UFS";
            case ZFS -> "FreeBSD ZFS";
            case HFS, HFS_PLUS, HFS_PLUS_JOURNALED -> "Apple HFS/HFS+";
            case APFS -> "Apple APFS";
            default -> "Microsoft basic data";
        };
    }

    /** A partition's type as fdisk names it on a Master Boot Record, with its id. */
    static String dosType(final DiskReport.Part part) {
        final FileSystemType fs = part.fileSystem();
        if (part.role() == PartitionRole.EFI) {
            return "ef EFI (FAT-12/16/32)";
        }
        if (fs == null) {
            return "83 Linux";
        }
        return switch (fs) {
            case FAT12 -> " 1 FAT12";
            case FAT16 -> " 6 FAT16";
            case FAT32 -> " c W95 FAT32 (LBA)";
            case NTFS, EXFAT -> " 7 HPFS/NTFS/exFAT";
            case SWAP -> "82 Linux swap / Solaris";
            case UFS, ZFS -> "a5 FreeBSD";
            case HFS, HFS_PLUS, HFS_PLUS_JOURNALED, APFS -> "af HFS / HFS+";
            default -> "83 Linux";
        };
    }

    /** A partition's file system as lsblk names it: vfat for every FAT. */
    static String lsblkType(final DiskReport.Part part) {
        final FileSystemType fs = part.fileSystem();
        if (fs == null) {
            return "";
        }
        return switch (fs) {
            case FAT12, FAT16, FAT32 -> "vfat";
            case ZFS -> "zfs_member";
            case HFS_PLUS, HFS_PLUS_JOURNALED -> "hfsplus";
            default -> fs.serializedName();
        };
    }

    /** A partition's type as gpart names it. */
    static String gpartType(final DiskReport.Part part) {
        final FileSystemType fs = part.fileSystem();
        if (part.role() == PartitionRole.EFI) {
            return "efi";
        }
        if (fs == null) {
            return "freebsd";
        }
        return switch (fs) {
            case SWAP -> "freebsd-swap";
            case UFS -> "freebsd-ufs";
            case ZFS -> "freebsd-zfs";
            case EXT2, EXT3, EXT4, XFS, BTRFS -> "linux-data";
            case HFS, HFS_PLUS, HFS_PLUS_JOURNALED, APFS -> "apple-hfs";
            default -> "ms-basic-data";
        };
    }

    /* A number of whole or tenths, as fdisk prints a disk's size: 2, 1.5. */
    private static String trim(final double value) {
        return value == Math.rint(value) ? Long.toString((long) value)
                : String.format(Locale.ROOT, "%.1f", value);
    }
}
