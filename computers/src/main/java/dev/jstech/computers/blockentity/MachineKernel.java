/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.operation.payload.ScreenSessions;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.RamLedger;
import dev.jstech.computers.os.boot.BootPhases;
import dev.jstech.computers.os.crash.CrashCause;
import dev.jstech.computers.os.crash.CrashPage;
import dev.jstech.computers.os.crash.CrashPages;
import dev.jstech.computers.os.crash.CrashStyle;
import dev.jstech.computers.os.crash.OomKiller;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * What one machine's system does when it runs short: the page it goes down on, the killer a Unix kernel ends things
 * with, and the system it remembers coming up, which is the one that goes down. A computer keeps one, and a rack keeps
 * one for each machine in it, so a server follows the rule of its system exactly as a computer on a desk does.
 *
 * <p>Transient, all but what the killer ended and logged in the run, which the machine saves with the rest of it.
 */
final class MachineKernel {

    /** Where the machine is on its way up and down, which a crash takes over. */
    private final BootPhases phases;
    /** The page the system crashed onto, while it stands. */
    private final MachineCrash crash = new MachineCrash();
    /** What the kernel ended for want of memory in this run, and what it logged of it. */
    private final MachineOom oom = new MachineOom();
    /**
     * The system that came up and is running, which is not always what the disks say now: a system disk taken out
     * from under it leaves the machine still running that system, on its way to crashing.
     */
    @Nullable
    private OsDef up;
    /** The number the next process the kernel names takes, on a machine that keeps no programs to count them. */
    private int nextPid = MachinePrograms.FIRST_PID;

    /** How often what the system holds is weighed against what it has: once a second. */
    private static final int WEIGH_TICKS = 20;

    MachineKernel(final BootPhases phases) {
        this.phases = phases;
    }

    /** One tick of the machine's kernel: once a second, what its system holds is weighed against what it has. */
    void tick(final IKernelHost machine, final ServerLevel level) {
        if (level.getGameTime() % WEIGH_TICKS == 0) {
            weigh(machine, level);
        }
    }

    /** The system has finished coming up: what is running now is what the disks booted. */
    void cameUp(final IOsHost machine) {
        this.up = machine.installedOs();
    }

    /** The run is over: the system that came up is gone, every service comes back and the log begins again. */
    void runEnded() {
        this.up = null;
        this.oom.reset();
        this.nextPid = MachinePrograms.FIRST_PID;
    }

    /** The system that came up in this run, or null when none has. */
    @Nullable
    OsDef upSystem() {
        return this.up;
    }

    /** The system that is running: the one that came up, or what the disks hold for a machine that never saw it. */
    @Nullable
    OsDef running(final IOsHost machine) {
        return this.up != null ? this.up : machine.installedOs();
    }

    /**
     * Brings the running system down on its crash page for {@code cause}: everything it was running dies where it
     * stands, the session ends, and whoever is watching is shown the page. It stands its time, and then the machine
     * starts over from its self-test; or, when what went was a part it cannot run without, the monitor goes dark.
     *
     * @return whether it went down: a machine that is off, testing itself, at its boot menu, still bringing its system
     *         up or already on its way down, or whose system has no crash page (the earliest had none), stays as it is
     */
    boolean crash(final IKernelHost machine, final ServerLevel level, final CrashCause cause,
                  final CrashPages.Moment moment) {
        final long now = level.getGameTime();
        if (!machine.isManualOn() || phases.goingDown() || page(now) != null || phases.needsPost() || phases.halted()
                || phases.atMenu() || phases.booting()) {
            return false;
        }
        final OsDef system = running(machine);
        final CrashStyle style = system == null ? null : CrashPages.styleFor(system.platform(), system.familyRank());
        if (style == null) {
            return false;
        }
        final CrashPage page = CrashPages.of(style, system.platform(), cause,
                MachineCrash.facts(machine, system, phases.upFor(now), moment));
        final boolean dark = !machine.buildValid();
        crash.begin(page, now, dark);
        if (dark) {
            // No computer is left to close down: the session ends now, and it owes a self-test once it is whole again.
            machine.setNeedsPost(true);
        } else {
            machine.goDown(now, page.totalTicks());
        }
        // Whatever each of them had up, a desktop, a prompt or a session of the monitor's own, the page takes it over.
        if (machine.onScreen()) {
            final BlockPos pos = machine.getBlockPos();
            ScreenSessions.eachAtMachine(level, pos,
                    (player, monitor) -> MonitorBlock.openCrash(player, level, monitor, pos, machine));
        }
        machine.setChanged();
        return true;
    }

    /**
     * A program asked the machine for memory it no longer has: a window opened on its desktop, a program started at its
     * prompt, a service. A system of the Frames family does not refuse it: it goes down on its blue page. A Unix kernel
     * ends what holds the most until it fits, and panics only once nothing is left to end. MC-DOS and MC-NET refuse it.
     *
     * @param needMb what the program asks for on top of what the machine holds, or zero when it holds it already
     * @return whether there is room for it now
     */
    boolean memoryShort(final IKernelHost machine, final ServerLevel level, final String program, final int needMb) {
        final OsDef system = running(machine);
        if (system == null) {
            return false;
        }
        return switch (system.platform()) {
            case FRAMES -> {
                crash(machine, level, CrashCause.MEMORY, CrashPages.Moment.memory(program, 0, List.of()));
                yield false;
            }
            case LINUX, FREEBSD, UNIX -> oom.makeRoom(machine, level, system, program, needMb,
                    phases.upFor(level.getGameTime()), () -> takePid(machine));
            case MC_DOS, MC_NET -> false;
        };
    }

    /** Whether the machine's system ends programs for their memory, and so lets every window open first. */
    boolean killsForMemory(final IOsHost machine) {
        final OsDef system = running(machine);
        return system != null && OomKiller.runsOn(system.platform());
    }

    /**
     * What the machine's system holds is weighed against what it has: a service installed, a server with more
     * listeners, anything that took memory without asking first. A system that holds more than there is answers as it
     * does to any program it has no room for.
     */
    void weigh(final IKernelHost machine, final ServerLevel level) {
        if (!machine.isRunning() || phases.goingDown() || phases.needsPost() || phases.booting() || phases.atMenu()
                || page(level.getGameTime()) != null) {
            return;
        }
        // A machine that was already up when it was loaded never saw its system come up, so it learns it here.
        if (this.up == null) {
            this.up = machine.installedOs();
        }
        final RamLedger ledger = machine.ramLedger();
        if (this.up == null || ledger.usedMb() <= ledger.totalMb()) {
            return;
        }
        // What tipped it over is whatever came last, so the newest entry stands for the program that asked.
        final List<RamLedger.Entry> entries = ledger.entries();
        memoryShort(machine, level, entries.isEmpty() ? "" : entries.getLast().name(), 0);
    }

    /**
     * A key on the console of a crashed system that offers to stop its automatic reboot: the first stops it and the
     * machine waits, the next reboots it; whoever is watching is shown the page as it now stands.
     */
    void key(final IKernelHost machine, final ServerLevel level) {
        final long now = level.getGameTime();
        if (!crash.key(now)) {
            return;
        }
        if (crash.held()) {
            phases.holdDown();
        } else {
            phases.releaseDown(now);
        }
        if (machine.onScreen()) {
            final BlockPos pos = machine.getBlockPos();
            ScreenSessions.eachWatcher(level, pos,
                    (player, monitor) -> MonitorBlock.openCrash(player, level, monitor, pos, machine));
        }
    }

    /** The page standing on the glass at {@code now}, or null when there is none or its time is up. */
    @Nullable
    CrashPage page(final long now) {
        return crash.page(now);
    }

    /** The ticks the page still stands, none while a key holds it. */
    int remaining(final long now) {
        return crash.remaining(now);
    }

    /** Whether a key on the console has stopped the automatic reboot of the page standing at {@code now}. */
    boolean held(final long now) {
        return crash.page(now) != null && crash.held();
    }

    /** Whether the monitor goes dark after the page rather than the machine starting over. */
    boolean endsDark() {
        return crash.endsDark();
    }

    /** What the kernel logged about its memory in this run, oldest first. */
    List<String> memoryLog() {
        return oom.logged();
    }

    /** Whether that service was ended in this run, by the kernel for its memory or by a signal sent to its process. */
    boolean serviceEnded(final ResourceLocation service) {
        return oom.stopped(service);
    }

    /** Ends that service for the rest of this run, as a signal sent to its process does. */
    void endService(final ResourceLocation service) {
        oom.stop(service);
    }

    /** Writes the run's memory down in {@code tag}, under a key of its own. */
    void save(final CompoundTag tag) {
        oom.save(tag);
    }

    void load(final CompoundTag tag) {
        oom.load(tag);
    }

    /*
     * A number no running process holds, for something the kernel names as it ends it: the programs' own count on a
     * machine that runs programs, so no number names two things, and the kernel's own on one that runs none.
     */
    private int takePid(final IOsHost machine) {
        final MachinePrograms programs = machine.programs();
        return programs != null ? programs.takePid() : this.nextPid++;
    }
}
