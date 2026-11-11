/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.hardware.DiskSpec;
import dev.jstech.computers.hardware.EraStorage;
import dev.jstech.computers.os.FileSystemType;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.MachineSwap;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsDisks;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.disk.DiskCommand;
import dev.jstech.computers.os.disk.DiskLayout;
import dev.jstech.computers.os.disk.DiskPartitions;
import dev.jstech.computers.os.disk.DiskReport;
import dev.jstech.computers.os.disk.DiskSnapshots;
import dev.jstech.computers.os.disk.PartitionEntry;
import dev.jstech.computers.os.disk.PartitionRole;
import dev.jstech.computers.os.disk.PartitionScheme;
import dev.jstech.computers.os.disk.ZfsMember;
import dev.jstech.computers.os.disk.ZfsMirrors;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FilesystemContents;
import dev.jstech.computers.os.fs.StoredFile;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.computers.storage.DriveVolumes;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.storage.StorageVolume;
import dev.jstech.computers.storage.VolumeSnapshot;
import dev.jstech.core.jstechseries.tier.Era;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * The machine's side of its disk tools (computers/docs/design/storage-devices.md): it reads its disks into a
 * {@link DiskReport} and makes the changes Disk Management, the KDE Partition Manager, GNOME Disks, diskpart, FDISK and
 * fdisk ask for, by the real rules. A partition the running system uses, the one it runs from or a swap it swaps to, is
 * neither deleted nor formatted, and resizes only as its file system allows while mounted; a table changes scheme only
 * on an empty disk; a new table erases the disk, never the one the system runs from.
 */
public final class DiskTools {

    private DiskTools() {
    }

    /**
     * Answers a disk tool's path: makes the change it asks, if any, at the world's time in {@code level}, and reports
     * the disks after it.
     */
    public static String reply(final IOsHost machine, final Level level, final String path) {
        final DiskCommand command = DiskCommand.parse(path);
        final DiskReport.Refusal refusal = command == null || command.verb() == DiskCommand.Verb.LOOK
                ? null : apply(machine, command, level.getDayTime());
        return report(machine, refusal).write();
    }

    /** The machine's disks as its tools see them, with why the last change was refused, if it was. */
    public static DiskReport report(final IOsHost machine, @Nullable final DiskReport.Refusal refusal) {
        final Set<String> mounted = mounted(machine);
        final List<ItemStack> all = disksOf(machine);
        final List<DiskReport.Disk> disks = new ArrayList<>();
        int index = 0;
        for (int slot = 0; slot < machine.diskSlots(); slot++) {
            final ItemStack disk = machine.diskInSlot(slot);
            if (!DriveVolumes.isDisk(disk)) {
                continue;
            }
            final DiskLayout layout = DiskPartitions.layout(disk);
            final List<DiskReport.Part> parts = new ArrayList<>();
            for (final PartitionEntry partition : layout.partitions()) {
                final boolean inUse = mounted.contains(index + ":" + partition.number());
                final DiskReport.Part part = partOf(disk, partition, inUse);
                parts.add(partition.holdsData() ? withSnapshotsAndPool(part, disk, partition, all) : part);
            }
            disks.add(new DiskReport.Disk(index, slot, disk.getHoverName().getString(), layout.diskMb(),
                    layout.scheme(), parts));
            index++;
        }
        return new DiskReport(disks, refusal == null ? "" : refusal.name());
    }

    /*
     * Makes one change, or says why not. A partition made or formatted is emptied; a new table erases the disk first,
     * as the tools that write one warn.
     */
    @Nullable
    private static DiskReport.Refusal apply(final IOsHost machine, final DiskCommand command, final long time) {
        final int slot = slotOf(machine, command.disk());
        if (slot < 0) {
            return DiskReport.Refusal.NO_SUCH_DISK;
        }
        final ItemStack disk = machine.diskInSlot(slot);
        final DiskLayout layout = DiskPartitions.layout(disk);
        final Set<String> mounted = mounted(machine);
        final int number = (int) command.number();
        final boolean inUse = mounted.contains(command.disk() + ":" + number);
        final DiskReport.Refusal refusal = switch (command.verb()) {
            case DELETE -> inUse ? DiskReport.Refusal.MOUNTED : write(disk, layout.delete(number), false);
            case CREATE -> {
                final FileSystemType fs = command.word().isEmpty() ? null : FileSystemType.byName(command.word());
                final DiskLayout.Change made = layout.create(command.number(), command.amount(), fs,
                        PartitionRole.DATA);
                yield fs != null && !fs.formattableIn(machine.installedEra()) ? DiskReport.Refusal.NOT_IN_THIS_ERA
                        : write(disk, made, true);
            }
            case FORMAT -> {
                final FileSystemType fs = FileSystemType.byName(command.word());
                yield inUse ? DiskReport.Refusal.MOUNTED : !fs.formattableIn(machine.installedEra())
                        ? DiskReport.Refusal.NOT_IN_THIS_ERA
                        : write(disk, layout.format(number, fs, PartitionRole.DATA), true);
            }
            case RESIZE -> {
                final PartitionEntry partition = layout.find(number);
                final long usedMb = partition == null ? 0L : usedMbOf(disk, partition);
                yield write(disk, layout.resize(number, command.amount(), inUse, usedMb), false);
            }
            case ACTIVE -> layout.scheme() != PartitionScheme.MBR ? DiskReport.Refusal.NOT_MBR
                    : write(disk, layout.activate(number), false);
            case CONVERT -> write(disk, layout.convert(PartitionScheme.byName(command.word())), false);
            case TABLE -> newTable(machine, slot, command.disk(), mounted, DiskCommand.NO_TABLE.equals(command.word())
                    ? null : PartitionScheme.byName(command.word()));
            case LABEL -> label(disk, number, command.word());
            case SNAPSHOT -> onPartition(disk, number, view -> DiskSnapshots.take(view, command.word(), time));
            case ROLLBACK -> onPartition(disk, number,
                    view -> DiskSnapshots.rollBack(view, command.word(), command.amount() > 0L));
            case DESTROY -> onPartition(disk, number, view -> DiskSnapshots.destroy(view, command.word()));
            case DEFAULT -> onPartition(disk, number, view -> DiskSnapshots.startFrom(view, (int) command.amount()));
            case SCRUB -> onPartition(disk, number, view -> scrub(view, disksOf(machine), time));
            case LOOK -> null;
        };
        if (refusal == null) {
            machine.markChanged();
        }
        return refusal;
    }

    /* Writes a change to the table, emptying the partition it made or formatted. */
    @Nullable
    private static DiskReport.Refusal write(final ItemStack disk, final DiskLayout.Change change,
                                            final boolean empties) {
        if (!change.ok() || change.layout() == null) {
            return change.refusal() == null ? DiskReport.Refusal.NO_ROOM : DiskReport.Refusal.of(change.refusal());
        }
        DiskPartitions.relayout(disk, change.layout());
        if (empties) {
            DiskPartitions.wipe(disk, change.number());
        }
        return null;
    }

    /*
     * A new, empty table, or none at all for a null scheme: the whole disk erased first, never the disk the running
     * system uses.
     */
    @Nullable
    private static DiskReport.Refusal newTable(final IOsHost machine, final int slot, final int index,
                                               final Set<String> mounted, @Nullable final PartitionScheme scheme) {
        for (final String used : mounted) {
            if (used.startsWith(index + ":")) {
                return DiskReport.Refusal.MOUNTED;
            }
        }
        final long diskMb = DiskPartitions.layout(machine.diskInSlot(slot)).diskMb();
        machine.formatDisk(slot);
        if (scheme != null) {
            DiskPartitions.relayout(machine.diskInSlot(slot), DiskLayout.empty(scheme, diskMb));
        }
        return null;
    }

    /* Does {@code change} to partition {@code number} as a volume, written back with its disk after. */
    @Nullable
    private static DiskReport.Refusal onPartition(final ItemStack disk, final int number,
                                                  final Function<ItemStack, DiskReport.Refusal> change) {
        final ItemStack volume = DiskPartitions.view(disk, number);
        if (volume.isEmpty()) {
            return DiskReport.Refusal.NO_SUCH_PARTITION;
        }
        final DiskReport.Refusal refusal = change.apply(volume);
        DiskPartitions.commit(disk, volume);
        return refusal;
    }

    /* Scrubs the ZFS pool a partition is the first member of, or the only one. */
    @Nullable
    private static DiskReport.Refusal scrub(final ItemStack volume, final List<ItemStack> disks, final long time) {
        if (DiskPartitions.fileSystemOf(volume) != FileSystemType.ZFS || ZfsMirrors.secondMember(volume)) {
            return DiskReport.Refusal.NOT_ZFS;
        }
        if (ZfsMirrors.memberOf(volume) == null) {
            // A pool on one disk scrubs too: it finds the damage, and has nothing to repair it from.
            volume.set(ComputingComponents.ZFS_MEMBER.get(), ZfsMember.alone(UUID.randomUUID()));
        }
        ZfsMirrors.scrub(volume, disks, time);
        return null;
    }

    /* A partition with the snapshots it keeps and, for ZFS, its pool. */
    private static DiskReport.Part withSnapshotsAndPool(final DiskReport.Part part, final ItemStack disk,
                                                        final PartitionEntry partition, final List<ItemStack> all) {
        final ItemStack volume = DiskPartitions.view(disk, partition.number());
        final StorageVolume store = DriveVolumes.peek(volume);
        final FilesystemContents files = volume.getOrDefault(ComputingComponents.FILESYSTEM.get(),
                FilesystemContents.EMPTY);
        final Era era = DiskFilesystem.eraOf(volume);
        final List<DiskReport.Snap> snaps = new ArrayList<>();
        for (final VolumeSnapshot kept : store.snapshots()) {
            long changedFiles = 0L;
            for (final StoredFile file : kept.files().files().values()) {
                if (!file.equals(files.files().get(file.path()))) {
                    changedFiles += file.weight(era);
                }
            }
            snaps.add(new DiskReport.Snap(kept.number(), kept.name(), kept.takenAt(),
                    mbOf(volume, kept.itemsWeight() + kept.files().usedWeight(era)),
                    mbOf(volume, store.heldOnlyBy(kept) + changedFiles), kept.id().equals(store.startsFrom())));
        }
        return part.with(snaps, partition.fs() == FileSystemType.ZFS ? poolOf(volume, store, all) : null);
    }

    /* A ZFS partition's pool: on one disk, or a mirror with where its other member is. */
    private static DiskReport.Pool poolOf(final ItemStack volume, final StorageVolume store,
                                          final List<ItemStack> all) {
        final int errors = store.corrupted().size() + store.corruptedFiles().files().size();
        final ZfsMember member = ZfsMirrors.memberOf(volume);
        if (member == null) {
            return new DiskReport.Pool(true, false, -1, -1, 0L, 0, ZfsMember.NEVER, 0L, errors);
        }
        final int[] other = !member.mirror() ? new int[] {-1, -1}
                : member.primary() ? partnerOf(volume, all) : firstMemberOf(member, all);
        return new DiskReport.Pool(member.primary(), member.mirror(), other[0], other[1],
                mbOf(volume, member.repaired()), member.repairs(), member.scrubbedAt(),
                mbOf(volume, member.scrubRepaired()), errors);
    }

    /* Where the pool's second member is, as a disk's place among the machine's and a partition, or -1s. */
    private static int[] partnerOf(final ItemStack volume, final List<ItemStack> all) {
        final ZfsMirrors.Partner partner = ZfsMirrors.partnerOf(volume, all);
        return partner == null ? new int[] {-1, -1} : new int[] {all.indexOf(partner.disk()), partner.number()};
    }

    /* Where the first member of the pool {@code member} belongs to is, or -1s. */
    private static int[] firstMemberOf(final ZfsMember member, final List<ItemStack> all) {
        for (int index = 0; index < all.size(); index++) {
            for (final PartitionEntry partition : DiskPartitions.layout(all.get(index)).partitions()) {
                final ZfsMember other = partition.holdsData()
                        ? ZfsMirrors.memberOf(DiskPartitions.view(all.get(index), partition.number())) : null;
                if (other != null && other.primary() && other.pool().equals(member.pool())) {
                    return new int[] {index, partition.number()};
                }
            }
        }
        return new int[] {-1, -1};
    }

    /* A data weight on that volume, in megabytes at its disk's era. */
    private static long mbOf(final ItemStack volume, final long weight) {
        final DiskSpec spec = HardwareRegistry.disk(volume);
        return spec == null ? 0L : weight * EraStorage.mbPerItem(spec.era()) / StorageKey.MB_EQ_PER_ITEM;
    }

    /* The machine's disks, in their order, which is how a disk tool numbers them. */
    private static List<ItemStack> disksOf(final IOsHost machine) {
        final List<ItemStack> out = new ArrayList<>();
        for (int slot = 0; slot < machine.diskSlots(); slot++) {
            if (DriveVolumes.isDisk(machine.diskInSlot(slot))) {
                out.add(machine.diskInSlot(slot));
            }
        }
        return out;
    }

    /* Names a partition's volume. */
    @Nullable
    private static DiskReport.Refusal label(final ItemStack disk, final int number, final String label) {
        final ItemStack volume = DiskPartitions.view(disk, number);
        if (volume.isEmpty()) {
            return DiskReport.Refusal.NO_SUCH_PARTITION;
        }
        if (label.isBlank()) {
            volume.remove(ComputingComponents.VOLUME_LABEL.get());
        } else {
            volume.set(ComputingComponents.VOLUME_LABEL.get(), label.strip());
        }
        DiskPartitions.commit(disk, volume);
        return null;
    }

    /*
     * The partitions the running system has in use, each as "disk:number": the one it runs from, and every swap
     * partition it swaps to.
     */
    private static Set<String> mounted(final IOsHost machine) {
        final Set<String> out = new HashSet<>();
        if (machine.installedOs() == null) {
            return out;
        }
        final ItemStack system = machine.systemDisk();
        int index = 0;
        for (int slot = 0; slot < machine.diskSlots(); slot++) {
            final ItemStack disk = machine.diskInSlot(slot);
            if (!DriveVolumes.isDisk(disk)) {
                continue;
            }
            if (disk == system) {
                out.add(index + ":" + DiskPartitions.table(disk).open());
            }
            index++;
        }
        for (final MachineSwap.Area area : MachineSwap.of(machine).areas()) {
            out.add(area.disk() + ":" + area.partition());
        }
        return out;
    }

    private static DiskReport.Part partOf(final ItemStack disk, final PartitionEntry partition,
                                          final boolean mounted) {
        final String fs = partition.fs() == null ? "" : partition.fs().serializedName();
        if (!partition.holdsData()) {
            return new DiskReport.Part(partition.number(), partition.startMb(), partition.sizeMb(), 0L, fs, "",
                    partition.role(), partition.active(), mounted, "");
        }
        final ItemStack volume = DiskPartitions.view(disk, partition.number());
        final ResourceLocation osId = OsDisks.systemOn(volume);
        final OsDef os = osId == null ? null : OsRegistry.getOs(osId);
        return new DiskReport.Part(partition.number(), partition.startMb(), partition.sizeMb(),
                usedMbOf(disk, partition), fs, volume.getOrDefault(ComputingComponents.VOLUME_LABEL.get(), ""),
                partition.role(), partition.active(), mounted, os == null ? "" : os.displayName());
    }

    private static long usedMbOf(final ItemStack disk, final PartitionEntry partition) {
        if (!partition.holdsData()) {
            return 0L;
        }
        return partition.sizeMb() - DiskPartitions.freeMbOf(DiskPartitions.view(disk, partition.number()));
    }

    /* The slot of the disk at {@code index} among the machine's disks, or -1. */
    private static int slotOf(final IOsHost machine, final int index) {
        int seen = 0;
        for (int slot = 0; slot < machine.diskSlots(); slot++) {
            if (!DriveVolumes.isDisk(machine.diskInSlot(slot))) {
                continue;
            }
            if (seen++ == index) {
                return slot;
            }
        }
        return -1;
    }
}
