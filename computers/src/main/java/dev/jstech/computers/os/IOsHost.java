/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import dev.jstech.computers.api.JsComputersApi;
import dev.jstech.computers.audio.ProgramCue;
import dev.jstech.computers.audio.SystemSound;
import dev.jstech.computers.crafting.PatternWorkbench;
import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.hardware.ThermalState;
import dev.jstech.core.text.Text;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.machine.MachineServices;
import dev.jstech.computers.machine.NetworkReadService;
import dev.jstech.computers.os.boot.IBootingMachine;
import dev.jstech.computers.os.crash.CrashCause;
import dev.jstech.computers.os.crash.CrashPage;
import dev.jstech.computers.os.crash.CrashPages;
import dev.jstech.computers.os.install.IInstallingMachine;
import dev.jstech.computers.os.player.PlayerSystems;
import dev.jstech.computers.os.player.SystemRun;
import dev.jstech.computers.power.UpsReading;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.core.jstechseries.peripheral.IPeripheralOwner;
import dev.jstech.core.jstechseries.tier.Era;
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

    /** How long the machine's system has been up at {@code now}, in ticks; zero for one that is not up. */
    default long upTicks(final long now) {
        return 0L;
    }

    /**
     * The kind of machine this computer is, as its entry names it: what decides which programs made for one machine
     * install and show a launcher on it. A host with no entry of its own is no machine in particular.
     */
    default HostScope machine() {
        return HostScope.ANY;
    }

    /**
     * Switches the machine on or off, as its own power control does. Shutting down from inside the
     * system has to reach this: a machine whose screen merely closed is still running, still on the
     * network, and still holding whatever was open.
     */
    void setPowered(boolean on);

    /**
     * Shuts the machine down from inside its system: whatever is running says goodbye first, and the power goes
     * when it has finished. A host with nothing to show switches off at once, which is what the plain form does.
     */
    default void shutDown() {
        setPowered(false);
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

    /**
     * The slot of the disk the firmware boots, the one {@link #systemDisk()} answers with, or {@code -1} when no disk
     * holds a system. What works on the running system works here, never on the first disk a new install would take.
     */
    default int systemDiskSlot() {
        return OsDisks.systemDiskSlot(this.diskSlots(), this::diskInSlot, this.bootDiskSlot());
    }

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
     * Whether the machine is running the live desktop a medium started (Linux Mint's), off a disk in its memory and
     * writing nothing to any disk of its own (computers/docs/design/operating-systems.md). A host that keeps none
     * never is.
     */
    default boolean liveSession() {
        return false;
    }

    /**
     * Asks for the live desktop of that system's medium at the machine's next start; answers whether this host keeps
     * live sessions at all, which only the computers a player sits at do.
     */
    default boolean startLiveSession(final ResourceLocation systemId) {
        return startLiveSession(systemId, false);
    }

    /**
     * The same, off the medium or, with {@code overNetwork}, brought from the Mirror through the firmware's network
     * boot, which the live desktop's installer then copies from too.
     */
    default boolean startLiveSession(final ResourceLocation systemId, final boolean overNetwork) {
        return false;
    }

    /** Whether the live desktop running came over the network rather than off a medium. */
    default boolean liveOverNetwork() {
        return false;
    }

    /**
     * Settles the live session at the end of the self-test: the one asked for comes up, and one that was running is
     * gone with the restart, as a live session is.
     */
    default void settleLiveSession() {
    }

    /**
     * Opens on each disk the partition it starts, at the end of the self-test (computers/docs/design/
     * storage-devices.md): the active one on a Master Boot Record, and otherwise the one with a system. Nothing on a
     * machine whose disks have no partitions to choose between.
     */
    default void openBootPartitions() {
    }

    /**
     * Plays one of its system's own sounds out of its monitors, when its sound hardware plays recordings. A machine
     * with no sound of its own, such as a server in a rack, plays none.
     */
    default void systemSound(final ServerLevel level, final SystemSound sound) {
    }

    /** Plays one of the series' programs' own sounds out of its monitors, as it plays its system's. */
    default void programSound(final ServerLevel level, final ProgramCue sound) {
    }

    /**
     * Rings its system's bell: the system's own through the sound card, or the speaker in the case where the system
     * has none. A machine with no sound of its own, such as a server in a rack, rings nothing.
     */
    default void bell(final ServerLevel level) {
    }

    /** It read or wrote its disk, which a hard drive is heard doing. A machine with no sound of its own is silent. */
    default void diskWorked(final ServerLevel level) {
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
     * How the UPS that carries this machine stands, for the battery its system shows: none without one. Answered on
     * both sides, as {@link #networkAttached()} is.
     */
    default UpsReading upsReading() {
        return UpsReading.NONE;
    }

    /** The UPS that carries this machine by name, as its Device Manager tells it; empty without one. On the server. */
    default Text upsName() {
        return Text.EMPTY;
    }

    /** The power supply that feeds this machine, the stack that carries its wear; empty for none. On the server. */
    default ItemStack psuStack() {
        return ItemStack.EMPTY;
    }

    /**
     * The share of its clock the machine's heat leaves it, from a quarter to all of it: what its programs' credits run
     * at (computers/docs/design/temperature.md). All of it for a machine with no temperature.
     */
    default double heatClock() {
        return 1.0;
    }

    /** How the machine's heat stands, for its firmware and its system's tools; none for one with no temperature. */
    default ThermalState thermal() {
        return ThermalState.NONE;
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
     * What this machine's disks store changed behind the network's back (a Defrag gave corrupted items back): it is
     * saved, and whatever lends that storage to the network has it read again.
     */
    default void storageChanged() {
        setChanged();
    }

    /**
     * Tells the world only that this machine's block has unsaved state, without writing the console onto its disks
     * first. For progress that moves every tick: the next save of the block writes it, and the moments that need the
     * disks to be current right now (a start, an end, a cancel) use {@link #markChanged()}.
     */
    default void markUnsaved() {
        markChanged();
    }

    /** The parts this machine is built from right now, or nothing when it is not built from parts. */
    @Nullable
    default ComputerBuild currentBuild() {
        return null;
    }

    /**
     * Whether the machine's board came out of the chain blank: switched on, it turns its fans and nothing more, and
     * its monitor has no signal, until a Preinstaller writes the firmware (computers/docs/design/survival.md).
     */
    default boolean firmwareMissing() {
        return false;
    }

    /** How the machine starts when its logic board is one of Quince's, whose firmware it carries; none otherwise. */
    default QuinceStart quinceStart() {
        final ComputerBuild build = currentBuild();
        return build == null ? QuinceStart.NONE
                : QuinceStart.of(build.motherboard().formFactor(), build.motherboard().era());
    }

    /**
     * What is wrong with a machine switched on with parts that make no computer, a board and a supply with no
     * processor or no memory, say: the first thing its board's self-test finds; null for a machine that runs, is off,
     * or has no board and supply to test anything. Only Quince's put it on the glass, as the Sad Quince.
     */
    @Nullable
    default Text hardwareFault() {
        return null;
    }

    /**
     * The parts seated in this machine as items, which carry the names its system shows them by; nothing when it is
     * not built from parts.
     */
    default List<ItemStack> hardwareStacks() {
        return List.of();
    }

    /**
     * Whether this machine's processor starts {@code os}: it runs what the system was built for. A machine with no
     * processor to ask, or built from no parts at all, holds no system against it.
     */
    default boolean runsSystem(final OsDef os) {
        // A system a player wrote runs only where a player sits, as the machine's process 1.
        if (PlayerSystems.os(os.id()) != null && !runsPlayerSystems()) {
            return false;
        }
        final ComputerBuild build = currentBuild();
        return os.runsOn(build == null ? null : build.isa());
    }

    /**
     * The id of the instruction set of this machine's processor, which Quince's systems name their architecture after;
     * empty for a machine that cannot say what it is built of.
     */
    default String processorIsa() {
        final ComputerBuild build = this.currentBuild();
        return build == null || build.cpus().isEmpty() ? "" : build.cpus().getFirst().isa().id();
    }

    /**
     * How many bits wide this machine's processor is, which is what a system names its architecture from. A
     * machine that cannot say what it is built of is taken for one of today's.
     */
    default int processorBits() {
        final ComputerBuild build = this.currentBuild();
        return build == null || build.cpus().isEmpty() ? 64 : build.cpus().getFirst().isa().bits();
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

    /** The player-given machine name, or an empty string. The system on the machine goes by the same name. */
    String customName();

    /**
     * Renames the machine (an empty name clears it), and the system on it with it: the assembly screen, the
     * system's settings, the prompt and the installer all name the one machine.
     */
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
    Era installedEra();

    /** The hardware era the GUI should wear (a fixed-era chassis wins over the board). */
    @Nullable
    Era displayEra();

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

    /**
     * What the operating system a player wrote has said while it runs here (computers/docs/design/player-systems.md),
     * or null on a host that runs none.
     */
    @Nullable
    default SystemRun systemRun() {
        return null;
    }

    /**
     * Whether this host runs operating systems players wrote: a computer a player sits at does; a server in a rack runs
     * the mod's own systems only.
     */
    default boolean runsPlayerSystems() {
        return false;
    }

    /**
     * What the programs on this machine reach through it, which is the host they run on; null on a host that runs
     * none, where a program is given a clock that never moves and nothing else.
     */
    @Nullable
    default MachineServices services() {
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
        // One ended in this run, for its memory or by a signal, is down until the machine starts over.
        if (serviceEnded(service.id())) {
            return false;
        }
        /*
         * A language's runtime is in memory while it has something to run, the way an interpreter is loaded for
         * a program and not for the disk it sits on. A service registered under a language's own name is that
         * language's runtime, so nothing has to be named here for this to hold for an addon's language too.
         */
        if (JsComputersApi.languages().get(service.id()) == null) {
            return true;
        }
        final MachinePrograms running = programs();
        return running != null && !running.isEmpty();
    }

    /**
     * What a service running here holds in memory past the floor it declares, from what it is doing now: a server
     * streaming to many computers holds more than one streaming to none. Nothing, unless the machine says.
     */
    default int serviceLoadMb(final ProgramSpec service) {
        return 0;
    }

    /**
     * Whether {@code program} is on this machine though no package of it is written down, because the machine came
     * with it: a Mainframe's engine. Nothing, unless the machine says.
     */
    default boolean cameWith(final ProgramSpec program) {
        return false;
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

    /** Whether that service was ended in this run, by the kernel for its memory or by a signal sent to its process. */
    default boolean serviceEnded(final ResourceLocation service) {
        return false;
    }

    /** Ends that service for the rest of this run, as a signal sent to its process does. */
    default void endService(final ResourceLocation service) {
    }

    /** Starts that service again after it was ended in this run, as Server Admin's Start does. */
    default void startService(final ResourceLocation service) {
    }

    /**
     * Brings the running system down on its crash page for {@code cause}: everything it was running dies where it
     * stands, and whoever is watching is shown the page.
     *
     * @return whether it went down; a host whose system cannot crash, or one that is not up, stays as it is
     */
    default boolean crash(final CrashCause cause, final CrashPages.Moment moment) {
        return false;
    }

    /**
     * Brings an operating system a player wrote down on {@code page} (computers/docs/design/player-systems.md), even
     * while it is still coming up, since a system that faults in its own start is down all the same.
     *
     * @return whether it went down; a host that runs no such system, or one that is not up, stays as it is
     */
    default boolean panic(final CrashPage page) {
        return false;
    }

    /**
     * A program asked this machine for memory it no longer has: Frames goes down on its page, a Unix kernel ends what
     * holds the most until it fits, and the earliest systems refuse it, which is all a host that keeps no account of
     * its memory does.
     *
     * @param needMb what the program asks for on top of what the machine holds, or zero when it holds it already
     * @return whether there is room for it now
     */
    default boolean memoryShort(final String program, final int needMb) {
        return false;
    }

    /** Whether this machine's system ends programs for their memory, and so lets every window open first. */
    default boolean killsForMemory() {
        return false;
    }

    /**
     * Whether this machine's system has paused what it runs to ask the player what to force quit, as Quince's later
     * systems do when the memory runs out.
     */
    default boolean askingForMemory() {
        return false;
    }

    /** The player chose what to force quit from the system's question of its memory, by the tokens it gave them. */
    default void forceQuit(final List<String> tokens) {
    }

    /**
     * Weighs what the system holds against what it has, and answers a system that holds more as it answers any program
     * it has no room for. The machine does it once a second, and again at once when a desktop hands it windows.
     */
    default void weighMemory() {
    }

    /** What the kernel logged about its memory in this run, oldest first, which {@code dmesg} prints. */
    default List<String> memoryLog() {
        return List.of();
    }

    /** This machine's memory ledger, see {@link MachineMemory#ledgerOf}. */
    default RamLedger ramLedger() {
        return MachineMemory.ledgerOf(this);
    }

    /**
     * What the machine runs for the devices linked to it at the tick {@code now}, each a process holding its memory:
     * a Lithography Station's exposure. None unless the machine says.
     */
    default List<RamLedger.Entry> hostedWork(final long now) {
        return List.of();
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
