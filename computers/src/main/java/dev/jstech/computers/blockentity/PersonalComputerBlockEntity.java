/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import com.mojang.serialization.Codec;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.PersonalComputerBlock;
import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.hardware.ExpansionCardKind;
import dev.jstech.computers.hardware.FormFactor;
import dev.jstech.computers.hardware.IExpansionCardSpec;
import dev.jstech.computers.hardware.WorkshopCardSpec;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.operation.payload.DesktopBalloonPayload;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.storage.IDataSink;
import dev.jstech.computers.storage.LocalStore;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.storage.StoreSink;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.computers.workshop.Workshop;
import dev.jstech.computers.workshop.WorkshopCard;
import dev.jstech.computers.workshop.WorkshopRates;
import dev.jstech.computers.workshop.WorkshopTexts;
import dev.jstech.core.blockentity.DerivedInt;
import dev.jstech.core.blockentity.FieldItemHandler;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.text.GameText;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.uuid.NetworkUuid;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Personal Computer: the player's hands-on access point to the network, assembled on a consumer ATX board (one CPU, four RAM, four PCIe, one PSU, two disks).
 */
public class PersonalComputerBlockEntity extends AbstractSmallComputerBlockEntity
        implements IComputerTerminalHost {

    // Slot layout, kept public so the assembly Menu and Screen address slots by name.
    public static final int MOTHERBOARD_SLOT = 0;
    public static final int CPU_SLOT = 1;
    public static final int RAM_SLOTS_START = 2;
    public static final int RAM_SLOTS = 4;
    public static final int GPU_SLOTS_START = 6;
    public static final int GPU_SLOTS = 4;
    public static final int PSU_SLOT = 10;
    public static final int DISK_SLOTS_START = 11;
    public static final int DISK_SLOTS = 2;
    public static final int HARDWARE_SLOTS = 13;

    public static final int STORAGE_SLOTS = 18;

    private static final ComputerHardwareLayout LAYOUT = new ComputerHardwareLayout(
            MOTHERBOARD_SLOT, CPU_SLOT, 1, RAM_SLOTS_START, RAM_SLOTS,
            GPU_SLOTS_START, GPU_SLOTS, PSU_SLOT, DISK_SLOTS_START, DISK_SLOTS, HARDWARE_SLOTS);

    /*
     * Bumped on any change to the disk contents (insert/extract/disk swap) AND on a privacy-slider
     * write, so the Mainframe's incremental ANALYZE re-reads this PC's public view exactly when it
     * could have changed, since moving a slider changes the public view with no item movement at all.
     */
    private long storageModCount;
    /*
     * What the personal-use cards hold: the crafting grid, the furnace, the enchanting item and the anvil's two, kept
     * with the computer and given back or dropped as a block's would be.
     */
    private final FieldItemHandler workshopSlots = fields().items("WorkshopSlots", Workshop.SLOTS).save();
    private final ValueField<Integer> furnaceProgress = fields().value("WorkshopFurnace", Codec.INT, 0).save();
    private final ValueField<Float> furnaceExperience = fields().value("WorkshopExperience", Codec.FLOAT, 0f).save();
    private final ValueField<String> anvilName = fields().value("WorkshopAnvilName", Codec.STRING, "").save();
    /* Whether the Furnace Card is smelting, which the desktop's tray shows. */
    private final ValueField<Boolean> smelting = fields().value("WorkshopSmelting", Codec.BOOL, false).toClient();
    private final Workshop workshop = new Workshop(workshopSlots, furnaceProgress, furnaceExperience, anvilName);

    public PersonalComputerBlockEntity(final BlockPos pos, final BlockState state) {
        super(ComputingModule.PERSONAL_COMPUTER_BE.get(), pos, state, LAYOUT);
    }

    /** What the personal-use cards hold and do on this computer. */
    public Workshop workshop() {
        return workshop;
    }

    /** The personal-use cards in this computer's build, as a mask of {@link WorkshopCard#bit()}; none while off. */
    public int workshopCards() {
        final ComputerBuild build = currentBuild();
        if (build == null || !isRunning()) {
            return 0;
        }
        int mask = 0;
        for (final IExpansionCardSpec card : build.cardsOfKind(ExpansionCardKind.WORKSHOP)) {
            if (card instanceof WorkshopCardSpec workshopCard) {
                mask |= workshopCard.card().bit();
            }
        }
        return mask;
    }

    /** How many times a furnace's pace the Furnace Card smelts at here: by the era of the computer's board. */
    public int furnaceSpeed() {
        final ComputerBuild build = currentBuild();
        return build == null ? 0 : WorkshopRates.furnaceSpeed(build.motherboard().era());
    }

    /** Whether the Furnace Card is smelting now, as the client sees it. */
    public boolean furnaceSmelting() {
        return Boolean.TRUE.equals(smelting.get());
    }

    /** Every item the personal-use cards hold, for the computer's drops. */
    public List<ItemStack> workshopDrops() {
        final List<ItemStack> drops = new ArrayList<>();
        for (int i = 0; i < Workshop.SLOTS; i++) {
            final ItemStack stack = workshopSlots.getStackInSlot(i);
            if (!stack.isEmpty()) {
                drops.add(stack.copy());
                workshopSlots.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        return drops;
    }

    public long storageModCount() {
        return storageModCount;
    }

    /**
     * Marks the PC's storage as changed so the network index re-reads it; used by the slider write, which
     * changes the public view without moving any item. Defers to {@link #setChanged()}, which advances the
     * counter once, so a slider write and a disk swap both bump it by exactly one.
     */
    public void bumpStorageModCount() {
        setChanged();
    }

    @Override
    public void setChanged() {
        /*
         * Any reason the BE is marked dirty (a disk swap, a content write) could have changed the
         * public view, so advance the counter the index keys its re-reads on.
         */
        storageModCount++;
        super.setChanged();
    }

    /**
     * The era this PC belongs to, read from its block. Defaults to Standard for any block that is not a
     * {@link PersonalComputerBlock} (which never happens in practice, but keeps the read total).
     */
    private HardwareEra blockEra() {
        return getBlockState().getBlock() instanceof PersonalComputerBlock pc
                ? pc.era()
                : HardwareEra.STANDARD;
    }

    @Override
    protected Set<FormFactor> acceptedFormFactors() {
        /*
         * Each era takes its own consumer form factor: Vintage on Baby-AT/AT, Legacy on ATX, and from the Transition
         * on ATX or the workstation's EATX, as the tower cases of the time did.
         */
        return switch (blockEra()) {
            case VINTAGE -> Set.of(FormFactor.BABY_AT, FormFactor.AT);
            case LEGACY -> Set.of(FormFactor.ATX);
            default -> Set.of(FormFactor.ATX, FormFactor.EATX);
        };
    }

    @Override
    protected HardwareEra requiredBoardEra() {
        /*
         * A PC accepts only a board of its own era, so a Legacy and a Standard ATX board are not
         * interchangeable: each installs in its matching machine alone.
         */
        return blockEra();
    }

    @Override
    protected boolean takesWorkshopCards() {
        return true;
    }

    // Network node, a passive Category-C node read from the adjacent cable

    public static void serverTick(final Level level, final BlockPos pos,
                                  final BlockState state, final PersonalComputerBlockEntity be) {
        if (level instanceof ServerLevel serverLevel) {
            be.tickNode(serverLevel);
            be.tickWorkshop(serverLevel);
        }
    }

    /*
     * The Furnace Card goes on smelting while the computer is on, window open or not; when the last of its input is
     * done the desktops showing this computer say so in a balloon.
     */
    private void tickWorkshop(final ServerLevel level) {
        final boolean furnace = WorkshopCard.FURNACE.in(workshopCards());
        final boolean finished = furnace && workshop.tickFurnace(level, furnaceSpeed());
        final boolean now = furnace && workshop.ticksPerItem(level, furnaceSpeed()) > 0;
        if (now != furnaceSmelting()) {
            smelting.set(now);
        }
        if (finished) {
            final ItemStack made = workshop.slot(Workshop.FURNACE_OUT);
            final DesktopBalloonPayload balloon = new DesktopBalloonPayload(worldPosition, WorkshopTexts.TITLE.text(),
                    WorkshopTexts.FURNACE_DONE.with(made.getCount(), GameText.of(made.getHoverName())),
                    Programs.WORKSHOP.toString());
            for (final ServerPlayer viewer : consoleViewers(level)) {
                PacketDistributor.sendToPlayer(viewer, balloon);
            }
        }
    }

    @Override
    protected void registerNode(final NetworkSystem system, final NetworkUuid network) {
        system.registerPersonalComputer(new NetworkSystem.PersonalComputerNode(
                nodeUuid(), network, capacity(), worldPosition.asLong()));
    }

    @Override
    protected void unregisterNode(final NetworkSystem system, final NetworkUuid network) {
        system.unregisterPersonalComputer(network, nodeUuid());
    }

    private boolean onServerNetwork() {
        return networkUuid() != null && level instanceof ServerLevel;
    }

    @Override
    public int networkServerCount() {
        if (!onServerNetwork()) {
            return 0;
        }
        return NetworkSystem.get((ServerLevel) level).serversOf(networkUuid()).size();
    }

    // Local storage (PC-specific: lives on the installed disks)

    @Override
    public LocalStore localStore() {
        final List<ItemStack> disks = new ArrayList<>(DISK_SLOTS);
        for (int i = 0; i < DISK_SLOTS; i++) {
            disks.add(getHardware().getStackInSlot(DISK_SLOTS_START + i));
        }
        return new LocalStore(disks, this::setChanged);
    }

    public Map<StorageKey, Long> localSnapshot() {
        return localStore().view();
    }

    /**
     * The net storage capacity in item-equivalents after subtracting the installed OS footprint.
     * This is the capacity available for local data; the OS occupies disk space from installation.
     */
    public long netStorageItems() {
        final ComputerBuild build = currentBuild();
        if (build == null) {
            return 0L;
        }
        return Math.max(0L, build.totalStorageItems() - reservedByOs());
    }

    @Override
    public int usableStorageSlots() {
        final long capacity = netStorageItems(); // already net of OS footprint
        return capacity <= 0 ? 0 : (int) Math.min(STORAGE_SLOTS, (capacity + 63) / 64);
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
        return networkUuid() != null ? 1 : 0; // a PC never conflicts; it only reads a network
    }

    @Override
    public long orchestrationCapacity() {
        return capacity();
    }

    @Override
    public int computerQueues() {
        return isRunning() ? 1 : 0; // a PC runs a single Operation queue
    }

    @Override
    public long computerRamBuffer() {
        return ramBuffer();
    }

    @Override
    public boolean isMainframeHost() {
        return false;
    }

    // Public/private storage slider: a PC publishes part of each disk to the network per disk.

    @Override
    public boolean storageHasSlider() {
        return true;
    }

    @Override
    public int diskPrivacyDiskCount() {
        return DISK_SLOTS;
    }

    @Override
    public int diskPrivacyPermille(final int diskIndex) {
        if (diskIndex < 0 || diskIndex >= DISK_SLOTS) {
            return 0;
        }
        return DiskItem.publicPermille(getHardware().getStackInSlot(DISK_SLOTS_START + diskIndex));
    }

    @Override
    public long diskUsedWeight(final int diskIndex) {
        return localStore().diskUsedWeight(diskIndex);
    }

    @Override
    public long diskCapacityWeight(final int diskIndex) {
        return localStore().diskCapacityWeight(diskIndex);
    }

    /**
     * Writes a clamped public-share permille onto the disk in {@code diskIndex} and marks storage changed so the network re-reads this PC's public view. A no-op for an out-of-range index or an empty slot.
     */
    public void setDiskPrivacy(final int diskIndex, final int permille) {
        if (diskIndex < 0 || diskIndex >= DISK_SLOTS) {
            return;
        }
        final ItemStack disk = getHardware().getStackInSlot(DISK_SLOTS_START + diskIndex);
        if (!(disk.getItem() instanceof DiskItem)) {
            return;
        }
        DiskItem.setPublicPermille(disk, permille);
        bumpStorageModCount();
    }

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
    private final DerivedInt assemblyServerCount = fields().derived("AssemblyServerCount",
            this::networkServerCount).toMenu();

    /** Whether the assembly menu should show this machine as running; mirrored to the client while it is open. */
    public boolean assemblyRunning() {
        return assemblyRunning.isSet();
    }

    /** Whether the assembly menu should show a valid build; mirrored to the client while it is open. */
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

    public int assemblyServerCount() {
        return assemblyServerCount.getAsInt();
    }
}
