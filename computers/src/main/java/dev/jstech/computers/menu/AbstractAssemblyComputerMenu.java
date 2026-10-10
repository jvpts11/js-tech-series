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
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.menu.CoreMenu;
import dev.jstech.core.menu.MenuValidity;
import dev.jstech.core.menu.PlayerSlots;
import dev.jstech.core.menu.SlotGroup;
import dev.jstech.core.tier.HardwareEra;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
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

    public static final int BUTTON_POWER = 0;
    public static final int BUTTON_AUTOSTART = 1;
    public static final int BUTTON_SIDE_PANEL = 2;

    /**
     * Builds the menu over {@code be}. {@code pcieKey} is the layout key prefix of the machine's expansion slots
     * ("gpu" or "pcie"), which the hardware layout's PCIe range fills.
     */
    protected <B extends AbstractSmallComputerBlockEntity & IAssemblyComputer> AbstractAssemblyComputerMenu(
            final MenuType<?> type, final int containerId, final Inventory inventory, final B be,
            final GuiLayout layout, final String pcieKey) {
        super(type, containerId, inventory, MenuValidity.blockEntity(be));
        this.computer = be;
        this.assembly = be;
        final ComputerHardwareLayout map = be.hardwareLayout();
        final IItemHandler hw = be.getHardware();

        final List<Slot> hardwareSlots = new ArrayList<>();
        hardwareSlots.add(slot(hw, map.motherboardSlot(), layout.slotAt("mobo")));
        hardwareSlots.add(slot(hw, map.psuSlot(), layout.slotAt("psu")));
        final GuiLayout.SlotPosition cpuAt = layout.slotAt("cpu");
        hardwareSlots.add(new BoardSlot(hw, map.cpuStart(), cpuAt.x(), cpuAt.y(), 0, be::boardCpuSlots));
        addBoardSlots(hardwareSlots, hw, layout, "ram_", map.ramStart(), map.ramCount(), be::boardRamSlots);
        addBoardSlots(hardwareSlots, hw, layout, pcieKey + "_", map.pcieStart(), map.pcieCount(), be::boardPcieSlots);
        addBoardSlots(hardwareSlots, hw, layout, "disk_", map.diskStart(), map.diskCount(), be::boardDiskSlots);
        final SlotGroup hardware = slots(hardwareSlots.toArray(Slot[]::new));

        final PlayerSlots player = playerInventory(inventory, layout.playerInventoryAt());
        shiftClick(hardware, player.all());
        shiftClick(player.all(), hardware);

        data(be.fields().menuData());

        button(BUTTON_POWER, p -> be.togglePower());
        button(BUTTON_AUTOSTART, p -> be.toggleAutoStart());
        button(BUTTON_SIDE_PANEL, p -> be.toggleSidePanel());
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
    public HardwareEra hardwareEra() {
        return computer.displayEra();
    }

    public boolean hasBoard() {
        return slots.get(0).hasItem();
    }

    public boolean hasPsu() {
        return slots.get(1).hasItem();
    }

    public boolean isRunning() {
        return assembly.assemblyRunning();
    }

    public boolean buildValid() {
        return assembly.assemblyBuildValid();
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

    private static void addBoardSlots(final List<Slot> into, final IItemHandler hw, final GuiLayout layout,
                                      final String keyPrefix, final int start, final int count,
                                      final IntSupplier boardLimit) {
        for (int i = 0; i < count; i++) {
            final GuiLayout.SlotPosition at = layout.slotAt(keyPrefix + i);
            into.add(new BoardSlot(hw, start + i, at.x(), at.y(), i, boardLimit));
        }
    }
}
