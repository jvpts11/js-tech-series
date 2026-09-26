/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import dev.jstech.computers.audio.SystemSound;
import dev.jstech.computers.crafting.PatternWorkbench;
import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.machine.NetworkReadService;
import dev.jstech.computers.os.boot.IBootingMachine;
import dev.jstech.computers.os.install.IInstallingMachine;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.core.JsCore;
import dev.jstech.core.peripheral.IPeripheralOwner;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A machine that can boot and run an operating system: the contract the whole OS stack (firmware,
 * POST, boot manager, terminals, desktops, and the CLI backend) talks to. Desk computers implement
 * it directly on their block entities; a rack implements it by delegating to the server mounted in
 * it, so every access route (a directly linked monitor, a KVM channel, ssh, remote control)
 * converges on one pipeline instead of duplicating it per machine shape.
 *
 * <p>Its phases on the way up and down are {@link IBootingMachine}'s, installing a system on it is
 * {@link IInstallingMachine}'s, and what its memory holds is worked out by {@link MachineMemory}.
 */
public interface IOsHost extends IPeripheralOwner, IBootingMachine, IInstallingMachine {

    /** Whether the machine is powered on with a valid build. */
    boolean isRunning();

    /**
     * Switches the machine on or off, as its own power control does. Shutting down from inside the
     * system has to reach this: a machine whose screen merely closed is still running, still on the
     * network, and still holding whatever was open.
     */
    void setPowered(boolean on);

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

    /**
     * Which of the desktop's workspaces is up, counted from nought. Machine state like the windows it sorts,
     * and cleared with them: a machine that comes up again comes up on the first. A host whose desktops never
     * have more than one keeps the first for good.
     */
    default int desktopWorkspace() {
        return 0;
    }

    default void setDesktopWorkspace(final int workspace) {
    }

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

    /** Erases everything the disk in {@code slot} carries; returns whether anything was formatted. */
    boolean formatDisk(int slot);

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
     * The operating space a network machine draws with, or null when it has none.
     *
     * <p>What a desktop environment is to a Linux, for the system that has no desktop: null here is a machine
     * that comes up at its prompt and nothing else, which is a network system whose space has been taken off
     * as much as it is every machine that never had one.
     */
    @Nullable
    ResourceLocation installedSpaceId();

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

    /**
     * Plays one of its system's own sounds out of its monitors, when its sound hardware plays recordings. A machine
     * with no sound of its own, such as a server in a rack, plays none.
     */
    default void systemSound(final ServerLevel level, final SystemSound sound) {
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
     * Whether a service installed here is running, and so holding the memory it asks for. A service with nothing
     * to switch is running as long as the machine is; a machine that can stop one of its own says so itself.
     */
    default boolean serviceRunning(final ProgramSpec service) {
        /*
         * A language's runtime is in memory while it has something to run, the way an interpreter is loaded for
         * a program and not for the disk it sits on. A service registered under a language's own name is that
         * language's runtime, so nothing has to be named here for this to hold for an addon's language too.
         */
        if (JsCore.languages().get(service.id()) == null) {
            return true;
        }
        final MachinePrograms running = programs();
        return running != null && !running.isEmpty();
    }

    /**
     * Told after a service was taken off this machine, so whatever it was keeping can go with it.
     *
     * <p>A service that holds something of its own, a history or a body of source, has to be able to let go
     * of it: what it kept is unreachable the moment the software is gone, and a machine still paying disk
     * space for it would be keeping something nobody can ever read again.
     *
     * @param program the program that was taken off
     */
    default void serviceUninstalled(final ResourceLocation program) {
    }

    /** This machine's memory ledger, see {@link MachineMemory#ledgerOf}. */
    default RamLedger ramLedger() {
        return MachineMemory.ledgerOf(this);
    }

    /** Everything the session holds before the player opens a window: the system, its desktop, its services. */
    default int ramReservedMb() {
        return MachineMemory.reservedMb(this);
    }

    /** The leading windows that fit beside everything else this machine holds, see {@link MachineMemory}. */
    default List<OpenWindow> windowsWithinBudget(final List<OpenWindow> windows) {
        return MachineMemory.withinBudget(this, windows);
    }
}
