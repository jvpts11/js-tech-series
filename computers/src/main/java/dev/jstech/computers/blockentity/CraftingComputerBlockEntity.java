/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.CraftingComputerBlock;
import dev.jstech.computers.block.part.CraftingInterfacePart;
import dev.jstech.computers.crafting.CraftingFloor;
import dev.jstech.computers.crafting.CraftingPattern;
import dev.jstech.computers.crafting.NetworkRecipe;
import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.hardware.CraftingCardSpec;
import dev.jstech.computers.hardware.ExpansionCardKind;
import dev.jstech.computers.hardware.FormFactor;
import dev.jstech.computers.hardware.IExpansionCardSpec;
import dev.jstech.computers.item.CraftingCardItem;
import dev.jstech.computers.storage.IDataSink;
import dev.jstech.computers.storage.LocalStore;
import dev.jstech.computers.storage.StoreSink;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.blockentity.DerivedInt;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The Crafting Computer: a Category-C computer that executes crafting recipes for the network. Its Crafting Cards keep
 * the bench recipes it crafts, each card its own few, and drive the Crafting Interfaces on its crafting cable, which
 * hold the processing recipes and feed the machines; what it can make is what its cards and its interfaces hold.
 */
public class CraftingComputerBlockEntity extends AbstractSmallComputerBlockEntity
        implements IComputerTerminalHost {

    /* The craft it runs right now: one at a time without a Supercomputer. */
    @Nullable
    private UUID activeCraftId;
    /* The crafting network as last read, and when; read again once it is older than FLOOR_FRESH_TICKS. */
    @Nullable
    private CraftingFloor floor;
    private long floorReadAt = Long.MIN_VALUE;

    /* Values the assembly menu shows: worked out on the server, received by the client while the menu is open. */
    private final DerivedInt assemblyRunning = fields().derived("AssemblyRunning", this::isRunning).toMenu();
    private final DerivedInt assemblyBuildValid = fields().derived("AssemblyBuildValid", this::buildValid).toMenu();
    private final DerivedInt assemblyCapacity = fields().derived("AssemblyCapacity",
            () -> (int) Math.min(Integer.MAX_VALUE, capacity())).toMenu();
    private final DerivedInt assemblyRamBuffer = fields().derived("AssemblyRamBuffer",
            () -> (int) Math.min(Integer.MAX_VALUE, ramBuffer())).toMenu();
    private final DerivedInt assemblyAutoStart = fields().derived("AssemblyAutoStart", this::isAutoStart).toMenu();
    private final DerivedInt assemblyOnNetwork = fields().derived("AssemblyOnNetwork",
            () -> networkUuid() != null).toMenu();
    private final DerivedInt assemblyCraftFactorX100 = fields().derived("AssemblyCraftFactorX100",
            () -> (int) Math.round(craftingCardFactor() * 100.0)).toMenu();
    private final DerivedInt assemblyCraftThroughput = fields().derived("AssemblyCraftThroughput",
            () -> (int) Math.min(Integer.MAX_VALUE, craftingThroughput())).toMenu();
    private final DerivedInt assemblyRomUsed = fields().derived("AssemblyRomUsed", this::romUsed).toMenu();
    private final DerivedInt assemblyRomSize = fields().derived("AssemblyRomSize", this::romSize).toMenu();
    private final DerivedInt assemblyCraftThreads = fields().derived("AssemblyCraftThreads",
            this::craftingThreads).toMenu();
    private final DerivedInt assemblyInterfaces = fields().derived("AssemblyInterfaces",
            this::interfaceBudget).toMenu();

    /*
     * Slot layout for an ATX board: one CPU, four RAM, four expansion slots (GPU and/or Crafting Card), one PSU,
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

    /** How long, in ticks, the crafting network this computer reads stays as it was last read. */
    public static final int FLOOR_FRESH_TICKS = 20;

    private static final ComputerHardwareLayout LAYOUT = new ComputerHardwareLayout(
            MOTHERBOARD_SLOT, CPU_SLOT, 1, RAM_SLOTS_START, RAM_SLOTS,
            PCIE_SLOTS_START, PCIE_SLOTS, PSU_SLOT, DISK_SLOTS_START, DISK_SLOTS, HARDWARE_SLOTS);

    public CraftingComputerBlockEntity(final BlockPos pos, final BlockState state) {
        super(ComputingModule.CRAFTING_COMPUTER_BE.get(), pos, state, LAYOUT);
    }

    public static void serverTick(final Level level, final BlockPos pos,
                                  final BlockState state, final CraftingComputerBlockEntity be) {
        if (level instanceof ServerLevel serverLevel) {
            be.tickNode(serverLevel);
        }
    }

    // Crafting hardware

    public double craftingCardFactor() {
        double factor = 0.0;
        for (final CraftingCardSpec card : craftingCards()) {
            factor += card.cpuFactor();
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
        int threads = 0;
        for (final CraftingCardSpec card : craftingCards()) {
            threads += card.threads();
        }
        return threads;
    }

    /** How many Crafting Interfaces this computer drives, its cards' together; none while it is off. */
    public int interfaceBudget() {
        if (!isRunning()) {
            return 0;
        }
        int budget = 0;
        for (final CraftingCardSpec card : craftingCards()) {
            budget += card.interfaces();
        }
        return budget;
    }

    public boolean canCraft() {
        return isRunning() && craftingCardFactor() > 0.0;
    }

    // Craft execution claim: one craft at a time without a Supercomputer

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

    // The bench recipes, kept in the ROM of each Crafting Card

    /** The expansion slots that hold a Crafting Card, in slot order. */
    public List<Integer> cardSlots() {
        final List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < PCIE_SLOTS; i++) {
            if (getHardware().getStackInSlot(PCIE_SLOTS_START + i).getItem() instanceof CraftingCardItem) {
                slots.add(PCIE_SLOTS_START + i);
            }
        }
        return slots;
    }

    /** The card in hardware slot {@code slot}, or an empty stack. */
    public ItemStack cardIn(final int slot) {
        final ItemStack stack = slot >= PCIE_SLOTS_START && slot < PCIE_SLOTS_START + PCIE_SLOTS
                ? getHardware().getStackInSlot(slot) : ItemStack.EMPTY;
        return stack.getItem() instanceof CraftingCardItem ? stack : ItemStack.EMPTY;
    }

    /** Every bench recipe the cards keep, card by card in slot order. */
    public List<CraftingPattern> romPatterns() {
        final List<CraftingPattern> all = new ArrayList<>();
        for (final int slot : cardSlots()) {
            all.addAll(CraftingCardItem.rom(getHardware().getStackInSlot(slot)));
        }
        return all;
    }

    /** How many bench recipes the cards keep together. */
    public int romUsed() {
        return romPatterns().size();
    }

    /** How many bench recipes the cards keep at most together. */
    public int romSize() {
        int size = 0;
        for (final int slot : cardSlots()) {
            size += CraftingCardItem.romSize(getHardware().getStackInSlot(slot));
        }
        return size;
    }

    public boolean romContains(final CraftingPattern pattern) {
        for (final CraftingPattern existing : romPatterns()) {
            if (existing.sameRecipe(pattern)) {
                return true;
            }
        }
        return false;
    }

    /** Writes {@code pattern} into the first card with room; false when every card is full or one keeps it. */
    public boolean loadPattern(final CraftingPattern pattern) {
        if (romContains(pattern)) {
            return false;
        }
        for (final int slot : cardSlots()) {
            if (loadPatternInto(slot, pattern)) {
                return true;
            }
        }
        return false;
    }

    /** Writes {@code pattern} into the card in hardware slot {@code slot}; false when it is full or none is there. */
    public boolean loadPatternInto(final int slot, final CraftingPattern pattern) {
        final ItemStack card = cardIn(slot);
        if (card.isEmpty() || romContains(pattern) || !CraftingCardItem.load(card, pattern)) {
            return false;
        }
        setChanged();
        return true;
    }

    /** Erases the bench recipe at {@code index} of {@link #romPatterns()}. */
    public void removePattern(final int index) {
        int at = index;
        for (final int slot : cardSlots()) {
            final ItemStack card = getHardware().getStackInSlot(slot);
            final int held = CraftingCardItem.rom(card).size();
            if (at < held) {
                if (CraftingCardItem.remove(card, at)) {
                    setChanged();
                }
                return;
            }
            at -= held;
        }
    }

    // The processing recipes, held by the interfaces this computer drives

    /**
     * The crafting network on this computer's crafting cable, read at most once every {@link #FLOOR_FRESH_TICKS}, or
     * null on a player's game.
     */
    @Nullable
    public CraftingFloor floor() {
        if (!(level instanceof ServerLevel server)) {
            return null;
        }
        final long now = server.getGameTime();
        if (floor == null || now - floorReadAt >= FLOOR_FRESH_TICKS || now < floorReadAt) {
            floor = CraftingFloor.around(server, worldPosition);
            floorReadAt = now;
        }
        return floor;
    }

    /** Reads the crafting network again at once: a part was placed, taken off or set. */
    public void forgetFloor() {
        floor = null;
    }

    /** The interfaces this computer drives, in the order they are driven. */
    public List<CraftingFloor.Site> drivenInterfaces() {
        final CraftingFloor here = floor();
        if (here == null) {
            return List.of();
        }
        final List<CraftingFloor.Site> mine = new ArrayList<>();
        for (final CraftingFloor.Site site : here.driven()) {
            if (worldPosition.equals(here.drivenBy(site))) {
                mine.add(site);
            }
        }
        return mine;
    }

    /** Every processing and pipeline recipe the interfaces this computer drives hold, each once. */
    public List<NetworkRecipe> machineRecipes() {
        final List<NetworkRecipe> out = new ArrayList<>();
        if (!(level instanceof ServerLevel server)) {
            return out;
        }
        for (final CraftingFloor.Site site : drivenInterfaces()) {
            final CraftingInterfacePart part = site.part(server, CraftingInterfacePart.class);
            if (part == null) {
                continue;
            }
            for (final CraftingInterfacePart.HeldPattern held : part.patterns()) {
                if (out.stream().noneMatch(r -> r.sameRecipe(held.recipe()))) {
                    out.add(held.recipe());
                }
            }
        }
        return out;
    }

    /**
     * Places {@code recipe} in the first interface this computer drives that has room for it and does not hold it;
     * the interface it went into, or null when none took it.
     */
    @Nullable
    public CraftingInterfacePart loadMachineRecipe(final NetworkRecipe recipe) {
        if (!(level instanceof ServerLevel server)) {
            return null;
        }
        for (final CraftingFloor.Site site : drivenInterfaces()) {
            final CraftingInterfacePart part = site.part(server, CraftingInterfacePart.class);
            if (part != null && part.place(recipe)) {
                return part;
            }
        }
        return null;
    }

    public boolean assemblyRunning() {
        return assemblyRunning.isSet();
    }

    public boolean assemblyBuildValid() {
        return assemblyBuildValid.isSet();
    }

    public long assemblyCapacity() {
        return assemblyCapacity.getAsInt();
    }

    public long assemblyRamBuffer() {
        return assemblyRamBuffer.getAsInt();
    }

    public boolean assemblyAutoStart() {
        return assemblyAutoStart.isSet();
    }

    public boolean assemblyOnNetwork() {
        return assemblyOnNetwork.isSet();
    }

    public int assemblyCraftFactorX100() {
        return assemblyCraftFactorX100.getAsInt();
    }

    public long assemblyCraftThroughput() {
        return assemblyCraftThroughput.getAsInt();
    }

    public int assemblyRomUsed() {
        return assemblyRomUsed.getAsInt();
    }

    public int assemblyRomSize() {
        return assemblyRomSize.getAsInt();
    }

    public int assemblyCraftThreads() {
        return assemblyCraftThreads.getAsInt();
    }

    public int assemblyInterfaces() {
        return assemblyInterfaces.getAsInt();
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

    @Override
    protected Set<FormFactor> acceptedFormFactors() {
        /*
         * A Crafting Computer is a PC-class machine: each era takes its own consumer form factor:
         * Vintage on Baby-AT/AT, the later eras on ATX.
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

    @Override
    protected void registerNode(final NetworkSystem system, final NetworkUuid network) {
        system.registerCraftingComputer(new NetworkSystem.CraftingComputerNode(
                nodeUuid(), network, capacity(), worldPosition.asLong()));
    }

    @Override
    protected void unregisterNode(final NetworkSystem system, final NetworkUuid network) {
        system.unregisterCraftingComputer(network, nodeUuid());
    }

    /* The Crafting Cards in the computer's build, in the build's order; none without a valid build. */
    private List<CraftingCardSpec> craftingCards() {
        final ComputerBuild build = currentBuild();
        if (build == null) {
            return List.of();
        }
        final List<CraftingCardSpec> cards = new ArrayList<>();
        for (final IExpansionCardSpec card : build.cardsOfKind(ExpansionCardKind.CRAFTING)) {
            if (card instanceof CraftingCardSpec craftingCard) {
                cards.add(craftingCard);
            }
        }
        return cards;
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

    /** Net local-storage capacity in item-equivalents, after the installed OS footprint. */
    private long netStorageItems() {
        final ComputerBuild build = currentBuild();
        return build == null ? 0L : Math.max(0L, build.totalStorageItems() - reservedByOs());
    }
}
