/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import dev.jstech.computers.crafting.PatternWorkbench;
import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.machine.NetworkReadService;
import dev.jstech.computers.os.boot.BootSequence;
import dev.jstech.computers.os.boot.SystemWelcome;
import dev.jstech.computers.os.install.InstallerFlow;
import dev.jstech.computers.os.install.OsInstallJob;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.core.peripheral.IPeripheralOwner;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A machine that can boot and run an operating system: the contract the whole OS stack (firmware,
 * POST, boot manager, terminals, desktops, and the CLI backend) talks to. Desk computers implement
 * it directly on their block entities; a rack implements it by delegating to the server mounted in
 * it, so every access route (a directly linked monitor, a KVM channel, ssh, remote control)
 * converges on one pipeline instead of duplicating it per machine shape.
 */
public interface IOsHost extends IPeripheralOwner {

    /** Whether the machine is powered on with a valid build. */
    boolean isRunning();

    /**
     * Switches the machine on or off, as its own power control does. Shutting down from inside the
     * system has to reach this: a machine whose screen merely closed is still running, still on the
     * network, and still holding whatever was open.
     */
    void setPowered(boolean on);

    /** Whether the next session must run POST before handing over to the boot manager. */
    boolean needsPost();

    void setNeedsPost(boolean value);

    /**
     * Starts the machine over, letting whatever is running say goodbye first.
     *
     * <p>Different from asking for a self-test outright: a running system closes its programs and shows what
     * it shows while it does, and the self-test begins when it has finished. A host with nothing to show, or
     * nothing running, starts over at once, which is what the plain form does.
     */
    default void restart() {
        setNeedsPost(true);
    }

    /**
     * Whether the machine is closing its system down on its way to starting over.
     *
     * <p>A phase of its own, and a monitor opened during it has to find the machine in it: without this, a
     * player who looked away mid-restart came back to the desktop of a system that was being closed.
     */
    default boolean goingDown() {
        return false;
    }

    /** How long that closing-down takes in all, so a screen joining it knows how far along it is. */
    default int downTotal() {
        return 0;
    }

    /** The ticks it still has to run, so a monitor opened part way through joins it where it is. */
    default int downRemaining() {
        return 0;
    }

    /**
     * Whether an installation is waiting for this machine to finish testing itself.
     *
     * <p>A machine told to install something is not a machine booting its own system: it was restarted in
     * order to start from the medium instead. Without this, a computer that already had a system installed
     * played that system's whole start before showing the installer it had been restarted for, and one with
     * two systems stopped at its boot manager on the way, asking which of them to boot when the answer was
     * neither.
     */
    default boolean installationWaiting() {
        return installer() != null || installing() != null
                || (console() != null && console().liveInstall() != null);
    }

    /**
     * Whether this machine is standing at the end of a self-test that found nothing to boot.
     *
     * <p>A machine that answers yes is waiting for a key at its own failure, and a monitor opened on it shows
     * that rather than its setup. A host with no self-test to stand at answers no.
     */
    default boolean haltedAtPost() {
        return false;
    }

    /** A key was pressed at that failure: the machine stops standing there. */
    default void resumeFromHalt() {
    }

    /** The value of {@link #pendingInstallSlot()} when no installation is waiting for its reboot. */
    int NO_PENDING_INSTALL = -2;

    /**
     * The disk slot a guided installer has just written a system to ({@code -1} for the default
     * disk), while the machine still sits in that installer waiting for the reboot that will boot
     * it; {@link #NO_PENDING_INSTALL} otherwise. A real machine does not become the new system the
     * moment the files are on the disk: until it restarts, the installer is what is running, so
     * leaving the monitor and coming back must find the installer's "reboot" prompt, not a booted
     * desktop. Any restart or power change clears it.
     */
    int pendingInstallSlot();

    void setPendingInstallSlot(int slot);

    /**
     * The desktop environment this machine actually booted, or {@code null} when it booted to a shell.
     * This is fixed at boot and does not follow later changes on disk: installing or removing a desktop
     * package changes what the NEXT boot will run, not what is running now. Without it, leaving the
     * monitor and coming back silently applied a change the machine was never restarted for.
     */
    @Nullable
    ResourceLocation bootedDesktopId();

    /** Fixes the desktop for this session. Called when POST hands over to the boot manager. */
    void setBootedDesktopId(@Nullable ResourceLocation id);

    /**
     * The program windows this machine has open, as the last player to leave its monitor left them.
     * Machine state, not viewer state: it persists with the machine and is cleared by a restart or a
     * shutdown, exactly like the windows on a real desktop.
     */
    List<OpenWindow> openWindows();

    void setOpenWindows(List<OpenWindow> windows);

    /** The RAM buffer of the current build, in items. */
    long ramBuffer();

    /** The best CPU clock in MHz across installed CPUs, or 0 with no valid build. */
    int maxCpuMhz();

    /**
     * How many cores this machine has, counted across every processor in it.
     *
     * <p>Not how many processors: work is spread over cores, so one four-core processor gets through four
     * times what a single core does, and that is the number anything timing work on this machine wants.
     * A host that cannot be asked what it is built of answers with a core for each processor it reports.
     */
    default int cpuCores() {
        return Math.max(1, this.installedCpus());
    }

    /** The total VRAM in MB across installed GPUs, or 0 with no valid build. */
    int totalVramMb();

    /** Free space on the system disk in MB. */
    long systemDiskFreeMb();

    /** How many disk slots this machine wires up. */
    int diskSlots();

    /** The disk stack in the given 0-based disk slot, or EMPTY. */
    ItemStack diskInSlot(int slot);

    /** The disk the firmware boots (the preferred slot when it holds a system), or EMPTY. */
    ItemStack systemDisk();

    /** The firmware's preferred boot disk slot, or {@code -1} for "the first disk with a system". */
    int bootDiskSlot();

    /** Sets the firmware's preferred boot disk slot ({@code -1} = the first disk with a system). */
    void setBootDiskSlot(int slot);

    /** The disk slot a guided OS install targets by default. */
    int defaultInstallSlot();

    /** Erases everything the disk in {@code slot} carries; returns whether anything was formatted. */
    boolean formatDisk(int slot);

    /** Installs the given OS onto {@code preferredSlot} (or the default target); true on success. */
    boolean installOs(ResourceLocation osId, int preferredSlot);

    /** Whether a bootable system disk is present. */
    boolean hasOs();

    @Nullable
    ResourceLocation installedOsId();

    @Nullable
    OsDef installedOs();

    /** The desktop environment this machine boots into, or null for a TTY-only or network OS. */
    @Nullable
    ResourceLocation installedDesktopId();

    /**
     * Whether this machine still has something to run: the installed system, or a live session whose medium
     * it can still see.
     *
     * <p>A question, and nothing more. It is asked by the gate on every payload a screen sends and by the
     * container every tick, so it must be safe to ask at any moment and must change nothing when it is: a
     * machine whose drive is a tick late to load says it cannot tell, and being unable to tell keeps the
     * session rather than ending it. Ending one is {@link #settleLiveInstall()}.
     */
    boolean validateOsSession();

    /**
     * Ends a live session whose medium has really been taken out, once a tick, and says whether it did.
     *
     * <p>A machine that keeps no live session has none to end.
     */
    default boolean settleLiveInstall() {
        return false;
    }

    /** The per-machine console state: history, installed programs, settings. */
    ComputerConsoleState console();

    /** The machine's node identity (assigned on first use). */
    NodeUuid nodeUuid();

    /** The network this machine currently belongs to, or null when unlinked. */
    @Nullable
    NetworkUuid networkUuid();

    /**
     * Whether this machine is attached to a data network. Unlike {@link #networkUuid()}, which is a server
     * fact, this one is answered on both sides, so a client screen can show the machine's connection.
     */
    default boolean networkAttached() {
        return networkUuid() != null;
    }

    /**
     * A system being copied onto this machine's disks right now, or nothing.
     *
     * <p>A machine that cannot hold one answers nothing and is installed the moment it is asked, which is what
     * a host with nowhere to keep the work has to do.
     */
    @Nullable
    default OsInstallJob installing() {
        return null;
    }

    /** Starts, replaces or ends the copy this machine is doing; a host that keeps none does nothing. */
    default void setInstalling(@Nullable final OsInstallJob job) {
    }

    /** The installer this machine is in: the page it is on and what has been answered so far. */
    @Nullable
    default InstallerFlow installer() {
        return null;
    }

    /** Puts the machine in an installer, or takes it out of one. */
    default void setInstaller(@Nullable final InstallerFlow flow) {
    }

    /** Whether this machine can hold a copy of its own rather than being written to there and then. */
    default boolean keepsInstalls() {
        return false;
    }

    /**
     * Whether a monitor watching this machine's block is showing THIS machine right now.
     *
     * <p>Every machine in a rack shares the rack's position, and its monitor shows one of them at a time, so a
     * screen meant for one of them must not be put in front of somebody looking at another.
     */
    default boolean onScreen() {
        return true;
    }

    /** Tells the world this machine's state changed, so it is written with the block that holds it. */
    default void markChanged() {
    }

    /**
     * What the system this machine boots remembers about being greeted, and whether its welcome comes back.
     *
     * <p>A host that does not keep it answers that nobody has met its system, which is what a machine with no
     * disk of its own means anyway.
     */
    default SystemWelcome systemWelcome() {
        return SystemWelcome.UNSEEN;
    }

    /** Writes the greeting back onto the disk the system is on; a host that cannot keep it does nothing. */
    default void setSystemWelcome(final SystemWelcome welcome) {
    }

    /*
     * Where the machine is on its way up. A host that runs no phases of its own answers that it is past all of
     * them, which is what a machine reached through something other than its own power amounts to: whoever asks
     * is told there is nothing to watch rather than being shown a self-test that will never end.
     */

    /** The ticks the self-test still has to run, for a monitor opened while it is under way. */
    default int postRemaining() {
        return 0;
    }

    /** Whether the machine is stopped at its boot manager, waiting to be told what to start. */
    default boolean atBootMenu() {
        return false;
    }

    /** The ticks left before the menu boots its first entry by itself, or zero once a key has stopped it. */
    default int menuRemaining() {
        return 0;
    }

    /** A key was pressed at the menu: the machine waits there for a choice. */
    default void holdBootMenu() {
    }

    /** Leaves the menu and brings the chosen system up. */
    default void leaveBootMenu() {
    }

    /**
     * Leaves the menu to start the machine over from its self-test, which is what a boot manager's own Reboot
     * is for. No system is up yet to say goodbye, so nothing is shown closing: the machine simply starts again.
     */
    default void restartFromBootMenu() {
    }

    /** Whether the system is coming up on this machine right now. */
    default boolean booting() {
        return false;
    }

    /** The ticks the system still needs, for a monitor opened while it comes up. */
    default int bootRemaining() {
        return 0;
    }

    /** How long the coming-up under way takes in all, for the bar on the screen watching it. */
    default int bootTotal() {
        return 0;
    }

    /** What this machine's system shows while it comes up, which is nothing at all for a machine with none. */
    default BootSequence bootSequence() {
        return BootSequence.NONE;
    }

    /** The parts this machine is built from right now, or nothing when it is not built from parts. */
    @Nullable
    default ComputerBuild currentBuild() {
        return null;
    }

    /**
     * How many bits wide this machine's processor is, which is what a system names its architecture from. A
     * machine that cannot say what it is built of is taken for one of today's.
     */
    default int processorBits() {
        final ComputerBuild build = this.currentBuild();
        return build == null || build.cpus().isEmpty() ? 64 : build.cpus().getFirst().architecture().bits();
    }

    /** Whether a drive this machine reaches holds something it could boot instead of one of its own disks. */
    default boolean hasBootableMedium() {
        return false;
    }

    /**
     * The data network as what runs on this machine reads it, or nothing when the machine has nobody to ask.
     *
     * <p>Nothing is not the same as a network that is down, and whoever reads this owes the difference: a
     * machine with nobody to ask has no grounds for a claim about the network either way.
     */
    @Nullable
    default NetworkReadService networkService() {
        return null;
    }

    /** The player-given machine name, or an empty string. */
    String customName();

    /** Renames the machine (an empty name clears it). */
    void setCustomName(String name);

    /** How many CPUs the current build carries. */
    int installedCpus();

    /** The installed disk stacks, in slot order. */
    List<ItemStack> diskStacks();

    /** Free space on the system disk in internal data-weight units. */
    long systemDiskFreeWeight();

    /** The disk footprint the installed OS reserves, in item-equivalents. */
    long reservedByOs();

    /** Installs the given OS onto the default target slot; true on success. */
    boolean installOs(ResourceLocation osId);

    /** Marks the machine's persistent state dirty after a mutation. */
    void setChanged();

    /** The hardware era of the installed motherboard, or null when no board is present. */
    @Nullable
    HardwareEra installedEra();

    /** The hardware era the GUI should wear (a fixed-era chassis wins over the board). */
    @Nullable
    HardwareEra displayEra();

    /**
     * The machine's recipe workbench: the drafts the Pattern Studio edits, kept with the machine so they survive
     * the window and the session. Null on a host that has no room for one (a machine that is not seated).
     */
    @Nullable
    default PatternWorkbench studio() {
        return null;
    }

    /**
     * The script processes this machine is running, or null on a host that cannot run any.
     *
     * <p>They hold memory like anything else the machine is doing, which is why the ledger asks for them.
     */
    @Nullable
    default MachinePrograms programs() {
        return null;
    }

    /** The installed RAM in megabytes: the modules' marketed sizes, read off the buffer items they stage. */
    default int ramTotalMb() {
        return (int) Math.min(Integer.MAX_VALUE, ramBuffer() * RamLedger.MB_PER_BUFFER_ITEM);
    }

    /**
     * This machine's memory ledger: the running system's own share, the desktop package it booted, the
     * services installed on it and the windows it has open, each weighed under the installed system. A
     * machine that is off or has no system holds nothing.
     */
    default RamLedger ramLedger() {
        final RamLedger ledger = new RamLedger(ramTotalMb());
        final OsDef os = installedOs();
        if (os == null || !isRunning()) {
            return ledger;
        }
        ledger.add(os.displayName(), os.ramMb(), RamLedger.Kind.SYSTEM);
        final ResourceLocation desktopId = bootedDesktopId();
        final DesktopEnvironmentDef desktop = desktopId != null ? OsRegistry.getDesktop(desktopId) : null;
        if (desktopId != null && !desktopId.equals(os.id())) {
            // A desktop package on a Linux system; a Frames desktop is the system itself and is counted above.
            final ProgramSpec pack = OsRegistry.getProgram(desktopId);
            if (pack != null) {
                ledger.add(desktop != null ? desktop.displayName() : pack.displayName(), pack.ramMbOn(os),
                        RamLedger.Kind.DESKTOP);
            }
        }
        final ComputerConsoleState console = console();
        if (console != null) {
            for (final ProgramSpec spec : OsRegistry.programs()) {
                if (spec.kind() == ProgramKind.SERVICE && console.isInstalled(spec.id().getPath())) {
                    ledger.add(spec.displayName(), spec.ramMbOn(os), RamLedger.Kind.SERVICE);
                }
            }
        }
        final MachinePrograms scripts = programs();
        if (scripts != null) {
            for (final var one : scripts.view()) {
                ledger.add(one.name(), one.heapMb(), RamLedger.Kind.PROCESS, one.id());
            }
        }
        for (final OpenWindow window : openWindows()) {
            ledger.add(window.key(), windowRamMb(window.key(), os, desktop), RamLedger.Kind.WINDOW);
        }
        return ledger;
    }

    /** Everything the session holds before the player opens a window: the system, its desktop, its services. */
    default int ramReservedMb() {
        final RamLedger ledger = ramLedger();
        return ledger.usedMb() - ledger.usedMb(RamLedger.Kind.WINDOW);
    }

    /**
     * The leading windows of {@code windows} that fit beside everything else this machine holds, in order;
     * the first past the budget and everything after it are dropped, so the oldest windows survive.
     */
    default List<OpenWindow> windowsWithinBudget(final List<OpenWindow> windows) {
        final OsDef os = installedOs();
        if (os == null) {
            return windows;
        }
        final ResourceLocation desktopId = bootedDesktopId();
        final DesktopEnvironmentDef desktop = desktopId != null ? OsRegistry.getDesktop(desktopId) : null;
        return RamLedger.withinBudget(windows, window -> windowRamMb(window.key(), os, desktop),
                ramTotalMb() - ramReservedMb());
    }

    /**
     * The megabytes a window opened under {@code key} holds: its program's weight under {@code os}, found by
     * the label the desktop gives the program, else by the program's own name; a window no program answers to
     * weighs what a bundled program of that system does.
     */
    static int windowRamMb(final String key, final OsDef os, @Nullable final DesktopEnvironmentDef desktop) {
        ProgramSpec spec = desktop != null ? desktop.programFor(key) : null;
        if (spec == null) {
            for (final ProgramSpec candidate : OsRegistry.programs()) {
                if (candidate.displayName().equals(key)) {
                    spec = candidate;
                    break;
                }
            }
        }
        return spec != null ? spec.ramMbOn(os) : RamLedger.bundledWeightMb(os.ramMb());
    }
}
