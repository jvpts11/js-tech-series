/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.DataCableBlock;
import dev.jstech.computers.block.ServerRackBlock;
import dev.jstech.computers.block.ServerRackPartBlock;
import dev.jstech.computers.block.ServerRackStructure;
import dev.jstech.computers.crafting.PatternWorkbench;
import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.hardware.CpuSpec;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.item.MotherboardItem;
import dev.jstech.computers.item.RackGadgetItem;
import dev.jstech.computers.item.RackUnitItem;
import dev.jstech.computers.item.ServerHardwareHandler;
import dev.jstech.computers.item.ServerItem;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OpenWindow;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsDisks;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.boot.BootLines;
import dev.jstech.computers.os.boot.BootPhases;
import dev.jstech.computers.os.boot.BootRunner;
import dev.jstech.computers.os.boot.BootSequence;
import dev.jstech.computers.os.install.InstallerFlow;
import dev.jstech.computers.os.install.Installers;
import dev.jstech.computers.os.install.OsInstallJob;
import dev.jstech.computers.os.install.OsInstallRunner;
import dev.jstech.computers.os.install.SetupRunner;
import dev.jstech.computers.os.media.LiveMedium;
import dev.jstech.computers.os.media.MediaKind;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.computers.program.install.LiveInstallState;
import dev.jstech.computers.rack.IMountableRackUnit;
import dev.jstech.computers.rack.RackChassis;
import dev.jstech.computers.rack.RackLayout;
import dev.jstech.computers.rack.RackThermals;
import dev.jstech.computers.rack.RaidMode;
import dev.jstech.computers.storage.IDataSink;
import dev.jstech.computers.storage.LocalStore;
import dev.jstech.computers.storage.ServerStore;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.storage.StoreSink;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.network.DataTier;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.network.ServerNode;
import dev.jstech.core.peripheral.IPeripheralOwnerSupport;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import java.util.Arrays;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A Server Rack: a passive container (network Category A, no UUID of its own) measured in rack
 * units. The cabinet offers {@link #CAPACITY_U} rack units; a mounted chassis occupies its height
 * in consecutive rows, addressed by the row of its top edge (server slot i = rack-unit row i, and
 * a taller chassis blocks the rows it covers).
 *
 * <p>The front-panel hotswap slots belong to the RACK, five per rack unit: a mounted chassis
 * cables the slots of the rows it occupies up to its drive and gadget budgets, and drives stay in
 * their slots when the server itself is pulled, and the next chassis mounted over those rows inherits
 * them. All server-side storage is therefore held by the bay drives (each drive's own disk
 * components), never by the Server item.
 */
public class ServerRackBlockEntity extends BlockEntity
        implements IPeripheralOwnerSupport,
        IOsHost,
        IComputerTerminalHost,
        IWatchedConsole,
        GeoBlockEntity {

    // the cabinet as one model: what the renderer needs to know about every row

    /** Codes for what a rack unit row holds, as the model names its bones. Row 0 is the bottom U. */
    public static final int UNIT_NONE = 0;
    public static final int UNIT_SERVER_STANDARD = 1;
    public static final int UNIT_SERVER_LEGACY = 2;
    public static final int UNIT_SERVER_VINTAGE = 3;
    public static final int UNIT_STORAGE_2U = 4;
    public static final int UNIT_COMPUTE_2U = 5;
    public static final int UNIT_NODE_2U = 6;
    public static final int UNIT_KVM_SWITCH = 7;
    public static final int UNIT_RACK_UPS = 8;
    public static final int UNIT_COOLING_UNIT = 9;

    /** The model bone names, indexed by unit code. */
    public static final String[] UNIT_BONES = {"", "server_1u_standard", "server_1u_legacy", "server_1u_vintage",
            "storage_2u", "compute_2u", "node_2u", "kvm_switch", "rack_ups", "cooling_unit"};

    private static final RawAnimation FANS =
            RawAnimation.begin().thenLoop("animation.rack.fans");

    private final AnimatableInstanceCache geckoCache =
            GeckoLibUtil.createInstanceCache(this);

    /*
     * What the client copy knows about the cabinet: it holds no inventory, so the server sends the unit
     * code of every row, the bay power mask and the service panel state with each block update.
     */
    private final byte[] clientUnits = new byte[CAPACITY_U];
    private int clientBayPowerOff;
    private boolean clientServicePanelOff;

    /** Supercomputer Rack only: the livery panel taken off, showing the nodes in the cavity. */
    private boolean servicePanelOff;

    @Override
    public void registerControllers(
            final AnimatableManager.ControllerRegistrar controllers) {
        /*
         * The roof fans turn while any bay is powered; the light bar and the seated units are bone
         * visibility set by the renderer, not animation.
         */
        controllers.add(new AnimationController<>(this, "fans", 0,
                state -> anyBayOn() ? state.setAndContinue(FANS)
                        : PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geckoCache;
    }

    /** The era of this cabinet, read from its block. */
    public HardwareEra rackEra() {
        return getBlockState().getBlock()
                instanceof ServerRackBlock rack
                ? rack.era() : HardwareEra.STANDARD;
    }

    /** What row {@code slot} holds, as a unit code, on either side. */
    public int unitCodeAt(final int slot) {
        if (level != null && level.isClientSide()) {
            return slot >= 0 && slot < CAPACITY_U ? clientUnits[slot] : UNIT_NONE;
        }
        final ItemStack stack = servers.getStackInSlot(slot);
        if (stack.isEmpty()) {
            return UNIT_NONE;
        }
        final RackChassis chassis = ServerItem.chassisOf(stack);
        if (chassis != null) {
            return switch (chassis) {
                case SERVER -> UNIT_SERVER_STANDARD;
                case LEGACY_SERVER -> UNIT_SERVER_LEGACY;
                case VINTAGE_SERVER -> UNIT_SERVER_VINTAGE;
                case STORAGE_SERVER -> UNIT_STORAGE_2U;
                case COMPUTE_SERVER -> UNIT_COMPUTE_2U;
                case SUPERCOMPUTER_NODE -> UNIT_NODE_2U;
            };
        }
        for (final RackUnitItem.Kind kind
                : RackUnitItem.Kind.values()) {
            if (RackUnitItem.is(stack, kind)) {
                return switch (kind) {
                    case KVM_SWITCH -> UNIT_KVM_SWITCH;
                    case RACK_UPS -> UNIT_RACK_UPS;
                    case COOLING_UNIT -> UNIT_COOLING_UNIT;
                };
            }
        }
        return UNIT_NONE;
    }

    /** Whether the bay at {@code slot} is switched on, on either side. */
    public boolean bayLit(final int slot) {
        if (level != null && level.isClientSide()) {
            return (clientBayPowerOff & (1 << slot)) == 0;
        }
        return bayPowerOn(slot);
    }

    /** Whether any seated machine's bay is on: what spins the roof fans and lights the bar. */
    public boolean anyBayOn() {
        for (int slot = 0; slot < CAPACITY_U; slot++) {
            if (unitCodeAt(slot) != UNIT_NONE && unitCodeAt(slot) <= UNIT_NODE_2U && bayLit(slot)) {
                return true;
            }
        }
        return false;
    }

    /** Whether any computer at all is seated (the bar glows amber for "seated but dark"). */
    public boolean anyComputerSeated() {
        for (int slot = 0; slot < CAPACITY_U; slot++) {
            final int code = unitCodeAt(slot);
            if (code != UNIT_NONE && code <= UNIT_NODE_2U) {
                return true;
            }
        }
        return false;
    }

    public boolean servicePanelOff() {
        return level != null && level.isClientSide() ? clientServicePanelOff : servicePanelOff;
    }

    /** Takes the supercomputer's livery panel off or puts it back; a server rack has none. */
    public void toggleServicePanel() {
        if (rackType() != RackChassis.RackType.SUPERCOMPUTER) {
            return;
        }
        servicePanelOff = !servicePanelOff;
        setChanged();
        syncVisuals();
    }

    /** Pushes the row contents, the bay power and the panel state to watching clients. */
    public void syncVisuals() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(),
                    Block.UPDATE_CLIENTS);
        }
    }

    /** The whole 2 x 3 x 2 footprint: the renderer draws the cabinet from this block alone. */
    public AABB renderBox() {
        final BlockState state = getBlockState();
        if (!state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return new AABB(worldPosition);
        }
        AABB box = new AABB(worldPosition);
        for (final BlockPos part : ServerRackStructure.allPositions(
                worldPosition, state.getValue(HorizontalDirectionalBlock.FACING))) {
            box = box.minmax(new AABB(part));
        }
        return box;
    }

    public static final int CAPACITY_U = 8;

    private final RackLayout layout = new RackLayout(CAPACITY_U);

    /** The drives of a bay treated as one volume: the arrays, what a pull costs, and the rebuilds. */
    private final RackArrays arrays = new RackArrays(this, CAPACITY_U);

    private final ItemStackHandler servers = new ItemStackHandler(CAPACITY_U) {
        @Override
        public boolean isItemValid(final int slot, final ItemStack stack) {
            /*
             * Servers and rack equipment (KVM, UPS, cooling) bid for the same rack units, but a
             * computer only mounts in the cabinet its chassis belongs to.
             */
            final IMountableRackUnit unit =
                    IMountableRackUnit.of(stack);
            if (unit == null || !acceptsChassis(stack)) {
                return false;
            }
            return layout.canPlace(slot, unit.heightU(), mountedUnitsExcept(slot));
        }

        @Override
        public int getSlotLimit(final int slot) {
            return 1; // each Server is unique
        }

        @Override
        public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
            if (!simulate) {
                /*
                 * The software state lives with the item: write the running console back onto the
                 * stack before it leaves, so the machine's history travels between racks with it.
                 */
                flushConsole(slot);
            }
            return super.extractItem(slot, amount, simulate);
        }

        @Override
        protected void onContentsChanged(final int slot) {
            /*
             * A freshly mounted (or swapped) machine runs POST on its next session; whatever
             * console state the old occupant left in memory dies with the swap.
             */
            unitStates.remove(slot);
            buildCached[slot] = false;
            markStorageChanged(slot);
            setChanged();
            updateBayVisuals();
            syncDisplayEra(); // a swapped machine can be of another era than the one it replaced
        }
    };

    /**
     * The transient software state of the machine mounted at one rack-unit row: its console (loaded
     * from and flushed back to the Server item, so it persists with the item per the design), the
     * POST flag, and the firmware's preferred boot drive. Recreated on every mount.
     */
    private static final class UnitState {
        final ComputerConsoleState console =
                new ComputerConsoleState();
        /*
         * Where this machine is on its way up. A machine in a rack is a machine: it tests itself, it stops at
         * its boot manager and its system takes time to come up, all of it on its own clocks, whether or not
         * the monitor is showing this bay.
         */
        final BootPhases phases = new BootPhases();
        int bootDiskSlot = -1;
        /** The desktop this machine booted into, fixed at POST so later package changes wait for a reboot. */
        @Nullable
        ResourceLocation bootedDesktopId;
        /** The windows open on this machine's desktop; machine state that rides on the Server item. */
        final List<OpenWindow> openWindows = new ArrayList<>();
        /** Which of that desktop's workspaces is up, which goes wherever the windows it sorts go. */
        int desktopWorkspace;
        /** A guided installer that wrote the system but is still waiting for its reboot. */
        int pendingInstallSlot = IOsHost.NO_PENDING_INSTALL;
        /*
         * A system being copied onto this machine's drives right now, and the installer it belongs to. Kept per
         * bay and flushed onto the Server item with the rest of its session: a machine in a rack is a machine,
         * and its copy has to survive a save and go on with nobody watching, like any other's.
         */
        @Nullable
        OsInstallJob installing;
        @Nullable
        InstallerFlow installer;
        /** The installer's answers, waiting for a level to build it back from after a reload. */
        @Nullable
        CompoundTag installerMemo;
        /** The recipe drafts the Pattern Studio edits on this machine; ride on the Server item like the windows. */
        final PatternWorkbench studio =
                new PatternWorkbench();

        UnitState() {
            // A machine that has just been mounted has never tested itself, so it owes one before anything else.
            phases.setNeedsPost(true);
        }
    }

    private final Map<Integer, UnitState> unitStates = new HashMap<>();

    /** Who is at the rack's glass, and what keeps each server's terminal moving. */
    private final RackTerminals terminals = new RackTerminals(this);

    @Override
    public void consoleOpenedBy(final ServerPlayer viewer) {
        terminals.opened(viewer);
    }

    @Override
    public void consoleClosedBy(final ServerPlayer viewer) {
        terminals.closed(viewer);
    }

    private UnitState unitState(final int slot) {
        return unitStates.computeIfAbsent(slot, s -> {
            final UnitState state = new UnitState();
            final ItemStack stack = servers.getStackInSlot(s);
            final CompoundTag saved =
                    stack.get(ComputingModule.SERVER_CONSOLE.get());
            if (saved != null) {
                state.console.load(saved);
                // Absent keys mean a machine that was never brought up, which still needs POST.
                state.phases.setNeedsPost(!saved.contains("NeedsPost") || saved.getBoolean("NeedsPost"));
                state.bootDiskSlot = saved.contains("BootDiskSlot") ? saved.getInt("BootDiskSlot") : -1;
                state.bootedDesktopId = saved.contains("BootedDesktop")
                        ? ResourceLocation.tryParse(saved.getString("BootedDesktop")) : null;
                state.openWindows.addAll(OpenWindow.loadAll(
                        saved.getList("OpenWindows", Tag.TAG_COMPOUND)));
                state.desktopWorkspace = OpenWindow.clampWorkspace(saved.getInt("DesktopWorkspace"));
                state.pendingInstallSlot = saved.contains("PendingInstall") ? saved.getInt("PendingInstall")
                        : IOsHost.NO_PENDING_INSTALL;
                if (saved.contains("Installing")) {
                    final CompoundTag copying = saved.getCompound("Installing");
                    state.installing = new OsInstallJob(
                            copying.getString("Os"), copying.getInt("Slot"), copying.getLong("Reader"),
                            copying.getInt("Total"), copying.getInt("Left"));
                }
                state.installerMemo = saved.contains("Installer") ? saved.getCompound("Installer") : null;
                if (saved.contains("Studio") && getLevel() != null) {
                    state.studio.load(saved.getCompound("Studio"), getLevel().registryAccess());
                }
            }
            return state;
        });
    }

    @Override
    @Nullable
    public PatternWorkbench studio() {
        final int slot = soleComputerSlot();
        return slot < 0 ? null : unitState(slot).studio;
    }

    /** The console of the machine mounted at {@code slot}, or null when that row holds none. */
    @Nullable
    public ComputerConsoleState consoleOf(final int slot) {
        if (slot < 0 || slot >= CAPACITY_U
                || !(servers.getStackInSlot(slot).getItem() instanceof ServerItem)) {
            return null;
        }
        return unitState(slot).console;
    }

    /**
     * Whether the machine at {@code slot} runs the given server service. Services are what give a
     * server its role: the hardware decides what it can do, the software decides what it does.
     */
    public boolean hasService(final int slot, final String programPath) {
        final ComputerConsoleState console = consoleOf(slot);
        return console != null && console.isInstalled("jsc:" + programPath);
    }

    /** Writes the in-memory console of the unit at {@code slot} back onto its Server item. */
    private void flushConsole(final int slot) {
        final UnitState state = unitStates.get(slot);
        final ItemStack stack = servers.getStackInSlot(slot);
        if (state == null || !(stack.getItem() instanceof ServerItem)) {
            return;
        }
        final CompoundTag tag = new CompoundTag();
        state.console.save(tag);
        /*
         * A machine that is already up must still be up after a reload. Keeping the POST flag only in
         * memory made every server re-run POST when the world came back, as if it had been switched off.
         */
        tag.putBoolean("NeedsPost", state.phases.needsPost());
        tag.putInt("BootDiskSlot", state.bootDiskSlot);
        if (state.bootedDesktopId != null) {
            tag.putString("BootedDesktop", state.bootedDesktopId.toString());
        }
        if (!state.openWindows.isEmpty()) {
            tag.put("OpenWindows",
                    OpenWindow.saveAll(state.openWindows));
        }
        if (state.desktopWorkspace != 0) {
            tag.putInt("DesktopWorkspace", state.desktopWorkspace);
        }
        if (state.pendingInstallSlot != IOsHost.NO_PENDING_INSTALL) {
            tag.putInt("PendingInstall", state.pendingInstallSlot);
        }
        if (state.installing != null) {
            final CompoundTag copying = new CompoundTag();
            copying.putString("Os", state.installing.osId());
            copying.putInt("Slot", state.installing.targetSlot());
            copying.putLong("Reader", state.installing.readerPos());
            copying.putInt("Total", state.installing.ticksTotal());
            copying.putInt("Left", state.installing.ticksLeft());
            tag.put("Installing", copying);
        }
        final CompoundTag pages = installerPages(state);
        if (pages != null) {
            tag.put("Installer", pages);
        }
        if (getLevel() != null) {
            final CompoundTag studioTag = new CompoundTag();
            state.studio.save(studioTag, getLevel().registryAccess());
            tag.put("Studio", studioTag);
        }
        stack.set(ComputingModule.SERVER_CONSOLE.get(), tag);
    }

    private void flushConsoles() {
        for (final int slot : unitStates.keySet()) {
            flushConsole(slot);
        }
    }

    // Five hotswap slots per rack unit, owned by the rack: index = uRow * SLOTS_PER_U + column.
    private final ItemStackHandler frontSlots = new ItemStackHandler(CAPACITY_U * RackLayout.SLOTS_PER_U) {
        @Override
        public boolean isItemValid(final int slot, final ItemStack stack) {
            /*
             * Each front slot takes only what its role cables: drives in a drive bay, bay gadgets
             * (RAID controller, cache card) in a gadget bay, nothing in a blocked one.
             */
            return switch (roleOfFrontSlot(slot)) {
                case DRIVE -> stack.getItem() instanceof DiskItem;
                case GADGET -> stack.getItem()
                        instanceof RackGadgetItem;
                case BLOCKED_NO_UNIT, BLOCKED_BUDGET -> false;
            };
        }

        @Override
        public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
            if (!simulate && getStackInSlot(slot).getItem() instanceof DiskItem) {
                /*
                 * Pulling a member out of an array is the moment the array's promise is tested:
                 * a redundant array keeps its data (and hands back a blank drive), a stripe dies.
                 */
                arrays.onMemberRemoved(slot);
            }
            return super.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(final int slot) {
            return 1; // one drive per hotswap slot
        }

        @Override
        protected void onContentsChanged(final int slot) {
            // Storage lives on the drives, so a hotswap is a storage change of the owning unit.
            final RackLayout.Unit unit = RackLayout.unitAt(
                    slot / RackLayout.SLOTS_PER_U, mountedUnits());
            final int top = unit != null ? unit.topU() : slot / RackLayout.SLOTS_PER_U;
            markStorageChanged(top);
            // A replacement drive completing a degraded array starts the rebuild.
            arrays.maybeStartRebuild(top);
            setChanged();
        }
    };

    private void updateBayVisuals() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        final BlockState self = getBlockState();
        if (!(self.getBlock() instanceof ServerRackBlock)) {
            return;
        }
        final Direction facing = self.getValue(
                HorizontalDirectionalBlock.FACING);
        /*
         * The cabinet model still shows 4 visual bays over 8 rack units: bay b lights up when
         * either of its two rows carries part of a mounted unit.
         */
        final List<RackLayout.Unit> mounted = mountedUnits();
        for (int h = 0; h < 2; h++) {
            for (int w = 0; w < 2; w++) {
                final int bay = h * 2 + w;
                final boolean occupied = RackLayout.unitAt(bay * 2, mounted) != null
                        || RackLayout.unitAt(bay * 2 + 1, mounted) != null;
                final int bits = occupied ? 3 : 0;
                final BlockPos bayPos = ServerRackStructure
                        .bayBlockPos(worldPosition, facing, w, h);
                final BlockState bayState = serverLevel.getBlockState(bayPos);
                final boolean isRackBlock = bayState.getBlock()
                        instanceof ServerRackBlock
                        || bayState.getBlock()
                        instanceof ServerRackPartBlock;
                if (isRackBlock && bayState.getValue(
                        ServerRackBlock.BAYS) != bits) {
                    serverLevel.setBlock(bayPos, bayState.setValue(
                            ServerRackBlock.BAYS, bits),
                            Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    private final long[] storageModCounts = new long[CAPACITY_U];

    /*
     * Each mounted machine's build, parsed from its item once per mount instead of on every tick: the
     * tick, the thermal load and the port count all read it, and a rack of eight was re-parsing eight
     * hardware inventories twenty times a second.
     */
    private final ComputerBuild[] buildCache =
            new ComputerBuild[CAPACITY_U];
    private final boolean[] buildCached = new boolean[CAPACITY_U];

    /*
     * The bay capacity each unit registers, kept until its storage mod count moves (a drive or the machine
     * itself went in or out): the tick asks for it for every unit, every tick, and computing it walks the
     * chassis, the mounted units and the whole front panel.
     */
    private final long[] bayItemsCache = new long[CAPACITY_U];
    private final long[] bayItemsMod = new long[CAPACITY_U];
    private final boolean[] bayItemsCached = new boolean[CAPACITY_U];

    /** The build of the machine mounted at {@code slot} (null for none, or an invalid one), parsed once per mount. */
    @Nullable
    private ComputerBuild buildAt(final int slot) {
        if (!buildCached[slot]) {
            buildCache[slot] = ServerItem.build(servers.getStackInSlot(slot));
            buildCached[slot] = true;
        }
        return buildCache[slot];
    }

    /**
     * What the server in that bay adds up to, or null when the bay holds no server.
     *
     * <p>Worked out once and kept until the bay's parts change, so whoever needs a mounted machine's measures
     * asks the cabinet rather than building it again from the item.
     */
    @Nullable
    public ComputerBuild buildIn(final int slot) {
        return slot >= 0 && slot < CAPACITY_U ? buildAt(slot) : null;
    }

    public void markStorageChanged(final int slot) {
        if (slot >= 0 && slot < storageModCounts.length) {
            storageModCounts[slot]++;
        }
    }

    public long storageModCount(final int slot) {
        return slot >= 0 && slot < storageModCounts.length ? storageModCounts[slot] : 0L;
    }

    private final Map<UUID, NetworkUuid> registered = new HashMap<>();

    // Per-bay power, stored inverted (bit set = switched OFF) so a fresh rack powers every bay on.
    private int bayPowerOff;

    /** Whether the bay whose unit tops at {@code slot} has its power switch on. */
    public boolean bayPowerOn(final int slot) {
        return slot >= 0 && slot < CAPACITY_U && (bayPowerOff & (1 << slot)) == 0;
    }

    /** Flips the power switch of the bay at {@code slot}; a powered-off machine POSTs on power-up. */
    public void toggleBayPower(final int slot) {
        if (slot < 0 || slot >= CAPACITY_U) {
            return;
        }
        bayPowerOff ^= 1 << slot;
        /*
         * The bay switch is this machine's power button, so it does to the phases what a power button does:
         * whatever was under way stops, and a machine coming on owes a self-test before anything else.
         */
        unitState(slot).phases.powered(bayPowerOn(slot));
        /*
         * Off or a cold start, no desktop survives; writing the self-test flag directly here had let it skip
         * the clearing setNeedsPost does.
         */
        unitState(slot).openWindows.clear();
        unitState(slot).desktopWorkspace = 0;
        unitState(slot).pendingInstallSlot = IOsHost.NO_PENDING_INSTALL;
        flushConsole(slot);
        setChanged();
    }

    public static final int DATA_LINKED = 0;
    public static final int DATA_BAY_POWER = 1;
    /** The cabinet's thermal throttle in percent (100 = running free). */
    public static final int DATA_THROTTLE = 2;
    /** Rebuild progress in permille for unit i, at {@code DATA_REBUILD_0 + i}. */
    public static final int DATA_REBUILD_0 = 3;
    public static final int DATA_COUNT = DATA_REBUILD_0 + CAPACITY_U;

    private final ContainerData data =
            new SimpleContainerData(DATA_COUNT);

    public ServerRackBlockEntity(final BlockPos pos, final BlockState state) {
        super(ComputingModule.SERVER_RACK_BE.get(), pos, state);
    }

    public ItemStackHandler getServers() {
        return servers;
    }

    public ItemStackHandler getFrontSlots() {
        return frontSlots;
    }

    public RackLayout layout() {
        return layout;
    }

    public ContainerData getDataAccess() {
        return data;
    }

    /** The occupancy and front-slot budgets of every mounted chassis. */
    public List<RackLayout.Unit> mountedUnits() {
        return mountedUnitsExcept(-1);
    }

    private List<RackLayout.Unit> mountedUnitsExcept(final int exceptSlot) {
        final List<RackLayout.Unit> mounted = new ArrayList<>();
        for (int i = 0; i < CAPACITY_U; i++) {
            if (i == exceptSlot) {
                continue;
            }
            final RackLayout.Unit unit = IMountableRackUnit.unitAt(servers.getStackInSlot(i), i);
            if (unit != null) {
                mounted.add(unit);
            }
        }
        return mounted;
    }

    /** The role of one front-panel slot given what is currently mounted. */
    public RackLayout.SlotRole roleOfFrontSlot(final int slot) {
        return layout.roleAt(slot / RackLayout.SLOTS_PER_U, slot % RackLayout.SLOTS_PER_U,
                mountedUnits());
    }

    /** The live stack in one front slot: writing its components writes the bay. */
    ItemStack frontSlot(final int index) {
        return frontSlots.getStackInSlot(index);
    }

    /** The live stack in one server bay, which is the machine mounted there. */
    ItemStack serverSlot(final int index) {
        return servers.getStackInSlot(index);
    }

    /** What a front slot of that row and column is for, given what is mounted in the rack. */
    RackLayout.SlotRole slotRoleAt(final int row, final int column, final List<RackLayout.Unit> mounted) {
        return layout.roleAt(row, column, mounted);
    }

    /** Which network each bay is registered on, by the machine's node id. */
    Map<UUID, NetworkUuid> registeredNetworks() {
        return registered;
    }

    /** Where the Mainframe of that network is, for a bay that has to tell its index something. */
    Optional<Long> mainframePositionOn(final ServerLevel level, final NetworkUuid network) {
        return NetworkSystem.get(level).mainframePositionOf(network);
    }

    /**
     * The drive stacks the unit mounted at {@code serverSlot} has claimed, in row-major slot order.
     * The returned stacks are the live front-slot stacks: writing their components writes the bay.
     */
    public List<ItemStack> claimedDriveStacks(final int serverSlot) {
        final RackChassis chassis = ServerItem.chassisOf(servers.getStackInSlot(serverSlot));
        if (chassis == null) {
            return List.of();
        }
        final List<RackLayout.Unit> mounted = mountedUnits();
        final List<ItemStack> drives = new ArrayList<>();
        for (int row = serverSlot; row < serverSlot + chassis.heightU() && row < CAPACITY_U; row++) {
            for (int column = 0; column < RackLayout.SLOTS_PER_U; column++) {
                if (layout.roleAt(row, column, mounted) != RackLayout.SlotRole.DRIVE) {
                    continue;
                }
                final ItemStack stack = frontSlots.getStackInSlot(row * RackLayout.SLOTS_PER_U + column);
                if (stack.getItem() instanceof DiskItem) {
                    drives.add(stack);
                }
            }
        }
        return drives;
    }

    /** Inserts a drive into the first free claimed drive slot of the unit at {@code serverSlot}. */
    public boolean insertDrive(final int serverSlot, final ItemStack drive) {
        final RackChassis chassis = ServerItem.chassisOf(servers.getStackInSlot(serverSlot));
        if (chassis == null || !(drive.getItem() instanceof DiskItem)) {
            return false;
        }
        final List<RackLayout.Unit> mounted = mountedUnits();
        for (int row = serverSlot; row < serverSlot + chassis.heightU() && row < CAPACITY_U; row++) {
            for (int column = 0; column < RackLayout.SLOTS_PER_U; column++) {
                if (layout.roleAt(row, column, mounted) != RackLayout.SlotRole.DRIVE) {
                    continue;
                }
                final int slot = row * RackLayout.SLOTS_PER_U + column;
                if (frontSlots.getStackInSlot(slot).isEmpty()) {
                    frontSlots.setStackInSlot(slot, drive.copyWithCount(1));
                    return true;
                }
            }
        }
        return false;
    }

    // RAID: the bay's drives as one logical volume

    /** The front-slot index holding the unit's RAID Controller, or -1 when it has none. */
    public int raidControllerSlot(final int serverSlot) {
        return arrays.controllerSlot(serverSlot);
    }

    /** Whether the unit at {@code serverSlot} has a Cache Card in one of its gadget bays. */
    public boolean hasCacheCard(final int serverSlot) {
        return arrays.hasCacheCard(serverSlot);
    }

    /** The RAID mode the unit at {@code serverSlot} runs, or {@link RaidMode#NONE}. */
    public RaidMode raidModeOf(final int serverSlot) {
        return arrays.modeOf(serverSlot);
    }

    /**
     * Sets the RAID mode of the unit at {@code serverSlot} and stamps the member count the array
     * forms with, so a later pull reads as a degraded array rather than a smaller one.
     */
    public boolean setRaidMode(final int serverSlot, final RaidMode mode) {
        return arrays.setMode(serverSlot, mode);
    }

    /** How many drives the unit's array was formed with (0 when it runs no array). */
    public int raidMemberCount(final int serverSlot) {
        return arrays.memberCount(serverSlot);
    }

    /** Whether the unit's array is missing members but still serving data. */
    public boolean raidDegraded(final int serverSlot) {
        return arrays.degraded(serverSlot);
    }

    /** Whether the unit's array has lost more members than its mode tolerates. */
    public boolean raidFailed(final int serverSlot) {
        return arrays.failed(serverSlot);
    }

    /** Whether the unit's array is currently rebuilding onto a replacement member. */
    public boolean raidRebuilding(final int serverSlot) {
        return arrays.rebuilding(serverSlot);
    }

    /** Rebuild progress of the unit's array in permille, or 0 when it is not rebuilding. */
    public int raidRebuildPermille(final int serverSlot) {
        return arrays.rebuildPermille(serverSlot);
    }

    /** The bay-backed storage capacity of the unit at {@code serverSlot}, in items: what the network registers. */
    public long bayStorageItems(final int serverSlot) {
        if (bayItemsCached[serverSlot] && bayItemsMod[serverSlot] == storageModCounts[serverSlot]) {
            return bayItemsCache[serverSlot];
        }
        long items = 0L;
        for (final ItemStack drive : claimedDriveStacks(serverSlot)) {
            if (drive.getItem() instanceof DiskItem disk) {
                items += disk.spec().capacityItems();
            }
        }
        bayItemsCache[serverSlot] = items;
        bayItemsMod[serverSlot] = storageModCounts[serverSlot];
        bayItemsCached[serverSlot] = true;
        return items;
    }

    /** The bay-backed storage capacity of the unit at {@code serverSlot}, in megabytes (each drive at its own era). */
    public long bayStorageMb(final int serverSlot) {
        long mb = 0L;
        for (final ItemStack drive : claimedDriveStacks(serverSlot)) {
            if (drive.getItem() instanceof DiskItem disk) {
                mb += disk.spec().capacityMb();
            }
        }
        return mb;
    }

    public ServerStore getServerStorage(final int slot) {
        return new ServerStore(this, slot);
    }

    public static void serverTick(final Level level, final BlockPos pos,
                                  final BlockState state, final ServerRackBlockEntity be) {
        if (level instanceof ServerLevel serverLevel) {
            be.tick(serverLevel);
        }
    }

    private void tick(final ServerLevel level) {
        final NetworkSystem system = NetworkSystem.get(level);
        final NetworkUuid network = adjacentNetwork(level, system);
        data.set(DATA_LINKED, network != null || fabricLinked ? 1 : 0);
        data.set(DATA_BAY_POWER, ~bayPowerOff & 0xFF);
        arrays.tick();
        for (int slot = 0; slot < CAPACITY_U; slot++) {
            data.set(DATA_REBUILD_0 + slot, raidRebuildPermille(slot));
        }

        final Set<UUID> present = new HashSet<>();
        for (int i = 0; i < CAPACITY_U; i++) {
            final ItemStack stack = servers.getStackInSlot(i);
            if (!(stack.getItem() instanceof ServerItem)) {
                continue;
            }
            // A Server is a node only when it is a valid, powered computer whose bay switch is on.
            final ComputerBuild build = buildAt(i);
            if (build == null || !build.isPowered() || !bayPowerOn(i)) {
                continue;
            }
            final UUID node = ensureNodeUuid(stack);
            present.add(node);
            /*
             * A machine in a rack is a machine: it comes up through its own self-test, boot manager and system,
             * it keeps setting a program up while it is a powered node, and it keeps a copy of its own going.
             */
            BootRunner.tick(unitHost(i), unitState(i).phases, level, worldPosition);
            SetupRunner.tick(unitHost(i), level, worldPosition);
            OsInstallRunner.tick(unitHost(i), level, worldPosition);
            // And whatever was left running in front of its terminal goes on running, watched or not.
            terminals.tick(level, i);

            final NetworkUuid previous = registered.get(node);
            if (network == null) {
                // No cable / not networked: the Server is inert, drop its registration.
                if (previous != null) {
                    system.unregisterServer(previous, new NodeUuid(node));
                    registered.remove(node);
                }
                continue;
            }
            if (previous != null && !previous.equals(network)) {
                system.unregisterServer(previous, new NodeUuid(node));
            }
            // Storage capacity comes from the bay drives this unit claims, not from the item.
            system.registerServer(new ServerNode(new NodeUuid(node), network, bayStorageItems(i)),
                    worldPosition.asLong(), i);
            data.set(DATA_THROTTLE, thermalThrottlePercent());
            registered.put(node, network);
        }

        // Unregister Servers that have left the Rack since the last tick.
        registered.entrySet().removeIf(entry -> {
            if (!present.contains(entry.getKey())) {
                system.unregisterServer(entry.getValue(), new NodeUuid(entry.getKey()));
                return true;
            }
            return false;
        });
    }

    private UUID ensureNodeUuid(final ItemStack stack) {
        UUID node = ServerItem.nodeUuid(stack);
        if (node == null) {
            node = UUID.randomUUID();
            stack.set(ComputingModule.SERVER_NODE_UUID.get(), node);
            setChanged();
        }
        return node;
    }

    /*
     * A compute cabinet's link state: its fabric reaches an HBW Interface that is on a network. The
     * interface reports it as it surveys the fabric; the cabinet only remembers when and what it said.
     */
    private boolean fabricLinked;
    private long fabricSeenAt = Long.MIN_VALUE;
    private boolean fabricNetworked;

    private NetworkUuid adjacentNetwork(final ServerLevel level, final NetworkSystem system) {
        final Direction facing = getBlockState().getValue(
                HorizontalDirectionalBlock.FACING);
        final Set<Long> inside = new HashSet<>();
        for (final BlockPos p : ServerRackStructure
                .allPositions(worldPosition, facing)) {
            if (p.equals(worldPosition)) {
                inside.add(p.asLong());
            } else if (level.getBlockEntity(p) instanceof ServerRackPartBlockEntity part
                    && worldPosition.equals(part.controllerPos())) {
                inside.add(p.asLong());
            }
        }
        if (rackType() == RackChassis.RackType.SUPERCOMPUTER) {
            /*
             * A compute cabinet sits on the high-compute fabric and answers to nothing else: a data cable
             * on it is ignored, whatever the face. Its nodes are reached through the HBW Interface at the
             * end of the fabric and are never servers on the data network. The link light is what the
             * interface's survey said as it walked the fabric (every tick, any face of the cabinet).
             */
            fabricLinked = fabricNetworked && level.getGameTime() - fabricSeenAt <= 2L;
            return null;
        }
        fabricLinked = false;
        /*
         * Cables attach ONLY through the cabinet's rear: the open front is the bay. The high-compute fabric
         * is the supercomputer cabinet's alone, so a server cabinet does not answer to it.
         */
        final Direction back = facing.getOpposite();
        final Set<Long> cables = new HashSet<>();
        for (final long posLong : inside) {
            final BlockPos p = BlockPos.of(posLong);
            final BlockPos neighbor = p.relative(back);
            if (inside.contains(neighbor.asLong())) {
                continue; // a face internal to the cabinet (the front layer's rear)
            }
            if (level.getBlockState(neighbor).getBlock() instanceof DataCableBlock cable
                    && cable.tier() != DataTier.HPC) {
                cables.add(neighbor.asLong());
            }
        }
        /*
         * Bridge the cable runs this rack touches into one segment, so a Mainframe on one side and a
         * standby on the other are on a single network connected through the rack.
         */
        system.connectivity().bridge(cables);
        for (final long cable : cables) {
            final var net = system.connectivity().networkOf(cable);
            if (net.isPresent()) {
                return net.get();
            }
        }
        return null;
    }

    /**
     * An HBW Interface surveying its fabric found this cabinet on it. Called every tick the cabinet is on
     * a fabric; a cabinet not told for a couple of ticks has come off it.
     */
    public void noteFabricUplink(final long gameTime, final boolean networked) {
        fabricSeenAt = gameTime;
        fabricNetworked = networked;
    }

    public void onBroken(final ServerLevel level) {
        final NetworkSystem system = NetworkSystem.get(level);
        registered.forEach((node, network) -> system.unregisterServer(network, new NodeUuid(node)));
        registered.clear();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        /*
         * Unregister the housed Servers on chunk unload too, not just on destruction (the block's
         * onRemove), so they never linger in the still-loaded per-level network. onBroken is idempotent.
         */
        if (level instanceof ServerLevel serverLevel) {
            onBroken(serverLevel);
        }
    }

    /*
     * The rack as the mounted computer's face
     *
     * A monitor (or a media reader) cables to the RACK; with exactly one computer mounted, the rack
     * answers the whole OS-host contract by delegating to that machine, so the server runs the same
     * POST / firmware / boot / console pipeline as a desk computer. With two or more computers the
     * monitor cannot address the bays (that takes a KVM switch), so the delegating host goes dark.
     */

    private final Set<Long> linkedPeripherals = new HashSet<>();
    private NodeUuid fallbackNode;

    /*
     * Thermal budget: what a dense cabinet costs you
     *
     * Every mounted machine dumps its power draw into the cabinet as heat. A rack sheds a fixed
     * amount on its own; past that the machines throttle unless cooling is mounted, which is the
     * trade-off the rack-unit budget is meant to create, since cooling spends the same U a server
     * would. The watt figures are balancing estimates.
     */

    /** The total power draw of every machine mounted in this rack, in watts. */
    public int thermalLoadWatts() {
        int watts = 0;
        for (int i = 0; i < CAPACITY_U; i++) {
            final ComputerBuild build = buildAt(i);
            if (build != null && bayPowerOn(i)) {
                watts += build.powerDraw();
            }
        }
        return watts;
    }

    /** The heat this cabinet can shed: its own dissipation plus every Cooling Unit mounted. */
    public int thermalBudgetWatts() {
        int cooling = 0;
        for (int i = 0; i < CAPACITY_U; i++) {
            if (RackUnitItem.is(servers.getStackInSlot(i), RackUnitItem.Kind.COOLING_UNIT)) {
                cooling++;
            }
        }
        return RackThermals.budgetWatts(cooling);
    }

    /** How much of its capacity a machine in this rack actually delivers, in percent. */
    public int thermalThrottlePercent() {
        return RackThermals.throttlePercent(thermalLoadWatts(), thermalBudgetWatts());
    }

    /** Whether the cabinet is over its thermal budget and holding the machines in it back. */
    public boolean thermalThrottled() {
        return thermalThrottlePercent() < 100;
    }

    /** The kind of cabinet this rack is: which computers it takes. Rack units fit every type. */
    public RackChassis.RackType rackType() {
        // The block decides: one block entity class serves every cabinet type.
        return getBlockState().getBlock()
                instanceof ServerRackBlock rack
                ? rack.rackType() : RackChassis.RackType.SERVER;
    }

    /** Whether this cabinet takes the given stack's chassis (rack equipment always fits). */
    public boolean acceptsChassis(final ItemStack stack) {
        final RackChassis chassis = ServerItem.chassisOf(stack);
        /*
         * The cabinet type must match, and a cabinet seats its own era or earlier: a Vintage server
         * still fits a Standard rack, a Standard server never fits a Vintage one.
         */
        return chassis == null
                || (chassis.rackType() == rackType() && chassis.era().isAtMost(rackEra()));
    }

    /** The rows holding computers, in rack order, the channels a KVM switch can address. */
    public List<Integer> computerSlots() {
        final List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < CAPACITY_U; i++) {
            if (servers.getStackInSlot(i).getItem() instanceof ServerItem) {
                slots.add(i);
            }
        }
        return slots;
    }

    /** Whether a KVM Switch is mounted, which is what lets a monitor address more than one machine. */
    public boolean hasKvmSwitch() {
        for (int i = 0; i < CAPACITY_U; i++) {
            if (RackUnitItem.is(
                    servers.getStackInSlot(i),
                    RackUnitItem.Kind.KVM_SWITCH)) {
                return true;
            }
        }
        return false;
    }

    /*
     * The bay a linked monitor is currently showing. Only meaningful with a KVM switch mounted;
     * a rack with a single computer always shows that one.
     */
    private int activeChannel;

    /** The channel (rack row) a linked monitor shows, clamped to the machines actually mounted. */
    public int activeChannel() {
        final List<Integer> computers = computerSlots();
        if (computers.isEmpty()) {
            return -1;
        }
        return computers.contains(activeChannel) ? activeChannel : computers.get(0);
    }

    /** Switches the monitor to the machine in {@code slot}; ignored when that row holds none. */
    public void setActiveChannel(final int slot) {
        if (computerSlots().contains(slot)) {
            activeChannel = slot;
            setChanged();
            syncDisplayEra(); // the new channel may be a machine of another era
        }
    }

    /**
     * The row of the machine a linked monitor addresses, or {@code -1} when it cannot address one.
     * One computer needs no extra hardware; two or more need a KVM Switch to pick between them,
     * and without one the monitor has no way to say which machine it means.
     */
    public int soleComputerSlot() {
        if (unitOverride >= 0) {
            return unitOverride; // a caller is addressing one unit directly (see asUnit)
        }
        final List<Integer> computers = computerSlots();
        if (computers.isEmpty()) {
            return -1;
        }
        if (computers.size() == 1) {
            return computers.get(0);
        }
        return hasKvmSwitch() ? activeChannel() : -1;
    }

    /*
     * The unit a caller is addressing directly, or -1 when the monitor's channel decides. Every
     * "this rack as a computer" method resolves its unit through soleComputerSlot(), so pointing that
     * one resolver at a row makes all of them act on that row, one code path, no second copy of the
     * machine logic per unit. Scoped, never persisted: the KVM channel is untouched.
     */
    private int unitOverride = -1;

    /** Runs {@code body} with every machine-level method of this rack addressing unit {@code row}. */
    public <T> T asUnit(final int row, final Supplier<T> body) {
        final int previous = unitOverride;
        unitOverride = row;
        try {
            return body.get();
        } finally {
            unitOverride = previous;
        }
    }

    /** The machine seated in {@code row} as a host of its own, independent of the monitor's channel. */
    public IOsHost unitHost(final int row) {
        return new RackUnitHost(this, row);
    }

    private ItemStack soleServerStack() {
        final int slot = soleComputerSlot();
        return slot < 0 ? ItemStack.EMPTY : servers.getStackInSlot(slot);
    }

    @Nullable
    private ComputerBuild soleBuild() {
        final int slot = soleComputerSlot();
        return slot < 0 ? null : buildAt(slot);
    }

    /** The front-slot index of the {@code i}-th DRIVE-role slot of the unit at {@code topRow}, or -1. */
    private int driveSlotIndex(final int topRow, final int i) {
        final RackChassis chassis = ServerItem.chassisOf(servers.getStackInSlot(topRow));
        if (chassis == null || i < 0) {
            return -1;
        }
        final List<RackLayout.Unit> mounted = mountedUnits();
        int seen = 0;
        for (int row = topRow; row < topRow + chassis.heightU() && row < CAPACITY_U; row++) {
            for (int column = 0; column < RackLayout.SLOTS_PER_U; column++) {
                if (layout.roleAt(row, column, mounted) != RackLayout.SlotRole.DRIVE) {
                    continue;
                }
                if (seen == i) {
                    return row * RackLayout.SLOTS_PER_U + column;
                }
                seen++;
            }
        }
        return -1;
    }

    // IOsHost: the machine identity and its bay-backed disks.

    @Override
    public boolean isRunning() {
        final ComputerBuild build = soleBuild();
        return build != null && build.isPowered() && bayPowerOn(soleComputerSlot());
    }

    @Override
    public void setPowered(final boolean on) {
        /*
         * A rack machine's power switch is its bay's, so shutting down from inside the system
         * flips the same switch the rack GUI shows.
         */
        final int slot = soleComputerSlot();
        if (slot >= 0 && bayPowerOn(slot) != on) {
            toggleBayPower(slot);
        }
    }

    @Override
    public boolean needsPost() {
        final int slot = soleComputerSlot();
        return slot >= 0 && unitState(slot).phases.needsPost();
    }

    @Override
    public boolean haltedAtPost() {
        final int slot = soleComputerSlot();
        return slot >= 0 && unitState(slot).phases.halted();
    }

    @Override
    public void resumeFromHalt() {
        final int slot = soleComputerSlot();
        if (slot >= 0) {
            unitState(slot).phases.resume();
        }
    }

    @Override
    public int postRemaining() {
        final int slot = soleComputerSlot();
        return slot < 0 || level == null ? 0 : unitState(slot).phases.postRemaining(level.getGameTime());
    }

    @Override
    public boolean atBootMenu() {
        final int slot = soleComputerSlot();
        return slot >= 0 && unitState(slot).phases.atMenu();
    }

    @Override
    public int menuRemaining() {
        final int slot = soleComputerSlot();
        return slot < 0 || level == null ? 0 : unitState(slot).phases.menuRemaining(level.getGameTime());
    }

    @Override
    public void holdBootMenu() {
        final int slot = soleComputerSlot();
        if (slot >= 0) {
            unitState(slot).phases.holdMenu();
        }
    }

    @Override
    public void leaveBootMenu() {
        final int slot = soleComputerSlot();
        if (slot >= 0) {
            unitState(slot).phases.endMenu();
            unitState(slot).phases.beginBoot();
        }
    }

    @Override
    public void restartFromBootMenu() {
        final int slot = soleComputerSlot();
        if (slot >= 0) {
            unitState(slot).phases.endMenu();
            setNeedsPost(true);
        }
    }

    @Override
    public boolean booting() {
        final int slot = soleComputerSlot();
        return slot >= 0 && unitState(slot).phases.booting();
    }

    @Override
    public int bootRemaining() {
        final int slot = soleComputerSlot();
        return slot < 0 || level == null ? 0 : unitState(slot).phases.bootRemaining(level.getGameTime());
    }

    @Override
    public int bootTotal() {
        final int slot = soleComputerSlot();
        return slot < 0 ? 0 : unitState(slot).phases.bootTotal();
    }

    @Override
    public BootSequence bootSequence() {
        return BootLines.forMachine(this, level instanceof ServerLevel server ? server : null);
    }

    @Override
    @Nullable
    public ComputerBuild currentBuild() {
        return soleBuild();
    }

    @Override
    public boolean hasBootableMedium() {
        if (level == null) {
            return false;
        }
        for (final long endpoint : linkedEndpoints()) {
            if (level.getBlockEntity(BlockPos.of(endpoint))
                    instanceof MediaReaderBlockEntity reader
                    && reader.insertedKind() == MediaKind.OS_INSTALL
                    && reader.insertedPayload() != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int pendingInstallSlot() {
        final int slot = soleComputerSlot();
        return slot < 0 ? IOsHost.NO_PENDING_INSTALL
                : unitState(slot).pendingInstallSlot;
    }

    @Override
    @Nullable
    public OsInstallJob installing() {
        final int slot = soleComputerSlot();
        return slot < 0 ? null : unitState(slot).installing;
    }

    @Override
    public void setInstalling(@Nullable
                              final OsInstallJob job) {
        final int slot = soleComputerSlot();
        if (slot >= 0) {
            unitState(slot).installing = job;
            flushConsole(slot);
            setChanged();
        }
    }

    @Override
    @Nullable
    public InstallerFlow installer() {
        final int slot = soleComputerSlot();
        if (slot < 0) {
            return null;
        }
        final UnitState state = unitState(slot);
        /*
         * Built back from the answers the first time it is asked for after a reload, since the disks and the
         * desktops it offers are read off the world and a rack is loaded before it can reach one.
         */
        if (state.installer == null && state.installerMemo != null && getLevel() instanceof ServerLevel level) {
            final CompoundTag memo = state.installerMemo;
            state.installerMemo = null;
            final OsDef system = OsRegistry.getOs(
                    ResourceLocation.tryParse(memo.getString("Os")));
            if (system != null) {
                state.installer = Installers.restored(unitHost(slot), level,
                        system, memo.getInt("Copy"), memo.getInt("Stage"), memo.getInt("Slot"),
                        memo.getString("Name"), memo.getString("Desktop"), memo.getInt("Erase"));
            }
        }
        return state.installer;
    }

    @Override
    public void setInstaller(@Nullable
                             final InstallerFlow flow) {
        final int slot = soleComputerSlot();
        if (slot >= 0) {
            unitState(slot).installer = flow;
            unitState(slot).installerMemo = null;
            flushConsole(slot);
            setChanged();
        }
    }

    @Override
    public boolean keepsInstalls() {
        return true;
    }

    @Override
    public boolean onScreen() {
        // The switch shows one machine at a time, so only the bay it is on is the one being looked at.
        final int slot = soleComputerSlot();
        return slot >= 0 && (!hasKvmSwitch() || activeChannel() == slot);
    }

    @Override
    public void markChanged() {
        setChanged();
    }

    /** The installer's answers as they are written onto the Server item, or null when the bay is in none. */
    @Nullable
    private static CompoundTag installerPages(final UnitState state) {
        if (state.installer == null) {
            return state.installerMemo;
        }
        final CompoundTag pages = new CompoundTag();
        pages.putString("Os", state.installer.systemId());
        pages.putInt("Copy", state.installer.copyTicks());
        pages.putInt("Stage", state.installer.stageIndex());
        pages.putInt("Slot", state.installer.targetSlot());
        pages.putString("Name", state.installer.computerName());
        pages.putString("Desktop", state.installer.desktopId());
        pages.putInt("Erase", state.installer.eraseSlot());
        return pages;
    }

    @Override
    public void setPendingInstallSlot(final int value) {
        final int slot = soleComputerSlot();
        if (slot >= 0) {
            unitState(slot).pendingInstallSlot = value;
            flushConsole(slot); // rides on the Server item, like the rest of the machine's session
            setChanged();
        }
    }

    @Override
    @Nullable
    public ResourceLocation bootedDesktopId() {
        final int slot = soleComputerSlot();
        return slot < 0 ? null : unitState(slot).bootedDesktopId;
    }

    @Override
    public void setBootedDesktopId(@Nullable final ResourceLocation id) {
        final int slot = soleComputerSlot();
        if (slot >= 0) {
            unitState(slot).bootedDesktopId = id;
            flushConsole(slot);
            setChanged();
        }
    }

    @Override
    public List<OpenWindow> openWindows() {
        final int slot = soleComputerSlot();
        return slot < 0 ? List.of() : List.copyOf(unitState(slot).openWindows);
    }

    @Override
    public void setOpenWindows(final List<OpenWindow> windows) {
        final int slot = soleComputerSlot();
        if (slot < 0) {
            return;
        }
        final UnitState state = unitState(slot);
        state.openWindows.clear();
        for (final OpenWindow window : windows) {
            if (state.openWindows.size() >= OpenWindow.MAX) {
                break;
            }
            state.openWindows.add(window);
        }
        flushConsole(slot);
        setChanged();
    }

    @Override
    public int desktopWorkspace() {
        final int slot = soleComputerSlot();
        return slot < 0 ? 0 : unitState(slot).desktopWorkspace;
    }

    @Override
    public void setDesktopWorkspace(final int workspace) {
        final int slot = soleComputerSlot();
        if (slot < 0) {
            return;
        }
        unitState(slot).desktopWorkspace = OpenWindow.clampWorkspace(workspace);
        flushConsole(slot);
        setChanged();
    }

    @Override
    public void setNeedsPost(final boolean value) {
        final int slot = soleComputerSlot();
        if (slot >= 0) {
            unitState(slot).phases.setNeedsPost(value);
            if (value) {
                unitState(slot).openWindows.clear(); // a restart closes everything
                unitState(slot).desktopWorkspace = 0;
                unitState(slot).pendingInstallSlot = // and is what a finished installer was waiting for
                        IOsHost.NO_PENDING_INSTALL;
            }
            /*
             * Write it through immediately: the flag lives on the Server item, and a world that unloads
             * before the next save would otherwise forget that this machine had finished booting.
             */
            flushConsole(slot);
            setChanged();
        }
    }

    @Override
    public long ramBuffer() {
        final ComputerBuild build = soleBuild();
        return build == null ? 0L : build.ramBuffer();
    }

    @Override
    public int maxCpuMhz() {
        final ComputerBuild build = soleBuild();
        if (build == null) {
            return 0;
        }
        int max = 0;
        for (final CpuSpec cpu : build.cpus()) {
            max = Math.max(max, cpu.freqMhz());
        }
        return max;
    }

    @Override
    public int totalVramMb() {
        final ComputerBuild build = soleBuild();
        return build == null ? 0 : build.effectiveVramMb();
    }

    @Override
    public int diskSlots() {
        final RackChassis chassis = ServerItem.chassisOf(soleServerStack());
        return chassis == null ? 0 : chassis.driveSlots();
    }

    @Override
    public ItemStack diskInSlot(final int slot) {
        final int top = soleComputerSlot();
        final int index = top < 0 ? -1 : driveSlotIndex(top, slot);
        return index < 0 ? ItemStack.EMPTY : frontSlots.getStackInSlot(index);
    }

    @Override
    public List<ItemStack> diskStacks() {
        final List<ItemStack> out = new ArrayList<>(diskSlots());
        for (int i = 0; i < diskSlots(); i++) {
            out.add(diskInSlot(i));
        }
        return out;
    }

    private void setDiskInSlot(final ItemStack stack, final int slot) {
        final int top = soleComputerSlot();
        final int index = top < 0 ? -1 : driveSlotIndex(top, slot);
        if (index >= 0) {
            frontSlots.setStackInSlot(index, stack);
        }
    }

    @Override
    public ItemStack systemDisk() {
        return OsDisks.systemDisk(
                diskSlots(), this::diskInSlot, bootDiskSlot());
    }

    @Override
    public int bootDiskSlot() {
        final int slot = soleComputerSlot();
        return slot < 0 ? -1 : unitState(slot).bootDiskSlot;
    }

    @Override
    public void setBootDiskSlot(final int slot) {
        final int top = soleComputerSlot();
        if (top >= 0) {
            unitState(top).bootDiskSlot = slot;
            setChanged();
        }
    }

    @Override
    public int defaultInstallSlot() {
        return OsDisks.defaultInstallSlot(
                diskSlots(), this::diskInSlot);
    }

    @Override
    public boolean formatDisk(final int slot) {
        final OsDisks.FormatResult result =
                OsDisks.formatDisk(
                        diskSlots(), this::diskInSlot, this::setDiskInSlot, slot);
        if (!result.formatted()) {
            return false;
        }
        final int top = soleComputerSlot();
        if (top >= 0 && unitState(top).bootDiskSlot == slot) {
            unitState(top).bootDiskSlot = -1;
        }
        if (result == OsDisks.FormatResult.ERASED_SYSTEM
                && console() != null) {
            console().wipeSoftware();
        }
        setChanged();
        return true;
    }

    @Override
    public boolean installOs(final ResourceLocation osId) {
        return installOs(osId, -1);
    }

    @Override
    public boolean installOs(final ResourceLocation osId, final int preferredSlot) {
        final boolean installed = OsDisks.installOs(
                diskSlots(), this::diskInSlot, this::setDiskInSlot, osId, preferredSlot);
        if (installed && level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(),
                    Block.UPDATE_CLIENTS);
        }
        return installed;
    }

    @Override
    public boolean hasOs() {
        return !systemDisk().isEmpty();
    }

    @Override
    @Nullable
    public ResourceLocation installedOsId() {
        final ItemStack disk = systemDisk();
        return OsDisks.systemOn(disk);
    }

    @Override
    @Nullable
    public OsDef installedOs() {
        final ResourceLocation osId = installedOsId();
        return osId == null ? null : OsRegistry.getOs(osId);
    }

    @Override
    @Nullable
    public ResourceLocation installedDesktopId() {
        return OsDisks.installedDesktopId(
                installedOs(), console());
    }

    @Override
    public boolean validateOsSession() {
        final LiveInstallState live = liveInstall();
        /* Asked, never acted on: see the same method on a personal computer for why that matters. */
        return (live != null
                && LiveMedium.holding(level, linkedEndpoints(), live.distro()) != LiveMedium.Answer.GONE)
                || installedOsId() != null;
    }

    @Override
    public boolean settleLiveInstall() {
        final LiveInstallState live = liveInstall();
        if (live == null || LiveMedium.holding(level, linkedEndpoints(), live.distro())
                != LiveMedium.Answer.GONE) {
            return false;
        }
        console().clearLiveInstall();
        setChanged();
        return true;
    }

    /** The live installation the mounted unit is running, or nothing when it is not running one. */
    @Nullable
    private LiveInstallState liveInstall() {
        final ComputerConsoleState console = console();
        return console == null ? null : console.liveInstall();
    }

    @Override
    @Nullable
    public ComputerConsoleState console() {
        final int slot = soleComputerSlot();
        return slot < 0 ? null : unitState(slot).console;
    }

    @Override
    public NodeUuid nodeUuid() {
        final ItemStack stack = soleServerStack();
        if (stack.getItem() instanceof ServerItem) {
            return new NodeUuid(ensureNodeUuid(stack));
        }
        if (fallbackNode == null) {
            fallbackNode = NodeUuid.random();
        }
        return fallbackNode;
    }

    @Override
    @Nullable
    public NetworkUuid networkUuid() {
        final ItemStack stack = soleServerStack();
        if (!(stack.getItem() instanceof ServerItem)) {
            return null;
        }
        final UUID node = ServerItem.nodeUuid(stack);
        return node == null ? null : registered.get(node);
    }

    @Override
    public String customName() {
        return ServerItem.customName(soleServerStack());
    }

    @Override
    public void setCustomName(final String name) {
        final ItemStack stack = soleServerStack();
        if (stack.getItem() instanceof ServerItem) {
            ServerItem.setCustomName(stack, name);
            setChanged();
        }
    }

    @Override
    public int installedCpus() {
        final ComputerBuild build = soleBuild();
        return build == null ? 0 : build.cpus().size();
    }

    @Override
    public long systemDiskFreeWeight() {
        return OsDisks.systemDiskFreeWeight(systemDisk());
    }

    @Override
    public long systemDiskFreeMb() {
        final ItemStack disk = systemDisk();
        return OsDisks.systemDiskFreeWeight(disk)
                * diskEra(disk).mbPerItem()
                / StorageKey.MB_EQ_PER_ITEM;
    }

    @Override
    public long reservedByOs() {
        // One lookup of the system disk serves both the system and the era the system sits on.
        final ItemStack disk = systemDisk();
        final ResourceLocation osId =
                OsDisks.systemOn(disk);
        final OsDef os =
                osId != null ? OsRegistry.getOs(osId) : null;
        return os != null ? os.footprintItemsOn(diskEra(disk)) : 0L;
    }

    /** The era a disk was made for, which sets what an item and a system cost on it; standard for no disk. */
    private static HardwareEra diskEra(final ItemStack disk) {
        return disk.getItem() instanceof DiskItem item
                ? item.spec().era() : HardwareEra.STANDARD;
    }

    @Override
    @Nullable
    public HardwareEra installedEra() {
        final int mobo = ServerHardwareHandler.MOBO;
        final ItemContainerContents parts =
                ServerItem.hardware(soleServerStack());
        /*
         * A server's hardware is a data component, so its container is only as long as what was written
         * to it: an empty one has no slots at all and rejects even index 0. Never index it blindly.
         */
        if (mobo >= parts.getSlots()) {
            return null;
        }
        return parts.getStackInSlot(mobo).getItem()
                instanceof MotherboardItem m
                ? m.spec().era() : null;
    }

    @Override
    @Nullable
    public HardwareEra displayEra() {
        /*
         * The client copy of a rack holds no mounted servers, so the era has to travel to it: screens
         * that dress themselves by era (the boot sequence, the terminal bezel) render client-side.
         */
        if (level != null && level.isClientSide()) {
            return clientEra;
        }
        return installedEra();
    }

    // IComputerTerminalHost: the physical terminal's monitoring surface over the same machine.

    @Override
    public boolean computerRunning() {
        return isRunning();
    }

    @Override
    public boolean computerBuildValid() {
        return isRunning();
    }

    @Override
    public int networkLinkState() {
        return networkUuid() != null ? 1 : 0; // a rack server reads its network from the rack cable
    }

    @Override
    public long orchestrationCapacity() {
        final ComputerBuild build = soleBuild();
        return build == null ? 0L : throttled(build.totalCapacity());
    }

    /** Applies the cabinet's thermal throttle to a machine's capacity. */
    public long throttled(final long capacity) {
        final int percent = thermalThrottlePercent();
        return percent >= 100 ? capacity : capacity * percent / 100;
    }

    @Override
    public int computerQueues() {
        final ComputerBuild build = soleBuild();
        return build == null || !build.isPowered() ? 0 : build.parallelQueues();
    }

    @Override
    public long computerRamBuffer() {
        return ramBuffer();
    }

    @Override
    public int networkServerCount() {
        final NetworkUuid network = networkUuid();
        if (network == null || !(level instanceof ServerLevel serverLevel)) {
            return 0;
        }
        return NetworkSystem.get(serverLevel).serversOf(network).size();
    }

    @Override
    public int cpuSlots() {
        final ComputerBuild build = soleBuild();
        final RackChassis chassis = ServerItem.chassisOf(soleServerStack());
        return build == null || chassis == null ? 0
                : Math.min(build.motherboard().cpuSlots(), chassis.maxCpus());
    }

    @Override
    public int installedRam() {
        final ComputerBuild build = soleBuild();
        return build == null ? 0 : build.rams().size();
    }

    @Override
    public int ramSlots() {
        final ComputerBuild build = soleBuild();
        return build == null ? 0 : build.motherboard().ramSlots();
    }

    @Override
    public int installedGpus() {
        final ComputerBuild build = soleBuild();
        return build == null ? 0 : build.gpus().size();
    }

    @Override
    public int gpuSlots() {
        final ComputerBuild build = soleBuild();
        final RackChassis chassis = ServerItem.chassisOf(soleServerStack());
        return build == null || chassis == null ? 0
                : Math.min(build.motherboard().pcieSlots(), chassis.maxPcie());
    }

    @Override
    public int installedDisks() {
        int count = 0;
        for (final ItemStack disk : diskStacks()) {
            if (disk.getItem() instanceof DiskItem) {
                count++;
            }
        }
        return count;
    }

    @Override
    public LocalStore localStore() {
        final int slot = Math.max(0, soleComputerSlot());
        return new LocalStore(
                claimedDriveStacks(slot), () -> {
                    markStorageChanged(slot);
                    setChanged();
                });
    }

    private long netStorageItems() {
        long capacity = 0L;
        for (final ItemStack disk : diskStacks()) {
            if (disk.getItem() instanceof DiskItem item) {
                capacity += item.spec().capacityItems();
            }
        }
        return Math.max(0L, capacity - reservedByOs());
    }

    @Override
    public long localStorageUsed() {
        return localStore().used();
    }

    @Override
    public long localStorageCapacity() {
        return netStorageItems();
    }

    @Override
    public IDataSink localStorage() {
        return new StoreSink(localStore());
    }

    @Override
    public int usableStorageSlots() {
        final long capacity = netStorageItems();
        return capacity <= 0 ? 0 : (int) Math.min(27, (capacity + 63) / 64);
    }

    @Override
    public boolean isMainframeHost() {
        return false;
    }

    // IPeripheralOwnerSupport: monitors and media readers cable to the rack itself.

    @Override
    public Set<Long> peripheralEndpoints() {
        return linkedPeripherals;
    }

    @Override
    public void markPeripheralChange() {
        setChanged();
    }

    @Override
    public int maxEndpoints() {
        /*
         * Cabling a monitor to the cabinet only needs SOME machine with ports, and which one the screen
         * ends up showing is the KVM's business, decided when the player uses the monitor. Reading
         * the active machine here would refuse the cable outright on a rack that merely lacks a
         * switch, and blame the hardware for it.
         */
        int ports = 0;
        for (final int slot : computerSlots()) {
            final ComputerBuild build = buildAt(slot);
            if (build != null) {
                ports = Math.max(ports, build.motherboard().peripheralPorts());
            }
        }
        return ports;
    }

    @Override
    public Set<Long> occupiedPositions(final long ownerPos) {
        // A peripheral cable may touch any block of the 2x2x? cabinet, not just the controller.
        final BlockState state = getBlockState();
        if (!state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return Set.of(ownerPos);
        }
        final Direction facing = state.getValue(
                HorizontalDirectionalBlock.FACING);
        final Set<Long> positions = new HashSet<>();
        for (final BlockPos p : ServerRackStructure
                .allPositions(BlockPos.of(ownerPos), facing)) {
            positions.add(p.asLong());
        }
        return positions;
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        servers.deserializeNBT(registries, tag.getCompound("Servers"));
        resizeAfterLoad(servers, CAPACITY_U);
        frontSlots.deserializeNBT(registries, tag.getCompound("FrontSlots"));
        resizeAfterLoad(frontSlots, CAPACITY_U * RackLayout.SLOTS_PER_U);
        linkedPeripherals.clear();
        for (final long endpoint : tag.getLongArray("Peripherals")) {
            linkedPeripherals.add(endpoint);
        }
        bayPowerOff = tag.getInt("BayPowerOff");
        servicePanelOff = tag.getBoolean("ServicePanelOff");
        arrays.load(tag);
    }

    /*
     * ItemStackHandler.deserializeNBT resizes the handler to the persisted Size, so a rack saved
     * under the old 4-bay layout would come back too small and out-of-range the rack-unit loops.
     * Re-expand it, keeping whatever fits (this is a size fixup, not a data migration).
     */
    private static void resizeAfterLoad(final ItemStackHandler handler, final int size) {
        if (handler.getSlots() == size) {
            return;
        }
        final NonNullList<ItemStack> kept = NonNullList.withSize(size, ItemStack.EMPTY);
        for (int i = 0; i < Math.min(handler.getSlots(), size); i++) {
            kept.set(i, handler.getStackInSlot(i));
        }
        handler.setSize(size);
        for (int i = 0; i < size; i++) {
            handler.setStackInSlot(i, kept.get(i));
        }
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        /*
         * Console state persists WITH each Server item, so flush the live sessions onto their
         * stacks before the stacks themselves are serialized.
         */
        flushConsoles();
        tag.put("Servers", servers.serializeNBT(registries));
        tag.put("FrontSlots", frontSlots.serializeNBT(registries));
        tag.putLongArray("Peripherals", new ArrayList<>(linkedPeripherals));
        tag.putInt("BayPowerOff", bayPowerOff);
        tag.putBoolean("ServicePanelOff", servicePanelOff);
        arrays.save(tag);
    }

    /*
     * The era of the machine the monitor is currently showing, as last received from the server. Only
     * ever written on the client; the server always answers from the mounted hardware itself.
     */
    @Nullable
    private HardwareEra clientEra;

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        final CompoundTag tag = super.getUpdateTag(registries);
        /*
         * Only the active channel's era travels. Sending the mounted stacks would put every server's
         * full build on the wire on every block update, for one enum the screens need.
         */
        final HardwareEra era = installedEra();
        tag.putInt("DisplayEra", era == null ? -1 : era.id());
        /*
         * The cabinet model: one byte per row says what is seated there, the mask says which bays are
         * off, and the panel flag whether the supercomputer's livery is on. Enough to draw it all.
         */
        final byte[] units = new byte[CAPACITY_U];
        for (int slot = 0; slot < CAPACITY_U; slot++) {
            units[slot] = (byte) unitCodeAt(slot);
        }
        tag.putByteArray("Units", units);
        tag.putInt("BayPowerOff", bayPowerOff);
        tag.putBoolean("ServicePanelOff", servicePanelOff);
        // Whether the rack is on a data network, for the notification area of a mounted server's desktop.
        tag.putBoolean("Networked", !registered.isEmpty());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener>
            getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(final Connection connection,
                             final ClientboundBlockEntityDataPacket packet,
                             final HolderLookup.Provider registries) {
        /*
         * Apply only the era. Running the full loadAdditional here would deserialize empty inventories
         * over the client copy and reset the transient rack state to its defaults.
         */
        final CompoundTag tag = packet.getTag();
        clientEra = HardwareEra.find(tag != null ? tag.getInt("DisplayEra") : -1);
        if (tag != null) {
            applyVisualTag(tag);
        }
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag, final HolderLookup.Provider registries) {
        // The chunk-load path: the same visual state the block-update path carries.
        super.handleUpdateTag(tag, registries);
        applyVisualTag(tag);
    }

    private void applyVisualTag(final CompoundTag tag) {
        final byte[] units = tag.getByteArray("Units");
        Arrays.fill(clientUnits, (byte) UNIT_NONE);
        System.arraycopy(units, 0, clientUnits, 0, Math.min(units.length, clientUnits.length));
        clientBayPowerOff = tag.getInt("BayPowerOff");
        clientServicePanelOff = tag.getBoolean("ServicePanelOff");
        clientNetworked = tag.getBoolean("Networked");
    }

    /** The client's copy of whether the rack is on a network; the server answers from its registered nodes. */
    private boolean clientNetworked;

    @Override
    public boolean networkAttached() {
        return level != null && level.isClientSide ? clientNetworked : networkUuid() != null;
    }

    /** Pushes the active channel's era to watching clients, so era-dressed screens match the machine. */
    private void syncDisplayEra() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(),
                    Block.UPDATE_CLIENTS);
        }
    }
}
