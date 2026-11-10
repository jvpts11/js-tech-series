/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.blockentity.AbstractSmallComputerBlockEntity;
import dev.jstech.computers.blockentity.ComputerHardwareLayout;
import dev.jstech.computers.blockentity.IAssemblyComputer;
import dev.jstech.computers.blockentity.PowerShown;
import dev.jstech.computers.gui.layout.AssemblyGeometry;
import dev.jstech.computers.hardware.CaseCooling;
import dev.jstech.computers.hardware.ThermalState;
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.menu.CoreMenu;
import dev.jstech.core.menu.MenuValidity;
import dev.jstech.core.menu.PlayerSlots;
import dev.jstech.core.menu.SlotGroup;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * A menu that assembles one computer out of hardware parts, backed by one block entity in the world: the
 * Personal Computer, the Crafting Computer and the Cluster Management Computer. It lays out the board-gated hardware
 * slots (motherboard, PSU, processor, memory, PCIe and disks, each clamped to the count the installed motherboard
 * offers), wires the shift-click both ways, syncs the machine's fields and handles the power, auto-start and side
 * panel buttons, so the three cannot drift apart. A payload that renames whichever of them is open is also admitted
 * by one gate instead of three.
 */
public abstract class AbstractAssemblyComputerMenu extends CoreMenu {

    private final AbstractSmallComputerBlockEntity computer;
    private final IAssemblyComputer assembly;
    /* How many of the menu's first slots are the machine's parts; the player's inventory follows them. */
    private final int hardwareSlots;
    /* The slots of the cooling face, the coolers' and the fans'. */
    private final Set<Slot> coolingSlots = new HashSet<>();
    /* The face the screen shows, which decides whose slots are there to click. */
    private AssemblyGeometry.Face face = AssemblyGeometry.Face.BOARD;

    public static final int BUTTON_POWER = 0;
    public static final int BUTTON_AUTOSTART = 1;
    public static final int BUTTON_SIDE_PANEL = 2;
    /** The tabs that turn the screen to the board's face and to the cooling face. */
    public static final int BUTTON_FACE_BOARD = 3;
    public static final int BUTTON_FACE_COOLING = 4;

    /**
     * Builds the menu over {@code be}, its slots where {@code geometry} has their seats. {@code pcieKey} is the layout
     * key prefix of the machine's expansion slots ("gpu" or "pcie"), which the hardware layout's PCIe range fills.
     */
    protected <B extends AbstractSmallComputerBlockEntity & IAssemblyComputer> AbstractAssemblyComputerMenu(
            final MenuType<?> type, final int containerId, final Inventory inventory, final B be,
            final AssemblyGeometry geometry, final String pcieKey) {
        super(type, containerId, inventory, MenuValidity.blockEntity(be));
        this.computer = be;
        this.assembly = be;
        final GuiLayout layout = geometry.layout(pcieKey);
        final ComputerHardwareLayout map = be.hardwareLayout();
        final IItemHandler hw = be.getHardware();
        final BooleanSupplier board = () -> face == AssemblyGeometry.Face.BOARD;

        final List<Slot> hardwareSlots = new ArrayList<>();
        hardwareSlots.add(faced(hw, map.motherboardSlot(), layout.slotAt("mobo"), board));
        hardwareSlots.add(faced(hw, map.psuSlot(), layout.slotAt("psu"), board));
        /*
         * The processors and the memory the case seats: one processor and its modules, and a second row of each on a
         * tower that takes a two-socket workstation board, wherever the layout keeps each one's slot.
         */
        for (int i = 0; i < map.cpuCount(); i++) {
            final String key = cpuKey(layout, i);
            if (key != null) {
                final GuiLayout.SlotPosition at = layout.slotAt(key);
                hardwareSlots.add(new BoardSlot(hw, map.cpuSlot(i), at.x(), at.y(), i, be::boardCpuSlots, board));
            }
        }
        for (int i = 0; i < map.ramCount() && layout.hasSlot("ram_" + i); i++) {
            final GuiLayout.SlotPosition at = layout.slotAt("ram_" + i);
            hardwareSlots.add(new BoardSlot(hw, map.ramSlot(i), at.x(), at.y(), i, be::boardRamSlots, board));
        }
        addBoardSlots(hardwareSlots, hw, layout, pcieKey + "_", map.pcieStart(), map.pcieCount(), be::boardPcieSlots,
                board);
        addBoardSlots(hardwareSlots, hw, layout, "disk_", map.diskStart(), map.diskCount(), be::boardDiskSlots, board);
        // The bays the case has, which take their drives whatever the board.
        for (int i = 0; i < be.caseBays(); i++) {
            hardwareSlots.add(faced(hw, map.bayStart() + i, layout.slotAt("bay_" + i), board));
        }
        addCoolingSlots(hardwareSlots, hw, geometry, be);
        final SlotGroup hardware = slots(hardwareSlots.toArray(Slot[]::new));
        this.hardwareSlots = hardwareSlots.size();

        final PlayerSlots player = playerInventory(inventory, layout.playerInventoryAt());
        shiftClick(hardware, player.all());
        shiftClick(player.all(), hardware);

        data(be.fields().menuData());

        button(BUTTON_POWER, p -> be.togglePower());
        button(BUTTON_AUTOSTART, p -> be.toggleAutoStart());
        button(BUTTON_SIDE_PANEL, p -> be.toggleSidePanel());
        button(BUTTON_FACE_BOARD, p -> showFace(AssemblyGeometry.Face.BOARD));
        button(BUTTON_FACE_COOLING, p -> showFace(AssemblyGeometry.Face.COOLING));
    }

    /** The face the screen shows. */
    public AssemblyGeometry.Face face() {
        return face;
    }

    /** Turns the screen to {@code face}: only its slots are there to click, on the player's side and the server's. */
    public void showFace(final AssemblyGeometry.Face face) {
        this.face = face;
    }

    /** Whether {@code slot} is one of the cooling face's, a cooler's or a fan's. */
    public boolean coolingSlot(final Slot slot) {
        return coolingSlots.contains(slot);
    }

    /** How the machine's heat stands, as the server last told the open screen. */
    public ThermalState thermal() {
        return computer.thermalShown();
    }

    /** How the machine's case cools: what its cooling face seats. */
    public CaseCooling cooling() {
        return computer.computer().cooling();
    }

    /** How many of the menu's first slots are the machine's parts, before the player's inventory. */
    public int hardwareSlots() {
        return hardwareSlots;
    }

    /** Why {@code stack} stays out of {@code slot}, when it is one of the machine's part slots; null otherwise. */
    @Nullable
    public Component refusal(final Slot slot, final ItemStack stack) {
        return PartSlots.refusal(slot, computer.getHardware(), stack, computer::refusal);
    }

    /** Where the block entity this menu assembles stands. */
    public BlockPos computerPos() {
        return computer.getBlockPos();
    }

    public String customName() {
        return computer.customName();
    }

    public int boardCpuSlots() {
        return computer.boardCpuSlots();
    }

    public int boardRamSlots() {
        return computer.boardRamSlots();
    }

    public int boardPcieSlots() {
        return computer.boardPcieSlots();
    }

    public int boardDiskSlots() {
        return computer.boardDiskSlots();
    }

    @Nullable
    public Era hardwareEra() {
        return computer.displayEra();
    }

    public boolean hasBoard() {
        return slots.get(0).hasItem();
    }

    public boolean hasPsu() {
        return slots.get(1).hasItem();
    }

    /** The supply in its slot, for the DRAW readout to name. */
    public ItemStack psu() {
        return slots.get(1).getItem();
    }

    /** What the machine's power draws, as the server last told the open screen. */
    public PowerShown powerShown() {
        return computer.powerShown();
    }

    public boolean isRunning() {
        return assembly.assemblyRunning();
    }

    public boolean buildValid() {
        return assembly.assemblyBuildValid();
    }

    /** Whether the machine is switched on with its fans turning, a blank board's too. */
    public boolean poweredUp() {
        return assembly.assemblyPoweredUp();
    }

    /** Whether the machine's board came out of the chain blank: NO FIRMWARE, its readings blank. */
    public boolean firmwareMissing() {
        return assembly.assemblyFirmwareMissing();
    }

    public long capacity() {
        return assembly.assemblyCapacity();
    }

    public long ramBuffer() {
        return assembly.assemblyRamBuffer();
    }

    public boolean isAutoStart() {
        return assembly.assemblyAutoStart();
    }

    /** Whether the case's left side is off. */
    public boolean isSidePanelOff() {
        return computer.sidePanelOff();
    }

    public boolean isOnNetwork() {
        return assembly.assemblyOnNetwork();
    }

    /** Whether the machine worked its disk a moment ago, as its disk lamp shows it. */
    public boolean diskBusy() {
        return computer.visualDiskBusy();
    }

    /** The era whose case the machine stands in, which seats the parts where that case has them. */
    protected static Era caseEra(final AbstractSmallComputerBlockEntity be) {
        final Era era = be.displayEra();
        return era == null ? HardwareEra.STANDARD : era;
    }

    /* The key of processor seat {@code index}: "cpu" on a case of one, "cpu_<i>" on one of more; none past them. */
    @Nullable
    private static String cpuKey(final GuiLayout layout, final int index) {
        if (layout.hasSlot("cpu")) {
            return index == 0 ? "cpu" : null;
        }
        final String key = "cpu_" + index;
        return layout.hasSlot(key) ? key : null;
    }

    private static void addBoardSlots(final List<Slot> into, final IItemHandler hw, final GuiLayout layout,
                                      final String keyPrefix, final int start, final int count,
                                      final IntSupplier boardLimit, final BooleanSupplier onFace) {
        for (int i = 0; i < count; i++) {
            final GuiLayout.SlotPosition at = layout.slotAt(keyPrefix + i);
            into.add(new BoardSlot(hw, start + i, at.x(), at.y(), i, boardLimit, onFace));
        }
    }

    /*
     * The cooling face's slots, after every board's: a cooler over each processor the board seats, and a fan in each of
     * the case's seats no radiator takes; a part already seated stays there to take out.
     */
    private void addCoolingSlots(final List<Slot> into, final IItemHandler hw, final AssemblyGeometry geometry,
                                 final AbstractSmallComputerBlockEntity be) {
        if (!geometry.cooled()) {
            return;
        }
        final GuiLayout cooling = geometry.coolingLayout();
        final ComputerHardwareLayout map = be.hardwareLayout();
        final BooleanSupplier coolingFace = () -> face == AssemblyGeometry.Face.COOLING;
        for (int i = 0; i < map.coolerCount() && cooling.hasSlot("cooler_" + i); i++) {
            final GuiLayout.SlotPosition at = cooling.slotAt("cooler_" + i);
            final Slot slot = new BoardSlot(hw, map.coolerStart() + i, at.x(), at.y(), i, be::boardCpuSlots,
                    coolingFace);
            into.add(slot);
            coolingSlots.add(slot);
        }
        for (int i = 0; i < map.fanCount() && cooling.hasSlot("fan_" + i); i++) {
            final int seat = i;
            final int index = map.fanStart() + i;
            final GuiLayout.SlotPosition at = cooling.slotAt("fan_" + i);
            final Slot slot = new FacedSlot(hw, index, at.x(), at.y(), () -> coolingFace.getAsBoolean()
                    && (be.fanSeatOpen(seat) || !hw.getStackInSlot(index).isEmpty()));
            into.add(slot);
            coolingSlots.add(slot);
        }
    }

    /* A slot shown while {@code shown} says so. */
    private static Slot faced(final IItemHandler hw, final int index, final GuiLayout.SlotPosition at,
                              final BooleanSupplier shown) {
        return new FacedSlot(hw, index, at.x(), at.y(), shown);
    }
}
