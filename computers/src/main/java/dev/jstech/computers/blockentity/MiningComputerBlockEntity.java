/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.IEraChassisBlock;
import dev.jstech.computers.block.MiningComputerStructure;
import dev.jstech.computers.block.MiningComputerTexts;
import dev.jstech.computers.hardware.CaseParts;
import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.hardware.ComputerDef;
import dev.jstech.computers.hardware.FormFactor;
import dev.jstech.computers.hardware.GpuSpec;
import dev.jstech.computers.item.MiningRiserItem;
import dev.jstech.computers.os.HostScope;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.computers.storage.IDataSink;
import dev.jstech.computers.storage.LocalStore;
import dev.jstech.computers.storage.StoreSink;
import dev.jstech.computers.teracoin.TeracoinHashrate;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.blockentity.BoolField;
import dev.jstech.core.blockentity.DerivedInt;
import dev.jstech.core.blockentity.FieldItemHandler;
import dev.jstech.core.jstechseries.network.NetworkSystem;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * The Mining Computer (computers/docs/design/teracoin.md): a computer with no case, an open frame holding its mining
 * board flat on the deck, the supply and the disk beside it, and its graphics cards standing on risers along the rail
 * above. The board, the processor, the memory, the supply, the disk and the risers go in at its assembly screen; the
 * cards are seated on the risers in the world, the rig switched off, each riser carrying a card of its own era or an
 * older one.
 *
 * <p>Its cards sit on risers, which carry each card on one lane, so a card mines at its whole power whatever slot it
 * hangs from ({@link TeracoinHashrate}). It is cooled by the air it stands in, so it has no cooler and no fan seats.
 */
public class MiningComputerBlockEntity extends AbstractSmallComputerBlockEntity
        implements IComputerTerminalHost, IAssemblyComputer {

    /* The risers put in at the assembly screen, one for each of the board's x1 slots, the n-th carrying card n. */
    private final FieldItemHandler risers = fields().items("Risers", MOST_RISERS).save().toClient().slotLimit(1)
            .accepts(this::takesRiser);
    /* MINE AT START: when the rig starts, its miner goes to work in the background with the settings it last had. */
    private final BoolField mineAtStart = fields().flag("MineAtStart", false).save().toMenu();
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
    private final DerivedInt assemblyHashrate = fields().derived("AssemblyHashrate",
            () -> (int) Math.min(Integer.MAX_VALUE, hashrate())).toMenu();
    private final DerivedInt assemblyMining = fields().derived("AssemblyMining", this::mining).toMenu();
    /* Whether it ran on the tick before: the tick it starts is when MINE AT START sets its miner going. */
    private boolean ranBefore;

    // Slot layout, kept public so the menu, the screen and the renderer address slots by name.
    public static final int MOTHERBOARD_SLOT = 0;
    public static final int CPU_SLOT = 1;
    public static final int RAM_SLOTS_START = 2;
    public static final int RAM_SLOTS = 2;
    public static final int CARD_SLOTS_START = 4;
    public static final int CARD_SLOTS = 8;
    public static final int PSU_SLOT = 12;
    public static final int DISK_SLOT = 13;
    /** The most risers a rig carries: the Advanced's eight. */
    public static final int MOST_RISERS = 8;
    /** What a rig's frame counts, whatever its era: one processor, two modules, eight cards, one disk, no bay. */
    public static final CaseParts PARTS = new CaseParts(1, RAM_SLOTS, CARD_SLOTS, 1, 0, 0);

    public MiningComputerBlockEntity(final BlockPos pos, final BlockState state) {
        super(ComputingModule.MINING_COMPUTER_BE.get(), pos, state);
    }

    /** What a Mining Computer of {@code era} is: its era's mining board in an open frame cooled by the air. */
    public static ComputerDef computer(final Era era) {
        return new ComputerDef(era, Set.of(FormFactor.MNG), HostScope.MINING_COMPUTER, PARTS);
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final MiningComputerBlockEntity be) {
        if (level instanceof ServerLevel serverLevel) {
            be.tickNode(serverLevel);
            be.tickStart();
        }
    }

    /** The risers, the n-th carrying card n. */
    public FieldItemHandler risers() {
        return risers;
    }

    /** The era of the rig's frame: the board it takes, its risers, and how many of them. */
    public Era rigEra() {
        return getBlockState().getBlock() instanceof IEraChassisBlock chassis ? chassis.chassisEra()
                : HardwareEra.STANDARD;
    }

    /** How many risers the rig carries: four, six or eight, as its era's frame has room for. */
    public int riserCount() {
        return MiningComputerStructure.risers(rigEra());
    }

    /** Whether riser {@code index} is in. */
    public boolean hasRiser(final int index) {
        return index >= 0 && index < riserCount() && !risers.getStackInSlot(index).isEmpty();
    }

    /** The card standing on riser {@code index}, or nothing. */
    public ItemStack cardOn(final int index) {
        return index < 0 || index >= CARD_SLOTS ? ItemStack.EMPTY
                : getHardware().getStackInSlot(CARD_SLOTS_START + index);
    }

    /**
     * The card standing on riser {@code index} as every player who sees the rig knows it, on either side: its item, or
     * null for none.
     */
    @Nullable
    public ResourceLocation cardPart(final int index) {
        return index < 0 || index >= CARD_SLOTS ? null : installedPart(CARD_SLOTS_START + index);
    }

    /** Whether every riser the rig carries is in, with a card standing on it. */
    public boolean everyRiserHoldsACard() {
        for (int i = 0; i < riserCount(); i++) {
            if (!hasRiser(i) || cardOn(i).isEmpty()) {
                return false;
            }
        }
        return riserCount() > 0;
    }

    /** How many cards stand on the risers, on either side. */
    public int seatedCards() {
        int seated = 0;
        for (int i = 0; i < riserCount(); i++) {
            seated += cardPart(i) == null ? 0 : 1;
        }
        return seated;
    }

    /** Whether the rig is switched off, the only way its cards are seated or taken off. */
    public boolean switchedOff() {
        return !isManualOn() && !isRunning();
    }

    /**
     * Why {@code card} cannot stand on riser {@code index} now, or null when it can: the rig must be off, the riser
     * in and free, and the card a graphics card the riser carries, of its era or an older one, that the board takes.
     */
    @Nullable
    public TextKey seatRefusal(final int index, final ItemStack card) {
        if (!switchedOff()) {
            return MiningComputerTexts.SWITCH_OFF_FIRST;
        }
        if (!hasRiser(index)) {
            return MiningComputerTexts.NO_RISER;
        }
        if (!cardOn(index).isEmpty()) {
            return MiningComputerTexts.RISER_TAKEN;
        }
        if (!(HardwareRegistry.card(card) instanceof GpuSpec gpu)) {
            return MiningComputerTexts.ONLY_GRAPHICS_CARDS;
        }
        if (!(risers.getStackInSlot(index).getItem() instanceof MiningRiserItem riser) || !riser.carries(gpu.era())) {
            return MiningComputerTexts.TOO_NEW_FOR_RISER;
        }
        return isValidForSlot(CARD_SLOTS_START + index, card) ? null : MiningComputerTexts.BOARD_REFUSES;
    }

    /** Seats one of {@code card} on riser {@code index}, taking it from the stack; whether it went on. */
    public boolean seat(final int index, final ItemStack card) {
        if (seatRefusal(index, card) != null) {
            return false;
        }
        getHardware().setStackInSlot(CARD_SLOTS_START + index, card.split(1));
        return true;
    }

    /** Takes the card off riser {@code index}, the rig off; nothing when it carries none or the rig runs. */
    public ItemStack unseat(final int index) {
        if (!switchedOff() || cardOn(index).isEmpty()) {
            return ItemStack.EMPTY;
        }
        final ItemStack card = cardOn(index).copy();
        getHardware().setStackInSlot(CARD_SLOTS_START + index, ItemStack.EMPTY);
        return card;
    }

    /**
     * Lets fall, as the rig breaks, what it carries on its risers and the risers themselves: the cards were seated in
     * the world and come off it there; the rest goes with the rig's item.
     */
    public void spillRigging(final ServerLevel level, final BlockPos at) {
        for (int i = 0; i < CARD_SLOTS; i++) {
            final ItemStack card = cardOn(i);
            if (!card.isEmpty()) {
                getHardware().setStackInSlot(CARD_SLOTS_START + i, ItemStack.EMPTY);
                Block.popResource(level, at, card);
            }
        }
        for (int i = 0; i < MOST_RISERS; i++) {
            final ItemStack riser = risers.getStackInSlot(i);
            if (!riser.isEmpty()) {
                risers.setStackInSlot(i, ItemStack.EMPTY);
                Block.popResource(level, at, riser);
            }
        }
    }

    /** Whether MINE AT START is on. */
    public boolean mineAtStart() {
        return mineAtStart.get();
    }

    /** Throws MINE AT START. */
    public void toggleMineAtStart() {
        mineAtStart.set(!mineAtStart.get());
    }

    /** What its build hashes a second, its cards on their risers. */
    public long hashrate() {
        final ComputerBuild build = currentBuild();
        return build == null ? 0L : TeracoinHashrate.of(build, true);
    }

    /** The hashrate the open assembly screen shows. */
    public long assemblyHashrate() {
        return assemblyHashrate.getAsInt();
    }

    /** Whether the open assembly screen shows the rig mining: running, its miner at work. */
    public boolean assemblyMining() {
        return assemblyMining.isSet();
    }

    /* Its cards hang from risers, a lane each: none is cut by the slot it hangs from. */
    @Override
    public boolean cardsOnRisers() {
        return true;
    }

    /* An open frame has no front: no USB port and no bay. */
    @Override
    public int casePorts() {
        return 0;
    }

    /* Its cards are seated in the world on the risers; only a graphics card a riser in place carries goes there. */
    @Override
    public boolean isValidForSlot(final int slot, final ItemStack stack) {
        if (slot >= CARD_SLOTS_START && slot < CARD_SLOTS_START + CARD_SLOTS) {
            final int index = slot - CARD_SLOTS_START;
            if (!(HardwareRegistry.card(stack) instanceof GpuSpec gpu) || !hasRiser(index)
                    || !(risers.getStackInSlot(index).getItem() instanceof MiningRiserItem riser)
                    || !riser.carries(gpu.era())) {
                return false;
            }
        }
        return super.isValidForSlot(slot, stack);
    }

    /** Every block of the rig, the origin among them: one, or two side by side. */
    @Override
    public List<BlockPos> bodyBlocks() {
        final BlockState state = getBlockState();
        if (!state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return List.of(worldPosition);
        }
        return MiningComputerStructure.allPositions(worldPosition,
                state.getValue(HorizontalDirectionalBlock.FACING), rigEra());
    }

    /** The whole rig is one connection surface: a peripheral cable may meet it on either of its blocks. */
    @Override
    public Set<Long> occupiedPositions(final long ownerPos) {
        final Set<Long> positions = new HashSet<>();
        for (final BlockPos block : bodyBlocks()) {
            positions.add(block.asLong());
        }
        return positions;
    }

    /** The whole rig: the renderer draws it from this block alone. */
    public AABB renderBox() {
        AABB box = new AABB(worldPosition);
        for (final BlockPos block : bodyBlocks()) {
            box = box.minmax(new AABB(block));
        }
        return box;
    }

    /** The facing of the rig: where its front, the cards' brackets, looks. */
    public Direction facing() {
        final BlockState state = getBlockState();
        return state.hasProperty(HorizontalDirectionalBlock.FACING)
                ? state.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
    }

    // Network node: on its network it is a computer addressed as a PC is.

    @Override
    protected void registerNode(final NetworkSystem system, final NetworkUuid network) {
        system.registerPersonalComputer(new NetworkSystem.PersonalComputerNode(
                nodeUuid(), network, capacity(), worldPosition.asLong()));
    }

    @Override
    protected void unregisterNode(final NetworkSystem system, final NetworkUuid network) {
        system.unregisterPersonalComputer(network, nodeUuid());
    }

    // Local storage: what its disk holds.

    @Override
    public LocalStore localStore() {
        return new LocalStore(storeDisks(), this::setChanged);
    }

    @Override
    public int usableStorageSlots() {
        return 0;
    }

    @Override
    public long localStorageUsed() {
        return localStore().used();
    }

    @Override
    public long localStorageCapacity() {
        final ComputerBuild build = currentBuild();
        return build == null ? 0L : Math.max(0L, build.totalStorageItems() - reservedByOs());
    }

    @Override
    public IDataSink localStorage() {
        return new StoreSink(localStore());
    }

    // IComputerTerminalHost: read-only monitoring (the rest is inherited from the base)

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
        return networkUuid() != null ? 1 : 0;
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
        if (networkUuid() != null && level instanceof ServerLevel serverLevel) {
            return NetworkSystem.get(serverLevel).serversOf(networkUuid()).size();
        }
        return 0;
    }

    // The values the assembly menu shows.

    @Override
    public boolean assemblyRunning() {
        return assemblyRunning.isSet();
    }

    @Override
    public boolean assemblyBuildValid() {
        return assemblyBuildValid.isSet();
    }

    @Override
    public long assemblyCapacity() {
        return assemblyCapacity.getAsInt();
    }

    @Override
    public long assemblyRamBuffer() {
        return assemblyRamBuffer.getAsInt();
    }

    @Override
    public boolean assemblyAutoStart() {
        return assemblyAutoStart.isSet();
    }

    @Override
    public boolean assemblyOnNetwork() {
        return assemblyOnNetwork.isSet();
    }

    /* A riser of the rig's own era, in one of the slots its frame has room for. */
    private boolean takesRiser(final int slot, final ItemStack stack) {
        return slot < riserCount() && stack.getItem() instanceof MiningRiserItem riser && riser.era() == rigEra();
    }

    /* The rig started this tick with MINE AT START on: its miner goes to work in the background. */
    private void tickStart() {
        final boolean running = isRunning();
        if (running && !ranBefore && mineAtStart.get()) {
            final ComputerConsoleState console = console();
            if (console != null) {
                console.miner().setBackground(true);
            }
        }
        ranBefore = running;
    }

    /* Running with its miner at work. */
    private boolean mining() {
        final ComputerConsoleState console = console();
        return isRunning() && console != null && console.miner().hashrate() > 0L
                && console.miner().running(level == null ? 0L : level.getGameTime());
    }
}
