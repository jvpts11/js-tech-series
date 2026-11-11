/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.backup;

import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.blockentity.ServerServices;
import dev.jstech.computers.hardware.EraStorage;
import dev.jstech.computers.machine.DriveTable;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.files.FileAccess;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.fs.BackupFile;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.text.Text;
import dev.jstech.core.util.Loaded;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The Backup Service at work: it runs a job, writing a backup of the network onto a tape of a Tape Library or into a
 * file on another server's drive, and restores one, its items through the network's book of losses so no item comes
 * back twice (computers/docs/design/programs.md). A tape winds before it reads or writes, so a job on a library waits
 * at its drive, and the library's rack moves it along every tick; a drive writes at once.
 */
public final class BackupService {

    /** The folder on a server's drive its backups are written into. */
    public static final String FOLDER = "Backups";

    private BackupService() {
    }

    /**
     * How a run or a restore went: its result, and what to tell the player.
     *
     * @param result how the job's run stands
     * @param said   what to tell whoever asked
     */
    public record Outcome(BackupJob.RunState result, Text said) {
    }

    /** The service running on {@code network}, or null when none runs there. */
    @Nullable
    public static ServerServices.Host serviceOf(final ServerLevel level, @Nullable final NetworkUuid network) {
        return ServerServices.find(level, network, Programs.BACKUP_SERVICE);
    }

    /**
     * Runs the job called {@code name} of the service at {@code host} now, for {@code network}: by hand, from the
     * console, or as its Automation Engine job.
     */
    public static Outcome run(final ServerLevel level, final NetworkUuid network, final ServerServices.Host host,
                              final String name) {
        final BackupJobs jobs = host.rack().backupsAt(host.slot());
        final BackupJob job = jobs == null ? null : jobs.named(name);
        if (job == null) {
            return new Outcome(BackupJob.RunState.NEVER, BackupTexts.NO_JOB.with(name));
        }
        if (job.result() == BackupJob.RunState.RUNNING && underWay(level, network, job)) {
            return new Outcome(BackupJob.RunState.RUNNING, BackupTexts.BUSY.with(job.name()));
        }
        final Outcome outcome = job.target().library() ? toLibrary(level, network, host, job)
                : toDrive(level, network, host, job);
        jobs.put(job.ran(level.getDayTime(), outcome.result()));
        host.changed();
        return outcome;
    }

    /**
     * Restores the backup {@code id} onto {@code network}: of it, {@code parts}, and the system images of the
     * computers {@code images} names. One on a drive comes back at once; one on a tape waits for the library's drive
     * to wind to it.
     */
    public static Text restore(final ServerLevel level, final NetworkUuid network, final UUID id,
                               final Set<BackupPart> parts, final Set<UUID> images) {
        final BackupArchive.Backup backup = BackupArchive.of(level).get(id);
        if (backup == null) {
            return BackupTexts.MISSING.text();
        }
        for (final TapeLibrary library : TapeLibrary.of(level, network)) {
            final int tape = library.tapeHolding(id);
            if (tape >= 0) {
                final TapeWork.Read work = new TapeWork.Read(id, backup.job(), network, parts, images);
                return library.rack().queueTapeWork(library.row(), work) ? BackupTexts.WINDING.with(tape + 1)
                        : BackupTexts.QUEUE_FULL.text();
            }
        }
        if (BackupHolders.onDrive(level, network, id) == null) {
            return BackupTexts.MISSING.text();
        }
        final Restored restored = apply(level, network, backup, parts, images);
        return BackupTexts.RESTORED.with(restored.items(), restored.machines());
    }

    /**
     * What restoring {@code backup} puts back: its items through the book of losses, then the rest of {@code parts}.
     * Each item comes back only as far as the network has room for it, and only that much is marked given back.
     */
    public static Restored apply(final ServerLevel level, final NetworkUuid network,
                                 final BackupArchive.Backup backup, final Set<BackupPart> parts,
                                 final Set<UUID> images) {
        long items = 0L;
        if (parts.contains(BackupPart.ITEMS) && backup.parts().contains(BackupPart.ITEMS)) {
            final NetworkLosses losses = NetworkLosses.of(level);
            final LossBook<StorageKey> book = losses.book(network);
            final NetworkStorage storage = NetworkStorage.of(level, network);
            final Map<StorageKey, Long> held = BackupContents.items(level, backup.content());
            for (final Map.Entry<StorageKey, Long> back : book.wouldRestore(held, backup.madeAt()).entrySet()) {
                final long put = storage.insert(back.getKey(), back.getValue());
                book.giveBack(back.getKey(), put, backup.madeAt());
                items += put;
            }
            losses.changed();
        }
        final Set<BackupPart> rest = EnumSet.noneOf(BackupPart.class);
        for (final BackupPart part : parts) {
            if (part != BackupPart.ITEMS && backup.parts().contains(part)) {
                rest.add(part);
            }
        }
        final int machines = rest.isEmpty() ? 0
                : BackupContents.restore(level, network, backup.content(), rest, images);
        return new Restored(items, machines);
    }

    /**
     * What a restore put back.
     *
     * @param items    how many items came back
     * @param machines how many computers were written
     */
    public record Restored(long items, int machines) {
    }

    /**
     * One tick of the drives of {@code rack}'s Tape Libraries that have work: a drive with none under way takes the
     * next, a winding one winds, and one that got there reads or writes. Work whose library or tape went is cut short.
     */
    public static void tick(final ServerLevel level, final ServerRackBlockEntity rack) {
        for (int row = 0; row < ServerRackBlockEntity.CAPACITY_U; row++) {
            final TapeWork work = rack.tapeWork(row);
            if (work == null) {
                continue;
            }
            final TapeLibrary library = TapeLibrary.at(rack, row);
            final TapeDrive drive = rack.tapeDrive(row);
            if (library == null || drive.busy() && library.tape(drive.tape()).isEmpty()) {
                stop(level, rack, row, work, BackupJob.RunState.INTERRUPTED);
                continue;
            }
            if (!drive.busy()) {
                begin(level, library, work);
                continue;
            }
            drive.tick();
            if (drive.there()) {
                finish(level, library, work);
            }
        }
    }

    /* A backup onto a Tape Library: taken now, written once the drive has wound the tape to its end. */
    private static Outcome toLibrary(final ServerLevel level, final NetworkUuid network,
                                     final ServerServices.Host host, final BackupJob job) {
        final BackupJob.Target target = job.target();
        final TapeLibrary library = TapeLibrary.find(level, network, target.rack(), target.row());
        if (library == null || library.tapes() == 0) {
            return new Outcome(BackupJob.RunState.NO_TARGET, BackupTexts.NO_TARGET.with(job.name()));
        }
        final BackupContents.Captured captured = BackupContents.capture(level, network, job.parts(),
                library.format().era());
        final long size = Math.max(1L, Math.ceilDiv(captured.weight(), StorageKey.MB_EQ_PER_ITEM));
        if (library.tapeWithRoom(size) < 0) {
            return new Outcome(BackupJob.RunState.NO_ROOM, BackupTexts.NO_ROOM.with(job.name()));
        }
        final BackupArchive.Backup backup = new BackupArchive.Backup(UUID.randomUUID(), job.name(),
                level.getGameTime(), level.getDayTime(), job.parts(), captured.weight(), captured.items(),
                captured.content());
        final TapeWork.Write work = new TapeWork.Write(backup, size, host.rack().getBlockPos().asLong(), host.slot());
        if (!library.rack().queueTapeWork(library.row(), work)) {
            return new Outcome(BackupJob.RunState.NO_TARGET, BackupTexts.QUEUE_FULL.text());
        }
        return new Outcome(BackupJob.RunState.RUNNING, BackupTexts.QUEUED.with(job.name()));
    }

    /* A backup onto another server's drive: a file naming the backup, weighing what the backup weighs there. */
    private static Outcome toDrive(final ServerLevel level, final NetworkUuid network, final ServerServices.Host host,
                                   final BackupJob job) {
        final BackupJob.Target target = job.target();
        final NetworkMachines.Machine server = NetworkMachines.find(level, network, new NodeUuid(target.server()));
        final List<ItemStack> drives = server == null ? List.of() : server.host().diskStacks();
        if (server == null || target.drive() < 0 || target.drive() >= drives.size()
                || drives.get(target.drive()).isEmpty()) {
            return new Outcome(BackupJob.RunState.NO_TARGET, BackupTexts.NO_TARGET.with(job.name()));
        }
        final ItemStack drive = drives.get(target.drive());
        final Era era = DiskFilesystem.eraOf(drive);
        final FilesystemKind kind = FileAccess.filesystemKindOf(server.host());
        final BackupContents.Captured captured = BackupContents.capture(level, network, job.parts(), era);
        final UUID id = UUID.randomUUID();
        final String content = new BackupFile(id, captured.weight() * EraStorage.bytesPerMbEq(era)).write();
        if (kind == FilesystemKind.NONE || !DiskFilesystem.mkdirs(drive, FOLDER, kind)) {
            return new Outcome(BackupJob.RunState.NO_TARGET, BackupTexts.NO_TARGET.with(job.name()));
        }
        final String path = DiskFilesystem.uniquePath(drive, FsPaths.join(FOLDER, job.name()), ".bak", content);
        final DiskFilesystem.WriteResult wrote = DiskFilesystem.write(drive, path, FileType.BAK, content,
                DriveTable.freeWeightOf(drive), kind, level.getGameTime());
        if (wrote != DiskFilesystem.WriteResult.OK) {
            return wrote == DiskFilesystem.WriteResult.DISK_FULL
                    ? new Outcome(BackupJob.RunState.NO_ROOM, BackupTexts.NO_ROOM.with(job.name()))
                    : new Outcome(BackupJob.RunState.NO_TARGET, BackupTexts.NO_TARGET.with(job.name()));
        }
        server.host().setChanged();
        BackupArchive.of(level).put(new BackupArchive.Backup(id, job.name(), level.getGameTime(), level.getDayTime(),
                job.parts(), captured.weight(), captured.items(), captured.content()));
        return new Outcome(BackupJob.RunState.DONE, BackupTexts.WRITTEN.with(job.name()));
    }

    /** Whether a running job's backup still waits at the drive of its library, rather than lost with its rack. */
    public static boolean underWay(final ServerLevel level, final NetworkUuid network, final BackupJob job) {
        final TapeLibrary library = job.target().library()
                ? TapeLibrary.find(level, network, job.target().rack(), job.target().row()) : null;
        if (library == null) {
            return false;
        }
        for (final TapeWork work : library.rack().tapeQueue(library.row())) {
            if (work instanceof TapeWork.Write && work.job().equalsIgnoreCase(job.name())) {
                return true;
            }
        }
        return false;
    }

    /* The drive takes its next work: the tape it needs, wound from where its head is to where the work is done. */
    private static void begin(final ServerLevel level, final TapeLibrary library, final TapeWork work) {
        final TapeFormat format = library.format();
        if (work instanceof TapeWork.Write write) {
            final int tape = library.tapeWithRoom(write.size());
            if (tape < 0) {
                stop(level, library.rack(), library.row(), work, BackupJob.RunState.NO_ROOM);
                return;
            }
            final TapeContents on = library.contents(tape);
            library.drive().start(tape, format.windTicks(on.head(), on.used()), TapeDrive.State.WRITING);
        } else if (work instanceof TapeWork.Read read) {
            final int tape = library.tapeHolding(read.backup());
            if (tape < 0) {
                stop(level, library.rack(), library.row(), work, BackupJob.RunState.INTERRUPTED);
                return;
            }
            final TapeContents on = library.contents(tape);
            library.drive().start(tape, format.windTicks(on.head(), on.spotOf(read.backup()).start()),
                    TapeDrive.State.READING);
        }
    }

    /* The tape got there: the backup is written after the last on it, or read back and restored. */
    private static void finish(final ServerLevel level, final TapeLibrary library, final TapeWork work) {
        final int tape = library.drive().tape();
        final TapeContents on = library.contents(tape);
        if (work instanceof TapeWork.Write write) {
            if (library.format().capacityItems() - on.used() < write.size()) {
                stop(level, library.rack(), library.row(), work, BackupJob.RunState.NO_ROOM);
                return;
            }
            final BackupArchive.Backup backup = write.backup();
            library.write(tape, on.written(new TapeContents.Spot(backup.id(), on.used(), write.size(), backup.job(),
                    backup.clock(), backup.items())));
            BackupArchive.of(level).put(backup);
            stop(level, library.rack(), library.row(), work, BackupJob.RunState.DONE);
            return;
        }
        final TapeWork.Read read = (TapeWork.Read) work;
        final TapeContents.Spot spot = on.spotOf(read.backup());
        final BackupArchive.Backup backup = BackupArchive.of(level).get(read.backup());
        if (spot != null) {
            library.write(tape, on.wound(spot.start() + spot.size()));
        }
        if (spot != null && backup != null) {
            apply(level, read.network(), backup, read.parts(), read.images());
        }
        stop(level, library.rack(), library.row(), work, BackupJob.RunState.DONE);
    }

    /* Ends the work at a drive, telling the job that wrote it how its run ended. */
    private static void stop(final ServerLevel level, final ServerRackBlockEntity rack, final int row,
                             final TapeWork work, final BackupJob.RunState how) {
        rack.tapeDrive(row).finish();
        rack.tapeWorkDone(row);
        if (work instanceof TapeWork.Write write
                && Loaded.blockEntity(level, BlockPos.of(write.serviceRack())) instanceof ServerRackBlockEntity at) {
            final BackupJobs jobs = at.backupsAt(write.serviceRow());
            final BackupJob job = jobs == null ? null : jobs.named(write.job());
            if (job != null) {
                jobs.put(job.ended(how));
                at.flushServices(write.serviceRow());
            }
        }
    }
}
