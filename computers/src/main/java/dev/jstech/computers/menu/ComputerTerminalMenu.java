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
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.menu.CoreMenu;
import dev.jstech.core.menu.MenuValidity;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.tier.HardwareEra;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Menu for the Monitor terminal, the tabbed interface a Monitor opens onto the computer it is linked to.
 */
@ApiStatus.Experimental
public class ComputerTerminalMenu extends CoreMenu implements IMonitorMenu {

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
    private final ComputerReadings readings;
    private int refreshTick;
    private boolean initialDataSent;
    private int activeTab;

    private List<NetworkItemEntry> networkItems = List.of();
    private List<NetworkItemEntry> localItems = List.of();
    private List<ServerBreakdownPayload.ServerHolding> serverBreakdown = List.of();
    private List<OperationRecord> operationsLog = List.of();
    private List<OperationRecord> activeOps = List.of();
    private List<NetworkServersPayload.ServerEntry> networkServers = List.of();
    private List<CraftCatalogPayload.Entry> craftCatalog = List.of();
    private List<ProcessListPayload.ProcessLine> processes = List.of();
    @Nullable
    private CraftPlanPayload craftPlan;
    /*
     * Per-disk privacy state for the Storage tab's slider, synced with the local snapshot. Empty for a
     * host with no slider (a Server/Mainframe), which the screen reads as "always public".
     */
    private List<LocalStorageSnapshotPayload.DiskInfo> diskPrivacy = List.of();

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

    private static final double MONITOR_REACH = 16.0;

    public ComputerTerminalMenu(final int containerId, final Inventory playerInventory,
                                @Nullable final IComputerTerminalHost host,
                                final BlockPos hostPos, final BlockPos monitorPos, final int initialTab,
                                @Nullable final ResourceLocation spaceId, final String systemName) {
        super(ComputingMenus.COMPUTER_TERMINAL_MENU.get(), containerId, playerInventory,
                validity(playerInventory.player.level(), hostPos.immutable(), monitorPos.immutable()));
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

        for (int tab = TAB_LOCAL; tab <= TAB_LAST; tab++) {
            final int id = tab;
            button(id, player -> selectTab(id, player));
        }

        /*
         * The Storage tab is a disk-backed quantity view (like the Network tab), not vanilla slots, so the
         * menu holds only the player inventory; local items are synced via snapshot. Shift-clicking a slot is
         * read by the screen itself, which sends it as a deposit/insert payload instead, so no route is ever
         * declared here and the inherited quickMoveStack always answers empty.
         */
        final GuiLayout layout = ComputerTerminalLayout.layout();
        playerInventory(playerInventory, layout.playerInventoryAt());

        this.readings = new ComputerReadings(host, level, this::value, this::flag);
    }

    /**
     * Where the player's own rows sit, which is the same place on every machine.
     *
     * <p>They used to drop by a band on a Mainframe, because the window grew to carry an extra heading. The
     * glass is the whole monitor now and the rail scrolls, so nothing about the host moves a slot.
     */
    public int invY() {
        return ComputerTerminalLayout.INV_Y;
    }

    public int hotbarY() {
        return ComputerTerminalLayout.HOTBAR_Y;
    }

    /** Where the player's own columns start, the same on every machine. */
    public int invX() {
        return ComputerTerminalLayout.INV_X;
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

    /**
     * The menu as the player's game opens it. The machine can stand beyond what this game has loaded while its
     * monitor is close by, so the menu opens without it then: everything it reads of the machine reads as nothing.
     */
    public static ComputerTerminalMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                                   final RegistryFriendlyByteBuf buf) {
        final BlockPos monitorPos = buf.readBlockPos();
        final BlockPos hostPos = buf.readBlockPos();
        final int initialTab = buf.readVarInt();
        final ResourceLocation spaceId = buf.readBoolean() ? buf.readResourceLocation() : null;
        final String systemName = buf.readUtf(MAX_SYSTEM_NAME);
        final var be = playerInventory.player.level().getBlockEntity(hostPos);
        return new ComputerTerminalMenu(containerId, playerInventory,
                be instanceof IComputerTerminalHost terminalHost ? terminalHost : null, hostPos, monitorPos,
                initialTab, spaceId, systemName);
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

    /**
     * Whether this machine can be taught a recipe, which is what the Patterns heading needs to exist.
     *
     * <p>A Crafting Computer with a Crafting Card: the same condition the Crafting Manager installs under,
     * asked of the machine rather than of the system, so taking the card out takes the heading away.
     */
    public boolean patternsAvailable() {
        return readings.patternsHost();
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
        if (tab == TAB_PATTERNS && !teachable(host)) {
            tab = TAB_NETWORK;
        }
        return tab;
    }

    public boolean craftAvailable() {
        return readings.craftComputers() > 0;
    }

    // Tabs

    public int activeTab() {
        return activeTab;
    }

    public void setActiveTab(final int tab) {
        this.activeTab = tab;
    }

    public void setProcesses(final List<ProcessListPayload.ProcessLine> processes) {
        this.processes = processes;
    }

    public List<ProcessListPayload.ProcessLine> processes() {
        return processes;
    }

    public void setCraftCatalog(final List<CraftCatalogPayload.Entry> entries) {
        this.craftCatalog = entries;
    }

    public List<CraftCatalogPayload.Entry> craftCatalog() {
        return craftCatalog;
    }

    public void setCraftPlan(@Nullable final CraftPlanPayload plan) {
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

    public void setServerBreakdown(final List<ServerBreakdownPayload.ServerHolding> rows) {
        this.serverBreakdown = rows;
    }

    public List<ServerBreakdownPayload.ServerHolding> serverBreakdown() {
        return serverBreakdown;
    }

    public void setNetworkServers(final List<NetworkServersPayload.ServerEntry> servers) {
        this.networkServers = servers;
    }

    public List<NetworkServersPayload.ServerEntry> networkServers() {
        return networkServers;
    }

    public void setOperationsLog(final List<OperationRecord> ops) {
        this.operationsLog = ops;
    }

    public List<OperationRecord> operationsLog() {
        return operationsLog;
    }

    public void setActiveOps(final List<OperationRecord> ops) {
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
        // Populate the first-shown tab once, right after the terminal opens; a tab button only sends data when pressed.
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

    // Screen accessors (read from the synced values on the client)

    public boolean running() {
        return readings.running();
    }

    public boolean buildValid() {
        return readings.buildValid();
    }

    public int networkLinkState() {
        return readings.networkLinkState();
    }

    public long capacity() {
        return readings.capacity();
    }

    public int queues() {
        return readings.queues();
    }

    public long ramBuffer() {
        return readings.ramBuffer();
    }

    public int serverCount() {
        return readings.serverCount();
    }

    public int installedCpus() {
        return readings.installedCpus();
    }

    public int cpuSlots() {
        return readings.cpuSlots();
    }

    public int installedRam() {
        return readings.installedRam();
    }

    public int ramSlots() {
        return readings.ramSlots();
    }

    public int installedGpus() {
        return readings.installedGpus();
    }

    public int gpuSlots() {
        return readings.gpuSlots();
    }

    public int installedDisks() {
        return readings.installedDisks();
    }

    public int diskSlots() {
        return readings.diskSlots();
    }

    public long storageUsed() {
        return readings.storageUsed();
    }

    public long storageCapacity() {
        return readings.storageCapacity();
    }

    public boolean mainframeHost() {
        return readings.mainframeHost();
    }

    public int indexedTypes() {
        return readings.indexedTypes();
    }

    public int indexedServers() {
        return readings.indexedServers();
    }

    public int activeLocks() {
        return readings.activeLocks();
    }

    /** The index's health state, read back from the id the host synced. */
    public IndexHealth.State indexHealth() {
        return IndexHealth.State.byId(readings.indexHealth());
    }

    /** How many item types the index has flagged. */
    public int indexHealthTypes() {
        return readings.indexHealthTypes();
    }

    public long networkStorageUsed() {
        return readings.networkStorageUsed();
    }

    public long networkStorageTotal() {
        return readings.networkStorageTotal();
    }

    public int usableStorageSlots() {
        return readings.usableStorageSlots();
    }

    public int pendingOps() {
        return readings.pendingOps();
    }

    public int runningOps() {
        return readings.runningOps();
    }

    public int completedOps() {
        return readings.completedOps();
    }

    public int pcCount() {
        return readings.pcCount();
    }

    public int subframeCount() {
        return readings.subframeCount();
    }

    /** The host computer's board-derived hardware era for the GUI skin, or {@code null} (STANDARD) when none. */
    @Nullable
    public HardwareEra hardwareEra() {
        return HardwareEra.find(readings.era());
    }

    /** Valid while the monitor shows this host, the player is within reach of it and the session lives. */
    private static Predicate<Player> validity(final Level level, final BlockPos hostPos, final BlockPos monitorPos) {
        return MenuValidity.near(level, monitorPos, MONITOR_REACH)
                // Covers both a cable link and a Remote Control session onto the same host.
                .and(player -> level.getBlockEntity(monitorPos) instanceof MonitorBlockEntity monitor
                        && monitor.shows(hostPos))
                // Closes the GUI the moment the machine powers off or its system disk is pulled.
                .and(player -> CommandPromptMenu.sessionAlive(level, hostPos));
    }

    private void selectTab(final int tab, final Player player) {
        this.activeTab = tab;
        // Remember the tab on the Monitor so reopening this terminal lands here again.
        if (level.getBlockEntity(monitorPos) instanceof MonitorBlockEntity monitor) {
            monitor.setLastTab(tab);
        }
        if (player instanceof ServerPlayer sp && level instanceof ServerLevel serverLevel) {
            dispatchTabData(tab, sp, serverLevel);
        }
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
                 * Servers it can wipe (the SERVER picker); the index stats arrive via the synced values.
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

    /** Whether {@code host} can be taught a recipe: a Crafting Computer with a Crafting Card installed. */
    static boolean teachable(@Nullable final IComputerTerminalHost host) {
        return host instanceof CraftingComputerBlockEntity cc && cc.craftingCardFactor() > 0.0;
    }
}
