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
import dev.jstech.computers.os.disk.DiskReport;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;

/**
 * Snapshots and mirrors at the prompt (computers/docs/design/storage-devices.md): Linux's {@code btrfs}, which takes a
 * snapshot of a partition, lists and deletes them, and sets one as what the next start mounts; and FreeBSD's
 * {@code zfs}, which takes them and rolls back to one at once, and {@code zpool}, which scrubs a pool and reports what
 * its mirror repaired. They ask the machine the way the other disk tools do, and read the same report.
 */
public final class SnapshotCommands {

    /* A btrfs file system's top level, which set-default takes to mean no snapshot at all. */
    private static final int TOP_LEVEL = 5;
    /* The subvolume a btrfs installer lays the root out in, which is the partition as it stands. */
    private static final int ROOT_ID = 256;
    private static final int HOME_ID = 257;
    private static final long TICKS_PER_DAY = 24_000L;
    private static final long TICKS_PER_SECOND = 20L;
    private static final long MB_PER_GB = 1024L;

    private SnapshotCommands() {
    }

    /** The snapshot tools of the Unix family's prompts. */
    static List<ICliCommand> posix() {
        return List.of(new Btrfs(), new Zfs(), new Zpool());
    }

    /* The partition the running system has mounted as its own: the one in use that is no swap. */
    @Nullable
    private static Found root(final DiskReport report) {
        for (final DiskReport.Disk disk : report.disks()) {
            for (final DiskReport.Part part : disk.parts()) {
                if (part.mounted() && !part.swap()) {
                    return new Found(disk, part);
                }
            }
        }
        return null;
    }

    /* The world's day a time falls on, counting from one, as the clocks on these machines do. */
    private static long dayOf(final long ticks) {
        return ticks / TICKS_PER_DAY + 1L;
    }

    /* The hour and minute of a time, the day starting at six in the morning. */
    private static String clockOf(final long ticks) {
        final long minutes = (ticks % TICKS_PER_DAY) * 60L / 1000L + 6L * 60L;
        return String.format(Locale.ROOT, "%02d:%02d", minutes / 60L % 24L, minutes % 60L);
    }

    /* A size as ZFS prints it: 0B, 512M, 41.2G. */
    private static String zfsSize(final long mb) {
        return mb <= 0L ? "0B" : DiskToolLines.human(mb);
    }

    /* A size as btrfs prints it: 0.00B, 512.00MiB, 7.81GiB. */
    private static String btrfsSize(final long mb) {
        if (mb <= 0L) {
            return "0.00B";
        }
        return mb >= MB_PER_GB ? String.format(Locale.ROOT, "%.2fGiB", mb / (double) MB_PER_GB)
                : String.format(Locale.ROOT, "%.2fMiB", (double) mb);
    }

    /* The machine's ZFS pools, each once by the member it is read from. */
    private static List<Pool> pools(final DiskReport report) {
        final List<Pool> out = new ArrayList<>();
        for (final DiskReport.Disk disk : report.disks()) {
            for (final DiskReport.Part part : disk.parts()) {
                if (part.fileSystem() != FileSystemType.ZFS || part.pool() != null && !part.pool().primary()) {
                    continue;
                }
                final String name = !part.label().isEmpty() ? part.label()
                        : part.mounted() ? "zroot" : DiskToolLines.bsdDisk(disk.index()) + "p" + part.number();
                out.add(new Pool(new Found(disk, part), name));
            }
        }
        return out;
    }

    /* The pool a dataset or snapshot is in, by the name before its first slash or at sign; null when none is. */
    @Nullable
    private static Pool pool(final DiskReport report, final String dataset) {
        final String head = dataset.split("[/@]", 2)[0];
        for (final Pool pool : pools(report)) {
            if (pool.name().equals(head)) {
                return pool;
            }
        }
        return null;
    }

    /** {@code btrfs}: Linux's tool for its copy-on-write file system's snapshots. */
    @TextHolder
    static final class Btrfs implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.snapshot.btrfs.summary",
                "take, list and delete a btrfs partition's snapshots");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.snapshot.btrfs.about", "subvolume snapshot / "
                + "/.snapshots/NAME keeps the partition as it is, items and files, weighing nothing until they change;"
                + " subvolume list / lists them by number; subvolume set-default NUMBER / has the next start mount"
                + " that snapshot instead, everything back as it was; subvolume delete lets one go; filesystem du -s"
                + " shows what one keeps and what only it keeps. Another partition is named by its device,"
                + " /dev/sdb1.");
        private static final TextKey USAGE = TextKey.of("jsc.cli.snapshot.btrfs.usage",
                "usage: btrfs subvolume {snapshot,list,set-default,delete} | btrfs filesystem du -s <path>");
        private static final TextKey CREATED = TextKey.of("jsc.cli.snapshot.btrfs.created",
                "Create a snapshot of '%s' in '%s'");
        private static final TextKey DELETED = TextKey.of("jsc.cli.snapshot.btrfs.deleted",
                "Delete subvolume (no-commit): '%s'");
        private static final TextKey LISTED = TextKey.of("jsc.cli.snapshot.btrfs.listed",
                "ID %s gen %s top level %s path %s");
        private static final TextKey DU_HEADER = TextKey.of("jsc.cli.snapshot.btrfs.du_header",
                "     Total   Exclusive  Set shared  Filename");
        private static final TextKey NEXT_START = TextKey.of("jsc.cli.snapshot.btrfs.next_start",
                "The next start mounts the snapshot as /: the files and the items as they were on day %s.");
        private static final TextKey WEIGHS = TextKey.of("jsc.cli.snapshot.btrfs.weighs",
                "The snapshot weighed nothing when taken; it weighs what changed since.");
        private static final TextKey AS_IT_STANDS = TextKey.of("jsc.cli.snapshot.btrfs.as_it_stands",
                "The next start mounts the partition as it stands.");
        private static final TextKey NO_PATH = TextKey.of("jsc.cli.snapshot.btrfs.no_path",
                "ERROR: cannot access '%s': No such file or directory");
        private static final TextKey NOT_BTRFS = TextKey.of("jsc.cli.snapshot.btrfs.not_btrfs",
                "ERROR: not a btrfs filesystem: %s");
        private static final TextKey EXISTS = TextKey.of("jsc.cli.snapshot.btrfs.exists",
                "ERROR: target path already exists: %s");
        private static final TextKey NO_DEFAULT = TextKey.of("jsc.cli.snapshot.btrfs.no_default",
                "ERROR: unable to set a default subvolume: No such file or directory");

        @Override public CommandScope scope() {
            return CommandScope.on(Platform.LINUX);
        }

        @Override public String name() {
            return "btrfs";
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
            final String verb = ctx.argCount() >= 2 ? ctx.arg(0) + " " + ctx.arg(1) : "";
            switch (verb) {
                case "subvolume snapshot" -> {
                    if (ctx.argCount() < 4) {
                        ctx.out().line(USAGE);
                    } else {
                        snapshot(ctx, ctx.arg(2), ctx.arg(3));
                    }
                }
                case "subvolume list" -> list(ctx, ctx.argCount() > 2 ? ctx.arg(2) : "/");
                case "subvolume set-default" -> {
                    if (ctx.argCount() < 3) {
                        ctx.out().line(USAGE);
                    } else {
                        setDefault(ctx, ctx.arg(2), ctx.argCount() > 3 ? ctx.arg(3) : "/");
                    }
                }
                case "subvolume delete" -> {
                    if (ctx.argCount() < 3) {
                        ctx.out().line(USAGE);
                    } else {
                        delete(ctx, ctx.arg(2));
                    }
                }
                case "filesystem du" -> du(ctx, ctx.arg(ctx.argCount() - 1));
                default -> ctx.out().line(USAGE);
            }
        }

        private static void snapshot(final CliContext ctx, final String source, final String target) {
            final Found found = btrfsAt(ctx, source);
            if (found == null) {
                return;
            }
            final DiskReport after = DiskToolLines.ask(ctx.computer(), DiskCommand.snapshot(found.disk().index(),
                    found.part().number(), nameOf(target)));
            if (after.refusal() == DiskReport.Refusal.SNAPSHOT_EXISTS) {
                ctx.out().error(EXISTS.with(target));
            } else {
                ctx.out().line(CREATED.with(source, target));
            }
        }

        private static void list(final CliContext ctx, final String path) {
            final Found found = btrfsAt(ctx, path);
            if (found == null) {
                return;
            }
            long latest = 0L;
            for (final DiskReport.Snap snap : found.part().snapshots()) {
                latest = Math.max(latest, snap.takenAt());
            }
            if (!found.part().system().isEmpty()) {
                // The layout a btrfs installer makes: the root and the homes in subvolumes of their own.
                ctx.out().line(LISTED.with(ROOT_ID, generationOf(latest), TOP_LEVEL, "root"));
                ctx.out().line(LISTED.with(HOME_ID, generationOf(latest), TOP_LEVEL, "home"));
            }
            for (final DiskReport.Snap snap : found.part().snapshots()) {
                ctx.out().line(LISTED.with(snap.number(), generationOf(snap.takenAt()), TOP_LEVEL, snap.name()));
            }
        }

        private static void setDefault(final CliContext ctx, final String id, final String path) {
            final Found found = btrfsAt(ctx, path);
            if (found == null) {
                return;
            }
            final int number = number(id);
            final DiskReport.Snap snap = snapshotNumbered(found.part(), number);
            if (snap == null && number != TOP_LEVEL && number != ROOT_ID) {
                ctx.out().error(NO_DEFAULT);
                return;
            }
            DiskToolLines.ask(ctx.computer(), DiskCommand.startFrom(found.disk().index(), found.part().number(),
                    snap == null ? 0 : number));
            if (snap == null) {
                ctx.out().dim(AS_IT_STANDS);
            } else {
                ctx.out().dim(NEXT_START.with(dayOf(snap.takenAt())));
                ctx.out().dim(WEIGHS);
            }
        }

        private static void delete(final CliContext ctx, final String path) {
            final Found found = btrfsAt(ctx, path);
            if (found == null) {
                return;
            }
            if (found.part().snapshot(nameOf(path)) == null) {
                ctx.out().error(NO_PATH.with(path));
                return;
            }
            DiskToolLines.ask(ctx.computer(), DiskCommand.destroy(found.disk().index(), found.part().number(),
                    nameOf(path)));
            ctx.out().line(DELETED.with(path));
        }

        private static void du(final CliContext ctx, final String path) {
            final Found found = btrfsAt(ctx, path);
            if (found == null) {
                return;
            }
            final DiskReport.Snap snap = found.part().snapshot(nameOf(path));
            ctx.out().line(DU_HEADER);
            if (snap == null) {
                // The partition as it stands: what it holds, which the snapshots share without a count of their own.
                ctx.out().line(String.format(Locale.ROOT, "%10s  %10s  %10s  %s", btrfsSize(found.part().usedMb()),
                        "-", "-", path));
                return;
            }
            ctx.out().line(String.format(Locale.ROOT, "%10s  %10s  %10s  %s", btrfsSize(snap.referMb()),
                    btrfsSize(snap.onlyMb()), btrfsSize(snap.referMb() - snap.onlyMb()), path));
        }

        /*
         * The btrfs partition a path is on, or null having said why: a device names its partition, /dev/sdb1; a
         * snapshot's path the partition it was taken of; and any other path the root's.
         */
        @Nullable
        private static Found btrfsAt(final CliContext ctx, final String path) {
            final DiskReport report = DiskToolLines.report(ctx.computer());
            Found found = null;
            if (path.startsWith("/dev/")) {
                final DiskReport.Disk disk = report.disk(DiskToolLines.linuxDiskOf(path));
                final DiskReport.Part part = disk == null ? null : disk.part(DiskToolLines.linuxPartitionOf(path));
                found = part == null ? null : new Found(disk, part);
            } else if (path.startsWith("/")) {
                found = holding(report, nameOf(path));
                found = found != null ? found : root(report);
            }
            if (found == null) {
                ctx.out().error(NO_PATH.with(path));
                return null;
            }
            if (found.part().fileSystem() != FileSystemType.BTRFS) {
                ctx.out().error(NOT_BTRFS.with(path));
                return null;
            }
            return found;
        }

        /* The btrfs partition keeping a snapshot named {@code name}, or null. */
        @Nullable
        private static Found holding(final DiskReport report, final String name) {
            for (final DiskReport.Disk disk : report.disks()) {
                for (final DiskReport.Part part : disk.parts()) {
                    if (part.fileSystem() == FileSystemType.BTRFS && part.snapshot(name) != null) {
                        return new Found(disk, part);
                    }
                }
            }
            return null;
        }

        /* A snapshot's name, the path it was taken in without the leading slash: .snapshots/day4. */
        private static String nameOf(final String path) {
            return path.startsWith("/dev/") ? path : path.replaceFirst("^/+", "");
        }

        /* A transaction count that only grows, which is what a btrfs generation is: the world's seconds. */
        private static long generationOf(final long ticks) {
            return 1L + ticks / TICKS_PER_SECOND;
        }

        @Nullable
        private static DiskReport.Snap snapshotNumbered(final DiskReport.Part part, final int number) {
            for (final DiskReport.Snap snap : part.snapshots()) {
                if (snap.number() == number) {
                    return snap;
                }
            }
            return null;
        }

        private static int number(final String text) {
            try {
                return Integer.parseInt(text);
            } catch (final NumberFormatException e) {
                return -1;
            }
        }
    }

    /** {@code zfs}: FreeBSD's tool for a pool's snapshots. */
    @TextHolder
    static final class Zfs implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.snapshot.zfs.summary",
                "take, list and roll back to a ZFS pool's snapshots");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.snapshot.zfs.about", "snapshot zroot@NAME keeps"
                + " the pool as it is, items and files, weighing nothing until they change (a dataset may come"
                + " before the at sign, zroot/usr/home@NAME); list -t snapshot lists them; rollback returns the pool"
                + " to one at once, refusing while later ones are kept unless -r lets them go; destroy lets one go."
                + " A pool is named zroot for the system's own, or by its partition's label.");
        private static final TextKey USAGE = TextKey.of("jsc.cli.snapshot.zfs.usage",
                "usage: zfs {snapshot <pool@name>,list [-t snapshot],rollback [-r] <pool@name>,destroy <pool@name>}");
        private static final TextKey HEADER = TextKey.of("jsc.cli.snapshot.zfs.header",
                "NAME                    USED  AVAIL  REFER  MOUNTPOINT");
        private static final TextKey NO_DATASET = TextKey.of("jsc.cli.snapshot.zfs.no_dataset",
                "cannot open '%s': dataset does not exist");
        private static final TextKey EXISTS = TextKey.of("jsc.cli.snapshot.zfs.exists",
                "cannot create snapshot '%s': dataset already exists");
        private static final TextKey NO_DELIMITER = TextKey.of("jsc.cli.snapshot.zfs.no_delimiter",
                "cannot open '%s': missing '@' delimiter in snapshot name");
        private static final TextKey NEWER = TextKey.of("jsc.cli.snapshot.zfs.newer",
                "cannot rollback to '%s': more recent snapshots or bookmarks exist");
        private static final TextKey USE_R = TextKey.of("jsc.cli.snapshot.zfs.use_r",
                "use '-r' to force deletion of the following snapshots and bookmarks:");
        private static final TextKey NONE = TextKey.of("jsc.cli.snapshot.zfs.none", "no datasets available");

        @Override public CommandScope scope() {
            return CommandScope.on(Platform.FREEBSD);
        }

        @Override public String name() {
            return "zfs";
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
            final String verb = ctx.hasArgs() ? ctx.arg(0) : "";
            final String last = ctx.argCount() > 1 ? ctx.arg(ctx.argCount() - 1) : "";
            switch (verb) {
                case "snapshot" -> snapshot(ctx, last);
                case "list" -> list(ctx, ctx.argCount() > 2 && "-t".equals(ctx.arg(1))
                        && ctx.arg(2).startsWith("snap"));
                case "rollback" -> rollBack(ctx, last, ctx.argCount() > 2 && "-r".equals(ctx.arg(1)));
                case "destroy" -> destroy(ctx, last);
                default -> ctx.out().line(USAGE);
            }
        }

        private static void snapshot(final CliContext ctx, final String dataset) {
            if (dataset.isEmpty() || dataset.indexOf('@') < 0) {
                ctx.out().error(dataset.isEmpty() ? USAGE.text() : NO_DELIMITER.with(dataset));
                return;
            }
            final Pool pool = pool(DiskToolLines.report(ctx.computer()), dataset);
            if (pool == null) {
                ctx.out().error(NO_DATASET.with(dataset.substring(0, dataset.indexOf('@'))));
                return;
            }
            final DiskReport after = DiskToolLines.ask(ctx.computer(), DiskCommand.snapshot(pool.found().disk()
                    .index(), pool.found().part().number(), pool.within(dataset)));
            if (after.refusal() == DiskReport.Refusal.SNAPSHOT_EXISTS) {
                ctx.out().error(EXISTS.with(dataset));
            }
        }

        private static void list(final CliContext ctx, final boolean snapshots) {
            final List<Pool> pools = pools(DiskToolLines.report(ctx.computer()));
            final List<String> rows = new ArrayList<>();
            for (final Pool pool : pools) {
                final DiskReport.Part part = pool.found().part();
                if (!snapshots) {
                    rows.add(row(pool.name(), zfsSize(part.usedMb()), zfsSize(part.freeMb()),
                            zfsSize(part.usedMb()), "/" + pool.name()));
                    continue;
                }
                for (final DiskReport.Snap snap : part.snapshots()) {
                    rows.add(row(pool.name() + snap.name(), zfsSize(snap.onlyMb()), "-", zfsSize(snap.referMb()),
                            "-"));
                }
            }
            if (rows.isEmpty()) {
                ctx.out().line(NONE);
                return;
            }
            ctx.out().line(HEADER);
            for (final String row : rows) {
                ctx.out().line(row);
            }
        }

        private static void rollBack(final CliContext ctx, final String dataset, final boolean letNewerGo) {
            final Pool pool = pool(DiskToolLines.report(ctx.computer()), dataset);
            if (pool == null || pool.found().part().snapshot(pool.within(dataset)) == null) {
                ctx.out().error(NO_DATASET.with(dataset));
                return;
            }
            final DiskReport after = DiskToolLines.ask(ctx.computer(), DiskCommand.rollBack(pool.found().disk()
                    .index(), pool.found().part().number(), pool.within(dataset), letNewerGo));
            if (after.refusal() != DiskReport.Refusal.NEWER_SNAPSHOTS) {
                return;
            }
            ctx.out().error(NEWER.with(dataset));
            ctx.out().line(USE_R);
            boolean later = false;
            for (final DiskReport.Snap snap : pool.found().part().snapshots()) {
                if (later) {
                    ctx.out().line(pool.name() + snap.name());
                }
                later |= snap.name().equals(pool.within(dataset));
            }
        }

        private static void destroy(final CliContext ctx, final String dataset) {
            final Pool pool = pool(DiskToolLines.report(ctx.computer()), dataset);
            if (pool == null || pool.found().part().snapshot(pool.within(dataset)) == null) {
                ctx.out().error(NO_DATASET.with(dataset));
                return;
            }
            DiskToolLines.ask(ctx.computer(), DiskCommand.destroy(pool.found().disk().index(),
                    pool.found().part().number(), pool.within(dataset)));
        }

        private static String row(final String name, final String used, final String avail, final String refer,
                                  final String mount) {
            return String.format(Locale.ROOT, "%-22s %5s  %5s  %5s  %s", name, used, avail, refer, mount);
        }
    }

    /** {@code zpool}: FreeBSD's tool for its pools, the mirrors among them. */
    @TextHolder
    static final class Zpool implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.snapshot.zpool.summary",
                "scrub a ZFS pool and report what its mirror repaired");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.snapshot.zpool.about", "status shows each pool,"
                + " its disks, and how many times the pool found a damaged part and repaired it from the other disk"
                + " of its mirror; scrub POOL reads all of it, repairing whatever corruption damaged. A pool on one"
                + " disk finds the damage, and has nothing to repair it from.");
        private static final TextKey USAGE = TextKey.of("jsc.cli.snapshot.zpool.usage",
                "usage: zpool {status [pool],scrub <pool>}");
        private static final TextKey NO_POOLS = TextKey.of("jsc.cli.snapshot.zpool.no_pools", "no pools available");
        private static final TextKey NO_SUCH = TextKey.of("jsc.cli.snapshot.zpool.no_such",
                "cannot open '%s': no such pool");
        private static final TextKey POOL = TextKey.of("jsc.cli.snapshot.zpool.pool", "  pool: %s");
        private static final TextKey STATE = TextKey.of("jsc.cli.snapshot.zpool.state", " state: %s");
        private static final TextKey DAMAGED = TextKey.of("jsc.cli.snapshot.zpool.damaged", "status: One or more"
                + " devices has experienced an error resulting in data corruption.");
        private static final TextKey MISSING = TextKey.of("jsc.cli.snapshot.zpool.missing", "status: One or more"
                + " devices has been removed; the pool continues without its mirror.");
        private static final TextKey NOT_SCRUBBED = TextKey.of("jsc.cli.snapshot.zpool.not_scrubbed",
                "  scan: none requested");
        private static final TextKey SCRUBBED = TextKey.of("jsc.cli.snapshot.zpool.scrubbed",
                "  scan: scrub repaired %s in 00:00:00 with %s errors on Day %s %s");
        private static final TextKey CONFIG = TextKey.of("jsc.cli.snapshot.zpool.config", "config:");
        private static final TextKey CONFIG_HEADER = TextKey.of("jsc.cli.snapshot.zpool.config_header",
                "        NAME        STATE     READ WRITE CKSUM");
        private static final TextKey REPAIRED = TextKey.of("jsc.cli.snapshot.zpool.repaired", "(repaired)");
        private static final TextKey NO_ERRORS = TextKey.of("jsc.cli.snapshot.zpool.no_errors",
                "errors: No known data errors");
        private static final TextKey ERRORS = TextKey.of("jsc.cli.snapshot.zpool.errors",
                "errors: %s data errors, use '-v' for a list");

        @Override public CommandScope scope() {
            return CommandScope.on(Platform.FREEBSD);
        }

        @Override public String name() {
            return "zpool";
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
            final String verb = ctx.hasArgs() ? ctx.arg(0) : "";
            final String named = ctx.argCount() > 1 ? ctx.arg(1) : "";
            switch (verb) {
                case "status" -> status(ctx, named);
                case "scrub" -> scrub(ctx, named);
                default -> ctx.out().line(USAGE);
            }
        }

        private static void status(final CliContext ctx, final String named) {
            final DiskReport report = DiskToolLines.report(ctx.computer());
            final List<Pool> pools = pools(report);
            if (pools.isEmpty()) {
                ctx.out().line(NO_POOLS);
                return;
            }
            boolean any = false;
            for (final Pool pool : pools) {
                if (!named.isEmpty() && !named.equals(pool.name())) {
                    continue;
                }
                if (any) {
                    ctx.out().blank();
                }
                any = true;
                print(ctx, pool, report);
            }
            if (!any) {
                ctx.out().error(NO_SUCH.with(named));
            }
        }

        private static void print(final CliContext ctx, final Pool pool, final DiskReport report) {
            final DiskReport.Pool state = pool.found().part().pool();
            final boolean degraded = state != null && state.degraded();
            final String health = degraded ? "DEGRADED" : "ONLINE";
            ctx.out().line(POOL.with(pool.name()));
            ctx.out().line(STATE.with(health));
            if (degraded) {
                ctx.out().line(MISSING);
            } else if (state != null && state.errors() > 0) {
                ctx.out().line(DAMAGED);
            }
            if (state == null || state.scrubbedAt() < 0L) {
                ctx.out().line(NOT_SCRUBBED);
            } else {
                ctx.out().line(SCRUBBED.with(zfsSize(state.scrubRepairedMb()), state.errors(),
                        dayOf(state.scrubbedAt()), clockOf(state.scrubbedAt())));
            }
            ctx.out().line(CONFIG);
            ctx.out().blank();
            ctx.out().line(CONFIG_HEADER);
            ctx.out().line(member(pool.name(), health, 0, ""));
            final String first = device(pool.found().disk().index(), pool.found().part().number());
            final int repairs = state == null ? 0 : state.repairs();
            final String mark = repairs > 0 ? "  " + REPAIRED.text().english() : "";
            if (state == null || !state.mirrored()) {
                ctx.out().line(member("  " + first, "ONLINE", repairs, mark));
            } else {
                ctx.out().line(member("  mirror-0", health, 0, ""));
                ctx.out().line(member("    " + first, "ONLINE", repairs, mark));
                ctx.out().line(member("    " + (degraded ? "-" : device(state.partnerDisk(), state.partnerNumber())),
                        degraded ? "REMOVED" : "ONLINE", 0, ""));
            }
            ctx.out().blank();
            ctx.out().line(state != null && state.errors() > 0 ? ERRORS.with(state.errors()) : NO_ERRORS.text());
        }

        private static void scrub(final CliContext ctx, final String named) {
            for (final Pool pool : pools(DiskToolLines.report(ctx.computer()))) {
                if (pool.name().equals(named)) {
                    DiskToolLines.ask(ctx.computer(), DiskCommand.scrub(pool.found().disk().index(),
                            pool.found().part().number()));
                    return;
                }
            }
            ctx.out().error(named.isEmpty() ? USAGE.text() : NO_SUCH.with(named));
        }

        private static String member(final String name, final String state, final int checksum,
                                     final String mark) {
            return String.format(Locale.ROOT, "        %-11s %-9s %4d %5d %5d%s", name, state, 0, 0, checksum, mark);
        }

        private static String device(final int disk, final int number) {
            return DiskToolLines.bsdDisk(disk) + "p" + number;
        }
    }

    /* A partition found by a name, with the disk it is on. */
    private record Found(DiskReport.Disk disk, DiskReport.Part part) {
    }

    /*
     * A ZFS pool: the partition it is read from, and the name it goes by, its partition's label, zroot for the one the
     * system runs from, or its device's.
     */
    private record Pool(Found found, String name) {

        /* What a dataset or snapshot names inside the pool: zroot/usr/home@day4 is /usr/home@day4. */
        String within(final String dataset) {
            return dataset.startsWith(this.name) ? dataset.substring(this.name.length()) : dataset;
        }
    }
}
