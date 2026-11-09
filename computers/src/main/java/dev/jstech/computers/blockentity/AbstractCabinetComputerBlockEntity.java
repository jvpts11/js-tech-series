/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.block.IEraChassisBlock;
import dev.jstech.computers.hardware.CaseParts;
import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.os.OsDisks;
import dev.jstech.computers.storage.IDataSink;
import dev.jstech.computers.storage.LocalStore;
import dev.jstech.computers.storage.StoreSink;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.blockentity.BoolField;
import dev.jstech.core.blockentity.DerivedInt;
import dev.jstech.core.jstechseries.network.NetworkSystem;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.model.network.ModelAnimations;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * A computer whose body is a cabinet of several blocks, drawn as one model from its origin block: the Mainframe and
 * the WAN Gateway Computer. Both seat the same MTX board (four sockets, eight banks, six cards), a supply and four
 * disks, with a cooler over each socket, and both take a service panel off their card bay.
 *
 * <p>The client copy of a computer does not carry the hardware handler, so what the cabinet's model shows travels as
 * two small numbers the server works out and sends when they change: which disks carry a system, and the machine's own
 * condition. Worked out every tick rather than pushed from each place that installs a part or flips the power, no path
 * can leave the model showing hardware that is no longer there.
 */
public abstract class AbstractCabinetComputerBlockEntity extends AbstractComputerBlockEntity
        implements IComputerTerminalHost {

    /** The service panel taken off, showing the card bay and everything seated in it. */
    private final BoolField servicePanelOff = fields().flag("ServicePanelOff", false).save();
    private final DerivedInt visualSystems = fields().derived("VisualSystems", this::systemsMask).toClient();
    private final DerivedInt visualFlags = fields().derived("VisualFlags", this::machineFlags).toClient();
    /* Values every cabinet's assembly menu shows: worked out on the server, received while the menu is open. */
    private final DerivedInt assemblyRunning = fields().derived("AssemblyRunning", this::isRunning).toMenu();
    private final DerivedInt assemblyBuildValid = fields().derived("AssemblyBuildValid", this::buildValid).toMenu();
    private final DerivedInt assemblyCapacity = fields().derived("AssemblyCapacity",
            () -> (int) Math.min(Integer.MAX_VALUE, capacity())).toMenu();
    private final DerivedInt assemblyAutoStart = fields().derived("AssemblyAutoStart", this::isAutoStart).toMenu();
    private final DerivedInt assemblyManualOn = fields().derived("AssemblyManualOn", this::isManualOn).toMenu();
    /* The blocks of the cabinet, worked out once for the facing it stands in. */
    @Nullable
    private List<BlockPos> body;
    @Nullable
    private Direction bodyFacing;

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
    /** A cooler over each processor's socket, after the disks so a cabinet saved before them keeps its parts. */
    public static final int COOLER_SLOTS_START = 24;
    public static final int COOLER_SLOTS = 4;
    public static final int TOTAL_SLOTS = 28;
    public static final int STORAGE_SLOTS = 27;
    /** What a cabinet counts, whatever its era: the slots above are where they sit; it has no bays. */
    public static final CaseParts PARTS = new CaseParts(CPU_SLOTS, RAM_SLOTS, GPU_SLOTS, DISK_SLOTS, 0, STORAGE_SLOTS);
    private static final int FLAG_RUNNING = 1;
    private static final int FLAG_BUILD_VALID = 2;
    private static final int FLAG_NETWORKED = 4;
    private static final int FLAG_PANEL_OFF = 8;
    /** The controller the power button's press plays on. */
    private static final String POWER = "power";

    protected AbstractCabinetComputerBlockEntity(final BlockEntityType<?> type, final BlockPos pos,
                                                 final BlockState state) {
        super(type, pos, state);
    }

    /** The power button pressed on the sill: the button goes in for whoever sees the cabinet. */
    public void showPowerPressed() {
        ModelAnimations.replay(this, POWER, pressClip());
    }

    /** The era of this cabinet, read from its block; it picks the model and the atlas. */
    public Era cabinetEra() {
        return getBlockState().getBlock() instanceof IEraChassisBlock chassis
                ? chassis.chassisEra() : HardwareEra.STANDARD;
    }

    /** Whether hardware slot {@code slot} holds a part, on either side. */
    public boolean hardwareInstalled(final int slot) {
        return installedPart(slot) != null;
    }

    /** Whether the disk in bay {@code bay} carries a system, which is what lights its lamp. */
    public boolean diskCarriesSystem(final int bay) {
        if (bay < 0 || bay >= DISK_SLOTS) {
            return false;
        }
        if (level != null && level.isClientSide()) {
            return (visualSystems.getAsInt() & (1 << bay)) != 0;
        }
        return OsDisks.hasSystem(getHardware().getStackInSlot(DISK_SLOTS_START + bay));
    }

    public boolean visualRunning() {
        return (visualFlags.getAsInt() & FLAG_RUNNING) != 0;
    }

    public boolean visualBuildValid() {
        return (visualFlags.getAsInt() & FLAG_BUILD_VALID) != 0;
    }

    public boolean visualNetworked() {
        return (visualFlags.getAsInt() & FLAG_NETWORKED) != 0;
    }

    public boolean servicePanelOff() {
        return (visualFlags.getAsInt() & FLAG_PANEL_OFF) != 0;
    }

    /** Takes the service panel off the card bay or puts it back. */
    public void toggleServicePanel() {
        servicePanelOff.set(!servicePanelOff.get());
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

    public boolean assemblyAutoStart() {
        return assemblyAutoStart.isSet();
    }

    public boolean assemblyManualOn() {
        return assemblyManualOn.isSet();
    }

    /*
     * The hardware handler is inherited; this shim keeps the historic public name so the menus, block drops and
     * GameTests address it unchanged.
     */
    public ItemStackHandler getInventory() {
        return getHardware();
    }

    /** What its four disks hold, the store a monitor's file manager and the network's index read. */
    @Override
    public LocalStore localStore() {
        final List<ItemStack> disks = new ArrayList<>(DISK_SLOTS);
        for (int i = 0; i < DISK_SLOTS; i++) {
            disks.add(getHardware().getStackInSlot(DISK_SLOTS_START + i));
        }
        return new LocalStore(disks, this::setChanged);
    }

    /**
     * The net storage capacity in item-equivalents after subtracting the installed OS footprint: what is left for data
     * held on the network and in the local store, the system taking its share of the disks from installation.
     */
    public long storageItems() {
        final ComputerBuild build = currentBuild();
        return build == null ? 0L : Math.max(0L, build.totalStorageItems() - reservedByOs());
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

    @Override
    public int usableStorageSlots() {
        final long capacity = storageItems();
        return capacity <= 0 ? 0 : (int) Math.min(computer().parts().storage(), (capacity + 63) / 64);
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

    /** The whole cabinet: the renderer draws it from this block alone. */
    public AABB renderBox() {
        AABB box = new AABB(worldPosition);
        for (final BlockPos block : bodyBlocks()) {
            box = box.minmax(new AABB(block));
        }
        return box;
    }

    /** The whole cabinet is one connection surface: a peripheral cable may meet it on any of its blocks. */
    @Override
    public Set<Long> occupiedPositions(final long ownerPos) {
        final Set<Long> positions = new HashSet<>();
        for (final BlockPos block : bodyBlocks()) {
            positions.add(block.asLong());
        }
        return positions;
    }

    /** Every block of the cabinet, the origin among them. */
    @Override
    public List<BlockPos> bodyBlocks() {
        final BlockState state = getBlockState();
        if (!state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return List.of(worldPosition);
        }
        final Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        if (body == null || facing != bodyFacing) {
            body = List.copyOf(cabinetBlocks(facing));
            bodyFacing = facing;
        }
        return body;
    }

    @Override
    protected String hardwareNbtKey() {
        // The Mainframe's historic key, so every saved Mainframe keeps its installed hardware.
        return "Inventory";
    }

    /** Every block of a cabinet standing at this block, facing {@code facing}. */
    protected abstract List<BlockPos> cabinetBlocks(Direction facing);

    /** The clip of the cabinet's model that presses its power button. */
    protected abstract String pressClip();

    /* Which disk bays hold a disk carrying a system, a bit each. */
    private int systemsMask() {
        int systems = 0;
        for (int bay = 0; bay < DISK_SLOTS; bay++) {
            if (OsDisks.hasSystem(getHardware().getStackInSlot(DISK_SLOTS_START + bay))) {
                systems |= 1 << bay;
            }
        }
        return systems;
    }

    /* The machine's own condition as the lamps and the panel show it. */
    private int machineFlags() {
        int flags = 0;
        flags |= isRunning() ? FLAG_RUNNING : 0;
        flags |= buildValid() ? FLAG_BUILD_VALID : 0;
        flags |= networkUuid() != null ? FLAG_NETWORKED : 0;
        flags |= servicePanelOff.get() ? FLAG_PANEL_OFF : 0;
        return flags;
    }
}
