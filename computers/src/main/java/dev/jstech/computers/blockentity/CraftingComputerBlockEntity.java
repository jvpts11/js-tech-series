/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.block.CraftingComputerBlock;
import dev.jstech.computers.block.DataCableBlock;
import dev.jstech.computers.crafting.CraftingPattern;
import dev.jstech.computers.crafting.NetworkRecipe;
import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.hardware.CraftingCardSpec;
import dev.jstech.computers.hardware.ExpansionCardKind;
import dev.jstech.computers.hardware.IExpansionCardSpec;
import dev.jstech.computers.hardware.FormFactor;
import dev.jstech.computers.storage.IDataSink;
import dev.jstech.computers.storage.LocalStore;
import dev.jstech.computers.storage.StoreSink;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.network.DataTier;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.persistence.SavedValue;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

/**
 * The Crafting Computer: a Category-C computer that executes crafting recipes for the network.
 */
public class CraftingComputerBlockEntity extends AbstractComputerBlockEntity
        implements IComputerTerminalHost {

    /*
     * Slot layout for an ATX board: one CPU, four RAM, four PCIe (GPU and/or Crafting Card), one PSU,
     * two disks. Kept public so the assembly Menu and Screen address slots by name.
     */
    public static final int MOTHERBOARD_SLOT = 0;
    public static final int CPU_SLOT = 1;
    public static final int RAM_SLOTS_START = 2;
    public static final int RAM_SLOTS = 4;
    public static final int PCIE_SLOTS_START = 6;
    public static final int PCIE_SLOTS = 4;
    public static final int PSU_SLOT = 10;
    public static final int DISK_SLOTS_START = 11;
    public static final int DISK_SLOTS = 2;
    public static final int HARDWARE_SLOTS = 13;

    public static final int RECIPE_ROM_LIMIT = 50;

    private static final ComputerHardwareLayout LAYOUT = new ComputerHardwareLayout(
            MOTHERBOARD_SLOT, CPU_SLOT, 1, RAM_SLOTS_START, RAM_SLOTS,
            PCIE_SLOTS_START, PCIE_SLOTS, PSU_SLOT, DISK_SLOTS_START, DISK_SLOTS, HARDWARE_SLOTS);

    public CraftingComputerBlockEntity(final BlockPos pos, final BlockState state) {
        super(ComputingModule.CRAFTING_COMPUTER_BE.get(), pos, state, LAYOUT);
    }

    @Override
    protected Set<FormFactor> acceptedFormFactors() {
        /*
         * A Crafting Computer is a PC-class machine: each era takes its own consumer form factor:
         * Vintage on Baby-AT/AT, Legacy and Standard on ATX.
         */
        return switch (blockEra()) {
            case VINTAGE -> Set.of(FormFactor.BABY_AT, FormFactor.AT);
            default -> Set.of(FormFactor.ATX);
        };
    }

    @Override
    protected HardwareEra requiredBoardEra() {
        /*
         * A Crafting Computer accepts only a board of its own era, so a Legacy and a Standard ATX board
         * are not interchangeable: each installs in its matching machine alone.
         */
        return blockEra();
    }

    /**
     * The era this Crafting Computer belongs to, read from its block. Defaults to Standard for any block that
     * is not a {@link CraftingComputerBlock} (never happens in practice, but keeps the read total).
     */
    private HardwareEra blockEra() {
        return getBlockState().getBlock()
                instanceof CraftingComputerBlock cc
                ? cc.era()
                : HardwareEra.STANDARD;
    }

    public static void serverTick(final Level level, final BlockPos pos,
                                  final BlockState state, final CraftingComputerBlockEntity be) {
        if (level instanceof ServerLevel serverLevel) {
            be.tickNode(serverLevel);
        }
    }

    @Override
    protected void registerNode(final NetworkSystem system, final NetworkUuid network) {
        system.registerCraftingComputer(new NetworkSystem.CraftingComputerNode(
                nodeUuid(), network, capacity(), worldPosition.asLong()));
    }

    @Override
    protected void unregisterNode(final NetworkSystem system, final NetworkUuid network) {
        system.unregisterCraftingComputer(network, nodeUuid());
    }

    // Crafting hardware

    public double craftingCardFactor() {
        final ComputerBuild build = currentBuild();
        if (build == null) {
            return 0.0;
        }
        double factor = 0.0;
        for (final IExpansionCardSpec card : build.cardsOfKind(ExpansionCardKind.CRAFTING)) {
            if (card instanceof CraftingCardSpec craftingCard) {
                factor += craftingCard.cpuFactor();
            }
        }
        return factor;
    }

    public long craftingThroughput() {
        return (long) (capacity() * craftingCardFactor());
    }

    /**
     * The number of crafting stages this computer can run in parallel, summed over its installed crafting cards'
     * thread counts. This is the hardware ceiling on a single craft's concurrent stages: the computer orchestrates
     * its own craft's stages up to this many at once, independent of the Mainframe's operation queues. Zero when no
     * crafting card is installed (the computer cannot craft at all).
     */
    public int craftingThreads() {
        final ComputerBuild build = currentBuild();
        if (build == null) {
            return 0;
        }
        int threads = 0;
        for (final IExpansionCardSpec card : build.cardsOfKind(ExpansionCardKind.CRAFTING)) {
            if (card instanceof CraftingCardSpec craftingCard) {
                threads += craftingCard.threads();
            }
        }
        return threads;
    }

    public boolean canCraft() {
        return isRunning() && craftingCardFactor() > 0.0;
    }

    // Craft execution claim: one craft at a time without a Supercomputer

    private UUID activeCraftId;

    public boolean craftBusy() {
        return activeCraftId != null;
    }

    public boolean tryClaimCraft(final UUID operationId) {
        if (activeCraftId != null && !activeCraftId.equals(operationId)) {
            return false;
        }
        activeCraftId = operationId;
        return true;
    }

    public void releaseCraft(final UUID operationId) {
        if (operationId.equals(activeCraftId)) {
            activeCraftId = null;
        }
    }

    // Recipe ROM: the computer's pattern store, hard-capped at 50

    private final List<CraftingPattern> rom =
            new ArrayList<>();

    /*
     * Machine recipes (processing / multi-stage) share the ROM's slot budget but live in their own list, so the
     * bench-craft path stays untouched. Both count toward RECIPE_ROM_LIMIT.
     */
    private final List<NetworkRecipe> machineRecipes =
            new ArrayList<>();

    public int romUsed() {
        return rom.size() + machineRecipes.size();
    }

    public List<NetworkRecipe> machineRecipes() {
        return Collections.unmodifiableList(machineRecipes);
    }

    public boolean loadMachineRecipe(final NetworkRecipe recipe) {
        if (romUsed() >= RECIPE_ROM_LIMIT) {
            return false;
        }
        for (final var existing : machineRecipes) {
            if (existing.sameRecipe(recipe)) {
                return false;
            }
        }
        machineRecipes.add(recipe);
        setChanged();
        return true;
    }

    public void removeMachineRecipe(final int index) {
        if (index >= 0 && index < machineRecipes.size()) {
            machineRecipes.remove(index);
            setChanged();
        }
    }

    /**
     * Per-machine concurrency settings the Machines tab edits and the engine honors: how many processing jobs
     * may run on a machine at once, whether it is paused, and whether to fill it rather than feed one lot.
     */
    public record MachineConfig(int maxJobs, boolean locked, boolean feedMax) {
        /*
         * maxJobs is an OPTIONAL per-type ceiling on concurrent jobs; 0 means "auto", which uses every machine of the
         * type that exists (the dispatcher gives each job a distinct physical machine, so concurrency already
         * scales with the machines present). A positive value caps below that.
         */
        public static final MachineConfig DEFAULT = new MachineConfig(0, false, false);

        public MachineConfig {
            maxJobs = Math.max(0, maxJobs);
        }
    }

    /*
     * Config lives in one map under two kinds of key: a machine TYPE (e.g. "mekanism:...factory") holds that
     * type's Max Jobs ceiling; a per-PHYSICAL-machine key (see machineStateKey, prefixed "@") holds that one
     * machine's Paused/Feed state. So Max Jobs is set once per type, while a machine can be paused on its own.
     */
    private final Map<String, MachineConfig> machineConfigs = new HashMap<>();

    /** The config key for one physical machine's per-machine state (Paused/Feed), by its world position. */
    public static String machineStateKey(final BlockPos pos) {
        return "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    public MachineConfig machineConfig(final String machineKey) {
        return machineConfigs.getOrDefault(machineKey, MachineConfig.DEFAULT);
    }

    public void setMachineConfig(final String machineKey, final MachineConfig config) {
        if (machineKey == null || machineKey.isBlank() || config == null) {
            return;
        }
        machineConfigs.put(machineKey, config);
        setChanged();
    }

    /**
     * Machines offered by the Crafting Switches wired to this computer over crafting cable, discovered by a BFS
     * through that cable (the mirror of how a switch finds its computer). The engine routes a processing
     * pattern's machine type/name to one of these to deliver inputs and collect outputs.
     */
    public List<CraftingSwitchBlockEntity.DeclaredMachine> availableMachines() {
        final List<CraftingSwitchBlockEntity.DeclaredMachine> out = new ArrayList<>();
        if (!(level instanceof ServerLevel)) {
            return out;
        }
        final Set<BlockPos> visited = new HashSet<>();
        final Deque<BlockPos> queue = new ArrayDeque<>();
        for (final Direction d : Direction.values()) {
            final BlockPos n = worldPosition.relative(d);
            if (visited.add(n)) {
                queue.add(n);
            }
        }
        int steps = 0;
        while (!queue.isEmpty() && steps++ < 128) {
            final BlockPos current = queue.poll();
            if (level.getBlockEntity(current) instanceof CraftingSwitchBlockEntity sw) {
                out.addAll(sw.declaredMachines());
                continue; // a switch terminates the search; do not cross it
            }
            if (level.getBlockState(current).getBlock()
                    instanceof DataCableBlock cable
                    && cable.tier() == DataTier.CRAFTING) {
                for (final Direction d : Direction.values()) {
                    final BlockPos nb = current.relative(d);
                    if (visited.add(nb)) {
                        queue.add(nb);
                    }
                }
            }
        }
        return out;
    }

    public List<CraftingPattern> romPatterns() {
        return Collections.unmodifiableList(rom);
    }

    public boolean romContains(final CraftingPattern pattern) {
        for (final CraftingPattern existing : rom) {
            if (existing.sameRecipe(pattern)) {
                return true;
            }
        }
        return false;
    }

    public boolean loadPattern(final CraftingPattern pattern) {
        // romUsed(), not rom.size(): bench and machine recipes share the one ROM budget.
        if (romUsed() >= RECIPE_ROM_LIMIT || romContains(pattern)) {
            return false;
        }
        rom.add(pattern);
        setChanged();
        return true;
    }

    public void removePattern(final int index) {
        if (index >= 0 && index < rom.size()) {
            rom.remove(index);
            setChanged();
        }
    }

    @Override
    protected void saveExtra(final CompoundTag tag,
                             final HolderLookup.Provider registries) {
        if (!rom.isEmpty()) {
            final var ops = RegistryOps.create(NbtOps.INSTANCE, registries);
            SavedValue.written(CraftingPattern.CODEC.listOf().encodeStart(ops, rom),
                            JsComputers.LOGGER, "this computer's recipes")
                    .ifPresent(encoded -> tag.put("RecipeRom", encoded));
        }
        if (!machineRecipes.isEmpty()) {
            final var ops = RegistryOps.create(NbtOps.INSTANCE, registries);
            SavedValue.written(NetworkRecipe.CODEC.listOf().encodeStart(ops, machineRecipes),
                            JsComputers.LOGGER, "this computer's machine recipes")
                    .ifPresent(encoded -> tag.put("MachineRom", encoded));
        }
        if (!machineConfigs.isEmpty()) {
            final CompoundTag configs = new CompoundTag();
            machineConfigs.forEach((key, cfg) -> {
                final CompoundTag c = new CompoundTag();
                c.putInt("MaxJobs", cfg.maxJobs());
                c.putBoolean("Locked", cfg.locked());
                c.putBoolean("FeedMax", cfg.feedMax());
                configs.put(key, c);
            });
            tag.put("MachineConfigs", configs);
        }
    }

    @Override
    protected void loadExtra(final CompoundTag tag,
                             final HolderLookup.Provider registries) {
        rom.clear();
        if (tag.contains("RecipeRom")) {
            final var ops = RegistryOps.create(NbtOps.INSTANCE, registries);
            SavedValue.read(CraftingPattern.CODEC.listOf().parse(ops, tag.get("RecipeRom")),
                            JsComputers.LOGGER, "this computer's recipes")
                    .ifPresent(rom::addAll);
        }
        machineRecipes.clear();
        if (tag.contains("MachineRom")) {
            final var ops = RegistryOps.create(NbtOps.INSTANCE, registries);
            SavedValue.read(NetworkRecipe.CODEC.listOf().parse(ops, tag.get("MachineRom")),
                            JsComputers.LOGGER, "this computer's machine recipes")
                    .ifPresent(machineRecipes::addAll);
        }
        machineConfigs.clear();
        if (tag.contains("MachineConfigs")) {
            final CompoundTag configs = tag.getCompound("MachineConfigs");
            for (final String key : configs.getAllKeys()) {
                final CompoundTag c = configs.getCompound(key);
                machineConfigs.put(key, new MachineConfig(
                        c.getInt("MaxJobs"), c.getBoolean("Locked"), c.getBoolean("FeedMax")));
            }
        }
    }

    // Screen sync (ContainerData wire layout, single source of truth shared with the Menu)

    public static final int DATA_RUNNING = 0;
    public static final int DATA_BUILD_VALID = 1;
    public static final int DATA_CAPACITY = 2;
    public static final int DATA_RAM_BUFFER = 3;
    public static final int DATA_AUTOSTART = 4;
    public static final int DATA_ON_NETWORK = 5;
    public static final int DATA_CRAFT_FACTOR_X100 = 6;
    public static final int DATA_CRAFT_THROUGHPUT = 7;
    public static final int DATA_ROM_USED = 8;
    public static final int DATA_CRAFT_THREADS = 9;
    public static final int DATA_COUNT = DATA_CRAFT_THREADS + 1;

    private final int[] clientData = new int[DATA_COUNT];

    private int computeData(final int index) {
        return switch (index) {
            case DATA_RUNNING -> isRunning() ? 1 : 0;
            case DATA_BUILD_VALID -> buildValid() ? 1 : 0;
            case DATA_CAPACITY -> (int) Math.min(Integer.MAX_VALUE, capacity());
            case DATA_RAM_BUFFER -> (int) Math.min(Integer.MAX_VALUE, ramBuffer());
            case DATA_AUTOSTART -> isAutoStart() ? 1 : 0;
            case DATA_ON_NETWORK -> networkUuid() != null ? 1 : 0;
            case DATA_CRAFT_FACTOR_X100 -> (int) Math.round(craftingCardFactor() * 100.0);
            case DATA_CRAFT_THROUGHPUT -> (int) Math.min(Integer.MAX_VALUE, craftingThroughput());
            case DATA_ROM_USED -> romUsed();
            case DATA_CRAFT_THREADS -> craftingThreads();
            default -> 0;
        };
    }

    private final ContainerData dataAccess =
            new ContainerData() {
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

    /*
     * IComputerTerminalHost: read-only monitoring so the Network Interactor works on a Crafting Computer
     * (the storage/hardware getters are inherited from the base; only these computer-semantic ones differ).
     */

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
        return networkUuid() != null ? 1 : 0; // a Crafting Computer never conflicts; it only reads a network
    }

    @Override
    public long orchestrationCapacity() {
        return capacity();
    }

    @Override
    public int computerQueues() {
        return isRunning() ? 1 : 0;
    }

    @Override
    public long computerRamBuffer() {
        return ramBuffer();
    }

    @Override
    public boolean isMainframeHost() {
        return false;
    }

    @Override
    public int networkServerCount() {
        if (networkUuid() == null || !(level instanceof ServerLevel serverLevel)) {
            return 0;
        }
        return NetworkSystem.get(serverLevel).serversOf(networkUuid()).size();
    }

    @Override
    public LocalStore localStore() {
        final List<ItemStack> disks = new ArrayList<>(DISK_SLOTS);
        for (int i = 0; i < DISK_SLOTS; i++) {
            disks.add(getHardware().getStackInSlot(DISK_SLOTS_START + i));
        }
        return new LocalStore(disks, this::setChanged);
    }

    /** Net local-storage capacity in item-equivalents, after the installed OS footprint. */
    private long netStorageItems() {
        final ComputerBuild build = currentBuild();
        return build == null ? 0L : Math.max(0L, build.totalStorageItems() - reservedByOs());
    }

    @Override
    public int usableStorageSlots() {
        final long capacity = netStorageItems();
        return capacity <= 0 ? 0 : (int) Math.min(18, (capacity + 63) / 64);
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
}
