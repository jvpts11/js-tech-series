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
import dev.jstech.computers.block.CaseStyle;
import dev.jstech.computers.block.IComputerCase;
import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.computers.client.monitor.MonitorPictureCache;
import dev.jstech.computers.hardware.CaseGlass;
import dev.jstech.computers.hardware.CaseParts;
import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.hardware.ComputerDef;
import dev.jstech.computers.hardware.ExpansionCardKind;
import dev.jstech.computers.hardware.IExpansionCardSpec;
import dev.jstech.computers.hardware.QuinceCase;
import dev.jstech.computers.hardware.TowerBoards;
import dev.jstech.computers.hardware.WorkshopCardSpec;
import dev.jstech.computers.monitor.IMonitorPicture;
import dev.jstech.computers.monitor.IScreen;
import dev.jstech.computers.monitor.MonitorPicturePayload;
import dev.jstech.computers.monitor.MonitorPictures;
import dev.jstech.computers.operation.payload.DesktopBalloonPayload;
import dev.jstech.computers.os.HostScope;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.storage.DiskCorruption;
import dev.jstech.computers.storage.DriveVolumes;
import dev.jstech.computers.storage.IDataSink;
import dev.jstech.computers.storage.LocalStore;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.storage.StoreSink;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.computers.workshop.IEnergyAccount;
import dev.jstech.computers.workshop.UpdateDrawer;
import dev.jstech.computers.workshop.Workshop;
import dev.jstech.computers.workshop.WorkshopCard;
import dev.jstech.computers.workshop.WorkshopRates;
import dev.jstech.computers.workshop.WorkshopTexts;
import dev.jstech.core.blockentity.DerivedInt;
import dev.jstech.core.blockentity.FieldItemHandler;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.jstechseries.network.NetworkSystem;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.live.LiveFeed;
import dev.jstech.core.text.GameText;
import dev.jstech.core.uuid.NetworkUuid;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Personal Computer: the player's hands-on access point to the network, assembled on a consumer ATX board (one CPU,
 * four RAM, four PCIe, one PSU, two disks), or from the Transition on a workstation's EATX board, which may carry a
 * second processor and four more modules.
 *
 * <p>One that is its own screen, as Quince's all-in-ones are, is a screen too ({@link IScreen}): its glass shows the
 * machine, and a Remote Control session started at it, as a monitor does.
 */
public class PersonalComputerBlockEntity extends AbstractSmallComputerBlockEntity
        implements IComputerTerminalHost, IAssemblyComputer, IEnergyAccount, IScreen {

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
    /** The 5.25-inch bays, as many as the most any era's case has; the case says how many it really has. */
    public static final int BAY_SLOTS_START = 13;
    public static final int BAY_SLOTS = 2;
    /*
     * A two-socket workstation board's second processor and the four modules beside it, after the bays, so a machine
     * saved before them keeps every part where it was.
     */
    public static final int SECOND_CPU_SLOT = 15;
    public static final int SECOND_RAM_SLOTS_START = 16;
    /*
     * A cooler over each processor's socket and the case's fan seats, as many as the case with the most has, after
     * all of those for the same reason.
     */
    public static final int COOLER_SLOTS_START = 20;
    public static final int FAN_SLOTS_START = 22;
    public static final int HARDWARE_SLOTS = 28;

    public static final int STORAGE_SLOTS = 18;
    /**
     * What a Personal Computer's case counts, whatever its era: a processor and its four modules, and a second of each
     * on a two-socket workstation board. The slots above are where they sit.
     */
    public static final CaseParts PARTS =
            new CaseParts(2, 2 * RAM_SLOTS, GPU_SLOTS, DISK_SLOTS, BAY_SLOTS, STORAGE_SLOTS, RAM_SLOTS);
    /* How often an all-in-one's glass is described for the players near it: twice a second, as a monitor's. */
    private static final int FACE_INTERVAL = 10;

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
    /* The cards' work is paid out of this computer's own energy. */
    private final Workshop workshop = new Workshop(workshopSlots, furnaceProgress, furnaceExperience, anvilName,
            this);
    /* Where an item the network sends to the cards waits while they work on it, out of every query. */
    private final FieldItemHandler updateSlots = fields().items("UpdateDrawer", UpdateDrawer.SLOTS).save();
    private final ValueField<Integer> updateProgress = fields().value("UpdateProgress", Codec.INT, 0).save();
    private final ValueField<Integer> updateCard = fields().value("UpdateCard", Codec.INT, -1).save();
    private final UpdateDrawer updateDrawer = new UpdateDrawer(updateSlots, updateProgress, updateCard);
    /* What an all-in-one's glass shows, sent to whoever is near and only when it changes. */
    private final LiveFeed<IMonitorPicture> face = new LiveFeed<>();
    /*
     * The machine an all-in-one's glass is showing on someone else's behalf, as a monitor's does: transient, a
     * Remote Control session does not outlive the window it was opened in.
     */
    @Nullable
    private BlockPos remoteSession;

    public PersonalComputerBlockEntity(final BlockPos pos, final BlockState state) {
        this(ComputingModule.PERSONAL_COMPUTER_BE.get(), pos, state);
    }

    /** A Personal Computer of another block entity type: a small computer another mod brings. */
    protected PersonalComputerBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    /**
     * What a Personal Computer of {@code era} is. Each era takes its own consumer form factor: Vintage on Baby-AT/AT,
     * Legacy on ATX, and from the Transition on ATX or the workstation's EATX, as the tower cases of the time did. Its
     * case's {@code style} decides how much it cools.
     */
    public static ComputerDef computer(final Era era, final CaseStyle style) {
        return new ComputerDef(era, TowerBoards.personalComputer(era), HostScope.PERSONAL_COMPUTER, PARTS,
                style.cooling(era));
    }

    /**
     * What one of Quince's computers is: a Personal Computer that takes only its own logic board, with no bay for a
     * drive, cooled by its case alone (computers/docs/design/quince.md).
     */
    public static ComputerDef quince(final QuinceCase machine) {
        return new ComputerDef(machine.era(), Set.of(machine.board()), HostScope.PERSONAL_COMPUTER,
                new CaseParts(PARTS.processors(), PARTS.memory(), PARTS.cards(), PARTS.disks(), 0, PARTS.storage(),
                        PARTS.memoryPerProcessor()), machine.cooling());
    }

    /** What the personal-use cards hold and do on this computer. */
    public Workshop workshop() {
        return workshop;
    }

    /** Where an item the network sends this computer's cards waits while they work on it. */
    public UpdateDrawer updateDrawer() {
        return updateDrawer;
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

    /** Every item the personal-use cards hold, the network's work on them included, for the computer's drops. */
    public List<ItemStack> workshopDrops() {
        final List<ItemStack> drops = new ArrayList<>();
        for (int i = 0; i < Workshop.SLOTS; i++) {
            final ItemStack stack = workshopSlots.getStackInSlot(i);
            if (!stack.isEmpty()) {
                drops.add(stack.copy());
                workshopSlots.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        drops.addAll(updateDrawer.drops());
        return drops;
    }

    public long storageModCount() {
        return storageModCount;
    }

    /*
     * Corruption reaches only what the network may read of a Personal Computer's disk: its private part is never
     * affected (computers/docs/design/storage.md).
     */
    @Override
    protected Map<StorageKey, Long> corruptible(final ItemStack disk) {
        return LocalStore.publicViewOf(disk);
    }

    /**
     * A network conflict corrupts {@code share} of what this computer lends its network, its disks' public parts, and
     * nothing of its own files. Returns whether anything was corrupted.
     */
    public boolean corruptPublic(final double share, final RandomSource random) {
        boolean hit = false;
        for (int slot = 0; slot < diskSlots(); slot++) {
            final ItemStack disk = diskInSlot(slot);
            hit |= DiskCorruption.corrupt(disk, share, corruptible(disk), false, random);
        }
        if (hit) {
            setChanged();
        }
        return hit;
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

    // Network node, a passive Category-C node read from the adjacent cable

    public static void serverTick(final Level level, final BlockPos pos,
                                  final BlockState state, final PersonalComputerBlockEntity be) {
        if (level instanceof ServerLevel serverLevel) {
            be.tickNode(serverLevel);
            be.tickWorkshop(serverLevel);
            be.tickFace(serverLevel);
        }
    }

    /** The glass it shows its own screen on, as an all-in-one does; null for a computer shown on a monitor. */
    @Nullable
    public CaseGlass glass() {
        return getBlockState().getBlock() instanceof IComputerCase chassis ? chassis.glass() : null;
    }

    /** Whether its glass is lit: it runs, or, one of Quince's, it stands at a hardware fault and shows it. */
    @Override
    public boolean lit() {
        return glass() != null && (isRunning() || MonitorBlock.standsAtFault(this));
    }

    @Override
    @Nullable
    public BlockPos shownMachine() {
        if (glass() == null) {
            return null;
        }
        return remoteSession != null ? remoteSession : worldPosition;
    }

    @Override
    public boolean shows(final BlockPos machine) {
        return glass() != null && (worldPosition.equals(machine) || machine.equals(remoteSession));
    }

    @Override
    public void setRemoteSession(@Nullable final BlockPos machine) {
        this.remoteSession = machine;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide) {
            MonitorPictureCache.forget(worldPosition);
        }
    }

    /* An all-in-one tells whoever is near what its glass shows, as a monitor does. */
    private void tickFace(final ServerLevel level) {
        if (glass() != null
                && level.getGameTime() % FACE_INTERVAL == Math.floorMod(worldPosition.asLong(), FACE_INTERVAL)) {
            face.tick(level, worldPosition, () -> MonitorPictures.describe(level, this),
                    (player, picture) -> PacketDistributor.sendToPlayer(player,
                            new MonitorPicturePayload(worldPosition, picture)));
        }
    }

    /*
     * The Furnace Card goes on smelting while the computer is on, window open or not; when the last of its input is
     * done the desktops showing this computer say so in a balloon. One furnace serves both of the card's doors: while
     * an UPDATE from the network smelts, the Workshop's own input waits its turn.
     */
    private void tickWorkshop(final ServerLevel level) {
        for (final ItemStack lost : updateDrawer.recover(workshop)) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), lost);
        }
        updateDrawer.tickOut(workshop);
        final boolean furnace = WorkshopCard.FURNACE.in(workshopCards());
        final boolean network = furnace && updateDrawer.smelting();
        if (network) {
            updateDrawer.tickSmelt(level, furnaceSpeed(), workshop);
        }
        final boolean finished = furnace && !network && workshop.tickFurnace(level, furnaceSpeed());
        final boolean now = network || furnace && workshop.ticksPerItem(level, furnaceSpeed()) > 0;
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
        return new LocalStore(storeDisks(), this::setChanged);
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
        return capacity <= 0 ? 0 : (int) Math.min(computer().parts().storage(), (capacity + 63) / 64);
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

    /*
     * Every volume the PC keeps items on has its slider: its own disk slots', then its hot-swap bays', a partitioned
     * disk giving one for each partition that holds data.
     */
    @Override
    public int diskPrivacyDiskCount() {
        return localStore().diskCount();
    }

    @Override
    public int diskPrivacyPermille(final int diskIndex) {
        return localStore().diskPublicPermille(diskIndex);
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
        if (localStore().setPublicPermille(diskIndex, permille)) {
            bumpStorageModCount();
        }
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
    @Override
    public boolean assemblyRunning() {
        return assemblyRunning.isSet();
    }

    /** Whether the assembly menu should show a valid build; mirrored to the client while it is open. */
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

    public int assemblyServerCount() {
        return assemblyServerCount.getAsInt();
    }
}
