/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.hardware.FormFactor;
import dev.jstech.computers.item.CpuItem;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.item.IExpansionCardItem;
import dev.jstech.computers.item.MotherboardItem;
import dev.jstech.computers.item.PsuItem;
import dev.jstech.computers.item.RamItem;
import dev.jstech.computers.os.OsDef;
import dev.jstech.core.network.IDataNetworkConnectable;
import dev.jstech.core.network.DataTier;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.peripheral.IPeripheralOwnerSupport;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Shared base for every computer that is a BLOCK (Personal Computer, Mainframe, Crafting Computer, and
 * future ones such as Subframe / Supercomputer / AI Server).
 *
 * <p>What a computer is made of is held in parts, each owning one matter of it: the hardware installed and
 * what it adds up to, the power, where it stands on the data network, what hangs off its peripheral cables,
 * the system it boots and the session it runs it in, the console that rides on its disk, the programs it is
 * running, the players watching it, and what it sends them.
 *
 * <p>This class is where those parts are put together. It holds them, answers to the names the rest of the
 * mod has always called, decides what THIS kind of computer accepts in a slot, which is the one thing each
 * kind settles for itself, and saves each part in turn.
 */
public abstract class AbstractComputerBlockEntity extends BlockEntity
        implements IPeripheralOwnerSupport, dev.jstech.computers.os.IOsHost {

    /** The parts installed and what they add up to; it is built with the layout, so the constructor sets it. */
    private final ComputerHardware hardware;
    /** Whether it is on, whether it comes up by itself, and whether the next look at it shows the self-test. */
    private final ComputerPower power = new ComputerPower(this::setChanged, this::endSession);
    /** Where this computer stands on the data network: its node, its network, and the cable it reads. */
    private final NetworkAttachment attachment = new NetworkAttachment(this);
    /** What is on the far end of its peripheral cables. */
    private final PeripheralEndpoints peripherals = new PeripheralEndpoints();
    /** The system it boots and the session it runs: disks, desktop, windows, installing and formatting. */
    private final OsSession session = new OsSession(this);
    /** The console it keeps, which rides on the system disk rather than on the machine. */
    private final DiskConsole diskConsole = new DiskConsole(this);
    /** The programs it is running and what they reach through it. */
    private final ProgramHost host = new ProgramHost(this);
    /** The players with this computer's console on screen. */
    private final Viewers viewers = new Viewers(this.worldPosition);
    /** What it sends them: the windows its programs have open, and what those programs print. */
    private final ClientReplication replication = new ClientReplication(this);

    /** The name a player gave this computer: the machine's own, and no part's. */
    private String computerName = "";

    /*
     * The recipe drafts the Pattern Studio edits, kept out of the session deliberately: a power cut ends a
     * session and closes its windows, while a draft half laid out is still there afterwards, for whoever
     * sits down next.
     */
    private final dev.jstech.computers.crafting.PatternWorkbench studio =
            new dev.jstech.computers.crafting.PatternWorkbench();

    protected AbstractComputerBlockEntity(final BlockEntityType<?> type, final BlockPos pos,
                                          final BlockState state, final ComputerHardwareLayout layout) {
        super(type, pos, state);
        this.hardware = new ComputerHardware(this, layout);
    }

    /* The parts installed have changed: the build is worked out again and the power reconsidered. */
    void hardwareChanged() {
        power.hardwareChanged(buildValid());
        setChanged();
    }

    // Hardware assembly

    protected abstract Set<FormFactor> acceptedFormFactors();

    /**
     * The hardware era a board must belong to for this computer to accept it, or {@code null} when the
     * computer takes a board of any era (the default). A non-null value gates both slot insertion and the
     * computed build: a board whose era differs is neither installable nor counted. Used by the era-specific
     * Personal Computers to keep, for example, a Legacy board out of a Standard machine even though both are
     * ATX.
     */
    @Nullable
    protected HardwareEra requiredBoardEra() {
        return null;
    }

    /**
     * Whether {@code stack} is a motherboard this computer accepts: it must match an accepted form factor
     * and, when {@link #requiredBoardEra()} is set, also match that era.
     */
    protected boolean isAcceptedBoard(final ItemStack stack) {
        if (!MotherboardItem.fits(stack, acceptedFormFactors())) {
            return false;
        }
        final HardwareEra required = requiredBoardEra();
        return required == null
                || (stack.getItem() instanceof MotherboardItem board && board.spec().era() == required);
    }

    /**
     * Whether {@code stack} is a processor this machine's board can seat: the socket has to match, and
     * so does the hardware generation. A chip that physically cannot go in the socket should not go in
     * the slot either, since letting it in only to refuse to boot tells the player nothing about why.
     */
    protected boolean isValidCpu(final ItemStack stack) {
        if (!(stack.getItem() instanceof CpuItem cpu)) {
            return false;
        }
        final ItemStack boardStack = getHardware().getStackInSlot(layout().motherboardSlot());
        if (!(boardStack.getItem() instanceof MotherboardItem board)) {
            return true; // no board yet: allow pre-staging, as the expansion slots do
        }
        return cpu.spec().socket() == board.spec().socket()
                && cpu.spec().era() == board.spec().era();
    }

    /**
     * Whether {@code stack} is memory this machine's board takes: the board lists the RAM generations
     * its slots are keyed for, and the module must belong to the same hardware generation.
     */
    protected boolean isValidRam(final ItemStack stack) {
        if (!(stack.getItem() instanceof RamItem ram)) {
            return false;
        }
        final ItemStack boardStack = getHardware().getStackInSlot(layout().motherboardSlot());
        if (!(boardStack.getItem() instanceof MotherboardItem board)) {
            return true; // no board yet: allow pre-staging
        }
        return board.spec().acceptedRam().contains(ram.spec().generation())
                && ram.spec().era() == board.spec().era();
    }

    protected boolean isValidPcieCard(final ItemStack stack) {
        if (!(stack.getItem() instanceof IExpansionCardItem card)) {
            return false;
        }
        final ItemStack boardStack = getHardware().getStackInSlot(layout().motherboardSlot());
        if (!(boardStack.getItem() instanceof MotherboardItem motherboard)) {
            /*
             * No board yet, so accept the card so it can be pre-staged; the slot stays inoperative
             * until a board arrives.
             */
            return true;
        }
        return card.cardSpec().bus().compatibleWith(motherboard.spec().pcieGeneration());
    }

    public boolean isValidForSlot(final int slot, final ItemStack stack) {
        if (slot == layout().motherboardSlot()) {
            return isAcceptedBoard(stack);
        }
        if (slot == layout().psuSlot()) {
            return stack.getItem() instanceof PsuItem;
        }
        if (layout().isCpu(slot)) {
            return isValidCpu(stack);
        }
        if (layout().isRam(slot)) {
            return isValidRam(stack);
        }
        if (layout().isPcie(slot)) {
            return isValidPcieCard(stack);
        }
        if (layout().isDisk(slot)) {
            return stack.getItem() instanceof DiskItem;
        }
        return false;
    }

    public ItemStackHandler getHardware() {
        return hardware.handler();
    }

    /** Where this computer's slots are: which one takes the board, which ones take disks, and how many. */
    ComputerHardwareLayout layout() {
        return hardware.layout();
    }

    protected void markBuildDirty() {
        hardware.markDirty();
    }

    @Nullable
    public ComputerBuild currentBuild() {
        return hardware.current();
    }

    public boolean buildValid() {
        return hardware.valid();
    }

    public boolean isRunning() {
        return buildValid() && power.on();
    }

    public boolean isManualOn() {
        return power.on();
    }

    public boolean isAutoStart() {
        return power.autoStart();
    }

    public void togglePower() {
        power.toggle();
    }

    @Override
    public void setPowered(final boolean on) {
        power.setPowered(on);
    }

    public void toggleAutoStart() {
        power.toggleAutoStart(buildValid());
    }

    private void endSession() {
        session.drop();
    }

    /** Whether the next monitor use should play the power-on self-test before booting. */
    public boolean needsPost() {
        return power.needsPost();
    }

    public void setNeedsPost(final boolean value) {
        power.setNeedsPost(value);
    }

    @Override
    public int pendingInstallSlot() {
        return session.pendingInstallSlot();
    }

    @Override
    public void setPendingInstallSlot(final int slot) {
        session.setPendingInstallSlot(slot);
    }

    @Override
    @Nullable
    public ResourceLocation bootedDesktopId() {
        return session.bootedDesktopId();
    }

    @Override
    public void setBootedDesktopId(@Nullable final ResourceLocation id) {
        session.setBootedDesktopId(id);
    }

    @Override
    public dev.jstech.computers.crafting.PatternWorkbench studio() {
        return studio;
    }

    @Override
    public java.util.List<dev.jstech.computers.os.OpenWindow> openWindows() {
        return session.openWindows();
    }

    @Override
    public void setOpenWindows(final java.util.List<dev.jstech.computers.os.OpenWindow> windows) {
        session.setOpenWindows(windows);
    }

    public long capacity() {
        return hardware.capacity();
    }

    public long ramBuffer() {
        return hardware.ramBuffer();
    }

    public int boardCpuSlots() {
        return hardware.boardCpuSlots();
    }

    public int boardRamSlots() {
        return hardware.boardRamSlots();
    }

    public int boardPcieSlots() {
        return hardware.boardPcieSlots();
    }

    public int boardDiskSlots() {
        return hardware.boardDiskSlots();
    }

    /** The hardware era of the installed motherboard, or {@code null} when no board is present. */
    @Nullable
    public HardwareEra installedEra() {
        return hardware.installedEra();
    }

    /**
     * The hardware era the GUI should wear. A per-era chassis (a Vintage or Legacy computer block) fixes its era
     * regardless of what is installed, so its assembly GUI shows the right era skin even when empty; every other
     * computer takes its look from the installed board, falling back to {@code null} (the STANDARD skin) when bare.
     */
    @Nullable
    public HardwareEra displayEra() {
        return getBlockState().getBlock() instanceof dev.jstech.computers.block.IEraChassisBlock chassis
                ? chassis.chassisEra()
                : installedEra();
    }

    public int installedCpus() {
        return hardware.installedCpus();
    }

    public int installedRam() {
        return hardware.installedRam();
    }

    public int installedGpus() {
        return hardware.installedGpus();
    }

    /** The best (max) CPU clock in MHz across installed CPUs, or 0 when there is no valid build. */
    public int maxCpuMhz() {
        return hardware.maxCpuMhz();
    }

    /** The usable VRAM in MB across installed GPUs, or 0 when there is no valid build. */
    public int totalVramMb() {
        return hardware.totalVramMb();
    }

    /**
     * Free space on the system disk in real MB, for the program-install disk-footprint gate: the free
     * mB-equivalent weight ({@link #systemDiskFreeWeight()}) at what an item costs on that disk's era.
     */
    public long systemDiskFreeMb() {
        return session.systemDiskFreeMb();
    }

    public int installedDisks() {
        return hardware.installedDisks();
    }

    /*
     * Host-facing slot-count names (alias the board-derived counts), so subclasses that implement
     * IComputerTerminalHost inherit these without boilerplate.
     */
    public int cpuSlots() {
        return boardCpuSlots();
    }

    public int ramSlots() {
        return boardRamSlots();
    }

    public int gpuSlots() {
        return boardPcieSlots();
    }

    public int diskSlots() {
        return boardDiskSlots();
    }

    // Identity & name

    public NodeUuid nodeUuid() {
        return attachment.node();
    }

    @Nullable
    public NetworkUuid networkUuid() {
        return attachment.network();
    }

    @Override
    public boolean networkAttached() {
        return attachment.attached();
    }

    /** Where this computer stands on the data network, for a Mainframe, which owns its network itself. */
    protected NetworkAttachment attachment() {
        return attachment;
    }

    public String customName() {
        return computerName;
    }

    public void setCustomName(final String name) {
        final String trimmed = name.strip();
        final String capped = trimmed.length() > 32 ? trimmed.substring(0, 32) : trimmed;
        if (!capped.equals(computerName)) {
            computerName = capped;
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    /*
     * OS installation (shared across all computer block entities)
     * The OS lives on the system disk's SYSTEM_OS component so it travels with the disk.
     */

    /**
     * Returns the first installed disk stack whose {@code SYSTEM_OS} component points to a
     * registered OS, or {@link ItemStack#EMPTY} when no bootable disk is present.
     *
     * <p>The system disk is defined as the first disk slot (lowest index) holding a
     * {@link DiskItem} with a {@code SYSTEM_OS} component that maps to a known {@link OsDef}.
     */
    public ItemStack systemDisk() {
        return session.systemDisk();
    }

    /** The disk slot index the firmware boots first, or {@code -1} for "the first disk with a system". */
    public int bootDiskSlot() {
        return session.bootDiskSlot();
    }

    /** Sets the preferred boot disk slot ({@code -1} = automatic) and marks the computer dirty. */
    public void setBootDiskSlot(final int slot) {
        session.setBootDiskSlot(slot);
    }

    /**
     * Returns {@code true} when a bootable system disk is present in the hardware inventory.
     */
    /**
     * The desktop environment this computer boots into: the OS's bundled one (the Frames editions), else the
     * first desktop-environment package installed on it (a Linux distribution after {@code apt install gnome}),
     * else null (a TTY-only or network OS).
     */
    public ResourceLocation installedDesktopId() {
        return session.installedDesktopId();
    }

    public boolean hasOs() {
        return session.hasOs();
    }

    /**
     * Returns the stacks in this computer's disk slots, in slot order. Entries may be empty or hold
     * non-disk items; callers filter as needed (used by the "This PC" disk listing).
     */
    public java.util.List<ItemStack> diskStacks() {
        return session.diskStacks();
    }

    /** The disk stack in the given 0-based disk slot (for renaming a specific installed disk), or EMPTY. */
    public ItemStack diskInSlot(final int slot) {
        return session.diskInSlot(slot);
    }

    /**
     * Returns the registry key of the OS on the system disk, or {@code null} when no bootable
     * disk is installed.
     */
    @Nullable
    public ResourceLocation installedOsId() {
        return session.installedOsId();
    }

    /**
     * Returns the {@link OsDef} for the OS on the system disk, or {@code null} when no bootable
     * disk is installed or the registry entry is absent.
     */
    @Nullable
    public OsDef installedOs() {
        return session.installedOs();
    }

    /**
     * Returns the disk footprint of the installed OS in item-equivalents, or zero when no OS is
     * present. This is subtracted from the usable storage capacity so the OS competes for disk
     * space alongside stored data.
     */
    public long reservedByOs() {
        return session.reservedByOs();
    }

    /**
     * Free weight in mB-equivalents available on the system disk for user files: the disk capacity
     * minus the stored items, the existing files, and the installed OS footprint. Returns 0 when no
     * system disk is present.
     */
    public long systemDiskFreeWeight() {
        return session.systemDiskFreeWeight();
    }

    /**
     * Installs the OS identified by {@code osId} onto a disk in this computer's hardware inventory.
     *
     * <p>The method scans disk slots in order and picks the first {@link DiskItem} slot (preferring
     * one that already carries a {@code SYSTEM_OS} over a plain data disk, so re-installing the
     * same OS is idempotent). The OS footprint in mB-equivalents must fit within the chosen disk's
     * free weight ({@code capacity − stored items' weight − FILESYSTEM.usedWeight}).
     *
     * <p>Returns {@code false} without making any change when:
     * <ul>
     *   <li>the id is unknown to the registry,</li>
     *   <li>no disk is installed in the hardware inventory, or</li>
     *   <li>the OS footprint does not fit the chosen disk's free space.</li>
     * </ul>
     *
     * Returns {@code true} on success; the component is written to the disk stack in the slot,
     * the change is persisted, and clients are notified.
     */
    public boolean installOs(final ResourceLocation osId) {
        return installOs(osId, -1);
    }

    /** Tells every console open on this computer how a source build is coming along. */
    public void tickBuildProgress(final net.minecraft.server.level.ServerLevel level) {
        replication.pushBuildProgress(level);
    }

    /**
     * Every player with this computer's console on screen: its terminal menus, or its open desktop.
     *
     * <p>The list is the machine's own, kept on the server thread: read it, do not keep it.
     */
    public java.util.List<net.minecraft.server.level.ServerPlayer> consoleViewers(
            final net.minecraft.server.level.ServerLevel level) {
        return viewers.at(level);
    }

    /** Tells the machine at {@code host} that this player opened its desktop or prompt; nothing on the client. */
    public static void screenOpened(final net.minecraft.world.entity.player.Player player, final BlockPos host) {
        if (player instanceof net.minecraft.server.level.ServerPlayer viewer
                && !(player instanceof net.neoforged.neoforge.common.util.FakePlayer)
                && viewer.level().isLoaded(host)
                && viewer.level().getBlockEntity(host) instanceof AbstractComputerBlockEntity machine) {
            machine.viewers.opened(viewer);
        }
    }

    /** Tells the machine at {@code host} that this player closed its desktop or prompt. */
    public static void screenClosed(final net.minecraft.world.entity.player.Player player, final BlockPos host) {
        if (player instanceof net.minecraft.server.level.ServerPlayer viewer && viewer.level().isLoaded(host)
                && viewer.level().getBlockEntity(host) instanceof AbstractComputerBlockEntity machine) {
            machine.viewers.closed(viewer);
        }
    }

    /**
     * Whether this computer still has something to run: the installed OS on a disk, or a live-install
     * session whose medium is still in a linked drive. A live session whose medium was pulled out is
     * dropped here (the machine "crashed"), so the next boot lands on the firmware instead of a ghost
     * installer shell.
     */
    public boolean validateOsSession() {
        return session.validateOsSession();
    }

    /**
     * Formats disk slot {@code slot}: erases the installed system, every file, the item storage and the
     * privacy split on it, leaving a blank disk. The boot-order pointer is cleared when it pointed here.
     * Returns whether a disk was actually formatted.
     */
    public boolean formatDisk(final int slot) {
        return session.formatDisk(slot);
    }

    /**
     * A disk carrying a system was just formatted: everything the software layer remembered lived on it,
     * so the console's history, session location and installed-program set go with it. Subclasses hosting
     * software services (the Mainframe) extend this to switch those off too.
     */
    protected void onSystemErased() {
        final dev.jstech.computers.program.ComputerConsoleState console = console();
        if (console != null) {
            console.wipeSoftware();
        }
    }

    /**
     * The disk slot the firmware installs onto by default: the first disk without a system (so a second OS
     * lands beside the first for dual boot), else the first disk; {@code -1} when no disk is installed.
     */
    public int defaultInstallSlot() {
        return session.defaultInstallSlot();
    }

    /**
     * Installs {@code osId} onto disk slot {@code preferredSlot}, or ({@code -1}) onto the default target:
     * a slot already carrying this OS (an idempotent re-install), else the first disk without a system, else
     * the first disk. Returns false when the OS is unknown, no disk is present, or the footprint does not fit.
     */
    public boolean installOs(final ResourceLocation osId, final int preferredSlot) {
        return session.installOs(osId, preferredSlot);
    }

    /**
     * Removes the OS from the system disk. A no-op when no bootable disk is installed.
     */
    public void uninstallOs() {
        session.uninstallOs();
    }

    /*
     * Peripheral ownership: the endpoint set + standard owner methods come from
     * IPeripheralOwnerSupport; only the capacity is hardware-dependent (4 monitors per GPU).
     */

    @Override
    public Set<Long> peripheralEndpoints() {
        return peripherals.all();
    }

    @Override
    public void markPeripheralChange() {
        setChanged();
    }

    @Override
    public int maxEndpoints() {
        final ComputerBuild build = currentBuild();
        if (build == null) {
            return 0;
        }
        return build.motherboard().peripheralPorts();
    }

    /*
     * Network participation (default: a passive client that reads its network from a cable).
     * The Mainframe overrides this entirely (it owns and orchestrates a network).
     */

    protected abstract void registerNode(NetworkSystem system, NetworkUuid network);

    protected abstract void unregisterNode(NetworkSystem system, NetworkUuid network);

    protected void tickNode(final ServerLevel level) {
        tickBuildProgress(level);
        tickSigma();
        dev.jstech.computers.os.install.SetupRunner.tick(this, level, worldPosition);
        attachment.tick(level);
    }

    public void onBroken(final ServerLevel level) {
        attachment.leave(level);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        /*
         * The block's onRemove only fires on destruction; a plain chunk unload removes the block entity
         * without it, so without this the node would stay registered in the still-loaded per-level
         * network as a phantom. onBroken is idempotent, so the destruction path running both is safe.
         */
        if (level instanceof ServerLevel serverLevel) {
            onBroken(serverLevel);
        }
    }

    protected boolean acceptsTier(final DataTier tier) {
        return getBlockState().getBlock() instanceof IDataNetworkConnectable device
                && device.acceptedCableTiers().contains(tier);
    }

    /** The Σ# programs this machine is running. */
    public dev.jstech.computers.machine.MachinePrograms programs() {
        return host.programs();
    }

    /** What the Σ# programs on this machine reach through it. */
    public dev.jstech.computers.machine.MachineServices services() {
        return host.services();
    }

    /**
     * How much of that the network holds, for the programs watching it.
     *
     * <p>Off a network, everything reads as none: a watch on a machine with no cable simply never goes
     * off, which is the truthful answer and not an error.
     */
    public long networkStock(final String item) {
        return host.networkStock(item);
    }

    /** The prompt this machine's shell would show, for giving it back when a program lets go. */
    String shellPrompt() {
        return host.shellPrompt();
    }

    /**
     * How many instructions this machine's processors are worth in one tick.
     *
     * <p>A machine with no build is worth nothing, which is the honest answer for one whose parts have
     * been taken out from under a running program.
     */
    public int sigmaCredits() {
        return host.credits();
    }

    /**
     * Runs whatever scripts the machine has, or stops them all if it is no longer up.
     *
     * <p>A computer that has been switched off is not running programs, so they are told so and given
     * their chance to say goodbye rather than being left frozen for whenever it comes back on.
     */
    protected void tickSigma() {
        if (!host.tick() || !(level instanceof ServerLevel server)) {
            return;
        }
        replication.pushOutput(server);
        replication.pushWindows(server);
        host.hearGateways();
    }

    /**
     * Whether the program on another machine that started one of this machine's programs is still there
     * to read what it left.
     *
     * <p>It is only asked about a program that has finished and was started from elsewhere, so an ordinary
     * tick never looks. A machine whose chunk is not loaded is not known to be gone: its programs come back
     * with it, so what was started for them is kept until it can be asked.
     */

    /**
     * Tells the program on another machine that started one of this machine's programs that the program has ended, so
     * a wait on it runs again at once. A machine that is not loaded is not told: its programs look again when they
     * come back.
     */

    /**
     * Hands the programs on this machine whatever the ComputerCraft computers said through its Gateways.
     *
     * <p>A message waits on the Gateway until this tick and no longer: whoever is listening hears it now,
     * and a machine where no program listens simply lets it go.
     */
    /** Says a Gateway linked to this machine has something waiting for its programs. */
    public void gatewayMailWaits() {
        host.gatewayMailWaits();
    }

    /*
     * Provided here (no @Override: this base does not itself declare IComputerTerminalHost) so the
     * computer subclasses that ARE hosts inherit it and satisfy the interface's console() method.
     */
    public dev.jstech.computers.program.ComputerConsoleState console() {
        return diskConsole.state();
    }

    @Override
    public void setChanged() {
        /*
         * Every mutation of installed software ends in setChanged, so this is the one place that
         * guarantees the disk is current before the player can pull it out. Without it, installing a
         * program and immediately removing the drive would lose the install: the in-memory state is
         * discarded when the slot changes, and the world may not have saved in between.
         */
        diskConsole.flush();
        super.setChanged();
    }

    // Persistence (common fields; subclasses add their own via the hooks)

    /**
     * The NBT key the hardware {@link ItemStackHandler} is stored under. Overridable so a subclass with
     * pre-existing saved worlds (the Mainframe, which historically persisted under {@code "Inventory"})
     * can keep its key and load every existing component without data migration.
     */
    protected String hardwareNbtKey() {
        return "Hardware";
    }

    protected void saveExtra(final CompoundTag tag, final HolderLookup.Provider registries) {
    }

    protected void loadExtra(final CompoundTag tag, final HolderLookup.Provider registries) {
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        hardware.load(tag, registries, hardwareNbtKey());
        power.load(tag);
        session.load(tag);
        computerName = tag.getString("ComputerName");
        attachment.load(tag);
        peripherals.load(tag);
        if (tag.contains("Studio")) {
            studio.load(tag.getCompound("Studio"), registries);
        }
        host.load(tag);
        diskConsole.loadLegacy(tag);
        loadExtra(tag, registries);
        hardware.markDirty();
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        /*
         * Push the software onto the disk first: the hardware handler below serializes the disk stacks,
         * and a flush after that point would be written to a copy and lost.
         */
        diskConsole.flush();
        hardware.save(tag, registries, hardwareNbtKey());
        power.save(tag);
        session.save(tag);
        if (!computerName.isEmpty()) {
            tag.putString("ComputerName", computerName);
        }
        attachment.save(tag);
        final CompoundTag studioTag = new CompoundTag();
        studio.save(studioTag, registries);
        tag.put("Studio", studioTag);
        host.save(tag);
        peripherals.save(tag);
        /*
         * The console rides on the system disk, so flush it there BEFORE the hardware handler is
         * serialized above, or the write would land on a disk stack that was already copied.
         */
        saveExtra(tag, registries);
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        final CompoundTag tag = super.getUpdateTag(registries);
        if (!computerName.isEmpty()) {
            tag.putString("ComputerName", computerName);
        }
        attachment.saveForClient(tag);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener>
            getUpdatePacket() {
        /*
         * Without this, a mid-session rename (which calls sendBlockUpdated) never reaches the client, so
         * reopening the assembly screen reads a stale, empty name from the client copy of this block entity.
         */
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(final net.minecraft.network.Connection connection,
                             final net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket packet,
                             final HolderLookup.Provider registries) {
        /*
         * Apply only the display name from a live block update. The rest of the client state is kept in sync
         * through the menu's ContainerData; running the full loadAdditional here would reset transient fields
         * (power, autostart, linked monitors) to their defaults because the update tag is intentionally minimal.
         */
        final CompoundTag tag = packet.getTag();
        computerName = tag != null ? tag.getString("ComputerName") : "";
        attachment.loadFromClient(tag);
    }
}
