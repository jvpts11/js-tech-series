/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import dev.jstech.computers.hardware.DiskSpec;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.os.disk.DiskPartitions;
import dev.jstech.computers.os.disk.PartitionEntry;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.computers.storage.DriveVolumes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The swap behind a machine's memory (computers/docs/design/hardware.md). Linux, FreeBSD, OpenBSD and UNIX keep it on
 * swap partitions, every one on the machine's disks; Frames in a page file on its own volume, which it sizes itself as
 * it comes up, one and a half times the memory on Frames 95 and XP and as big as the memory from Frames 7 on; MC-DOS,
 * MC-NET and Quince keep none. What is in swap runs at a share of its speed by the disk it is on: a quarter on a hard
 * disk, six tenths on an SSD, nine tenths on NVMe. Estimates.
 */
public final class MachineSwap {

    /** No swap at all. */
    public static final Swap NONE = new Swap(List.of(), 0, 100, null);

    /* The share of its speed what is in swap runs at, in hundredths, by the disk the swap is on. Estimates. */
    private static final int HDD_PERCENT = 25;
    private static final int SSD_PERCENT = 60;
    private static final int NVME_PERCENT = 90;
    /* Frames XP is the second of its family; it and Frames 95 give themselves one and a half times the memory. */
    private static final int FRAMES_XP_RANK = 2;

    private MachineSwap() {
    }

    /** The swap behind {@code host}'s memory, by the system it runs; none for a machine with no system. */
    public static Swap of(final IOsHost host) {
        final OsDef os = host.installedOs();
        if (os == null) {
            return NONE;
        }
        return switch (os.platform()) {
            case FRAMES -> pageFile(host, os);
            case LINUX, FREEBSD, OPENBSD, UNIX -> partitions(host);
            default -> NONE;
        };
    }

    /** The page file a Frames edition gives itself on a machine with {@code ramMb} of memory. */
    public static int pageFileMb(final OsDef os, final int ramMb) {
        return os.familyRank() <= FRAMES_XP_RANK ? ramMb * 3 / 2 : ramMb;
    }

    /**
     * Frames sizes its page file as it comes up and writes it on its volume, where it weighs as files do.
     *
     * @return whether the size written changed, which the machine then has to save
     */
    public static boolean sizePageFile(final IOsHost host) {
        final OsDef os = host.installedOs();
        final ItemStack disk = host.systemDisk();
        if (os == null || os.platform() != Platform.FRAMES || disk.isEmpty()) {
            return false;
        }
        final int mb = pageFileMb(os, host.ramTotalMb());
        if (OsDisks.systemsOn(disk).pageMbOf(os.id()) == mb) {
            return false;
        }
        OsDisks.keepPageFile(disk, os.id(), mb);
        return true;
    }

    /**
     * A swap partition's name as {@code platform} writes it: {@code /dev/sda5} on Linux and UNIX,
     * {@code /dev/ada0p3} on FreeBSD, {@code /dev/wd0b} on OpenBSD.
     */
    public static String deviceName(final Platform platform, final Area area) {
        return switch (platform) {
            case FREEBSD -> "/dev/ada" + area.disk() + "p" + area.partition();
            case OPENBSD -> "/dev/wd" + area.disk() + "b";
            default -> "/dev/sd" + (char) ('a' + area.disk()) + area.partition();
        };
    }

    /** The share of its speed what is in swap on a disk of {@code tier} runs at, in hundredths. */
    public static int speedPercentOn(@Nullable final StorageTier tier) {
        if (tier == null) {
            return HDD_PERCENT;
        }
        return switch (tier) {
            case HDD -> HDD_PERCENT;
            case SSD -> SSD_PERCENT;
            case NVME -> NVME_PERCENT;
        };
    }

    private static Swap pageFile(final IOsHost host, final OsDef os) {
        final ItemStack disk = host.systemDisk();
        final int mb = OsDisks.systemsOn(disk).pageMbOf(os.id());
        if (mb <= 0) {
            return NONE;
        }
        final StorageTier tier = tierOf(disk);
        return new Swap(List.of(), mb, speedPercentOn(tier), os.familyRank() <= 1 ? "WIN386.SWP" : "pagefile.sys");
    }

    private static Swap partitions(final IOsHost host) {
        final List<Area> areas = new ArrayList<>();
        int total = 0;
        StorageTier tier = null;
        int drive = 0;
        for (int slot = 0; slot < host.diskSlots(); slot++) {
            final ItemStack disk = host.diskInSlot(slot);
            if (!DriveVolumes.isDisk(disk)) {
                continue;
            }
            for (final PartitionEntry partition : DiskPartitions.layout(disk).partitions()) {
                if (partition.swap()) {
                    areas.add(new Area(drive, partition.number(), (int) Math.min(Integer.MAX_VALUE,
                            partition.sizeMb())));
                    total += (int) Math.min(Integer.MAX_VALUE - total, partition.sizeMb());
                    tier = tier == null ? tierOf(disk) : tier;
                }
            }
            drive++;
        }
        return areas.isEmpty() ? NONE : new Swap(areas, total, speedPercentOn(tier), null);
    }

    @Nullable
    private static StorageTier tierOf(final ItemStack disk) {
        final DiskSpec spec = HardwareRegistry.disk(disk);
        return spec == null ? null : spec.tier();
    }

    /**
     * The swap behind a machine's memory.
     *
     * @param areas        the swap partitions, on the systems that keep it on them
     * @param sizeMb       how big it is, in megabytes
     * @param speedPercent the share of its speed what is in it runs at, in hundredths
     * @param pageFile     the page file's name on a Frames, or null for swap on partitions
     */
    public record Swap(List<Area> areas, int sizeMb, int speedPercent, @Nullable String pageFile) {

        public Swap {
            areas = List.copyOf(areas);
        }
    }

    /**
     * One swap partition.
     *
     * @param disk      the disk it is on, by its place among the machine's disks, from 0
     * @param partition its number
     * @param sizeMb    its size, in megabytes
     */
    public record Area(int disk, int partition, int sizeMb) {
    }
}
