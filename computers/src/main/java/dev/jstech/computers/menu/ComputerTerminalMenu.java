/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.registry.ComputingMenus;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.MonitorBlockEntity;
import dev.jstech.computers.gui.layout.ComputerTerminalLayout;
import dev.jstech.computers.operation.index.IndexHealth;
import dev.jstech.computers.operation.payload.CraftCatalogPayload;
import dev.jstech.computers.operation.payload.CraftPlanPayload;
import dev.jstech.computers.operation.payload.LocalStorageSnapshotPayload;
import dev.jstech.computers.operation.payload.NetworkItemEntry;
import dev.jstech.computers.operation.payload.NetworkServersPayload;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.operation.payload.ProcessListPayload;
import dev.jstech.computers.operation.payload.ServerBreakdownPayload;
import dev.jstech.computers.operation.payload.crafting.CraftingPayloads;
import dev.jstech.computers.operation.payload.network.NetworkPayloads;
import dev.jstech.computers.operation.payload.operations.OperationsPayloads;
import dev.jstech.computers.operation.payload.program.ProgramPayloads;
import dev.jstech.computers.operation.payload.terminal.TerminalLocalPayloads;
import dev.jstech.computers.operation.payload.terminal.TerminalPayloads;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.tier.HardwareEra;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Menu for the Monitor terminal, the tabbed interface a Monitor opens onto the computer it is linked to.
 */
public class ComputerTerminalMenu extends AbstractComputerMenu implements IMonitorMenu {

    public static final int TAB_LOCAL = 0;
    public static final int TAB_STORAGE = 1;
    public static final int TAB_NETWORK = 2;
    public static final int TAB_OPS = 3;
    public static final int TAB_TASKS = 4;
    public static final int TAB_MAINTENANCE = 5;
    public static final int TAB_CRAFT = 6;
    /** The process/service manager: the network's background services (the IQL Engine and its state). */
    public static final int TAB_PROCESSES = 7;
    /**
     * The machine's own prompt, as a heading like any other.
     *
     * <p>It used to be the one rail entry that opened something: clicking it threw a separate Command Prompt
     * window over the screen. A machine whose whole interface is this screen has no business opening a window
     * onto itself, so the prompt lives inside it.
     */
    public static final int TAB_CONSOLE = 8;
    /**
     * Teaching the machine a recipe: what is on a medium, and what its Recipe ROM holds.
     *
     * <p>It appears on a Crafting Computer with a Crafting Card, which is the same condition the Crafting
     * Manager installs under. Without it a machine running this system could load no pattern at all, so the
     * one player this system exists for could not autocraft with recipes of their own.
     */
    public static final int TAB_PATTERNS = 9;

    /** The highest heading that shows content, which is what an incoming tab id is clamped to. */
    public static final int TAB_LAST = TAB_PATTERNS;

    /** How long a system's name may be on the wire; a name is a name, not a paragraph. */
    private static final int MAX_SYSTEM_NAME = 64;

    /*
     * Slot layout (relative to the screen's top-left). The screen draws the slot
     * backgrounds and the inventory at these exact positions.
     */
    public static final int STORAGE_COLS = 9;
    public static final int STORAGE_X = 68;
    public static final int STORAGE_Y = 40;
    /*
     * The inventory positions come from ComputerTerminalLayout, the single source the layout test
     * validates, so the real slots placed here are covered by that test.
     */
    public static final int INV_X = ComputerTerminalLayout.INV_X;
    public static final int INV_Y = ComputerTerminalLayout.INV_Y;
    public static final int HOTBAR_Y = ComputerTerminalLayout.HOTBAR_Y;

    private static final double MONITOR_REACH = 16.0;

    private static final int DATA_COUNT = 34;
    private static final int DATA_INDEX_HEALTH = 31;
    private static final int DATA_INDEX_HEALTH_TYPES = 32;
    private static final int DATA_USABLE_SLOTS = 18;
    private static final int DATA_CRAFT_COMPUTERS = 29;
    /*
     * The host's board-derived hardware-era id (or -1 when no board), synced so the client can skin the
     * terminal in the host computer's era. It re-resolves each tick, so swapping the board repaints live.
     */
    private static final int DATA_ERA = 30;
    /*
     * Whether this host can be taught a recipe: a Crafting Computer with a Crafting Card in it. It is a synced
     * reading rather than a guess from the block, because the card can be pulled while somebody is looking.
     */
    private static final int DATA_PATTERNS_HOST = 33;

    private final Level level;
    @Nullable
    private final IComputerTerminalHost host;
    @Nullable
    private final ServerPlayer serverPlayer;
    private final BlockPos hostPos;
    private final BlockPos monitorPos;
    /* Which operating space the client is to draw over these slots; null when nothing registered draws. */
    @Nullable
    private final ResourceLocation spaceId;
    /*
     * What the machine's system calls itself, which is what its prompt greets a player with. Sent rather
     * than read off the disks on the client: the server is the side that knows, and a console that cannot
     * say what it is on says nothing at all instead.
     */
    private final String systemName;
    private final int storageCount;
    private int refreshTick;
    private boolean initialDataSent;

    private int activeTab;

    private List<NetworkItemEntry> networkItems = List.of();

    private List<NetworkItemEntry> localItems = List.of();

    private List<ServerBreakdownPayload.ServerHolding>
            serverBreakdown = List.of();

    private List<OperationRecord>
            operationsLog = List.of();

    private List<OperationRecord>
            activeOps = List.of();

    private List<NetworkServersPayload.ServerEntry>
            networkServers = List.of();

    private final int[] clientData = new int[DATA_COUNT];

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(final int index) {
            if (level.isClientSide) {
                return index >= 0 && index < clientData.length ? clientData[index] : 0;
            }
            return serverValue(index);
        }

        @Override
        public void set(final int index, final int value) {
            if (index >= 0 && index < clientData.length) {
                clientData[index] = value;
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public ComputerTerminalMenu(final int containerId, final Inventory playerInventory,
                                @Nullable final IComputerTerminalHost host,
                                final BlockPos hostPos, final BlockPos monitorPos, final int initialTab,
                                @Nullable final ResourceLocation spaceId, final String systemName) {
        super(ComputingMenus.COMPUTER_TERMINAL_MENU.get(), containerId);
        this.level = playerInventory.player.level();
        this.serverPlayer = playerInventory.player instanceof ServerPlayer sp ? sp : null;
        this.host = host;
        this.hostPos = hostPos.immutable();
        this.monitorPos = monitorPos.immutable();
        this.spaceId = spaceId;
        this.systemName = systemName == null ? "" : systemName;
        /*
         * Open on the tab the server worked out before the window opened (openingTab), which is the one the
         * client was sent too. A server building the menu works it out again, which changes nothing; a client
         * cannot see the network, so it only keeps the tab in range.
         */
        final int inRange = initialTab >= TAB_LOCAL && initialTab <= TAB_LAST ? initialTab : TAB_NETWORK;
        this.activeTab = level instanceof ServerLevel server ? openingTab(host, server, inRange) : inRange;

        /*
         * The Storage tab is now a disk-backed quantity view (like the Network tab), not vanilla
         * slots, so the menu holds only the player inventory; local items are synced via snapshot.
         */
        this.storageCount = 0;
        addPlayerInventory(playerInventory, INV_X, INV_Y);
        addDataSlots(data);
    }

    /**
     * Where the player's own rows sit, which is the same place on every machine.
     *
     * <p>They used to drop by a band on a Mainframe, because the window grew to carry an extra heading. The
     * glass is the whole monitor now and the rail scrolls, so nothing about the host moves a slot.
     */
    public int invY() {
        return INV_Y;
    }

    public int hotbarY() {
        return HOTBAR_Y;
    }

    /**
     * The operating space drawing this machine, or null when nothing registered is.
     *
     * <p>Which screen the client puts over these slots: the mod owns the menu, because the items are the
     * server's and every space needs the same ones, and the space owns the drawing and the keyboard. It is
     * sent with the window rather than asked of the machine, because what a space is lives in a console the
     * client never sees.
     */
    @Nullable
    public ResourceLocation spaceId() {
        return spaceId;
    }

    /** What the machine's system calls itself, which is what its prompt greets a player with. */
    public String systemName() {
        return systemName;
    }

    @Nullable
    public static ComputerTerminalMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                                   final RegistryFriendlyByteBuf buf) {
        final BlockPos monitorPos = buf.readBlockPos();
        final BlockPos hostPos = buf.readBlockPos();
        final int initialTab = buf.readVarInt();
        final ResourceLocation spaceId = buf.readBoolean() ? buf.readResourceLocation() : null;
        final String systemName = buf.readUtf(MAX_SYSTEM_NAME);
        final var be = playerInventory.player.level().getBlockEntity(hostPos);
        if (be instanceof IComputerTerminalHost terminalHost) {
            return new ComputerTerminalMenu(containerId, playerInventory, terminalHost, hostPos, monitorPos,
                    initialTab, spaceId, systemName);
        }
        return null;
    }

    /** Writes what the client needs before the menu exists: where it is, which heading, and what it runs. */
    public static void writeOpenBuffer(final RegistryFriendlyByteBuf buf, final BlockPos monitorPos,
                                       final BlockPos hostPos, final int initialTab,
                                       @Nullable final ResourceLocation spaceId, final String systemName) {
        buf.writeBlockPos(monitorPos);
        buf.writeBlockPos(hostPos);
        buf.writeVarInt(initialTab);
        buf.writeBoolean(spaceId != null);
        if (spaceId != null) {
            buf.writeResourceLocation(spaceId);
        }
        /*
         * Cut rather than trusted: a string longer than its wire field throws while it is being encoded,
         * which disconnects the player instead of shortening a name.
         */
        final String name = systemName == null ? "" : systemName;
        buf.writeUtf(name.length() > MAX_SYSTEM_NAME ? name.substring(0, MAX_SYSTEM_NAME) : name,
                MAX_SYSTEM_NAME);
    }

    private int serverValue(final int index) {
        if (host == null) {
            return 0;
        }
        return switch (index) {
            case 0 -> host.computerRunning() ? 1 : 0;
            case 1 -> host.computerBuildValid() ? 1 : 0;
            case 2 -> host.networkLinkState();
            case 3 -> clampInt(host.orchestrationCapacity());
            case 4 -> host.computerQueues();
            case 5 -> clampInt(host.computerRamBuffer());
            case 6 -> host.networkServerCount();
            case 7 -> host.installedCpus();
            case 8 -> host.cpuSlots();
            case 9 -> host.installedRam();
            case 10 -> host.ramSlots();
            case 11 -> host.installedGpus();
            case 12 -> host.gpuSlots();
            case 13 -> host.installedDisks();
            case 14 -> host.diskSlots();
            case 15 -> clampInt(host.localStorageUsed());
            case 16 -> clampInt(host.localStorageCapacity());
            case 17 -> host.isMainframeHost() ? 1 : 0;
            case DATA_USABLE_SLOTS -> host.usableStorageSlots();
            case 19 -> host.pendingOperations();
            case 20 -> host.runningOperations();
            case 21 -> host.completedOperations();
            case 22 -> host.networkPcCount();
            case 23 -> host.networkSubframeCount();
            case 24 -> host.indexedTypes();
            case 25 -> host.indexedServers();
            case 26 -> host.activeLocks();
            case 27 -> clampInt(host.networkStorageUsed());
            case 28 -> clampInt(host.networkStorageTotal());
            case DATA_CRAFT_COMPUTERS -> craftComputerCount();
            case DATA_ERA -> {
                final HardwareEra era = host.displayEra();
                yield era == null ? -1 : era.id();
            }
            case DATA_INDEX_HEALTH -> host.indexHealthState();
            case DATA_INDEX_HEALTH_TYPES -> host.indexHealthTypeCount();
            case DATA_PATTERNS_HOST -> host instanceof CraftingComputerBlockEntity cc
                    && cc.craftingCardFactor() > 0.0 ? 1 : 0;
            default -> 0;
        };
    }

    /**
     * Whether this machine can be taught a recipe, which is what the Patterns heading needs to exist.
     *
     * <p>A Crafting Computer with a Crafting Card: the same condition the Crafting Manager installs under,
     * asked of the machine rather than of the system, so taking the card out takes the heading away.
     */
    public boolean patternsAvailable() {
        return data.get(DATA_PATTERNS_HOST) > 0;
    }

    /**
     * The tab a terminal opens on: the player's last one, unless it is not there for this host any more.
     *
     * <p>Worked out on the server before the window opens, and handed to both the menu the server keeps and the
     * message the client builds its own from, so the two start on the same tab. Worked out only inside the menu,
     * the client, which cannot see the network, kept a tab the server had already left for Network.
     */
    public static int openingTab(final IComputerTerminalHost host, final ServerLevel level, final int asked) {
        int tab = asked >= TAB_LOCAL && asked <= TAB_LAST ? asked : TAB_NETWORK;
        // The Mainframe-only views are not there on a plain computer.
        if ((tab == TAB_TASKS || tab == TAB_MAINTENANCE) && (host == null || !host.isMainframeHost())) {
            tab = TAB_NETWORK;
        }
        // The Craft tab went with the last Crafting Computer on the network.
        if (tab == TAB_CRAFT && (host == null || host.networkUuid() == null
                || NetworkSystem.get(level).craftingComputersOf(host.networkUuid()).isEmpty())) {
            tab = TAB_NETWORK;
        }
        // The Patterns tab went with the card, or this was never a machine that could be taught.
        if (tab == TAB_PATTERNS && !(host instanceof CraftingComputerBlockEntity cc && cc.craftingCardFactor() > 0.0)) {
            tab = TAB_NETWORK;
        }
        return tab;
    }

    private int craftComputerCount() {
        if (host == null || host.networkUuid() == null || !(level instanceof ServerLevel serverLevel)) {
            return 0;
        }
        return NetworkSystem.get(serverLevel)
                .craftingComputersOf(host.networkUuid()).size();
    }

    public boolean craftAvailable() {
        return data.get(DATA_CRAFT_COMPUTERS) > 0;
    }

    private static int clampInt(final long value) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, value));
    }

    // Tabs

    public int activeTab() {
        return activeTab;
    }

    public void setActiveTab(final int tab) {
        this.activeTab = tab;
    }

    @Override
    public boolean clickMenuButton(final Player player, final int id) {
        if (id >= TAB_LOCAL && id <= TAB_LAST) {
            this.activeTab = id;
            // Remember the tab on the Monitor so reopening this terminal lands here again.
            if (level.getBlockEntity(monitorPos) instanceof MonitorBlockEntity monitor) {
                monitor.setLastTab(id);
            }
            if (player instanceof ServerPlayer sp && level instanceof ServerLevel serverLevel) {
                dispatchTabData(id, sp, serverLevel);
            }
            return true;
        }
        return false;
    }

    private void dispatchTabData(final int id, final ServerPlayer serverPlayer, final ServerLevel serverLevel) {
        if (host == null) {
            return;
        }
        if (id == TAB_STORAGE) {
            TerminalLocalPayloads.dispatchLocalSnapshot(serverPlayer, host);
        } else if (host.networkUuid() != null) {
            if (id == TAB_NETWORK) {
                TerminalPayloads.dispatchTerminalQuery(serverPlayer, host.networkUuid(), serverLevel);
            } else if (id == TAB_OPS || id == TAB_TASKS) {
                OperationsPayloads.dispatchTerminalOpsLog(serverPlayer, host.networkUuid(), serverLevel);
                OperationsPayloads.dispatchActiveOperations(serverPlayer, host.networkUuid(), serverLevel);
            } else if (id == TAB_MAINTENANCE) {
                /*
                 * The DROP popup needs the network's data types (the TYPES grid) and the list of
                 * Servers it can wipe (the SERVER picker); the index stats arrive via ContainerData.
                 */
                TerminalPayloads.dispatchTerminalQuery(serverPlayer, host.networkUuid(), serverLevel);
                NetworkPayloads.dispatchNetworkServers(serverPlayer, host.networkUuid(), serverLevel);
            } else if (id == TAB_CRAFT) {
                /*
                 * The Craft tab needs the catalog plus the live/logged Operations for its
                 * RUNNING and RECENT panels.
                 */
                CraftingPayloads.dispatchCraftCatalog(serverPlayer, host.networkUuid(), serverLevel);
                OperationsPayloads.dispatchTerminalOpsLog(serverPlayer, host.networkUuid(), serverLevel);
                OperationsPayloads.dispatchActiveOperations(serverPlayer, host.networkUuid(), serverLevel);
            } else if (id == TAB_PROCESSES) {
                // Per-host: the processes shown are the ones running on THIS computer (the host).
                ProgramPayloads.dispatchProcesses(serverPlayer, hostPos(), serverLevel);
            }
        }
    }

    private List<CraftCatalogPayload.Entry> craftCatalog = List.of();

    private List<ProcessListPayload.ProcessLine> processes = List.of();

    public void setProcesses(final List<ProcessListPayload.ProcessLine> processes) {
        this.processes = processes;
    }

    public List<ProcessListPayload.ProcessLine> processes() {
        return processes;
    }

    @Nullable
    private CraftPlanPayload craftPlan;

    public void setCraftCatalog(final List<
            CraftCatalogPayload.Entry> entries) {
        this.craftCatalog = entries;
    }

    public List<CraftCatalogPayload.Entry> craftCatalog() {
        return craftCatalog;
    }

    public void setCraftPlan(
            @Nullable final CraftPlanPayload plan) {
        this.craftPlan = plan;
    }

    @Nullable
    public CraftPlanPayload craftPlan() {
        return craftPlan;
    }

    public void setNetworkItems(final List<NetworkItemEntry> items) {
        this.networkItems = items;
    }

    public List<NetworkItemEntry> networkItems() {
        return networkItems;
    }

    public void setLocalItems(final List<NetworkItemEntry> items) {
        this.localItems = items;
    }

    public List<NetworkItemEntry> localItems() {
        return localItems;
    }

    /*
     * Per-disk privacy state for the Storage tab's slider, synced with the local snapshot. Empty for a
     * host with no slider (a Server/Mainframe), which the screen reads as "always public".
     */
    private List<LocalStorageSnapshotPayload.DiskInfo> diskPrivacy = List.of();

    public void setDiskPrivacy(final List<LocalStorageSnapshotPayload.DiskInfo> disks) {
        this.diskPrivacy = disks;
    }

    public List<LocalStorageSnapshotPayload.DiskInfo> diskPrivacy() {
        return diskPrivacy;
    }

    /** Whether the open host has a per-disk privacy slider (true once the snapshot carried disk rows). */
    public boolean storageHasSlider() {
        return host != null && host.storageHasSlider();
    }

    public int diskCount() {
        return diskPrivacy.size();
    }

    public int diskPermille(final int index) {
        return index >= 0 && index < diskPrivacy.size() ? diskPrivacy.get(index).permille() : 0;
    }

    public long diskUsedWeight(final int index) {
        return index >= 0 && index < diskPrivacy.size() ? diskPrivacy.get(index).usedWeight() : 0L;
    }

    public long diskCapacityWeight(final int index) {
        return index >= 0 && index < diskPrivacy.size() ? diskPrivacy.get(index).capacityWeight() : 0L;
    }

    public void setServerBreakdown(
            final List<ServerBreakdownPayload.ServerHolding> rows) {
        this.serverBreakdown = rows;
    }

    public List<ServerBreakdownPayload.ServerHolding> serverBreakdown() {
        return serverBreakdown;
    }

    public void setNetworkServers(
            final List<NetworkServersPayload.ServerEntry> servers) {
        this.networkServers = servers;
    }

    public List<NetworkServersPayload.ServerEntry> networkServers() {
        return networkServers;
    }

    public void setOperationsLog(
            final List<OperationRecord> ops) {
        this.operationsLog = ops;
    }

    public List<OperationRecord> operationsLog() {
        return operationsLog;
    }

    public void setActiveOps(
            final List<OperationRecord> ops) {
        this.activeOps = ops;
    }

    public List<OperationRecord> activeOps() {
        return activeOps;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (serverPlayer == null || host == null || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        // Populate the first-shown tab once, right after the terminal opens. clickMenuButton only
        if (!initialDataSent) {
            initialDataSent = true;
            dispatchTabData(activeTab, serverPlayer, serverLevel);
        }
        if (host.networkUuid() == null) {
            return;
        }
        if (++refreshTick < 10) {
            return;
        }
        refreshTick = 0;
        /*
         * The Tasks view always re-syncs (so finished ops drop off); the Network grid re-queries
         * only while something is in flight (its snapshot is already pushed on deposit/withdraw/settle).
         */
        if (activeTab == TAB_TASKS || activeTab == TAB_OPS) {
            OperationsPayloads.dispatchActiveOperations(serverPlayer, host.networkUuid(), serverLevel);
        }
        if (activeTab == TAB_OPS) {
            /*
             * Keep the log live too: a craft that just settled drops out of the active list and must appear in
             * the recent log the same tick, so the Operations view is fully real-time (in flight and just done).
             */
            OperationsPayloads.dispatchTerminalOpsLog(serverPlayer, host.networkUuid(), serverLevel);
        }
        if (activeTab == TAB_CRAFT
                && TerminalPayloads.networkHasActiveOps(serverLevel, host.networkUuid())) {
            // Keep the RUNNING bars moving and settle finished crafts into RECENT.
            OperationsPayloads.dispatchActiveOperations(serverPlayer, host.networkUuid(), serverLevel);
            OperationsPayloads.dispatchTerminalOpsLog(serverPlayer, host.networkUuid(), serverLevel);
        }
        if (activeTab == TAB_NETWORK
                && TerminalPayloads.networkHasActiveOps(serverLevel, host.networkUuid())) {
            TerminalPayloads.dispatchTerminalQuery(serverPlayer, host.networkUuid(), serverLevel);
        }
    }

    @Override
    public BlockPos monitorPos() {
        return monitorPos;
    }

    public BlockPos hostPos() {
        return hostPos;
    }

    // Screen accessors (read from the synced data on the client)

    public boolean running() {
        return data.get(0) != 0;
    }

    public boolean buildValid() {
        return data.get(1) != 0;
    }

    public int networkLinkState() {
        return data.get(2);
    }

    public long capacity() {
        return data.get(3);
    }

    public int queues() {
        return data.get(4);
    }

    public long ramBuffer() {
        return data.get(5);
    }

    public int serverCount() {
        return data.get(6);
    }

    public int installedCpus() {
        return data.get(7);
    }

    public int cpuSlots() {
        return data.get(8);
    }

    public int installedRam() {
        return data.get(9);
    }

    public int ramSlots() {
        return data.get(10);
    }

    public int installedGpus() {
        return data.get(11);
    }

    public int gpuSlots() {
        return data.get(12);
    }

    public int installedDisks() {
        return data.get(13);
    }

    public int diskSlots() {
        return data.get(14);
    }

    public long storageUsed() {
        return data.get(15);
    }

    public long storageCapacity() {
        return data.get(16);
    }

    public boolean mainframeHost() {
        return data.get(17) != 0;
    }

    public int indexedTypes() {
        return data.get(24);
    }

    public int indexedServers() {
        return data.get(25);
    }

    public int activeLocks() {
        return data.get(26);
    }

    /** The index's health state, read back from the id the host synced. */
    public IndexHealth.State indexHealth() {
        return IndexHealth.State.byId(data.get(DATA_INDEX_HEALTH));
    }

    /** How many item types the index has flagged. */
    public int indexHealthTypes() {
        return data.get(DATA_INDEX_HEALTH_TYPES);
    }

    public long networkStorageUsed() {
        return data.get(27);
    }

    public long networkStorageTotal() {
        return data.get(28);
    }

    public int usableStorageSlots() {
        return data.get(DATA_USABLE_SLOTS);
    }

    public int pendingOps() {
        return data.get(19);
    }

    public int runningOps() {
        return data.get(20);
    }

    public int completedOps() {
        return data.get(21);
    }

    public int pcCount() {
        return data.get(22);
    }

    public int subframeCount() {
        return data.get(23);
    }

    public int storageSlotCount() {
        return storageCount;
    }

    /** The host computer's board-derived hardware era for the GUI skin, or {@code null} (STANDARD) when none. */
    @Nullable
    public HardwareEra hardwareEra() {
        return HardwareEra.find(data.get(DATA_ERA));
    }

    @Override
    public boolean stillValid(final Player player) {
        // Reach is to the Monitor, and the Monitor must still link to this computer.
        if (!(level.getBlockEntity(monitorPos) instanceof MonitorBlockEntity monitor)) {
            return false;
        }
        // Either the cable links this machine, or a Remote Control session put it on the screen.
        return monitor.shows(hostPos)
                && player.distanceToSqr(monitorPos.getX() + 0.5, monitorPos.getY() + 0.5,
                monitorPos.getZ() + 0.5) <= MONITOR_REACH * MONITOR_REACH
                // The network GUI dies with its machine (power off, system disk pulled).
                && CommandPromptMenu.sessionAlive(level, hostPos);
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        final Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = slot.getItem();
        final ItemStack original = stack.copy();
        final int invStart = storageCount;
        final int invEnd = storageCount + 36;

        if (index < invStart) {
            // storage -> player inventory
            if (!moveItemStackTo(stack, invStart, invEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // player inventory -> storage, but only while the Storage tab is open
            final int limit = activeTab == TAB_STORAGE ? Math.min(storageCount, usableStorageSlots()) : 0;
            if (limit <= 0 || !moveItemStackTo(stack, 0, limit, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }
}
