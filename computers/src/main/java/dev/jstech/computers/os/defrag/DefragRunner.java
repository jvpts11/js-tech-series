/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.defrag;

import dev.jstech.computers.backup.LossCause;
import dev.jstech.computers.backup.NetworkLosses;
import dev.jstech.computers.eventlog.EventLevel;
import dev.jstech.computers.eventlog.NetworkEvents;
import dev.jstech.computers.hardware.DiskSpec;
import dev.jstech.computers.hardware.EraStorage;
import dev.jstech.computers.machine.DriveTable;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.fs.FilesystemContents;
import dev.jstech.computers.os.fs.StoredFile;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.computers.storage.Corruption;
import dev.jstech.computers.storage.DiskHealth;
import dev.jstech.computers.storage.DriveVolumes;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.storage.StorageVolume;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.uuid.NodeUuid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The Defrag as something a machine does over time (computers/docs/design/storage.md): a pass over one disk tries
 * every corrupted item once, with the chance that kind of disk gives, spread over the time a pass takes on it; what
 * comes back is part of the disk again, what does not is lost, and at the end a quarter of the bad sectors left are
 * mended. While a pass runs, that disk answers the network twice as slowly.
 *
 * <p>The pass is held by the machine's console and moved along by the machine, like a setup; every way of starting
 * one comes here: DEFRAG's screen, the Frames windows, fsck, and the weekly schedule of Frames 7, 10 and 11.
 */
@TextHolder
public final class DefragRunner {

    private static final TextKey NO_SYSTEM = TextKey.of("jsc.defrag.no_system",
            "The Defrag runs on a system: install one first.");
    private static final TextKey NO_DISK = TextKey.of("jsc.defrag.no_disk", "There is no disk %s.");
    private static final TextKey DISK_FAILED = TextKey.of("jsc.defrag.disk_failed", "%s: this disk has failed.");
    private static final TextKey ALREADY = TextKey.of("jsc.defrag.already", "The Defrag is already running on %s.");

    /* How often the schedule comes round, in ticks: once a week of game days. */
    private static final long WEEK = 7L * 24_000L;
    /*
     * The machines whose disk a pass is running on, by the tick they last said so: whoever reads them a disk takes
     * twice as long while it runs. Told every tick by the pass itself, so one that ended or stopped is let go at once.
     */
    private static final Map<NodeUuid, Long> PASSES = new ConcurrentHashMap<>();

    private DefragRunner() {
    }

    /** How a machine's system shows its Defrag; DEFRAG's when it has none to tell. */
    public static DefragLook look(final IOsHost host) {
        final OsDef os = host.installedOs();
        return os == null ? DefragLook.DOS : DefragLook.of(os.platform(), os.familyRank());
    }

    /** The machine's disks in the order its systems name them: the system disk first, then the others in slot order. */
    public static List<ItemStack> disks(final IOsHost host) {
        final List<ItemStack> out = new ArrayList<>();
        final ItemStack system = host.systemDisk();
        if (DriveVolumes.isDisk(system)) {
            out.add(system);
        }
        for (int slot = 0; slot < host.diskSlots(); slot++) {
            final ItemStack disk = host.diskInSlot(slot);
            if (disk != system && DriveVolumes.isDisk(disk)) {
                out.add(disk);
            }
        }
        return out;
    }

    /**
     * How many times longer a disk of {@code node} takes to answer the network at {@code now}: twice while a pass runs
     * on that machine, once otherwise.
     */
    public static int waitFactor(final NodeUuid node, final long now) {
        final Long last = PASSES.get(node);
        return last != null && now - last <= 1L ? 2 : 1;
    }

    /** Starts a pass over the machine's {@code index}-th disk, or says why it cannot. */
    public static Optional<Text> begin(final IOsHost host, final ServerLevel level, final int index) {
        final ComputerConsoleState console = host.console();
        if (console == null || host.installedOs() == null) {
            return Optional.of(NO_SYSTEM.text());
        }
        final DefragLook look = look(host);
        final List<ItemStack> disks = disks(host);
        if (index < 0 || index >= disks.size()) {
            return Optional.of(NO_DISK.with(look.drive(Math.max(0, index))));
        }
        final MachineDefrag book = console.defrag();
        if (book.job() != null) {
            return Optional.of(ALREADY.with(look.drive(book.job().disk())));
        }
        final ItemStack disk = disks.get(index);
        if (DriveVolumes.health(disk).dead() || !(HardwareRegistry.disk(disk) instanceof DiskSpec spec)) {
            return Optional.of(DISK_FAILED.with(look.drive(index)));
        }
        final StorageVolume volume = DriveVolumes.of(disk);
        long found = volume.corruptedFiles().files().size();
        for (final Map.Entry<StorageKey, Long> entry : volume.corrupted().entrySet()) {
            found += units(entry.getKey(), entry.getValue());
        }
        final int work = volume.corrupted().size() + volume.corruptedFiles().files().size();
        book.begin(new DefragJob(volume.id(), index, Corruption.passTicks(spec.tier()),
                Corruption.recoveryChance(spec.tier(), volume.damage()), work, found, volume.badSectors()));
        host.markChanged();
        return Optional.empty();
    }

    /** Stops the pass where it stands: what it gave back stays given back, the rest waits for the next pass. */
    public static void stop(final IOsHost host) {
        final ComputerConsoleState console = host.console();
        if (console != null && console.defrag().job() != null) {
            console.defrag().stopped();
            host.markChanged();
        }
    }

    /** Pauses the pass where it stands, or lets a paused one go on. */
    public static void togglePause(final IOsHost host) {
        final ComputerConsoleState console = host.console();
        final DefragJob job = console == null ? null : console.defrag().job();
        if (job != null) {
            job.paused(!job.paused());
            host.markChanged();
        }
    }

    /** Turns the weekly schedule on or off, on the systems that have one. */
    public static void toggleSchedule(final IOsHost host) {
        final ComputerConsoleState console = host.console();
        if (console != null && look(host).scheduled()) {
            console.defrag().schedule(!console.defrag().schedule());
            host.markChanged();
        }
    }

    /** One tick of the machine's Defrag: the schedule, and the pass under way, while the machine runs a system. */
    public static void tick(final IOsHost host, final ServerLevel level) {
        final ComputerConsoleState console = host.console();
        if (console == null || !host.isRunning() || host.installedOs() == null) {
            return;
        }
        final MachineDefrag book = console.defrag();
        final long now = level.getGameTime();
        if (book.job() == null) {
            schedule(host, level, book, now);
            return;
        }
        final DefragJob job = book.job();
        final ItemStack disk = diskOf(host, job.volume());
        if (disk == null) {
            // The disk was taken out or wiped under the pass: there is nothing left to repair.
            book.stopped();
            host.markChanged();
            return;
        }
        if (host.nodeUuid() != null) {
            PASSES.put(host.nodeUuid(), now);
        }
        final StorageVolume volume = DriveVolumes.of(disk);
        final int due = job.advance();
        // What the pass gives up on stops existing, so a server's network writes it in its book of losses.
        final Map<StorageKey, Long> lost = new HashMap<>();
        for (int i = 0; i < due; i++) {
            tryOne(volume, disk, job, level.random, lost);
        }
        NetworkLosses.lost(level, host, lost, LossCause.DEFRAG, job.disk() + 1);
        if (due > 0) {
            DriveVolumes.refreshUsage(disk, volume);
            host.storageChanged();
        } else {
            host.markUnsaved();
        }
        if (job.done()) {
            finish(host, level, book, job, disk, volume, now);
        }
    }

    /** What the machine's Defrag shows now, after doing what {@code path} asks of it. */
    public static String reply(final IOsHost host, final ServerLevel level, final String path) {
        final String action = DefragState.actionOf(path);
        final int index = DefragState.diskOf(path);
        switch (action) {
            case DefragState.START -> begin(host, level, index);
            case DefragState.STOP -> stop(host);
            case DefragState.PAUSE -> togglePause(host);
            case DefragState.SCHEDULE -> toggleSchedule(host);
            default -> {
                // Only asked to be told.
            }
        }
        return state(host, level).write();
    }

    /** What the machine's Defrag shows now. */
    public static DefragState state(final IOsHost host, final ServerLevel level) {
        final DefragLook look = look(host);
        final ComputerConsoleState console = host.console();
        final MachineDefrag book = console == null ? new MachineDefrag() : console.defrag();
        final OsDef os = host.installedOs();
        final List<DefragState.Disk> rows = new ArrayList<>();
        final List<ItemStack> disks = disks(host);
        for (int i = 0; i < disks.size(); i++) {
            rows.add(row(host, level, os, look, i, disks.get(i), book));
        }
        final DefragJob job = book.job();
        final DefragState.Run running = job == null ? null : new DefragState.Run(job.disk(), job.percent(),
                job.found(), job.recovered(), job.lost(), job.badBefore(), job.paused());
        return new DefragState(look, rows, running, book.last(), book.schedule());
    }

    /*
     * A machine on a weekly schedule repairs, once a week, the first disk that has something to repair: corrupted
     * items or bad sectors.
     */
    private static void schedule(final IOsHost host, final ServerLevel level, final MachineDefrag book,
                                 final long now) {
        // Asked every tick, so the cheapest question first.
        if (now - book.scheduledAt() < WEEK || !book.schedule() || !look(host).scheduled()) {
            return;
        }
        book.scheduledAt(now);
        final List<ItemStack> disks = disks(host);
        for (int i = 0; i < disks.size(); i++) {
            final DiskHealth health = DriveVolumes.health(disks.get(i));
            if (!health.dead() && (health.corruptedItems() > 0L || health.badSectors() > 0)) {
                begin(host, level, i);
                return;
            }
        }
        host.markUnsaved();
    }

    /*
     * Tries the next corrupted thing on the disk: a corrupted kind, all of it at once, each item coming back at the
     * pass's chance; then the system files, each one coming back at the same chance into the place it had, unless
     * something has taken that place since.
     */
    private static void tryOne(final StorageVolume volume, final ItemStack disk, final DefragJob job,
                               final RandomSource random, final Map<StorageKey, Long> lost) {
        final Iterator<Map.Entry<StorageKey, Long>> kinds = volume.corrupted().entrySet().iterator();
        if (kinds.hasNext()) {
            final Map.Entry<StorageKey, Long> next = kinds.next();
            final StorageKey key = next.getKey();
            final long count = next.getValue();
            final long back = Corruption.share(count, job.chance() / 100.0, random::nextDouble);
            volume.recover(key, back);
            volume.lose(key, count - back);
            if (count > back) {
                lost.merge(key, count - back, Long::sum);
            }
            final long backUnits = units(key, back);
            job.tried(backUnits, units(key, count) - backUnits);
            return;
        }
        final Iterator<StoredFile> files = volume.corruptedFiles().files().values().iterator();
        if (!files.hasNext()) {
            job.tried(0L, 0L);
            return;
        }
        final StoredFile file = files.next();
        final boolean back = random.nextInt(100) < job.chance();
        volume.settleFile(file.path());
        final FilesystemContents now = disk.getOrDefault(ComputingComponents.FILESYSTEM.get(),
                FilesystemContents.EMPTY);
        if (back && !now.files().containsKey(file.path())) {
            disk.set(ComputingComponents.FILESYSTEM.get(), now.with(file));
        }
        job.tried(back ? 1L : 0L, back ? 0L : 1L);
    }

    /* The pass has run its time: a quarter of the bad sectors left are mended, and its end is kept and told. */
    private static void finish(final IOsHost host, final ServerLevel level, final MachineDefrag book,
                               final DefragJob job, final ItemStack disk, final StorageVolume volume, final long now) {
        final int badBefore = volume.badSectors();
        final int badAfter = Corruption.afterPass(badBefore);
        volume.badSectors(badAfter);
        DriveVolumes.refreshUsage(disk, volume);
        host.storageChanged();
        final DefragState.Result result = new DefragState.Result(job.disk(), job.found(), job.recovered(), job.lost(),
                job.badBefore(), badAfter);
        book.ended(result, volume.id(), now);
        host.markChanged();
        if (host.nodeUuid() != null) {
            PASSES.remove(host.nodeUuid());
            NetworkEvents.record(level, host.networkUuid(), job.lost() > 0L ? EventLevel.WARN : EventLevel.INFO,
                    host.nodeUuid(), NetworkEvents.machineName(host.customName(), host.machine(), host.nodeUuid()),
                    NetworkEvents.DEFRAG_DONE.with(look(host).drive(job.disk()), job.found(), job.recovered(),
                            job.lost(), job.badBefore(), badAfter));
        }
    }

    private static DefragState.Disk row(final IOsHost host, final ServerLevel level, @Nullable final OsDef os,
                                        final DefragLook look, final int index, final ItemStack disk,
                                        final MachineDefrag book) {
        final DiskSpec spec = HardwareRegistry.disk(disk);
        final DiskHealth health = DriveVolumes.health(disk);
        final long capacityMb = spec == null ? 0L : spec.capacityMb();
        final long mbPerItem = spec == null ? 0L : EraStorage.mbPerItem(spec.era());
        final long freeMb = DriveTable.freeWeightOf(disk) / StorageKey.MB_EQ_PER_ITEM * mbPerItem;
        final int used = capacityMb <= 0L ? 0 : (int) Math.min(Corruption.WHOLE,
                (capacityMb - Math.min(capacityMb, freeMb)) * Corruption.WHOLE / capacityMb);
        final String fileSystem = os == null ? "" : OsRegistry.fileSystemOf(os, host.installedEra()).displayName();
        final UUID volume = disk.get(ComputingComponents.DISK_VOLUME.get());
        return new DefragState.Disk(index, look.drive(index), kindOf(spec), fileSystem,
                capacityMb, freeMb, used, health.damage(), health.badSectors(), health.dead(),
                health.corruptedItems(), book.lastRun(volume), DriveVolumes.usage(disk).types());
    }

    /* A disk's kind as the state says it: 0 a hard disk, 1 an SSD, 2 NVMe. */
    private static int kindOf(@Nullable final DiskSpec spec) {
        if (spec == null) {
            return 0;
        }
        return switch (spec.tier()) {
            case HDD -> 0;
            case SSD -> 1;
            case NVME -> 2;
        };
    }

    /* The machine's disk whose volume is {@code volume}, or null when it is no longer there. */
    @Nullable
    private static ItemStack diskOf(final IOsHost host, final UUID volume) {
        for (final ItemStack disk : disks(host)) {
            if (volume.equals(disk.get(ComputingComponents.DISK_VOLUME.get()))) {
                return disk;
            }
        }
        return null;
    }

    /* How many corrupted things {@code count} of a key is, as the Defrag counts them: items by the item, fluids by
     * the thousand millibuckets, any part of a thousand counting as one. */
    private static long units(final StorageKey key, final long count) {
        if (count <= 0L) {
            return 0L;
        }
        return key.isItem() ? count : (count + StorageKey.MB_EQ_PER_ITEM - 1) / StorageKey.MB_EQ_PER_ITEM;
    }
}
