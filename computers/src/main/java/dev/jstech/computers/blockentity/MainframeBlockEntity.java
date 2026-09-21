/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.MainframeBlock;
import dev.jstech.computers.block.MainframeStructure;
import dev.jstech.computers.crafting.CraftPlanner;
import dev.jstech.computers.crafting.CraftingPattern;
import dev.jstech.computers.crafting.ICraftIo;
import dev.jstech.computers.crafting.MultiStagePattern;
import dev.jstech.computers.crafting.NetworkCraftOperation;
import dev.jstech.computers.crafting.NetworkMultiStageOperation;
import dev.jstech.computers.crafting.NetworkProcessingOperation;
import dev.jstech.computers.crafting.NetworkRecipe;
import dev.jstech.computers.crafting.ProcessingPattern;
import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.hardware.FormFactor;
import dev.jstech.computers.operation.INetworkOperation;
import dev.jstech.computers.operation.IPersistentOperation;
import dev.jstech.computers.operation.NetworkIndex;
import dev.jstech.computers.operation.NetworkInsertOperation;
import dev.jstech.computers.operation.NetworkSelectOperation;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.os.OsDisks;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.install.OsInstallRunner;
import dev.jstech.computers.os.install.SetupRunner;
import dev.jstech.computers.program.iql.IqlCatalog;
import dev.jstech.computers.storage.IDataSink;
import dev.jstech.computers.storage.LocalStore;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.storage.StoreSink;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.network.DataTier;
import dev.jstech.core.network.FailoverRole;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.operation.OperationBalance;
import dev.jstech.core.operation.OperationDispatch;
import dev.jstech.core.operation.OperationPriority;
import dev.jstech.core.operation.IOperationTask;
import dev.jstech.core.operation.OperationStatistics;
import dev.jstech.core.operation.SelfTestOperationTask;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The Mainframe BlockEntity: the binding that turns installed hardware item stacks into a
 * {@link ComputerBuild} and exposes the powered state, capacity and parallel-queue count. Unlike the
 * passive computers it shares a base with, the Mainframe OWNS and orchestrates a data network rather
 * than reading one from a cable.
 */
public class MainframeBlockEntity extends AbstractComputerBlockEntity
        implements IComputerTerminalHost,
        GeoBlockEntity {

    @Override
    public Set<Long> occupiedPositions(final long ownerPos) {
        // The whole 3x2x2 footprint is one connection surface: a peripheral cable
        final Direction facing = getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        final Set<Long> positions = new HashSet<>();
        for (final BlockPos p : MainframeStructure.allPositions(worldPosition, facing)) {
            positions.add(p.asLong());
        }
        return positions;
    }

    public static final int MOTHERBOARD_SLOT = 0;
    public static final int CPU_SLOTS_START = 1;
    public static final int CPU_SLOTS = 4;
    public static final int RAM_SLOTS_START = 5;
    public static final int RAM_SLOTS = 8;
    public static final int GPU_SLOTS_START = 13;
    public static final int GPU_SLOTS = 6;
    public static final int PSU_SLOT = 19;
    public static final int DISK_SLOTS_START = 20;
    public static final int DISK_SLOTS = 4;
    public static final int TOTAL_SLOTS = 24;

    private static final ComputerHardwareLayout LAYOUT = new ComputerHardwareLayout(
            MOTHERBOARD_SLOT, CPU_SLOTS_START, CPU_SLOTS, RAM_SLOTS_START, RAM_SLOTS,
            GPU_SLOTS_START, GPU_SLOTS, PSU_SLOT, DISK_SLOTS_START, DISK_SLOTS, TOTAL_SLOTS);

    public static final int STORAGE_SLOTS = 27;

    /*
     * the cabinet as one model: what the renderer needs to know
     *
     * The Mainframe is drawn as a single GeckoLib cabinet by its controller, with a bone per installed
     * part. The client copy of a computer only carries its name (the hardware handler is deliberately
     * not synced), so the visual state travels as three small numbers in the block update: which
     * hardware slots are filled, which disks carry a system, and the machine's own condition.
     */

    private static final int FLAG_RUNNING = 1;
    private static final int FLAG_BUILD_VALID = 2;
    private static final int FLAG_NETWORKED = 4;
    private static final int FLAG_PANEL_OFF = 8;

    private static final RawAnimation WORK =
            RawAnimation.begin().thenLoop("animation.mainframe.work");

    private final AnimatableInstanceCache geckoCache =
            GeckoLibUtil.createInstanceCache(this);

    private int clientHardwareMask;
    private int clientDiskSystemMask;
    private int clientFlags;

    /** The service panel taken off, showing the card bay and everything seated in it. */
    private boolean servicePanelOff;

    /** The last visual state pushed to clients, so a tick only sends a packet when something changed. */
    private long sentVisuals = -1L;

    @Override
    public void registerControllers(
            final AnimatableManager.ControllerRegistrar controllers) {
        /*
         * The roof fans and the tape reels turn while the machine is up; every other visual (installed
         * hardware, the lamps, the panel) is bone visibility set by the renderer, not animation.
         */
        controllers.add(new AnimationController<>(this, "work", 0,
                state -> visualRunning() ? state.setAndContinue(WORK)
                        : PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geckoCache;
    }

    /** The era of this cabinet, read from its block; it picks the model and the atlas. */
    public HardwareEra mainframeEra() {
        return getBlockState().getBlock()
                instanceof MainframeBlock mainframe
                ? mainframe.era() : HardwareEra.STANDARD;
    }

    /** Whether hardware slot {@code slot} holds a part, on either side. */
    public boolean hardwareInstalled(final int slot) {
        if (slot < 0 || slot >= TOTAL_SLOTS) {
            return false;
        }
        if (level != null && level.isClientSide()) {
            return (clientHardwareMask & (1 << slot)) != 0;
        }
        return !getHardware().getStackInSlot(slot).isEmpty();
    }

    /** Whether the disk in bay {@code bay} carries a system, which is what lights its lamp. */
    public boolean diskCarriesSystem(final int bay) {
        if (bay < 0 || bay >= DISK_SLOTS) {
            return false;
        }
        if (level != null && level.isClientSide()) {
            return (clientDiskSystemMask & (1 << bay)) != 0;
        }
        return OsDisks.hasSystem(
                getHardware().getStackInSlot(DISK_SLOTS_START + bay));
    }

    public boolean visualRunning() {
        return level != null && level.isClientSide() ? (clientFlags & FLAG_RUNNING) != 0 : isRunning();
    }

    public boolean visualBuildValid() {
        return level != null && level.isClientSide() ? (clientFlags & FLAG_BUILD_VALID) != 0 : buildValid();
    }

    public boolean visualNetworked() {
        return level != null && level.isClientSide() ? (clientFlags & FLAG_NETWORKED) != 0 : networkUuid() != null;
    }

    public boolean servicePanelOff() {
        return level != null && level.isClientSide() ? (clientFlags & FLAG_PANEL_OFF) != 0 : servicePanelOff;
    }

    /** Takes the service panel off the card bay or puts it back. */
    public void toggleServicePanel() {
        servicePanelOff = !servicePanelOff;
        setChanged();
        syncVisuals();
    }

    /** Everything the renderer reads, packed so a tick can tell at a glance whether it moved. */
    private long visualState() {
        int hardware = 0;
        for (int slot = 0; slot < TOTAL_SLOTS; slot++) {
            if (!getHardware().getStackInSlot(slot).isEmpty()) {
                hardware |= 1 << slot;
            }
        }
        int systems = 0;
        for (int bay = 0; bay < DISK_SLOTS; bay++) {
            if (OsDisks.hasSystem(
                    getHardware().getStackInSlot(DISK_SLOTS_START + bay))) {
                systems |= 1 << bay;
            }
        }
        int flags = 0;
        flags |= isRunning() ? FLAG_RUNNING : 0;
        flags |= buildValid() ? FLAG_BUILD_VALID : 0;
        flags |= networkUuid() != null ? FLAG_NETWORKED : 0;
        flags |= servicePanelOff ? FLAG_PANEL_OFF : 0;
        return ((long) hardware << 8) | ((long) systems << 4) | flags;
    }

    /**
     * Pushes the cabinet's look to watching clients when it changed. Called every server tick rather
     * than from each place that installs a part or flips the power, so no path can leave the model
     * showing hardware that is no longer there.
     */
    private void syncVisualsIfChanged() {
        final long state = visualState();
        if (state != sentVisuals) {
            sentVisuals = state;
            syncVisuals();
        }
    }

    public void syncVisuals() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(),
                    Block.UPDATE_CLIENTS);
        }
    }

    /** The whole 3 x 2 x 2 footprint: the renderer draws the cabinet from this block alone. */
    public AABB renderBox() {
        final BlockState state = getBlockState();
        if (!state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return new AABB(worldPosition);
        }
        AABB box = new AABB(worldPosition);
        for (final BlockPos part : MainframeStructure.allPositions(
                worldPosition, state.getValue(HorizontalDirectionalBlock.FACING))) {
            box = box.minmax(new AABB(part));
        }
        return box;
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        final CompoundTag tag = super.getUpdateTag(registries);
        final long state = visualState();
        tag.putInt("VisualHardware", (int) (state >>> 8));
        tag.putInt("VisualSystems", (int) ((state >>> 4) & 0xF));
        tag.putInt("VisualFlags", (int) (state & 0xF));
        return tag;
    }

    @Override
    public void onDataPacket(final Connection connection,
                             final ClientboundBlockEntityDataPacket packet,
                             final HolderLookup.Provider registries) {
        super.onDataPacket(connection, packet, registries);
        final CompoundTag tag = packet.getTag();
        if (tag != null) {
            clientHardwareMask = tag.getInt("VisualHardware");
            clientDiskSystemMask = tag.getInt("VisualSystems");
            clientFlags = tag.getInt("VisualFlags");
        }
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag, final HolderLookup.Provider registries) {
        /*
         * A chunk arriving carries the same three numbers as a live update; without this the cabinet
         * would render empty until something changed and pushed a packet.
         */
        super.handleUpdateTag(tag, registries);
        clientHardwareMask = tag.getInt("VisualHardware");
        clientDiskSystemMask = tag.getInt("VisualSystems");
        clientFlags = tag.getInt("VisualFlags");
    }

    /** Which network this Mainframe owns, who else is claiming it, and whose turn it is to run it. */
    private final MainframeNetworking networking = new MainframeNetworking(this);
    @Nullable
    private NetworkUuid nativeNetworkUuid;

    @Nullable
    private OperationDispatch dispatch;
    private int dispatchQueues;
    /** The holds this Mainframe was saved with, waiting for the catalog to be read before being taken again. */
    private Map<StorageKey, Long> heldOnLoad;

    private final NetworkIndex networkIndex =
            new NetworkIndex();
    /** What the network is carrying out, whose turn it is, and what became of each: all of it lives here. */
    private final MainframeScheduler scheduler = new MainframeScheduler(this);
    /** What the network knows how to make, and the making of it. */
    private final MainframeCrafts crafts = new MainframeCrafts(this);
    // Operations that were in flight when the world was saved, waiting for the first booted tick to resume.
    @Nullable
    private ListTag pendingOperations;
    /** The game time the pending list was written at, so the resume can tell how long it sat unresumed. */
    private long pendingSavedAt;
    private int resumeCountdown;
    // Ticks to let the storage index and the switch surveys settle after a boot before resuming operations.
    private static final int RESUME_DELAY_TICKS = 20;
    // Set when the block is being destroyed, so setRemoved can tell a break (discard) from a chunk unload (keep).
    private boolean broken;

    /** The software installed on this Mainframe: the IQL Engine, the Automation Engine and the Mirror. */
    private final MainframeServices services = new MainframeServices(this);

    private static final int OPERATION_LOG_MAX = 32;
    private static final int FAILOVER_PROMOTE_DELAY = 60;
    private final Deque<OperationRecord>
            operationLog = new ArrayDeque<>();

    public MainframeBlockEntity(final BlockPos pos, final BlockState state) {
        super(ComputingModule.MAINFRAME_BE.get(), pos, state, LAYOUT);
    }

    @Override
    protected Set<FormFactor> acceptedFormFactors() {
        // A Mainframe takes the MTX board of every shipped era.
        return Set.of(FormFactor.MTX);
    }

    /**
     * A Mainframe accepts only an MTX board of its own era: a board of a different era is neither
     * installable nor counted in the build.
     */
    @Override
    protected HardwareEra requiredBoardEra() {
        return blockEra();
    }

    private HardwareEra blockEra() {
        return getBlockState().getBlock()
                instanceof MainframeBlock mf
                ? mf.era()
                : HardwareEra.STANDARD;
    }

    /*
     * Slot validity is governed by the inherited instance isValidForSlot, which the acceptedFormFactors
     * override above ties to MTX-only boards over the CPU/RAM/PCIe/disk ranges of the shared layout.
     */

    /*
     * The hardware handler is inherited; this shim keeps the historic public name so the Menu/Screen,
     * block drops and GameTests address it unchanged.
     */
    public ItemStackHandler getInventory() {
        return getHardware();
    }

    @Override
    protected String hardwareNbtKey() {
        // Preserve the historic key so every existing saved Mainframe keeps its installed hardware.
        return "Inventory";
    }

    public LocalStore localStore() {
        final List<ItemStack> disks = new ArrayList<>(DISK_SLOTS);
        for (int i = 0; i < DISK_SLOTS; i++) {
            disks.add(getHardware().getStackInSlot(DISK_SLOTS_START + i));
        }
        return new LocalStore(disks, this::setChanged);
    }

    /**
     * The net storage capacity in item-equivalents after subtracting the installed OS footprint.
     * This is the capacity available for data held on the network and in the local store; the OS
     * occupies disk space from installation.
     */
    public long storageItems() {
        final ComputerBuild build = currentBuild();
        if (build == null) {
            return 0L;
        }
        return Math.max(0L, build.totalStorageItems() - reservedByOs());
    }

    public int parallelQueues() {
        return buildValid() ? currentBuild().parallelQueues() : 0;
    }

    // Network connection

    public static void serverTick(final Level level, final BlockPos pos,
                                  final BlockState state, final MainframeBlockEntity be) {
        if (level instanceof ServerLevel serverLevel) {
            be.tickTerminal(serverLevel);
            /*
             * Before tick(), which returns early on a powered-down machine: a cabinet that was just
             * switched off still has to put its lamps out on the client, and a cabinet coming up has a
             * self-test to carry along, which the base tick does for every other kind of computer.
             */
            be.tickBootPhases(serverLevel);
            OsInstallRunner.tick(be, serverLevel, be.getBlockPos());
            be.syncVisualsIfChanged();
            be.tick(serverLevel);
        }
    }

    private void tick(final ServerLevel level) {
        nativeNetworkUuid(); // the mainframe owns a network identity from placement on
        /*
         * The Mainframe runs its own tick rather than the base's node tick, so the programs a player
         * wrote are ticked here as well, and told to stop when the cabinet is switched off.
         */
        tickSigma();
        if (!isRunning()) {
            networking.leave(level);
            closeDispatch();
            return;
        }
        // Whatever the machine is setting up copies on while it is up, network or no network.
        SetupRunner.tick(this, level, worldPosition);
        networking.update(level);
        if (networking.conflicted()) {
            // A contested network collapses: discard every in-flight Operation (its progress is
            closeDispatch();
            return;
        }
        if (networking.standingBy()) {
            /*
             * A Passive standby holds no dispatcher and runs no Operations until it is promoted; the
             * Active member owns the network. closeDispatch settles anything left from a demotion.
             */
            closeDispatch();
            return;
        }
        if (!hasOs()) {
            /*
             * Network orchestration requires a booted OS. Without one the Mainframe holds its network
             * UUID and topology but skips the dispatcher, index, Operation processing, and the IQL job
             * agent. Any submitted Operations remain PENDING until an OS is installed; on the tick when
             * the OS becomes present the dispatcher picks them up automatically (self-healing).
             */
            return;
        }
        runDispatch();
        // Reconcile the in-RAM storage catalog with the network's servers: a changes-only ANALYZE
        networkIndex.analyzeIncremental(level, networkUuid());
        /*
         * The holds a player put on the storage last: taken again now that the catalog has been read, since
         * a hold lasts until somebody lets it go and closing a world is not somebody letting it go.
         */
        if (heldOnLoad != null) {
            networkIndex.restoreManualLocks(heldOnLoad);
            heldOnLoad = null;
        }
        restorePendingOperations(level);
        scheduler.tick();
        // The IQL Engine's job agent fires scheduled/conditional jobs (no-op unless the Engine runs).
        services.tick(level);
    }

    public NetworkIndex networkIndex() {
        return networkIndex;
    }

    /*
     * Network OWNERSHIP: the Mainframe orchestrates its own network instead of reading one from a
     * single cable, so the base's passive registerNode/unregisterNode/tickNode path is unused here.
     */

    @Override
    protected void registerNode(final NetworkSystem system, final NetworkUuid network) {
        /*
         * No-op: the Mainframe owns and orchestrates its network through updateNetwork/orchestrate
         * rather than registering as a passive member; the base's tickNode is never invoked for it.
         */
    }

    @Override
    protected void unregisterNode(final NetworkSystem system, final NetworkUuid network) {
        // No-op: ownership teardown runs through unregister(NetworkSystem)/onBroken(), not this hook.
    }

    public boolean submitOperation(final IOperationTask task, final OperationPriority priority) {
        if (dispatch == null || !isRunning()) {
            return false;
        }
        dispatch.submit(task, priority);
        return true;
    }

    /** What a Mainframe being broken leaves behind, which the networking side decides. */
    public void onBroken() {
        broken = true;
        if (level instanceof ServerLevel serverLevel) {
            networking.onBroken(serverLevel);
        }
    }

    /** The base class's record of which network this computer is attached to and registered on. */
    NetworkAttachment networkAttachment() {
        return attachment();
    }

    /** Whether a cable of that tier is one this Mainframe will talk over. */
    boolean acceptsDataTier(final DataTier tier) {
        return acceptsTier(tier);
    }

    // Operation dispatch (the virtual-thread runtime)

    private void runDispatch() {
        final int queues = Math.max(1, pooledQueues());
        if (dispatch == null) {
            dispatch = new OperationDispatch(queues);
            dispatchQueues = queues;
        } else if (dispatchQueues != queues) {
            /*
             * The GPU count changed (a hot-swap): resize the dispatcher's lanes in place. Rebuilding it would run
             * closeDispatch, which abandons every in-flight Operation, so pulling a GPU mid-craft would discard
             * the craft and leave its machines stranded. Resizing keeps the active Operations running untouched.
             */
            dispatch.setParallelQueues(queues);
            dispatchQueues = queues;
        }
        dispatch.tick();
    }

    /**
     * The virtual-thread dispatcher, created on demand. A timed Operation submitted between ticks (from
     * a terminal, the CLI or a bus) parks each disk's read latency on it; the per-tick reconciliation in
     * {@link #runDispatch()} still owns recreating it when the parallel-queue count changes.
     */
    private OperationDispatch ensureDispatch() {
        if (dispatch == null) {
            final int queues = Math.max(1, pooledQueues());
            dispatch = new OperationDispatch(queues);
            dispatchQueues = queues;
        }
        return dispatch;
    }

    private void closeDispatch() {
        closeDispatch(false);
    }

    /**
     * Tears the dispatcher down. With {@code keepPersistent}, the resumable Operations are dropped WITHOUT
     * being abandoned or logged: this is the chunk-unload path, where the block entity was just written to
     * disk with those Operations inside it, and abandoning them here would move items after the save (a
     * craft returning its pool), items the resumed Operation would move again on reload.
     */
    private void closeDispatch(final boolean keepPersistent) {
        if (dispatch != null) {
            /*
             * Fold the dying dispatcher's tally into the persisted lifetime total so the
             * completed count carries across power cycles and chunk unloads.
             */
            scheduler.completedTotal(scheduler.completedTotal() + dispatch.completedCount());
            dispatch.close();
            dispatch = null;
            dispatchQueues = 0;
            /*
             * Settle every in-flight multi-tick Operation first, so a holder polling isDone() (an
             * INSERT returning leftover, a SELECT freeing its lock) recovers; then record each as
             * DISCARDED so a conflict or power-off leaves a trace in the log instead of vanishing.
             */
            for (final var operation : scheduler.live()) {
                if (keepPersistent && operation instanceof IPersistentOperation persistent
                        && !persistent.isEphemeral()) {
                    continue; // already saved with the block entity; it resumes on reload
                }
                operation.abandon();
                if (operation.silent()) {
                    continue; // its record travels with the Operation it turned into
                }
                recordOperation(operation.toRecord().withStatus(OperationRecord.STATUS_DISCARDED));
                scheduler.postDiscarded(operation);
            }
            scheduler.clear();
            // The index lives in RAM: powering off clears the catalog, rebuilt on the next start.
            networkIndex.clear();
        }
    }

    public int submitSelfTest(final int count, final int workUnits) {
        if (dispatch == null || !isRunning()) {
            return 0;
        }
        final SelfTestOperationTask task = new SelfTestOperationTask(workUnits);
        for (int i = 0; i < count; i++) {
            dispatch.submit(task, OperationPriority.MEDIUM);
        }
        return count;
    }

    public int pendingOps() {
        return scheduler.queuedCount(Math.max(1, pooledQueues()))
                + (dispatch == null ? 0 : dispatch.pendingCount());
    }

    public int runningOps() {
        return scheduler.runningCount(Math.max(1, pooledQueues()))
                + (dispatch == null ? 0 : dispatch.runningCount());
    }

    public long completedOps() {
        return scheduler.completedTotal() + (dispatch == null ? 0L : dispatch.completedCount());
    }

    public void recordOperation(final byte type, final ItemStack icon, final long requested,
                                final long moved, final byte status,
                                final List<OperationRecord.MoveRow>
                                        moves) {
        recordOperation(new OperationRecord(
                type, StorageKey.of(icon),
                requested, moved, status, List.copyOf(moves)));
    }

    public void recordOperation(
            final OperationRecord record) {
        operationLog.addFirst(record);
        while (operationLog.size() > OPERATION_LOG_MAX) {
            operationLog.removeLast();
        }
        setChanged();
    }

    // Multi-tick network Operations (decomposed into SubOperations)

    @Nullable
    public NetworkSelectOperation submitNetworkSelect(
            final StorageKey key, final long demand,
            final IDataSink destination, final String destinationLabel) {
        return submitPull(key, demand, destination, destinationLabel,
                OperationRecord.TYPE_SELECT, null);
    }

    @Nullable
    public NetworkSelectOperation submitNetworkSelect(
            final Item item, final long demand,
            final IDataSink destination, final String destinationLabel) {
        return submitNetworkSelect(StorageKey.of(item),
                demand, destination, destinationLabel);
    }

    @Nullable
    public NetworkSelectOperation submitNetworkSelect(
            final StorageKey key, final long demand,
            final IDataSink destination, final String destinationLabel,
            final Set<NodeUuid> sources) {
        return submitPull(key, demand, destination, destinationLabel,
                OperationRecord.TYPE_SELECT, sources);
    }

    @Nullable
    public NetworkSelectOperation submitNetworkSelect(
            final Item item, final long demand,
            final IDataSink destination, final String destinationLabel,
            final Set<NodeUuid> sources) {
        return submitNetworkSelect(StorageKey.of(item),
                demand, destination, destinationLabel, sources);
    }

    @Nullable
    public NetworkSelectOperation submitNetworkMove(
            final StorageKey key, final long demand,
            final IDataSink destination, final String destinationLabel,
            final Set<NodeUuid> sources) {
        return submitPull(key, demand, destination, destinationLabel,
                OperationRecord.TYPE_MOVE, sources);
    }

    @Nullable
    public NetworkSelectOperation submitNetworkDelete(
            final StorageKey key, final long demand,
            final IDataSink destination, final String destinationLabel) {
        return submitPull(key, demand, destination, destinationLabel,
                OperationRecord.TYPE_DELETE, null);
    }

    @Nullable
    public NetworkSelectOperation submitNetworkDelete(
            final Item item, final long demand,
            final IDataSink destination, final String destinationLabel) {
        return submitNetworkDelete(StorageKey.of(item),
                demand, destination, destinationLabel);
    }

    @Nullable
    private NetworkSelectOperation submitPull(
            final StorageKey key, final long demand,
            final IDataSink destination, final String destinationLabel,
            final byte recordType,
            final Set<NodeUuid> sources) {
        /*
         * A SELECT/MOVE/DELETE also needs the dispatcher; without an OS the Operation would never tick and would
         * just pile up in activeOperations. Refuse it so callers no-op cleanly instead of accumulating dead work.
         */
        if (!isRunning() || !hasOs() || !(level instanceof ServerLevel serverLevel) || networkUuid() == null) {
            return null;
        }
        final var operation = new NetworkSelectOperation(
                serverLevel, networkUuid(), key, demand, destination, destinationLabel, recordType,
                UUID.randomUUID(), networkIndex, ensureDispatch(), sources);
        track(operation);
        return operation;
    }

    @Nullable
    public NetworkInsertOperation submitNetworkInsert(
            final StorageKey key, final long demand,
            final String sourceLabel) {
        /*
         * Without a booted OS the dispatcher never ticks (see tick()), so an Operation submitted here would
         * sit forever in activeOperations holding items the caller already took out of the world. Refuse it so
         * callers hit their op == null branch and return the items to the player instead of losing them.
         */
        if (!isRunning() || !hasOs() || !(level instanceof ServerLevel serverLevel) || networkUuid() == null) {
            return null;
        }
        final var operation = new NetworkInsertOperation(
                serverLevel, networkUuid(), key, demand, sourceLabel, networkIndex, ensureDispatch());
        track(operation);
        return operation;
    }

    @Nullable
    public NetworkInsertOperation submitNetworkInsert(
            final Item item, final long demand, final String sourceLabel) {
        return submitNetworkInsert(StorageKey.of(item),
                demand, sourceLabel);
    }

    // CRAFT: recursive autocrafting over the network's Crafting Computers

    public List<BlockPos> craftingComputerPositions() {
        return crafts.craftingComputers();
    }

    public List<BlockPos> supercomputerPositions() {
        return crafts.supercomputers();
    }

    /**
     * The network's parallel craft-slot capacity from its online supercomputers, as {@code [used, total]}.
     * The Tasks view uses it to show how many crafts can run at once and how many are currently running.
     */
    public int[] supercomputerCraftSlots() {
        return crafts.craftSlots();
    }

    public List<CraftingPattern> networkPatterns() {
        return crafts.patterns();
    }

    /** The plain machine (processing) patterns on the network, for the recursive craft planner. */
    public List<ProcessingPattern> networkProcessingPatterns() {
        return crafts.processingPatterns();
    }

    /** Every machine recipe (processing / multi-stage) the network's running Crafting Computers hold. */
    public List<NetworkRecipe> networkMachineRecipes() {
        return crafts.machineRecipes();
    }

    @Nullable
    public NetworkCraftOperation submitNetworkCraft(
            final StorageKey key, final long demand,
            final boolean partial, final String requesterLabel) {
        return crafts.craft(key, demand, partial, requesterLabel, null);
    }

    /**
     * Same as {@link #submitNetworkCraft(StorageKey, long, boolean, String)}, but plans with one extra
     * pattern alongside the network's Recipe ROMs. A multi-stage
     * pipeline's bench stage carries its own embedded pattern, so it must craft even when that pattern was
     * never loaded into any Recipe ROM on the network.
     */
    @Nullable
    public NetworkCraftOperation submitNetworkCraft(
            final StorageKey key, final long demand,
            final boolean partial, final String requesterLabel,
            @Nullable final CraftingPattern extraPattern) {
        return crafts.craft(key, demand, partial, requesterLabel, extraPattern);
    }

    /**
     * Runs an already-made plan as a craft. The record keeps the ORIGINAL request: a scaled-down partial
     * run settles as COMPLETED_PARTIAL showing produced vs requested, exactly what the player asked to see.
     */
    @Nullable
    public NetworkCraftOperation submitPlannedCraft(
            final StorageKey key, final long demand,
            final CraftPlanner.Plan plan, final String requesterLabel,
            @Nullable final CraftingPattern extraPattern) {
        return crafts.plannedCraft(key, demand, plan, requesterLabel, extraPattern);
    }

    /**
     * Whether anything on the network produces {@code key}: a bench pattern in a Recipe ROM, or a machine
     * recipe. The cheap answer a prompt needs at once, before the plan itself is made.
     */
    public boolean anythingMakes(final StorageKey key) {
        return crafts.anythingMakes(key);
    }

    /**
     * Runs a machine recipe: feeds a {@link ProcessingPattern}'s
     * inputs into the matching machine (declared on a Crafting Switch) and collects its outputs back into the
     * network, until {@code demand} of the primary output is produced or the pattern times out.
     */
    public NetworkProcessingOperation submitNetworkProcessing(
            final ProcessingPattern pattern, final long demand,
            final String requesterLabel) {
        return crafts.machineRun(pattern, demand, requesterLabel, null);
    }

    /**
     * As above, but the step draws its inputs from and returns its outputs to {@code io} instead of the network.
     * A recursive craft passes its own pool here so its machine steps pipeline through the pool (concurrent,
     * race-free) rather than through the shared network; such a step is ephemeral and does not persist a reload.
     */
    public NetworkProcessingOperation submitNetworkProcessing(
            final ProcessingPattern pattern, final long demand,
            final String requesterLabel,
            @Nullable final ICraftIo io) {
        return crafts.machineRun(pattern, demand, requesterLabel, io);
    }

    /** Runs a multi-stage recipe: an ordered pipeline of bench/processing stages, one at a time. */
    public NetworkMultiStageOperation submitNetworkMultiStage(
            final MultiStagePattern pattern, final long demand,
            final String requesterLabel) {
        return crafts.pipeline(pattern, demand, requesterLabel);
    }

    /** The network's bench patterns with {@code preferred} first (when it is one of them, or given at all). */
    public List<CraftingPattern> patternsPreferring(
            @Nullable final CraftingPattern preferred) {
        return crafts.patternsPreferring(preferred);
    }

    /**
     * Every recipe on the network that makes {@code key}, in a stable order: the machine recipes first
     * (processing and multi-stage, in the order the Recipe ROMs hold them), then each bench pattern with that
     * result. A craft dialog lists these so the player can pick one, and the index into this list is what a
     * craft request names; the list only changes when a ROM does.
     */
    public List<NetworkRecipe> recipesFor(
            final StorageKey key) {
        return crafts.recipesFor(key);
    }

    /**
     * The single craft entry point every OS surface (the terminal, the Network Interactor, and the CLI/IQL)
     * routes a request through, so all three behave the same. If a machine recipe on the network produces
     * {@code key} directly, that recipe runs (a processing run, or a multi-stage pipeline); otherwise a recursive
     * bench-and-machine craft is planned. When a machine recipe's own inputs are not all in stock and other
     * patterns can make them, the whole tree runs as one craft with the machine as a step; if nothing can make a
     * missing input, the bare machine run delivers what the network does hold. {@code partial} applies only to
     * the recursive fallback (a machine run always delivers what it can). {@code onSettle}, if given, fires when
     * the resulting operation settles. Returns the operation, or null if nothing on the network makes {@code key}.
     */
    @Nullable
    public INetworkOperation submitCraftRequest(
            final StorageKey key, final long demand,
            final boolean partial, final String label, @Nullable final Runnable onSettle) {
        return submitCraftRequest(key, demand, partial, label, onSettle, true);
    }

    /**
     * As above, run through one particular recipe: {@code recipe} indexes {@link #recipesFor}. A processing
     * recipe runs the machine (or the whole tree when an input is short and something makes it), a multi-stage
     * one runs its pipeline, and a bench one is planned recursively with that pattern first. An index out of
     * range runs the machine's own choice, the way the five-argument form does.
     */
    @Nullable
    public INetworkOperation submitCraftRequest(
            final StorageKey key, final long demand,
            final boolean partial, final String label, @Nullable final Runnable onSettle,
            final int recipe) {
        return crafts.request(key, demand, partial, label, onSettle, recipe);
    }

    /**
     * As above, but {@code preferMultiStage} chooses which recipe wins when an item can be made BOTH by a
     * multi-stage pipeline and by composing the individual step patterns: true runs the multi-stage recipe; false
     * skips it and lets the recursive planner build the tree from the flat patterns. Only affects results that
     * have a multi-stage recipe; everything else routes the same way regardless.
     */
    @Nullable
    public INetworkOperation submitCraftRequest(
            final StorageKey key, final long demand,
            final boolean partial, final String label, @Nullable final Runnable onSettle,
            final boolean preferMultiStage) {
        return crafts.request(key, demand, partial, label, onSettle, preferMultiStage);
    }

    /**
     * Rebuilds the storage catalog from every disk on the network: the disks are read on this tick, the
     * catalog is built on a virtual thread and swapped in whole on a later tick, when {@code onDone} runs.
     * Reads keep the old catalog meanwhile. A Mainframe that is not running rebuilds on the spot instead.
     */
    public void reindexAsync(@Nullable final Runnable onDone) {
        if (!(level instanceof ServerLevel serverLevel) || networkUuid() == null) {
            if (onDone != null) {
                onDone.run();
            }
            return;
        }
        if (!isRunning()) {
            networkIndex.rebuild(serverLevel, networkUuid());
            if (onDone != null) {
                onDone.run();
            }
            return;
        }
        networkIndex.rebuildAsync(serverLevel, networkUuid(), ensureDispatch(), onDone);
    }

    /** Whether the network has a multi-stage recipe whose end result is {@code key} (so a caller can offer the
     *  player the choice between the pipeline and the flat, recursively-planned path). */
    public boolean hasMultiStageRecipe(final StorageKey key) {
        return crafts.hasMultiStageRecipe(key);
    }

    /*
     * Manual LOCK / UNLOCK: player-issued holds on a network item type that make concurrent
     * Operations WAIT, the explicit handle on storage concurrency.
     */

    public long lockType(final StorageKey key,
                         final long demand,
                         @Nullable final Set<NodeUuid> sources) {
        if (!isRunning() || networkUuid() == null) {
            return 0L;
        }
        return networkIndex.manualLock(key, demand, sources);
    }

    public long unlockType(final StorageKey key) {
        return networkIndex.manualUnlock(key);
    }

    public Map<StorageKey, Long> lockedTypes() {
        return networkIndex.manualLockView();
    }

    /** The ceiling on how many jobs of one machine type may run at once, as the crafting side declares it. */
    int maxJobsFor(final String machineKey) {
        return crafts.maxJobsFor(machineKey);
    }

    /** The world's clock, which is what the statistics measure an hour and a day against. */
    long gameTime() {
        return level == null ? 0L : level.getGameTime();
    }

    /** How fast the Crafting Computer at that position drives a machine, or nothing where there is none. */
    long craftingThroughputAt(final BlockPos pos) {
        return level != null && level.getBlockEntity(pos) instanceof CraftingComputerBlockEntity computer
                ? computer.craftingThroughput() : 0L;
    }

    /** Takes an Operation on: the scheduler holds it, ticks it and writes it down when it settles. */
    void takeOn(final INetworkOperation operation) {
        scheduler.track(operation);
    }

    /** The virtual-thread runtime, made if this is the first thing that needs it. */
    OperationDispatch dispatch() {
        return ensureDispatch();
    }

    /**
     * Rebuilds the Operations that were in flight when the world was saved, once the boot has settled (the
     * storage index is analyzed and the Crafting Switch surveys have run). Stage operations are restored
     * first so a multi-stage pipeline can find the stage it was waiting on by id.
     */
    private void restorePendingOperations(final ServerLevel level) {
        if (pendingOperations == null || networkUuid() == null) {
            return;
        }
        if (++resumeCountdown < RESUME_DELAY_TICKS) {
            return;
        }
        final ListTag saved = pendingOperations;
        pendingOperations = null;
        final long savedAt = pendingSavedAt;
        pendingSavedAt = 0L;
        /*
         * Operations that sat unresumed past the expiry have lost whoever wanted them: they are restored just far
         * enough to hand back what they held (a craft's pool goes back to storage), then discarded and logged.
         */
        final long expiry = OperationBalance.orphanedOperationsExpiryTicks();
        final boolean expired = expiry > 0L && savedAt > 0L && level.getGameTime() - savedAt >= expiry;
        final int liveBefore = scheduler.liveCount();
        final HolderLookup.Provider registries = level.registryAccess();
        final Map<UUID, INetworkOperation>
                byId = new HashMap<>();
        final Set<UUID> completedStages = new HashSet<>();
        final List<NetworkMultiStageOperation>
                pipelines = new ArrayList<>();
        /*
         * Crafts that had machine steps in flight re-plan only once every operation is back, so the steps they
         * were running can be found by id and their output counted before the remaining demand is planned.
         */
        final List<CompoundTag> craftsOnMachines = new ArrayList<>();
        for (int i = 0; i < saved.size(); i++) {
            final CompoundTag tag = saved.getCompound(i);
            final UUID savedId = tag.hasUUID(
                    IPersistentOperation.ID_KEY)
                    ? tag.getUUID(IPersistentOperation.ID_KEY)
                    : UUID.randomUUID();
            switch (tag.getString(IPersistentOperation.KIND_KEY)) {
                case NetworkProcessingOperation.KIND -> {
                    final var op = NetworkProcessingOperation
                            .restore(tag, level, networkUuid(), craftingComputerPositions(), registries);
                    if (op != null) {
                        track(op);
                        byId.put(savedId, op);
                    }
                }
                case NetworkCraftOperation.KIND -> {
                    /*
                     * An expired craft is not re-planned after its machine steps: it hands its pool back now and
                     * is discarded with the rest, so it takes the plain restore path below.
                     */
                    if (!expired && !NetworkCraftOperation
                            .savedMachineSteps(tag).isEmpty()) {
                        craftsOnMachines.add(tag);
                        continue;
                    }
                    // submitNetworkCraft already registers the re-planned craft in activeOperations.
                    final var restored = NetworkCraftOperation
                            .restore(tag, this, level, networkUuid(), registries);
                    if (restored.operation() != null) {
                        byId.put(savedId, restored.operation());
                    } else if (restored.complete()) {
                        completedStages.add(savedId);
                    }
                }
                case NetworkMultiStageOperation.KIND -> {
                    final var op = NetworkMultiStageOperation
                            .restore(tag, this, registries);
                    if (op != null) {
                        track(op);
                        pipelines.add(op);
                        byId.put(savedId, op);
                    }
                }
                default -> { }
            }
        }
        for (final CompoundTag tag : craftsOnMachines) {
            final List<NetworkProcessingOperation> steps =
                    new ArrayList<>();
            for (final UUID stepId : NetworkCraftOperation
                    .savedMachineSteps(tag)) {
                if (byId.get(stepId)
                        instanceof NetworkProcessingOperation proc) {
                    steps.add(proc);
                }
            }
            NetworkCraftOperation
                    .restore(tag, this, level, networkUuid(), registries, steps);
        }
        for (final var pipeline : pipelines) {
            final UUID stageId = pipeline.pendingStageId();
            if (stageId != null && completedStages.contains(stageId)) {
                pipeline.skipCompletedStage();
            } else {
                pipeline.adoptStage(stageId == null ? null : byId.get(stageId));
            }
        }
        if (expired) {
            for (int i = liveBefore; i < scheduler.liveCount(); i++) {
                scheduler.liveAt(i).cancel(); // the next operations tick logs each one as DISCARDED
            }
        }
        setChanged();
    }

    /**
     * Stops an in-flight Operation on request: it hands back what it held and is logged as DISCARDED on the
     * next tick. Returns false when no Operation with that id is in flight any more.
     */
    public boolean cancelOperation(final UUID id) {
        final var operation = findOperation(id);
        if (operation == null || operation.isDone()) {
            return false;
        }
        operation.cancel();
        setChanged();
        return true;
    }

    /** The rolling statistics of this Mainframe's Operations. */
    public OperationStatistics statistics() {
        return scheduler.statistics();
    }

    /** Runs {@code work} on the next operations tick, after the storage index has caught up with this one. */
    public void runNextTick(final Runnable work) {
        scheduler.runNextTick(work);
    }

    /** Takes an Operation on: the scheduler holds it, ticks it and writes it down when it settles. */
    private void track(final INetworkOperation operation) {
        scheduler.track(operation);
    }

    /** The operations in flight right now, for views that need the live objects rather than the log. */
    public List<INetworkOperation> liveOperations() {
        return scheduler.live();
    }

    public List<OperationRecord> recentOperations() {
        return List.copyOf(operationLog);
    }

    public List<OperationRecord> activeOperationRecords() {
        return scheduler.liveRecords();
    }

    /** The in-flight Operation with this id, or null when it has settled or never existed. */
    @Nullable
    public INetworkOperation findOperation(final UUID id) {
        return scheduler.find(id);
    }

    /**
     * Changes the scheduling level of an in-flight Operation; the next tick re-grants the queue slots with the
     * new level. Returns false when no Operation with that id is in flight any more.
     */
    public boolean setOperationPriority(final UUID id, final OperationPriority priority) {
        return scheduler.setPriority(id, priority);
    }

    public boolean hasActiveOperations() {
        return scheduler.hasActive();
    }

    public NetworkUuid nativeNetworkUuid() {
        if (nativeNetworkUuid == null) {
            nativeNetworkUuid = NetworkUuid.random();
            setChanged();
        }
        return nativeNetworkUuid;
    }

    public boolean hasNetworkConflict() {
        return networking.conflicted();
    }

    public boolean failoverEnabled() {
        return networking.failoverEnabled();
    }

    public FailoverRole failoverRole() {
        return networking.failoverRole();
    }

    /**
     * Switches this Mainframe between owning a network and standing by for whoever owns it.
     *
     * <p>Becoming a standby gives up the network it had: a machine that is about to back somebody else's
     * cannot go on holding one of its own, or the two would be a conflict the moment they were wired up.
     */
    public void toggleFailover() {
        networking.toggleFailover();
        if (networking.failoverEnabled() && level instanceof ServerLevel serverLevel) {
            networking.eraseOwnedNetwork(serverLevel);
        }
    }

    // IComputerTerminalHost: read-only monitoring for the Monitor terminal

    @Override
    public boolean computerRunning() {
        return isRunning();
    }

    @Override
    public boolean computerBuildValid() {
        return buildValid();
    }

    @Override
    public int networkLinkState() {
        return linkState();
    }

    /** Whether this Mainframe is on a network, on none, or on one somebody else is claiming too. */
    private int linkState() {
        if (networking.conflicted()) {
            return NET_STATE_CONFLICT;
        }
        return networkUuid() != null ? NET_STATE_LINKED : NET_STATE_NONE;
    }

    @Override
    public long orchestrationCapacity() {
        return pooledCapacity();
    }

    @Override
    public int computerQueues() {
        return pooledQueues();
    }

    /**
     * The orchestration capacity the scheduler runs at: this Mainframe's own plus the share every active
     * Subframe on the network lends it. Read from the network registry each tick, so a Subframe powering on
     * or off changes the rate at once.
     */
    public long pooledCapacity() {
        long total = capacity();
        if (networkUuid() != null && level instanceof ServerLevel serverLevel) {
            total += NetworkSystem.get(serverLevel).subframeCapacityOf(networkUuid());
        }
        return total;
    }

    /** The parallel queues the scheduler grants: this Mainframe's (CPU + GPUs) plus the Subframes' GPUs. */
    public int pooledQueues() {
        int total = parallelQueues();
        if (networkUuid() != null && level instanceof ServerLevel serverLevel) {
            total += NetworkSystem.get(serverLevel).subframeQueuesOf(networkUuid());
        }
        return total;
    }

    @Override
    public long computerRamBuffer() {
        return ramBuffer();
    }

    @Override
    public int networkServerCount() {
        if (networkUuid() != null && level instanceof ServerLevel serverLevel) {
            return NetworkSystem.get(serverLevel).serversOf(networkUuid()).size();
        }
        return 0;
    }

    @Override
    public long localStorageUsed() {
        return localStore().used();
    }

    @Override
    public long localStorageCapacity() {
        return storageItems();
    }

    @Override
    public IDataSink localStorage() {
        return new StoreSink(localStore());
    }

    public Map<StorageKey, Long> localSnapshot() {
        return localStore().view();
    }

    @Override
    public int usableStorageSlots() {
        final long capacity = storageItems(); // already net of OS footprint
        return capacity <= 0 ? 0 : (int) Math.min(STORAGE_SLOTS, (capacity + 63) / 64);
    }

    @Override
    public int pendingOperations() {
        return pendingOps();
    }

    @Override
    public int runningOperations() {
        return runningOps();
    }

    @Override
    public int completedOperations() {
        return (int) Math.min(Integer.MAX_VALUE, completedOps());
    }

    @Override
    public int networkPcCount() {
        if (networkUuid() != null && level instanceof ServerLevel serverLevel) {
            return NetworkSystem.get(serverLevel).personalComputersOf(networkUuid()).size();
        }
        return 0;
    }

    @Override
    public int networkSubframeCount() {
        if (networkUuid() != null && level instanceof ServerLevel serverLevel) {
            return NetworkSystem.get(serverLevel).subframesOf(networkUuid()).size();
        }
        return 0;
    }

    @Override
    public boolean isMainframeHost() {
        return true;
    }

    @Override
    public int indexedTypes() {
        return networkIndex.catalogSize();
    }

    @Override
    public int indexedServers() {
        return networkIndex.indexedServerCount();
    }

    @Override
    public int activeLocks() {
        return networkIndex.activeLockCount();
    }

    @Override
    public int indexHealthState() {
        return networkIndex.health().state().id();
    }

    @Override
    public int indexHealthTypeCount() {
        return networkIndex.health().affectedTypes().size();
    }

    @Override
    public long networkStorageUsed() {
        return networkIndex.usedWeight()
                / StorageKey.MB_EQ_PER_ITEM;
    }

    @Override
    public long networkStorageTotal() {
        if (networkUuid() == null || !(level instanceof ServerLevel serverLevel)) {
            return 0L;
        }
        // Counted in items as the racks registered them: what a megabyte holds differs by era, an item does not.
        return NetworkSystem.get(serverLevel).totalStorageItemsOf(networkUuid());
    }

    public static final int DATA_RUNNING = 0;
    public static final int DATA_BUILD_VALID = 1;
    public static final int DATA_CAPACITY = 2;
    public static final int DATA_PARALLEL_QUEUES = 3;
    public static final int DATA_RAM_BUFFER = 4;
    public static final int DATA_AUTOSTART = 5;
    public static final int DATA_MANUAL_ON = 6;
    public static final int DATA_NETWORK_STATE = 7;
    public static final int DATA_PENDING_OPS = 8;
    public static final int DATA_RUNNING_OPS = 9;
    public static final int DATA_COMPLETED_OPS = 10;
    public static final int DATA_FAILOVER_ENABLED = 11;
    public static final int DATA_FAILOVER_ROLE = 12;
    public static final int DATA_COUNT = DATA_FAILOVER_ROLE + 1;

    public static final int NET_STATE_NONE = 0;
    public static final int NET_STATE_LINKED = 1;
    public static final int NET_STATE_CONFLICT = 2;

    private final int[] clientData = new int[DATA_COUNT];

    private int computeData(final int index) {
        return switch (index) {
            case DATA_RUNNING -> isRunning() ? 1 : 0;
            case DATA_BUILD_VALID -> buildValid() ? 1 : 0;
            case DATA_CAPACITY -> (int) Math.min(Integer.MAX_VALUE, capacity());
            case DATA_PARALLEL_QUEUES -> parallelQueues();
            case DATA_RAM_BUFFER -> (int) Math.min(Integer.MAX_VALUE, ramBuffer());
            case DATA_AUTOSTART -> isAutoStart() ? 1 : 0;
            case DATA_MANUAL_ON -> isManualOn() ? 1 : 0;
            case DATA_NETWORK_STATE -> linkState();
            case DATA_PENDING_OPS -> pendingOps();
            case DATA_RUNNING_OPS -> runningOps();
            case DATA_COMPLETED_OPS -> (int) Math.min(Integer.MAX_VALUE, completedOps());
            case DATA_FAILOVER_ENABLED -> networking.failoverEnabled() ? 1 : 0;
            case DATA_FAILOVER_ROLE -> networking.failoverRole().id();
            default -> 0;
        };
    }

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(final int index) {
            if (level != null && level.isClientSide) {
                return index >= 0 && index < clientData.length ? clientData[index] : 0;
            }
            return computeData(index);
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

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        /*
         * Covers both destruction and chunk-unload: stop the virtual-thread executor
         * and drop the registry snapshot, so neither leaks for a mainframe that is gone.
         * This override is authoritative; the base setRemoved's passive onBroken/unregisterNode
         * path is a no-op for the Mainframe (its node hooks are no-ops), so there is no double teardown.
         * A chunk unload keeps the resumable Operations (they are in the saved NBT); a break discards them.
         */
        closeDispatch(!broken);
        if (level instanceof ServerLevel serverLevel) {
            networking.unregisterFrom(NetworkSystem.get(serverLevel));
        }
    }

    @Override
    protected void loadExtra(final CompoundTag tag, final HolderLookup.Provider registries) {
        /*
         * Hardware (under the "Inventory" key), ManualOn, AutoStart, NodeUuid, LinkedMonitors and
         * Console are loaded by the base; only the Mainframe-only state is restored here.
         */
        networking.load(tag);
        servicePanelOff = tag.getBoolean("ServicePanelOff");
        // Kept until the catalog has been read, since a hold can only be taken against what is there.
        heldOnLoad = null;
        if (tag.contains("ManualLocks")) {
            final Map<StorageKey, Long> held = new LinkedHashMap<>();
            final ListTag holds = tag.getList("ManualLocks", Tag.TAG_COMPOUND);
            for (int i = 0; i < holds.size(); i++) {
                final CompoundTag hold = holds.getCompound(i);
                StorageKey.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE),
                                hold.get("key"))
                        .result().ifPresent(key -> held.put(key, hold.getLong("amount")));
            }
            if (!held.isEmpty()) {
                heldOnLoad = held;
            }
        }
        if (tag.contains("NetworkUuid")) {
            nativeNetworkUuid = NetworkUuid.fromString(tag.getString("NetworkUuid"));
        }
        scheduler.completedTotal(tag.getLong("CompletedTotal"));
        operationLog.clear();
        final ListTag ops = tag.getList("OperationLog", Tag.TAG_COMPOUND);
        for (int i = 0; i < ops.size() && i < OPERATION_LOG_MAX; i++) {
            operationLog.addLast(OperationRecord
                    .fromNbt(ops.getCompound(i), registries));
        }
        if (tag.contains("ActiveOperations", Tag.TAG_LIST)) {
            pendingOperations = tag.getList("ActiveOperations", Tag.TAG_COMPOUND).copy();
            pendingSavedAt = tag.getLong("ActiveOperationsSavedAt");
            resumeCountdown = 0;
        }
        services.load(tag);
    }

    @Override
    protected void saveExtra(final CompoundTag tag, final HolderLookup.Provider registries) {
        networking.save(tag);
        tag.putBoolean("ServicePanelOff", servicePanelOff);
        if (nativeNetworkUuid != null) {
            tag.putString("NetworkUuid", nativeNetworkUuid.asString());
        }
        /*
         * Save the full lifetime total (persisted base plus the live dispatcher's tally);
         * the live dispatcher itself is transient, so the snapshot reloads as the new base.
         */
        tag.putLong("CompletedTotal", completedOps());
        if (!operationLog.isEmpty()) {
            final ListTag ops = new ListTag();
            for (final OperationRecord rec : operationLog) {
                ops.add(rec.toNbt(registries));
            }
            tag.put("OperationLog", ops);
        }
        // Operations in flight resume after a reload: save their state (plus any not yet resumed).
        final ListTag inFlight = new ListTag();
        for (final var operation : scheduler.live()) {
            if (operation instanceof IPersistentOperation persistent
                    && !operation.isDone() && !persistent.isEphemeral()) {
                /*
                 * A craft's machine steps read and write its in-memory pool, which does not survive a reload, so
                 * they are not persisted; the parent craft re-plans and re-creates them from the handed-back pool.
                 */
                inFlight.add(persistent.saveState(registries));
            }
        }
        if (pendingOperations != null) {
            inFlight.addAll(pendingOperations);
        }
        if (!inFlight.isEmpty()) {
            tag.put("ActiveOperations", inFlight);
            /*
             * When the saved Operations resume, the time they spent unresumed decides whether they are still
             * wanted: a list that never got to resume since it was loaded keeps its original stamp.
             */
            tag.putLong("ActiveOperationsSavedAt", pendingOperations != null && pendingSavedAt > 0L
                    ? pendingSavedAt : level != null ? level.getGameTime() : 0L);
        }
        /*
         * What a player put a hold on. The reservation itself points at where the things were, and a reload
         * reads the network afresh, so what is kept is what was held and of what; it is taken again once the
         * catalog is back.
         */
        final Map<StorageKey, Long> held = networkIndex.manualLockView();
        if (!held.isEmpty()) {
            final ListTag holds = new ListTag();
            held.forEach((key, amount) -> StorageKey.CODEC
                    .encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), key)
                    .result().ifPresent(encoded -> {
                        final CompoundTag hold = new CompoundTag();
                        hold.put("key", encoded);
                        hold.putLong("amount", amount);
                        holds.add(hold);
                    }));
            tag.put("ManualLocks", holds);
        }
        services.save(tag);
    }

    // IQL Engine (the saved-object service installed on the Mainframe)

    /**
     * The services of a network are the ones a player starts and stops by hand, so this machine answers for its
     * own: one that is installed but stopped is not running, and holds nothing while it is not.
     */
    @Override
    public boolean serviceRunning(final ProgramSpec service) {
        return switch (service.id().getPath()) {
            case "iqlengine" -> isIqlEngineActive();
            case "automation_engine" -> isAutomationEngineActive();
            case "mirror" -> isMirrorActive();
            default -> super.serviceRunning(service);
        };
    }

    public IqlCatalog iqlCatalog() {
        return services.catalog();
    }

    public boolean isIqlEngineInstalled() {
        return services.iqlEngineInstalled();
    }

    public boolean isIqlEngineRunning() {
        return services.iqlEngineRunning();
    }

    /** The Engine is usable only when installed, not stopped, and the Mainframe itself is powered. */
    public boolean isIqlEngineActive() {
        return services.iqlEngineActive();
    }

    /** Installs the Engine on the Mainframe; returns false if it was already installed. */
    public boolean installIqlEngine() {
        return services.installIqlEngine();
    }

    /** Starts or stops the installed Engine service; returns false if there is nothing to change. */
    public boolean setIqlEngineRunning(final boolean running) {
        return services.setIqlEngineRunning(running);
    }

    public boolean isAutomationEngineInstalled() {
        return services.automationEngineInstalled();
    }

    /** Active when installed and the Mainframe is powered; enables the job agent like the IQL Engine does. */
    public boolean isAutomationEngineActive() {
        return services.automationEngineActive();
    }

    /** Installs the Automation Engine on the Mainframe; returns false if it was already installed. */
    public boolean installAutomationEngine() {
        return services.installAutomationEngine();
    }

    public boolean isMirrorInstalled() {
        return services.mirrorInstalled();
    }

    /** Whether the Mirror serves packages: installed and the Mainframe is running. */
    public boolean isMirrorActive() {
        return services.mirrorActive();
    }

    /** Installs the Mirror service on the Mainframe; returns false if it was already installed. */
    public boolean installMirror() {
        return services.installMirror();
    }

    /** How many packages one network's Mirror will hold, so a shelf cannot grow without end. */
    public static final int SHELF_MAX = MainframeServices.SHELF_MAX;

    /** Everything on the Mirror's shelf, by name. */
    public Map<String, String> shelvedPackages() {
        return services.shelvedPackages();
    }

    /** One of them, or null. */
    @Nullable
    public String shelvedPackage(final String name) {
        return services.shelvedPackage(name);
    }

    /** Puts one on the shelf, replacing any build of it already there. */
    public boolean shelve(final String name, final String text) {
        return services.shelve(name, text);
    }

    /** Takes one off the shelf; false when it was not there. */
    public boolean unshelve(final String name) {
        return services.unshelve(name);
    }

    /** Removes the Mirror service; returns false if it was not installed. */
    public boolean uninstallMirror() {
        return services.uninstallMirror();
    }

    /** Removes the IQL Engine service (stopping it); returns false if it was not installed. */
    public boolean uninstallIqlEngine() {
        return services.uninstallIqlEngine();
    }

    /** Removes the Automation Engine service; returns false if it was not installed. */
    public boolean uninstallAutomationEngine() {
        return services.uninstallAutomationEngine();
    }

    @Override
    protected void onSystemErased() {
        super.onSystemErased();
        // The services were software on the formatted disk: a wiped Mainframe serves nothing any more.
        services.eraseInstalls();
    }

    public void markIqlCatalogChanged() {
        setChanged();
    }

    public boolean isJobPaused(final String jobName) {
        return services.jobPaused(jobName);
    }

    /** Pauses a job (a resumable "End"): the agent stops firing it until it is restarted. */
    public void pauseJob(final String jobName) {
        services.pauseJob(jobName);
    }

    /** Restarts a job: resumes it if paused and re-arms its trigger so it reschedules from now. */
    public void restartJob(final String jobName) {
        services.restartJob(jobName);
    }

    /** The persisted NMS editor script for this Mainframe, or "" if none has been saved. */
    public String savedScript() {
        return services.savedScript();
    }

    /** Persists the NMS editor script so it survives closing and reopening the studio (and a reload). */
    public void setSavedScript(final String script) {
        services.savedScript(script);
    }
}
