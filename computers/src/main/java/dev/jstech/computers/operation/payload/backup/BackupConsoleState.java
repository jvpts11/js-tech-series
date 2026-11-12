/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.backup;

import dev.jstech.computers.backup.BackupArchive;
import dev.jstech.computers.backup.BackupContents;
import dev.jstech.computers.backup.BackupHolders;
import dev.jstech.computers.backup.BackupJob;
import dev.jstech.computers.backup.BackupJobs;
import dev.jstech.computers.backup.BackupPart;
import dev.jstech.computers.backup.BackupSchedule;
import dev.jstech.computers.backup.BackupService;
import dev.jstech.computers.backup.LossBook;
import dev.jstech.computers.backup.NetworkLosses;
import dev.jstech.computers.backup.NetworkMachines;
import dev.jstech.computers.backup.TapeContents;
import dev.jstech.computers.backup.TapeDrive;
import dev.jstech.computers.backup.TapeLibrary;
import dev.jstech.computers.backup.TapeWork;
import dev.jstech.computers.blockentity.RackUnitHost;
import dev.jstech.computers.blockentity.ServerServices;
import dev.jstech.computers.hardware.EraStorage;
import dev.jstech.computers.machine.DriveTable;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.BackupStatePayload;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextBounds;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Builds what a Backup Console shows from the network as it is now: the service's jobs, what a new job would keep and
 * where it could write, the backups the network holds, the backup chosen with what the book of losses lost since it,
 * and the tapes of the library shown.
 */
final class BackupConsoleState {

    private BackupConsoleState() {
    }

    /**
     * The console's state for {@code network}, served by {@code host} or by no service, showing the library
     * {@code library} names and the backup {@code chosen}, with {@code note} saying how the last thing asked went.
     */
    static BackupStatePayload of(final ServerLevel level, @Nullable final NetworkUuid network,
                                 @Nullable final ServerServices.Host host, final BackupJob.Target library,
                                 final UUID chosen, final Text note) {
        if (network == null || host == null) {
            return new BackupStatePayload(new BackupStatePayload.Service(false, "", false, note), List.of(),
                    BackupStatePayload.Form.EMPTY, List.of(), BackupStatePayload.Detail.NONE,
                    BackupStatePayload.Tapes.NONE);
        }
        final List<NetworkMachines.Machine> machines = NetworkMachines.of(level, network);
        final List<TapeLibrary> libraries = TapeLibrary.of(level, network);
        final List<BackupHolders.Held> held = BackupHolders.of(level, network);
        return new BackupStatePayload(
                new BackupStatePayload.Service(true, clip(host.hostname()), BackupSchedule.engineRuns(level, network),
                        note),
                jobs(level, network, host, machines),
                form(level, network, host, machines, libraries),
                rows(held),
                detail(level, network, held, chosen),
                tapes(libraries, library));
    }

    private static List<BackupStatePayload.Job> jobs(final ServerLevel level, final NetworkUuid network,
                                                     final ServerServices.Host host,
                                                     final List<NetworkMachines.Machine> machines) {
        final BackupJobs jobs = host.rack().backupsAt(host.slot());
        final List<BackupStatePayload.Job> out = new ArrayList<>();
        if (jobs == null) {
            return out;
        }
        for (final BackupJob job : jobs.all()) {
            final BackupJob.Target target = job.target();
            final TapeLibrary library = target.library()
                    ? TapeLibrary.find(level, network, target.rack(), target.row()) : null;
            final String where = target.library() ? library == null ? "" : BackupHolders.libraryName(library)
                    : machineName(machines, target.server());
            // A job whose backup went with its library's rack is shown as cut short, which is what it was.
            final BackupJob.RunState result = job.result() == BackupJob.RunState.RUNNING
                    && !BackupService.underWay(level, network, job) ? BackupJob.RunState.INTERRUPTED : job.result();
            out.add(new BackupStatePayload.Job(job.name(), BackupPart.mask(job.parts()), target, clip(where),
                    job.when(), job.lastRun(), result));
        }
        return out;
    }

    private static BackupStatePayload.Form form(final ServerLevel level, final NetworkUuid network,
                                                final ServerServices.Host host,
                                                final List<NetworkMachines.Machine> machines,
                                                final List<TapeLibrary> libraries) {
        long items = 0L;
        long weight = 0L;
        for (final Map.Entry<StorageKey, Long> stored : NetworkStorage.of(level, network).query().entrySet()) {
            items += stored.getValue();
            weight += stored.getKey().weight(stored.getValue());
        }
        final List<String> files = new ArrayList<>();
        final List<String> names = new ArrayList<>();
        final List<String> wallets = new ArrayList<>();
        final List<BackupStatePayload.Place> drives = new ArrayList<>();
        for (final NetworkMachines.Machine machine : machines) {
            names.add(clip(machine.name()));
            if (BackupContents.keepsWallets(machine.host())) {
                wallets.add(clip(machine.name()));
            }
            final String home = BackupContents.homeOf(machine.host().installedOs());
            if (!home.isEmpty()) {
                files.add(TextBounds.clip(machine.name() + " " + shown(machine.host().installedOs(), home),
                        BackupStatePayload.MOST_LINE));
            }
            if (machine.host() instanceof RackUnitHost unit
                    && !(unit.rack() == host.rack() && unit.row() == host.slot())) {
                addDrives(drives, machine);
            }
        }
        final List<BackupStatePayload.Place> places = new ArrayList<>();
        for (final TapeLibrary library : libraries) {
            final long mbPerItem = EraStorage.mbPerItem(library.format().era());
            long free = 0L;
            for (int n = 0; n < library.slots(); n++) {
                free += library.tape(n).isEmpty() ? 0L : library.format().capacityItems() - library.contents(n).used();
            }
            places.add(new BackupStatePayload.Place(
                    BackupJob.Target.library(library.rack().getBlockPos().asLong(), library.row()),
                    BackupHolders.libraryName(library), free * mbPerItem, mbPerItem));
        }
        return new BackupStatePayload.Form(items, weight, first(files), first(names), first(wallets), first(places),
                first(drives));
    }

    private static void addDrives(final List<BackupStatePayload.Place> out, final NetworkMachines.Machine machine) {
        final List<ItemStack> drives = machine.host().diskStacks();
        for (int d = 0; d < drives.size(); d++) {
            final ItemStack drive = drives.get(d);
            if (!drive.isEmpty()) {
                final long mbPerItem = EraStorage.mbPerItem(DiskFilesystem.eraOf(drive));
                out.add(new BackupStatePayload.Place(BackupJob.Target.disk(machine.node().value(), d),
                        clip(machine.name()), DriveTable.freeWeightOf(drive) * mbPerItem / StorageKey.MB_EQ_PER_ITEM,
                        mbPerItem));
            }
        }
    }

    private static List<BackupStatePayload.Held> rows(final List<BackupHolders.Held> held) {
        final List<BackupStatePayload.Held> out = new ArrayList<>();
        for (final BackupHolders.Held one : first(held, BackupStatePayload.MOST_ROWS)) {
            out.add(new BackupStatePayload.Held(one.id(), one.job(), one.clock(), BackupPart.mask(one.parts()),
                    one.items(), one.sizeMb(), one.onTape(), one.tape(), clip(one.library()), clip(one.machine()),
                    one.drive()));
        }
        return out;
    }

    /*
     * The backup chosen: its images, and the losses since it that the book has not given back, each line with what
     * restoring the backup gives back for it, the oldest first, as the book gives back.
     */
    private static BackupStatePayload.Detail detail(final ServerLevel level, final NetworkUuid network,
                                                    final List<BackupHolders.Held> held, final UUID chosen) {
        BackupHolders.Held where = null;
        for (final BackupHolders.Held one : held) {
            if (one.id().equals(chosen)) {
                where = one;
            }
        }
        final BackupArchive.Backup backup = where == null ? null : BackupArchive.of(level).get(chosen);
        if (backup == null) {
            return BackupStatePayload.Detail.NONE;
        }
        final List<BackupStatePayload.Image> images = new ArrayList<>();
        for (final BackupContents.Image image : BackupContents.images(backup.content())) {
            images.add(new BackupStatePayload.Image(image.node(), clip(image.machine()), clip(image.system()),
                    Math.ceilDiv(image.weight() * where.mbPerItem(), StorageKey.MB_EQ_PER_ITEM)));
        }
        final List<BackupStatePayload.Loss> losses = new ArrayList<>();
        if (backup.parts().contains(BackupPart.ITEMS)) {
            final Map<StorageKey, Long> kept = BackupContents.items(level, backup.content());
            final Map<StorageKey, Long> budget = new HashMap<>(
                    NetworkLosses.of(level).book(network).wouldRestore(kept, backup.madeAt()));
            for (final LossBook.Loss<StorageKey> loss : NetworkLosses.of(level).book(network).since(backup.madeAt())) {
                if (loss.left() <= 0L || losses.size() == BackupStatePayload.MOST_ROWS) {
                    continue;
                }
                final long back = Math.min(loss.left(), budget.getOrDefault(loss.key(), 0L));
                budget.merge(loss.key(), -back, Long::sum);
                losses.add(new BackupStatePayload.Loss(loss.key(), loss.left(), loss.clock(), loss.cause().id(),
                        clip(loss.machine()), loss.disk(), kept.getOrDefault(loss.key(), 0L), back));
            }
        }
        int windTape = -1;
        int permille = 0;
        long ticksLeft = 0L;
        for (final TapeLibrary library : TapeLibrary.of(level, network)) {
            if (library.work() instanceof TapeWork.Read read && read.backup().equals(chosen)
                    && library.drive().busy()) {
                windTape = library.drive().tape();
                permille = library.drive().permille();
                ticksLeft = library.drive().ticksLeft();
            }
        }
        return new BackupStatePayload.Detail(chosen, first(images), losses, windTape, permille, ticksLeft);
    }

    private static BackupStatePayload.Tapes tapes(final List<TapeLibrary> libraries,
                                                  final BackupJob.Target wanted) {
        if (libraries.isEmpty()) {
            return BackupStatePayload.Tapes.NONE;
        }
        TapeLibrary shown = libraries.getFirst();
        for (final TapeLibrary library : libraries) {
            if (wanted.library() && library.rack().getBlockPos().asLong() == wanted.rack()
                    && library.row() == wanted.row()) {
                shown = library;
            }
        }
        final long mbPerItem = EraStorage.mbPerItem(shown.format().era());
        final List<BackupStatePayload.Tape> slots = new ArrayList<>();
        for (int n = 0; n < shown.slots(); n++) {
            final TapeContents on = shown.contents(n);
            slots.add(new BackupStatePayload.Tape(n, !shown.tape(n).isEmpty(), clip(on.label()),
                    on.used() * mbPerItem, on.spots().isEmpty() ? -1L : on.spots().getLast().madeAt()));
        }
        final TapeDrive drive = shown.drive();
        final TapeWork work = shown.work();
        final BackupStatePayload.Drive state = work == null ? BackupStatePayload.Drive.IDLE
                : new BackupStatePayload.Drive(drive.tape(), drive.state().id(), drive.permille(),
                drive.ticksLeft(), work.job(), work instanceof TapeWork.Read);
        return new BackupStatePayload.Tapes(BackupJob.Target.library(shown.rack().getBlockPos().asLong(), shown.row()),
                BackupHolders.libraryName(shown), libraries.size(), shown.format().label(),
                shown.format().capacityMb(), slots, state);
    }

    /* The home folder as its system writes it: a drive and backslashes on Frames and MC-DOS, a slash elsewhere. */
    private static String shown(@Nullable final OsDef os, final String home) {
        final boolean windows = os != null && (os.platform() == Platform.FRAMES || os.platform() == Platform.MC_DOS);
        return windows ? "C:\\" + home.replace('/', '\\') : "/" + home;
    }

    private static String machineName(final List<NetworkMachines.Machine> machines, final UUID node) {
        for (final NetworkMachines.Machine machine : machines) {
            if (machine.node().value().equals(node)) {
                return machine.name();
            }
        }
        return "";
    }

    private static String clip(final String text) {
        return TextBounds.clip(text, BackupStatePayload.MOST_NAME);
    }

    /* The first of a list of places, as many as travel. */
    private static <T> List<T> first(final List<T> all) {
        return first(all, BackupStatePayload.MOST_PLACES);
    }

    private static <T> List<T> first(final List<T> all, final int most) {
        return all.size() > most ? List.copyOf(all.subList(0, most)) : all;
    }
}
