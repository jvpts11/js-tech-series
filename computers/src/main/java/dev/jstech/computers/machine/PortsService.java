/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.ProgramKind;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.os.install.SetupTiming;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.DosPath;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.program.cli.ICliPackages;
import dev.jstech.computers.program.install.voice.PortsVoices;
import dev.jstech.computers.program.install.voice.WorldStamp;
import dev.jstech.computers.program.tty.TtyScript;
import dev.jstech.computers.program.tty.TtyScriptProcess;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.tier.HardwareEra;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The ports tree on a system that keeps one, and the ports built from it.
 *
 * <p>FreeBSD's other way of installing a program. Instead of a package somebody else built, the tree of every
 * program the Mirror serves the system, fetched as one snapshot and laid out on the disk, and in each program's
 * folder the recipe {@code make install clean} builds it by, on this machine and for it. The build is timed by this
 * machine's processor with all of its cores, which is how the ports build, and what it installs asks a little less
 * of the machine than the package would have (see SourceAdvantage).
 *
 * <p>Everything a port leaves is a real file: the tree, the snapshot's tag, and the mark a finished build leaves in
 * the port's work folder until it is cleaned. So the tree takes room on the disk, a build without {@code clean}
 * keeps its work folder, and a later {@code make install} finds the build already done.
 */
public final class PortsService {

    private final IComputerTerminalHost terminal;
    private final ServerLevel level;
    /** The Mirror, the gates every way of installing keeps, and what the Mirror serves this system. */
    private final PackageService packages;

    private static final String USAGE = "usage: portsnap fetch | extract | update | auto ...";

    /** How long laying out one port's folder takes, and the least the whole tree takes. */
    private static final int TICKS_PER_PORT = 2;
    private static final int LEAST_LAY_TICKS = 20;

    public PortsService(final IComputerTerminalHost terminal, final ServerLevel level,
                        final PackageService packages) {
        this.terminal = terminal;
        this.level = level;
        this.packages = packages;
    }

    /**
     * portsnap, doing what the commands ask in their order: {@code fetch} brings the snapshot from the Mirror,
     * {@code extract} lays the tree out afresh from it, {@code update} brings a tree already there up to date, and
     * {@code auto} fetches and then does whichever of the last two the machine needs.
     */
    public ICliPackages.Installing snapshot(final List<String> commands) {
        boolean fetch = false;
        boolean extract = false;
        boolean update = false;
        for (final String command : commands) {
            switch (command.toLowerCase(Locale.ROOT)) {
                case "fetch" -> fetch = true;
                case "extract" -> extract = true;
                case "update" -> update = true;
                case "auto" -> {
                    fetch = true;
                    update = true;
                }
                default -> {
                    return refused(USAGE);
                }
            }
        }
        final DriveTable.Drive system = this.systemDrive();
        if (commands.isEmpty() || system == null) {
            return refused(system == null ? "portsnap: this computer has no system disk" : USAGE);
        }
        final boolean treeThere = DiskFilesystem.exists(system.disk(), PortsTree.INDEX);
        // An update of a tree that is not there is what auto turns into an extract.
        if (update && !treeThere && commands.stream().anyMatch("auto"::equalsIgnoreCase)) {
            update = false;
            extract = true;
        }
        final String refusal = this.snapshotRefusal(system.disk(), fetch, extract, update, treeThere);
        if (refusal != null) {
            return refused(refusal);
        }
        final List<ProgramSpec> ports = this.packages.offered();
        final Map<String, String> files = PortsTree.files(ports,
                this.level.getGameTime() / SetupTiming.TICKS_PER_SECOND);
        final TtyScript.Builder script = TtyScript.script();
        if (fetch) {
            final double kb = FileService.bytesOf(String.join("", files.values())) / 1024.0;
            script.then(PortsVoices.fetch(kb, this.fetchTicks(Math.max(1.0, kb / 1024.0)),
                    WorldStamp.of(this.level.getDayTime()), () -> this.tag(ports.size())));
        }
        if (extract || update) {
            final boolean refreshing = !extract;
            final List<String> origins = new ArrayList<>();
            for (final ProgramSpec port : ports) {
                if (!refreshing || changed(system.disk(), port, files)) {
                    origins.add(PortsTree.origin(port));
                }
            }
            final AtomicReference<CliLine> indexed = new AtomicReference<>(PortsVoices.noSpace());
            script.then(PortsVoices.extract(refreshing, origins,
                    Math.max(LEAST_LAY_TICKS, ports.size() * TICKS_PER_PORT),
                    () -> indexed.set(this.lay(files, ports, !refreshing)), indexed::get));
        }
        return ICliPackages.Installing.running(new TtyScriptProcess(script.done()));
    }

    /**
     * make, in the folder of a port: builds it, installs it, cleans up after it or takes it off the machine, as the
     * targets ask, and nothing at all in a folder that is no port's.
     *
     * @param where where the shell stands
     * @param shown the same, as the shell shows it, for make to say where it stopped
     */
    public ICliPackages.Installing make(final DosPath.Location where, final String shown, final List<String> targets) {
        final DriveTable.Drive system = this.systemDrive();
        final ProgramSpec port = system == null ? null : PortsTree.at(this.packages.offered(), where);
        final String first = targets.isEmpty() ? "" : targets.getFirst();
        if (port == null || !DiskFilesystem.exists(system.disk(), PortsTree.dir(port) + "/Makefile")) {
            return refused((first.isEmpty() ? "make: no target to make." : dontKnow(first)) + stoppedIn(shown));
        }
        boolean build = targets.isEmpty();
        boolean install = false;
        boolean reinstall = false;
        boolean deinstall = false;
        boolean clean = false;
        for (final String target : targets) {
            switch (target.toLowerCase(Locale.ROOT)) {
                case "all", "build" -> build = true;
                case "install" -> install = true;
                case "reinstall" -> reinstall = true;
                case "deinstall" -> deinstall = true;
                case "clean" -> clean = true;
                default -> {
                    return refused(dontKnow(target) + stoppedIn(shown));
                }
            }
        }
        final boolean installing = install || reinstall;
        if (deinstall && !installing) {
            return this.deinstall(port, clean);
        }
        final String barred = installing ? this.barred(port, reinstall) : null;
        if (barred != null) {
            return refused(barred + "\n*** Error code 1\n\nStop." + stoppedIn(shown));
        }
        final boolean building = (build || installing) && !DiskFilesystem.exists(system.disk(),
                PortsTree.buildCookie(port));
        if (building && this.packages.mirrorMainframe() == null) {
            return refused(unfetchable(port) + stoppedIn(shown));
        }
        return ICliPackages.Installing.running(new TtyScriptProcess(PortsVoices.make(this.voiceOf(port),
                building ? () -> this.markBuilt(port) : null,
                installing ? () -> this.installed(port) : null,
                clean ? () -> this.clean(port) : null)));
    }

    /** Why portsnap will not do that, in its own words, or null when it will. */
    @Nullable
    private String snapshotRefusal(final ItemStack disk, final boolean fetch, final boolean extract,
                                   final boolean update, final boolean treeThere) {
        if (fetch && this.packages.mirrorMainframe() == null) {
            return "Looking up the Mirror for the ports tree... none found.\nNo mirrors remaining, giving up.";
        }
        if ((extract || update) && !fetch && !DiskFilesystem.exists(disk, PortsTree.SNAPSHOT_TAG)) {
            return "No snapshot available.  Try running\nportsnap fetch";
        }
        if (update && !extract && !treeThere) {
            return "/usr/ports was not created by portsnap.\n"
                    + "You must run 'portsnap extract' before running 'portsnap update'.";
        }
        return null;
    }

    /** What stands in the way of installing that port on this machine, in the ports' words, or null when nothing. */
    @Nullable
    private String barred(final ProgramSpec port, final boolean reinstall) {
        final String pkg = PortsTree.pkgName(port);
        if (!reinstall && this.packages.has(port)) {
            return "===>  " + pkg + " is already installed\n"
                    + "      You may wish to ``make deinstall'' and install this port again\n"
                    + "      by ``make reinstall'' to upgrade it properly.";
        }
        final ICliComputer.OpResult tooOld = this.packages.eraGate(port);
        final ICliComputer.OpResult elsewhere = this.packages.wrongMachine(port);
        final ICliComputer.OpResult broken = tooOld != null ? tooOld : elsewhere;
        if (broken != null) {
            return "===>  " + pkg + " is marked as broken: " + broken.message() + ".";
        }
        final BlockEntity machine = (BlockEntity) this.terminal;
        if (machine instanceof MainframeBlockEntity && port.kind() == ProgramKind.SERVICE) {
            return "===>  " + pkg + " cannot install: a Mainframe's services come prebuilt; use pkg install "
                    + PortsTree.name(port) + ".";
        }
        if (!(machine instanceof IOsHost host) || this.terminal.console() == null) {
            return "===>  " + pkg + " cannot install: this computer cannot store installed programs.";
        }
        if (!this.packages.fits(host, port, true)) {
            return "===>  " + pkg + " cannot install: this machine is short of the processor or the free disk"
                    + " space it needs.";
        }
        return null;
    }

    /** make deinstall: the package the port installed taken off, the way pkg takes it off. */
    private ICliPackages.Installing deinstall(final ProgramSpec port, final boolean clean) {
        final String name = PortsTree.name(port);
        final String cleaned = clean ? "\n===>  Cleaning for " + PortsTree.pkgName(port) : "";
        if (clean) {
            this.clean(port);
        }
        if (!this.packages.has(port)) {
            return ICliPackages.Installing.said(ICliComputer.OpResult.ok("===>  Deinstalling for " + name + "\n"
                    + "===>   " + name + " not installed, skipping" + cleaned));
        }
        final ICliComputer.OpResult removed = this.packages.remove(name);
        final String said = "===>  Deinstalling for " + name + "\n===>   Deinstalling " + PortsTree.pkgName(port)
                + "\n" + removed.message() + cleaned;
        return ICliPackages.Installing.said(removed.ok() ? ICliComputer.OpResult.ok(said)
                : ICliComputer.OpResult.fail(said));
    }

    /** The port as the ports speak of it, timed by this machine. */
    private PortsVoices.Port voiceOf(final ProgramSpec port) {
        final IOsHost host = (IOsHost) this.terminal;
        final double sourceMb = PortsTree.sourceMb(port);
        return new PortsVoices.Port(PortsTree.pkgName(port), PortsTree.name(port), PortsTree.license(port),
                PortsTree.distfile(port), sourceMb, this.fetchTicks(sourceMb),
                SourceBuild.buildTicks(port, host.maxCpuMhz(), Math.max(1, host.cpuCores())));
    }

    /** How long this machine's connection takes over that many megabytes from the Mirror. */
    private int fetchTicks(final double megabytes) {
        final HardwareEra era = this.terminal instanceof IOsHost host ? host.installedEra() : null;
        return SetupTiming.networkTicks((int) Math.ceil(megabytes), false, SetupTiming.eraFactor(era));
    }

    /** The mark a finished build leaves, which a later install finds instead of building again. */
    private void markBuilt(final ProgramSpec port) {
        final DriveTable.Drive system = this.systemDrive();
        if (system != null) {
            DiskFilesystem.write(system.disk(), PortsTree.buildCookie(port), FileType.OTHER, "",
                    DriveTable.freeWeightOf(system.disk()), system.kind(), this.level.getGameTime());
            system.commit().run();
        }
    }

    /** The build installed: on the machine, recorded as built there, and the port's advancement earned. */
    private void installed(final ProgramSpec port) {
        final BlockEntity machine = (BlockEntity) this.terminal;
        final ComputerConsoleState console = this.terminal.console();
        if (console != null && !machine.isRemoved()) {
            PackageService.builtHere(machine, console, port);
            JscEvents.awardOperator(machine, JscEvents.BUILT_FROM_PORTS, port.id().getPath());
        }
    }

    /** The port's work folder taken away, and with it the build it held. */
    private void clean(final ProgramSpec port) {
        final DriveTable.Drive system = this.systemDrive();
        if (system != null && DiskFilesystem.rmdir(system.disk(), PortsTree.workDir(port), system.kind())) {
            system.commit().run();
        }
    }

    /** What the snapshot leaves once it has come: the day it was fetched and how many ports it held. */
    private void tag(final int ports) {
        final DriveTable.Drive system = this.systemDrive();
        if (system == null) {
            return;
        }
        final String tag = "portsnap|" + WorldStamp.of(this.level.getDayTime()).day() + "|" + ports + "\n";
        final ItemStack disk = system.disk();
        final long had = DiskFilesystem.read(disk, PortsTree.SNAPSHOT_TAG).map(text -> weight(text, disk)).orElse(0L);
        DiskFilesystem.write(disk, PortsTree.SNAPSHOT_TAG, FileType.OTHER, tag, DriveTable.freeWeightOf(disk) + had,
                system.kind(), this.level.getGameTime());
        system.commit().run();
    }

    /**
     * Writes the tree onto the system disk and says how that went: how many ports and what they take there, or that
     * the disk filled first, in which case what was written stays written, as it would.
     *
     * @param fresh whether the tree is laid out afresh, which takes away what was there first, builds and all; an
     *              update only takes away the ports that are no longer served
     */
    private CliLine lay(final Map<String, String> files, final List<ProgramSpec> ports, final boolean fresh) {
        final DriveTable.Drive system = this.systemDrive();
        if (system == null) {
            return PortsVoices.noSpace();
        }
        final ItemStack disk = system.disk();
        this.clear(system, ports, fresh);
        long free = DriveTable.freeWeightOf(disk);
        long taken = 0L;
        for (final Map.Entry<String, String> file : files.entrySet()) {
            final long had = DiskFilesystem.read(disk, file.getKey()).map(text -> weight(text, disk)).orElse(0L);
            final DiskFilesystem.WriteResult written = DiskFilesystem.write(disk, file.getKey(), FileType.OTHER,
                    file.getValue(), free + had, system.kind(), this.level.getGameTime());
            if (written != DiskFilesystem.WriteResult.OK) {
                system.commit().run();
                return PortsVoices.noSpace();
            }
            final long cost = weight(file.getValue(), disk);
            free += had - cost;
            taken += cost;
        }
        system.commit().run();
        final double megabytes = taken * (double) DiskFilesystem.eraOf(disk).mbPerItem() / StorageKey.MB_EQ_PER_ITEM;
        return PortsVoices.indexed(ports.size(), onDisk(megabytes));
    }

    /** Takes away the tree that was there before it is laid out again, or the ports no longer served. */
    private void clear(final DriveTable.Drive system, final List<ProgramSpec> ports, final boolean fresh) {
        if (fresh) {
            DiskFilesystem.rmdir(system.disk(), PortsTree.ROOT, system.kind());
            return;
        }
        final Set<String> served = new HashSet<>();
        for (final ProgramSpec port : ports) {
            served.add(PortsTree.dir(port));
        }
        for (final String category : DiskFilesystem.listDirs(system.disk(), PortsTree.ROOT, system.kind())) {
            for (final String dir : DiskFilesystem.listDirs(system.disk(), category, system.kind())) {
                if (!served.contains(dir)) {
                    DiskFilesystem.rmdir(system.disk(), dir, system.kind());
                }
            }
        }
    }

    /** The system disk, as long as it keeps folders; null when there is none to keep a tree on. */
    @Nullable
    private DriveTable.Drive systemDrive() {
        final DriveTable.Drive drive = DriveTable.of((BlockEntity) this.terminal, this.level).find('C');
        return drive == null || drive.disk().isEmpty() || drive.kind() != FilesystemKind.HIERARCHICAL ? null : drive;
    }

    /** Whether the port on the disk is not the port the snapshot holds, which is what an update lays out again. */
    private static boolean changed(final ItemStack disk, final ProgramSpec port, final Map<String, String> files) {
        final String makefile = PortsTree.dir(port) + "/Makefile";
        return !DiskFilesystem.read(disk, makefile).map(files.get(makefile)::equals).orElse(false);
    }

    /** What a text weighs on that disk. */
    private static long weight(final String text, final ItemStack disk) {
        return FsPaths.sizeMbEq((int) FileService.bytesOf(text), DiskFilesystem.eraOf(disk));
    }

    /** A size on a disk the way the ports say one: kilobytes below a megabyte, whole megabytes from ten. */
    private static String onDisk(final double megabytes) {
        if (megabytes < 1.0) {
            return Math.max(1L, Math.round(megabytes * 1024.0)) + " kB";
        }
        return megabytes < 10.0 ? String.format(Locale.ROOT, "%.1f MB", megabytes)
                : Math.round(megabytes) + " MB";
    }

    /** What make says when the Mirror is not there to fetch a port's source from. */
    private static String unfetchable(final ProgramSpec port) {
        final String distfile = PortsTree.distfile(port);
        return "=> " + distfile + " doesn't seem to exist in /usr/ports/distfiles/.\n"
                + "=> Attempting to fetch from the Mirror.\n"
                + "fetch: mirror://mainframe/distfiles/" + distfile + ": No address record\n"
                + "=> Couldn't fetch it - please try to retrieve this\n"
                + "=> port manually into /usr/ports/distfiles/ and try again.\n"
                + "*** Error code 1\n\nStop.";
    }

    private static String dontKnow(final String target) {
        return "make: don't know how to make " + target + ". Stop";
    }

    private static String stoppedIn(final String shown) {
        return "\n\nmake: stopped in " + shown;
    }

    private static ICliPackages.Installing refused(final String message) {
        return ICliPackages.Installing.said(ICliComputer.OpResult.fail(message));
    }
}
