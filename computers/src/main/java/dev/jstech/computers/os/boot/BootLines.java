/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.boot;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.os.FirmwareKind;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsRegistry;
import net.minecraft.resources.ResourceLocation;
import dev.jstech.computers.machine.NetworkReadService;
import dev.jstech.computers.os.Branding;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.PackageManagerKind;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * What each system has to say while it comes up, read off the machine it is coming up on.
 *
 * <p>Nothing here is written in advance. A drive letter is a drive that is in the machine, the memory is the
 * memory that is seated, and a network step reports what answered, or that nothing did. A system that says it
 * found something it did not find is worse than a system that says nothing, so a step that cannot be answered
 * says what actually happened instead.
 */
public final class BootLines {

    /** What a machine of the earliest age reserves below the line, in kilobytes, as those machines did. */
    private static final int BASE_MEMORY_KB = 640;

    /** The kernel these machines run, named after the mod so it is plainly this world's own. */
    private static final String KERNEL_VERSION = "6.8-jsc";

    private BootLines() {
    }

    /** The sequence this machine's system shows while it comes up. */
    public static BootSequence forMachine(final AbstractComputerBlockEntity machine) {
        final OsDef system = machine.installedOs();
        if (system == null) {
            return BootSequence.NONE;
        }
        final HardwareEra era = machine.installedEra();
        final String copyright = Branding.systemCopyright(system.displayName(),
                era != null ? era : HardwareEra.STANDARD);
        return switch (system.platform()) {
            case MC_DOS -> dos(machine, system, copyright);
            case MC_NET -> net(machine, system, copyright);
            case LINUX -> linux(machine, system);
            case FRAMES -> frames(machine, system);
            default -> new BootSequence.Builder().title(system.displayName()).subtitle(copyright).build();
        };
    }

    /**
     * The Frames family comes up behind its maker's name and its own, with no account of what it is doing.
     *
     * <p>That silence is the system's character rather than a gap: these are the machines that put a picture up
     * and a bar under it, and tell you nothing until they are ready. The bar is what the screen draws when a
     * sequence has no steps.
     *
     * <p>The newest of them says one thing, and only the once: the first time it comes up it greets the machine
     * by name instead of showing the maker's, and it does it inside the same wait, so a first start is no longer
     * than any other.
     */
    private static BootSequence frames(final AbstractComputerBlockEntity machine, final OsDef system) {
        if (firstTime(machine, system)) {
            return new BootSequence.Builder()
                    .title("Hi.")
                    .subtitle(dev.jstech.computers.os.install.Installers.machineName(machine)
                            + " is getting ready for you")
                    .build();
        }
        return new BootSequence.Builder()
                .title(system.house().name())
                .subtitle(system.displayName())
                .build();
    }

    /** Whether this is the newest edition coming up for the first time, which is the one start that greets. */
    private static boolean firstTime(final AbstractComputerBlockEntity machine, final OsDef system) {
        return "frames_11".equals(system.id().getPath()) && !machine.systemWelcome().seen();
    }

    /**
     * The menu this machine stops at on its way up, or nothing when it stops at none.
     *
     * <p>Only the systems that bring a boot manager with them show one, which here is the Linux family, and only
     * when the server is set to let them. Every disk that carries a system is listed, so a player finds out from
     * the menu that the other one is there, and the machines whose firmware is reached this way offer that too.
     *
     * @param countdownTicks how long the machine waits before booting the first entry by itself
     */
    public static BootMenu menuFor(final IOsHost machine, final int countdownTicks) {
        final OsDef booting = machine.installedOs();
        if (booting == null || booting.platform() != Platform.LINUX) {
            return BootMenu.NONE;
        }
        final BootMenu.Builder out = new BootMenu.Builder();
        final ItemStack bootDisk = machine.systemDisk();
        int drive = 0;
        for (int slot = 0; slot < machine.diskSlots(); slot++) {
            final ItemStack disk = machine.diskInSlot(slot);
            if (!(disk.getItem() instanceof DiskItem)) {
                continue;
            }
            final ResourceLocation id = disk.get(ComputingModule.SYSTEM_OS.get());
            final OsDef system = id == null ? null : OsRegistry.getOs(id);
            final String device = "/dev/sd" + (char) ('a' + drive++) + "1";
            if (system == null) {
                continue;
            }
            if (disk == bootDisk) {
                out.entry(system.displayName(), slot).defaultsToLast();
            } else {
                out.entry(system.displayName() + " Boot Manager (on " + device + ")", slot);
            }
        }
        if (out.build(0).isEmpty()) {
            return BootMenu.NONE;
        }
        /*
         * The machines whose firmware is reached from the boot manager offer it here; the earlier ones are
         * entered with a key during the self-test and nowhere else, so their menu does not pretend otherwise.
         */
        if (FirmwareKind.forEra(machine.installedEra() != null ? machine.installedEra() : HardwareEra.STANDARD)
                == FirmwareKind.UEFI) {
            out.entry("Firmware Settings", BootMenu.FIRMWARE);
        }
        return out.build(countdownTicks);
    }

    /**
     * What this machine's system shows on its way down, or nothing at all.
     *
     * <p>The earliest systems had no such screen: switching the machine off switched it off, and the glass went
     * dark where it stood. The later ones close their programs first and say so, so they are the ones with
     * something to show.
     */
    public static BootSequence shutdownFor(final AbstractComputerBlockEntity machine) {
        final OsDef system = machine.installedOs();
        if (system == null || system.platform() != Platform.FRAMES) {
            return BootSequence.NONE;
        }
        return new BootSequence.Builder()
                .title(system.displayName())
                .subtitle(system.displayName() + " is shutting down...")
                .build();
    }

    /**
     * A Linux machine reads out its kernel and then its services, in the words of whichever init it runs.
     *
     * <p>The kernel line names the architecture the processor really understands, so a machine of the earlier
     * generation says i686 where a later one says x86_64, and the service lines are the services this machine
     * actually has: a network target only when a cable reaches a Mainframe, a mirror only when that Mainframe
     * runs one, a display manager only when a desktop is installed.
     */
    private static BootSequence linux(final AbstractComputerBlockEntity machine, final OsDef system) {
        final boolean openRc = system.packageManager() == PackageManagerKind.EMERGE;
        final String arch = kernelArch(machine);
        final BootSequence.Builder out = new BootSequence.Builder()
                .title(openRc
                        ? "OpenRC is starting up " + system.displayName() + " Linux (" + arch + ")"
                        : "Loading Linux " + KERNEL_VERSION + " ...")
                .subtitle("");
        if (openRc) {
            out.line("Mounting /proc ...", "ok");
            out.line("Starting udev ...", "ok");
            out.line("Checking local filesystems ...", "ok");
            out.line("Mounting local filesystems ...", "ok");
            out.line("Setting hostname ...", machine.customName().isEmpty() ? "localhost" : machine.customName());
        } else {
            out.line("Linux version " + KERNEL_VERSION + " (" + arch + ")");
            out.line("CPU: " + cpuName(machine));
            out.line("Memory: " + machine.ramTotalMb() + " MB available");
            int drive = 0;
            for (int slot = 0; slot < machine.diskSlots(); slot++) {
                final ItemStack disk = machine.diskInSlot(slot);
                if (disk.getItem() instanceof DiskItem) {
                    out.line("sd" + (char) ('a' + drive++) + ":", disk.getHoverName().getString());
                }
            }
            out.line("Started Journal Service.", "OK");
            out.line("Reached target Local File Systems.", "OK");
        }
        final NetworkReadService network = machine.services().network();
        if (network != null && network.online()) {
            out.line("Reached target Network is Online.", "OK");
        }
        final String desktop = machine.installedDesktopId() == null ? ""
                : machine.installedDesktopId().getPath().replace('_', ' ');
        if (!desktop.isEmpty()) {
            out.line("Started " + desktop + " Display Manager.", "OK");
        }
        return out.build();
    }

    /** What a kernel of this machine calls the architecture it is running on. */
    private static String kernelArch(final AbstractComputerBlockEntity machine) {
        final ComputerBuild build = machine.currentBuild();
        if (build == null || build.cpus().isEmpty()) {
            return "x86_64";
        }
        final int bits = build.cpus().getFirst().architecture().bits();
        return bits >= 64 ? "x86_64" : "i686";
    }

    /** The processor as a kernel names it: its model and how many cores it has. */
    private static String cpuName(final AbstractComputerBlockEntity machine) {
        final ComputerBuild build = machine.currentBuild();
        if (build == null || build.cpus().isEmpty()) {
            return "unknown";
        }
        final int cores = build.cpus().getFirst().cores();
        return build.cpus().getFirst().freqMhz() + " MHz, " + cores + (cores == 1 ? " core" : " cores");
    }

    /**
     * The earliest machines: memory counted above the line, then a letter for every drive that is in, then the
     * network only when a cable actually reaches one.
     */
    private static BootSequence dos(final AbstractComputerBlockEntity machine, final OsDef system,
                                    final String copyright) {
        final BootSequence.Builder out = new BootSequence.Builder()
                .title("Starting " + system.displayName() + "...")
                .subtitle(copyright);
        final int extendedKb = Math.max(0, machine.ramTotalMb() * 1024 - BASE_MEMORY_KB);
        out.line("HIMEM", String.format(Locale.ROOT, "%,d KB extended memory", extendedKb));
        char letter = 'C';
        for (int slot = 0; slot < machine.diskSlots(); slot++) {
            final ItemStack disk = machine.diskInSlot(slot);
            if (!(disk.getItem() instanceof DiskItem)) {
                continue;
            }
            out.line(letter + ":", disk.getHoverName().getString());
            letter++;
        }
        final String network = networkName(machine);
        if (!network.isEmpty()) {
            out.line("NET", network);
        }
        return out.build();
    }

    /**
     * The network machines: every step is a question put to the network, and an unanswered one says so rather
     * than opening on a list with nothing in it and no reason given.
     */
    private static BootSequence net(final AbstractComputerBlockEntity machine, final OsDef system,
                                    final String copyright) {
        final BootSequence.Builder out = new BootSequence.Builder()
                .title(system.displayName() + " 1.0")
                .subtitle(copyright);
        final NetworkReadService network = machine.services().network();
        final ICliComputer.NetSummary summary = network == null ? null : network.summary();
        if (summary == null || !summary.linked()) {
            out.line("network link", "down");
            out.line("mainframe", "skipped");
            out.line("index", "skipped");
            out.line("storage", "skipped");
            return out.build();
        }
        out.line("network link", "done");
        if (!summary.mainframePresent()) {
            out.line("mainframe", "none answering");
            out.line("index", "not available");
            out.line("storage", "not available");
            return out.build();
        }
        out.line("mainframe", network.current());
        out.line("index", String.format(Locale.ROOT, "%,d item types", summary.indexedTypes()));
        final long capacity = network.capacity();
        final long used = network.used();
        final int percent = capacity > 0 ? (int) (used * 100L / capacity) : 0;
        out.line("storage", summary.servers() + (summary.servers() == 1 ? " server, " : " servers, ")
                + percent + "% full");
        return out.build();
    }

    /** The network this machine is on, by the name it answers to, or nothing when no cable reaches one. */
    private static String networkName(final AbstractComputerBlockEntity machine) {
        final NetworkReadService network = machine.services().network();
        if (network == null || !network.online()) {
            return "";
        }
        final String name = network.current();
        return name == null ? "" : name;
    }
}
