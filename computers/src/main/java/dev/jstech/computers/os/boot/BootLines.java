/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.boot;

import dev.jstech.computers.os.OsDisks;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.os.FirmwareKind;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.KernelNames;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.install.Installers;
import net.minecraft.resources.ResourceLocation;
import dev.jstech.computers.machine.NetworkReadService;
import dev.jstech.computers.os.Branding;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.PackageManagerKind;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.hardware.DiskSpec;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

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

    /** The kernel the Linux machines run. */
    private static final String KERNEL_VERSION = KernelNames.LINUX_VERSION;

    /** The init the machines of the other Linux line run, which prints in a hand of its own. */
    private static final String OPENRC_VERSION = "0.54";

    /** What a service manager writes at the head of a line that went well, and the star its rival uses. */
    private static final String MARK_OK = "[  OK  ]";
    private static final String MARK_STAR = " *";
    private static final String MARK_DONE = "[ ok ]";

    private BootLines() {
    }

    /** The sequence this machine's system shows while it comes up. */
    public static BootSequence forMachine(final IOsHost machine, @Nullable final ServerLevel level) {
        final OsDef system = machine.installedOs();
        if (system == null) {
            return BootSequence.NONE;
        }
        final HardwareEra era = machine.installedEra();
        final String copyright = Branding.systemCopyright(system.displayName(),
                era != null ? era : HardwareEra.STANDARD);
        return switch (system.platform()) {
            case MC_DOS -> dos(machine, system, copyright);
            case MC_NET -> net(machine, system, copyright, level);
            case LINUX -> linux(machine, system, level);
            case FREEBSD -> BsdBootLines.up(machine, system, level);
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
     *
     * <p>Which is why the ordinary start carries no words at all. The maker and the edition are in the picture
     * already, drawn as that system drew them, and writing them out again in the game's font underneath was
     * the picture saying its own name twice.
     */
    private static BootSequence frames(final IOsHost machine, final OsDef system) {
        if (firstTime(machine, system)) {
            return new BootSequence.Builder()
                    .title("Hi.")
                    .subtitle(Installers.machineName(machine)
                            + " is getting ready for you")
                    .build();
        }
        return BootSequence.NONE;
    }

    /** Whether this is the newest edition coming up for the first time, which is the one start that greets. */
    private static boolean firstTime(final IOsHost machine, final OsDef system) {
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
        if (booting == null || BootManager.of(booting.platform()) == BootManager.NONE) {
            return BootMenu.NONE;
        }
        final BootManager manager = BootManager.of(booting.platform());
        if (!manager.listsSystems()) {
            return loaderMenu(machine, manager, countdownTicks);
        }
        final BootMenu.Builder out = new BootMenu.Builder(manager);
        final ResourceLocation runningId = machine.installedOsId();
        int drive = 0;
        for (int slot = 0; slot < machine.diskSlots(); slot++) {
            final ItemStack disk = machine.diskInSlot(slot);
            if (!(disk.getItem() instanceof DiskItem)) {
                continue;
            }
            final String device = "/dev/sd" + (char) ('a' + drive++) + "1";
            /*
             * Every system on every disk, not one per disk. A disk carries several now, and a manager that
             * listed only the one each disk boots would hide the rest, which is the one thing a boot manager
             * exists not to do.
             */
            for (final ResourceLocation id : OsDisks.systemsOn(disk).ids()) {
                final OsDef system = OsRegistry.getOs(id);
                if (system == null) {
                    continue;
                }
                if (id.equals(runningId)) {
                    out.entry(system.displayName(), slot, id.toString()).defaultsToLast();
                } else {
                    out.entry(manager.label(system.displayName(), device, slot), slot, id.toString());
                }
            }
        }
        final BootMenu listed = out.build(0);
        if (listed.isEmpty()) {
            return BootMenu.NONE;
        }
        /*
         * A manager with nothing to choose between does not stop a machine that never stopped for one. The
         * firmware entry is not something to choose: it is the way out, and a machine with one system and a way
         * out is still a machine with one system.
         */
        if (!manager.stopsForOne() && listed.entries().size() < 2) {
            return BootMenu.NONE;
        }
        /*
         * The machines whose firmware is reached from the boot manager offer it here; the earlier ones are
         * entered with a key during the self-test and nowhere else, so their menu does not pretend otherwise.
         */
        if (FirmwareKind.forEra(machine.installedEra() != null ? machine.installedEra() : HardwareEra.STANDARD)
                == FirmwareKind.UEFI) {
            out.firmware(manager.firmwareLabel());
        }
        return out.build(countdownTicks);
    }

    /**
     * The menu of a manager that boots its own system and chooses between none: the boot, starting over, and
     * the firmware on the machines that reach it this way. Only what the machine can really do is listed.
     */
    private static BootMenu loaderMenu(final IOsHost machine, final BootManager manager, final int countdownTicks) {
        final ResourceLocation running = machine.installedOsId();
        final int slot = running == null ? -1 : slotOf(machine, running);
        if (slot < 0) {
            return BootMenu.NONE;
        }
        final BootMenu.Builder out = new BootMenu.Builder(manager)
                .entry("Boot", slot, running.toString())
                .defaultsToLast()
                .restart("Reboot");
        if (FirmwareKind.forEra(machine.installedEra() != null ? machine.installedEra() : HardwareEra.STANDARD)
                == FirmwareKind.UEFI) {
            out.firmware(manager.firmwareLabel());
        }
        return out.build(countdownTicks);
    }

    /**
     * The disk that system really sits on, the firmware's preferred one first, or -1 when no disk carries it.
     *
     * <p>Never the preference itself, which is -1 for "whichever disk has a system": an entry has to name a disk,
     * and -1 in an entry is the way into the firmware.
     */
    private static int slotOf(final IOsHost machine, final ResourceLocation system) {
        final int preferred = machine.bootDiskSlot();
        if (preferred >= 0 && preferred < machine.diskSlots()
                && OsDisks.systemsOn(machine.diskInSlot(preferred)).ids().contains(system)) {
            return preferred;
        }
        for (int slot = 0; slot < machine.diskSlots(); slot++) {
            if (machine.diskInSlot(slot).getItem() instanceof DiskItem
                    && OsDisks.systemsOn(machine.diskInSlot(slot)).ids().contains(system)) {
                return slot;
            }
        }
        return -1;
    }

    /**
     * What this machine's system shows on its way down, or nothing at all.
     *
     * <p>The earliest systems had no such screen: switching the machine off switched it off, and the glass went
     * dark where it stood. The later ones close their programs first and say so, so they are the ones with
     * something to show, and each says it in its own words: the picture systems put one sentence over their
     * own picture, and the ones that read their start out read their stopping out the same way.
     *
     * @param restarting the machine is coming straight back up, which is a different word on every one of them
     */
    public static BootSequence shutdownFor(final IOsHost machine, final boolean restarting) {
        final OsDef system = machine.installedOs();
        if (system == null) {
            return BootSequence.NONE;
        }
        return switch (system.platform()) {
            case FRAMES -> new BootSequence.Builder()
                    .title(system.displayName())
                    .subtitle(framesGoodbye(system, restarting))
                    .build();
            case LINUX -> linuxDown(system, restarting);
            case FREEBSD -> BsdBootLines.down(machine, restarting);
            default -> BootSequence.NONE;
        };
    }

    /**
     * What an edition of the Frames line says while it closes, which changed with every one of them.
     *
     * <p>The oldest spoke to the person in front of it, the one after it named the system, and the newest says
     * the one word and nothing else. They are quoted rather than composed because the wording is the thing.
     */
    private static String framesGoodbye(final OsDef system, final boolean restarting) {
        return switch (system.familyRank()) {
            case 1 -> restarting ? "Please wait while your computer restarts."
                    : "Please wait while your computer shuts down.";
            case 2 -> restarting ? "Frames is restarting..." : "Frames is shutting down...";
            default -> restarting ? "Restarting" : "Shutting down";
        };
    }

    /**
     * A Linux machine stops in reverse: the desktop, then the network, then the disks, then the power.
     *
     * <p>Each init says it in its own hand, the same hand it started in, which is the point of showing it at
     * all: a machine that reads its start out reads its stop out too.
     */
    private static BootSequence linuxDown(final OsDef system, final boolean restarting) {
        final boolean openRc = system.packageManager() == PackageManagerKind.EMERGE;
        final BootSequence.Builder out = new BootSequence.Builder().title("").subtitle("");
        final String[] steps = {
                "Stopped target Graphical Interface.",
                "Stopped target Network is Online.",
                "Stopped target Network.",
                "Unmounted /boot/efi.",
                "Reached target Unmount All Filesystems.",
                "Reached target System Shutdown.",
                "Reached target Late Shutdown Services.",
        };
        for (final String step : steps) {
            if (openRc) {
                out.marked(MARK_STAR, openRcWording(step), MARK_DONE, true);
            } else {
                out.marked(MARK_OK, step, true);
            }
        }
        out.marked(stamp(steps.length + 1),
                restarting ? "reboot: Restarting system" : "reboot: Power down", false);
        return out.build();
    }

    /**
     * The same step in the other init's words, which stops services rather than reaching targets.
     *
     * <p>Borrowing systemd's sentences for a machine running OpenRC was the thing that made the two look like
     * one system in two colours, when the whole reason a player can tell those distributions apart on sight is
     * that they do not say the same words.
     */
    private static String openRcWording(final String step) {
        return switch (step) {
            case "Stopped target Graphical Interface." -> "Stopping display manager ...";
            case "Stopped target Network is Online." -> "Bringing down interface eth0 ...";
            case "Stopped target Network." -> "Stopping netmount ...";
            case "Unmounted /boot/efi." -> "Unmounting /boot ...";
            case "Reached target Unmount All Filesystems." -> "Unmounting filesystems ...";
            case "Reached target System Shutdown." -> "Saving the system clock ...";
            default -> "Stopping local ...";
        };
    }

    /**
     * The time a kernel writes at the head of each of its lines.
     *
     * <p>Worked out from where the line sits rather than taken from a clock, so the same machine reads out the
     * same log twice and a screen rebuilt half way through it does not jump.
     */
    private static String stamp(final int index) {
        final double seconds = index * 0.0937 + (index * index % 7) * 0.0031;
        return String.format(Locale.ROOT, "[%12.6f]", seconds);
    }

    /**
     * A Linux machine reads out its kernel and then its services, in the words of whichever init it runs.
     *
     * <p>The kernel line names the architecture the processor really understands, so a machine of the earlier
     * generation says i686 where a later one says x86_64, and the service lines are the services this machine
     * actually has: a network target only when a cable reaches a Mainframe, a mirror only when that Mainframe
     * runs one, a display manager only when a desktop is installed.
     */
    private static BootSequence linux(final IOsHost machine, final OsDef system,
                                      @Nullable final ServerLevel level) {
        final boolean openRc = system.packageManager() == PackageManagerKind.EMERGE;
        final String desktop = machine.installedDesktopId() == null ? ""
                : machine.installedDesktopId().getPath().replace('_', ' ');
        return openRc ? openRc(machine, system, desktop) : systemd(machine, desktop, level);
    }

    /**
     * The kernel reading itself out, then systemd reporting each target it reaches.
     *
     * <p>The kernel's own lines are stamped with the time they happened and the init's are marked with whether
     * the thing started, which is the whole of how that log reads: a player skims the left column and sees at a
     * glance which part of the machine is talking.
     */
    private static BootSequence systemd(final IOsHost machine, final String desktop,
                                        @Nullable final ServerLevel level) {
        final BootSequence.Builder out = new BootSequence.Builder()
                .title("Loading Linux " + KERNEL_VERSION + " ...")
                .subtitle("Loading initial ramdisk ...");
        int at = 0;
        out.marked(stamp(at++), "Linux version " + KERNEL_VERSION + " (" + kernelArch(machine) + ")", false);
        out.marked(stamp(at++), "CPU: " + cpuName(machine), false);
        out.marked(stamp(at++), "Memory: " + machine.ramTotalMb() + " MB available", false);
        int drive = 0;
        for (int slot = 0; slot < machine.diskSlots(); slot++) {
            final ItemStack disk = machine.diskInSlot(slot);
            if (disk.getItem() instanceof DiskItem) {
                out.marked(stamp(at++), "sd" + (char) ('a' + drive++) + ": "
                        + disk.getHoverName().getString(), false);
            }
        }
        out.marked(MARK_OK, "Started Journal Service.", true);
        out.marked(MARK_OK, "Reached target Local File Systems.", true);
        /*
         * The claim is about the network, not about what happens to be reading it, so it is put to the machine
         * rather than to the shell running on it: a machine on a cable is on a network whether or not anything
         * is up yet to ask about it.
         */
        if (machine.networkAttached()) {
            out.marked(MARK_OK, "Reached target Network is Online.", true);
        }
        final String mirror = mirrorOf(machine, level);
        if (!mirror.isEmpty()) {
            out.marked(MARK_OK, "Found package mirror on " + mirror + ".", true);
        }
        if (!desktop.isEmpty()) {
            /*
             * A service manager says it is starting a thing before it says the thing started, and the line with
             * nothing in its mark is the one still under way. It is the only pair on the screen, and it is what
             * makes the last moment of a start read as a moment rather than as another finished step.
             */
            out.marked("", "Starting " + desktop + " Display Manager...", false);
            out.marked(MARK_OK, "Started " + desktop + " Display Manager.", true);
        }
        return out.build();
    }

    /** The other init: a star for every service it brings up, and its own column saying each one is up. */
    private static BootSequence openRc(final IOsHost machine, final OsDef system, final String desktop) {
        final BootSequence.Builder out = new BootSequence.Builder()
                .title("OpenRC " + OPENRC_VERSION + " is starting up " + system.displayName()
                        + " Linux (" + kernelArch(machine) + ")")
                .subtitle("");
        out.marked(MARK_STAR, "Mounting /proc ...", MARK_DONE, true);
        out.marked(MARK_STAR, "Starting udev ...", MARK_DONE, true);
        out.marked(MARK_STAR, "Checking local filesystems ...", MARK_DONE, true);
        out.marked(MARK_STAR, "Mounting local filesystems ...", MARK_DONE, true);
        out.marked(MARK_STAR, "Setting hostname to "
                + (machine.customName().isEmpty() ? "localhost" : machine.customName()) + " ...",
                MARK_DONE, true);
        if (machine.networkAttached()) {
            out.marked(MARK_STAR, "Bringing up interface eth0 ...", MARK_DONE, true);
        }
        if (!desktop.isEmpty()) {
            out.marked(MARK_STAR, "Starting " + desktop + " ...", MARK_DONE, true);
        }
        out.marked(MARK_STAR, "Starting local ...", MARK_DONE, true);
        return out.build();
    }

    /** The Mirror serving this machine by the name it answers to, or nothing when none does. */
    private static String mirrorOf(final IOsHost machine, @Nullable final ServerLevel level) {
        return level == null ? "" : Installers.mirrorHost(machine, level);
    }

    /** What a kernel of this machine calls the architecture it is running on. */
    private static String kernelArch(final IOsHost machine) {
        return KernelNames.architecture(Platform.LINUX, machine.processorBits());
    }

    /** The processor as a kernel names it: its model and how many cores it has. */
    static String cpuName(final IOsHost machine) {
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
    private static BootSequence dos(final IOsHost machine, final OsDef system,
                                    final String copyright) {
        final BootSequence.Builder out = new BootSequence.Builder()
                .title("Starting " + system.displayName() + "...")
                .subtitle("");
        /*
         * A system of this age loaded its drivers one at a time and each one printed its own name and what it
         * had found, which is why the drive letters come after them and not before: the letters exist because
         * those drivers gave them out.
         */
        final int extendedKb = Math.max(0, machine.ramTotalMb() * 1024 - BASE_MEMORY_KB);
        out.line("MCMEM.SYS testing extended memory ... done");
        out.line(String.format(Locale.ROOT, "%,d KB extended memory available", extendedKb));
        final String network = networkName(machine);
        if (!network.isEmpty()) {
            out.line("NETLINK.SYS  network link up, Mainframe " + network);
        }
        char letter = 'C';
        for (int slot = 0; slot < machine.diskSlots(); slot++) {
            final ItemStack disk = machine.diskInSlot(slot);
            if (!(disk.getItem() instanceof DiskItem)) {
                continue;
            }
            out.marked("Drive " + letter + ":", disk.getHoverName().getString(), freeOn(disk), false);
            letter++;
        }
        out.line(system.displayName() + " Version 1.0");
        out.line(copyright);
        return out.build();
    }

    /**
     * The network machines: every step is a question put to the network, and an unanswered one says so rather
     * than opening on a list with nothing in it and no reason given.
     *
     * <p>A machine with nobody to ask is a different thing from one whose link is down, and it says nothing
     * instead of the other. Every line here is a claim about the network, and a machine that cannot put the
     * question has no grounds for any of them, least of all for the one that says the cable is dead.
     */
    private static BootSequence net(final IOsHost machine, final OsDef system, final String copyright,
                                    @Nullable final ServerLevel level) {
        final BootSequence.Builder out = new BootSequence.Builder()
                .title(system.displayName() + " 1.0    "
                        + (machine.customName().isEmpty() ? "" : machine.customName()))
                .subtitle(copyright);
        out.line("Loading kernel", "done");
        final NetworkReadService network = machine.networkService();
        if (network == null) {
            return out.build();
        }
        final ICliComputer.NetSummary summary = network.summary();
        if (summary == null || !summary.linked()) {
            out.line("Network link", "down");
            out.line("Mainframe", "skipped");
            out.line("Network index", "skipped");
            /*
             * The reason, in words, and not only the word that says a step was skipped. A player looking at a
             * column of "skipped" learns that something did not happen and nothing about what to do about it,
             * and what to do about it here is plug a data cable in.
             */
            out.line("No data cable reaches this computer.");
            out.line(system.displayName() + " opens with local storage only.");
            return out.build();
        }
        out.line("Network link", "up");
        if (!summary.mainframePresent()) {
            out.line("Mainframe", "none answering");
            out.line("Network index", "not available");
            out.line("No Mainframe answers on this network.");
            out.line(system.displayName() + " opens with local storage only.");
            return out.build();
        }
        out.line("Mainframe", network.current());
        out.line("Network index", String.format(Locale.ROOT, "%,d item types", summary.indexedTypes()));
        final long capacity = network.capacity();
        final long used = network.used();
        final int percent = capacity > 0 ? (int) (used * 100L / capacity) : 0;
        out.line("Storage", summary.servers() + (summary.servers() == 1 ? " server, " : " servers, ")
                + percent + "% full");
        final String services = level == null ? "" : Installers.servicesOn(machine, level);
        if (!services.isEmpty()) {
            out.line("Services on " + network.current(), services);
        }
        out.line("Starting the network terminal ...");
        return out.build();
    }

    /** How much room a drive still has, in the words the system of that age printed beside its letter. */
    private static String freeOn(final ItemStack disk) {
        if (!(disk.getItem() instanceof DiskItem drive)) {
            return "";
        }
        final HardwareEra era = drive.spec().era();
        final long freeMb = OsDisks.systemDiskFreeWeight(disk) / StorageKey.MB_EQ_PER_ITEM * era.mbPerItem();
        return DiskSpec.sizeLabel(freeMb) + " free";
    }

    /** The network this machine is on, by the name it answers to, or nothing when no cable reaches one. */
    private static String networkName(final IOsHost machine) {
        final NetworkReadService network = machine.networkService();
        if (network == null || !network.online()) {
            return "";
        }
        final String name = network.current();
        return name == null ? "" : name;
    }
}
