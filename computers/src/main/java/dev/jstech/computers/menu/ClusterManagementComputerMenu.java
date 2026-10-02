/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.blockentity.ClusterManagementComputerBlockEntity;
import dev.jstech.computers.gui.layout.ClusterManagementComputerLayout;
import dev.jstech.computers.registry.ComputingMenus;
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.menu.MenuOpening;
import dev.jstech.core.menu.MenuValidity;
import dev.jstech.core.menu.PlayerSlots;
import dev.jstech.core.menu.SlotGroup;
import dev.jstech.core.tier.HardwareEra;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/** The Cluster Management Computer's assembly menu: the hardware slots and the screen's numbers. */
public class ClusterManagementComputerMenu extends AbstractAssemblyComputerMenu {

    private final ClusterManagementComputerBlockEntity blockEntity;

    public static final int BUTTON_POWER = 0;
    public static final int BUTTON_AUTOSTART = 1;
    public static final int BUTTON_SIDE_PANEL = 2;

    public ClusterManagementComputerMenu(final int containerId, final Inventory playerInventory,
                                         final ClusterManagementComputerBlockEntity be) {
        super(ComputingMenus.CLUSTER_MANAGEMENT_COMPUTER_MENU.get(), containerId, playerInventory,
                MenuValidity.blockEntity(be));
        this.blockEntity = be;
        final GuiLayout layout = ClusterManagementComputerLayout.layout();
        final IItemHandler hw = be.getHardware();

        final List<Slot> hardwareSlots = new ArrayList<>();
        hardwareSlots.add(slot(hw, ClusterManagementComputerBlockEntity.MOTHERBOARD_SLOT, layout.slotAt("mobo")));
        hardwareSlots.add(slot(hw, ClusterManagementComputerBlockEntity.PSU_SLOT, layout.slotAt("psu")));
        final GuiLayout.SlotPosition cpuAt = layout.slotAt("cpu");
        hardwareSlots.add(new BoardSlot(hw, ClusterManagementComputerBlockEntity.CPU_SLOT, cpuAt.x(), cpuAt.y(), 0,
                be::boardCpuSlots));
        for (int i = 0; i < ClusterManagementComputerBlockEntity.RAM_SLOTS; i++) {
            final GuiLayout.SlotPosition at = layout.slotAt("ram_" + i);
            hardwareSlots.add(new BoardSlot(hw, ClusterManagementComputerBlockEntity.RAM_SLOTS_START + i, at.x(),
                    at.y(), i, be::boardRamSlots));
        }
        for (int i = 0; i < ClusterManagementComputerBlockEntity.PCIE_SLOTS; i++) {
            final GuiLayout.SlotPosition at = layout.slotAt("pcie_" + i);
            hardwareSlots.add(new BoardSlot(hw, ClusterManagementComputerBlockEntity.PCIE_SLOTS_START + i, at.x(),
                    at.y(), i, be::boardPcieSlots));
        }
        for (int i = 0; i < ClusterManagementComputerBlockEntity.DISK_SLOTS; i++) {
            final GuiLayout.SlotPosition at = layout.slotAt("disk_" + i);
            hardwareSlots.add(new BoardSlot(hw, ClusterManagementComputerBlockEntity.DISK_SLOTS_START + i, at.x(),
                    at.y(), i, be::boardDiskSlots));
        }
        final SlotGroup hardware = slots(hardwareSlots.toArray(Slot[]::new));

        final PlayerSlots player = playerInventory(playerInventory, layout.playerInventoryAt());
        shiftClick(hardware, player.all());
        shiftClick(player.all(), hardware);

        data(be.fields().menuData());

        button(BUTTON_POWER, p -> blockEntity.togglePower());
        button(BUTTON_AUTOSTART, p -> blockEntity.toggleAutoStart());
        button(BUTTON_SIDE_PANEL, p -> blockEntity.toggleSidePanel());
    }

    public static ClusterManagementComputerMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                                            final RegistryFriendlyByteBuf buf) {
        return new ClusterManagementComputerMenu(containerId, playerInventory,
                MenuOpening.blockEntity(playerInventory, buf, ClusterManagementComputerBlockEntity.class));
    }

    @Override
    public BlockPos computerPos() {
        return blockEntity.getBlockPos();
    }

    public String customName() {
        return blockEntity.customName();
    }

    public int boardCpuSlots() {
        return blockEntity.boardCpuSlots();
    }

    public int boardRamSlots() {
        return blockEntity.boardRamSlots();
    }

    public int boardPcieSlots() {
        return blockEntity.boardPcieSlots();
    }

    public int boardDiskSlots() {
        return blockEntity.boardDiskSlots();
    }

    @Nullable
    public HardwareEra hardwareEra() {
        return blockEntity.displayEra();
    }

    public boolean hasBoard() {
        return slots.get(0).hasItem();
    }

    public boolean hasPsu() {
        return slots.get(1).hasItem();
    }

    public boolean isRunning() {
        return blockEntity.assemblyRunning();
    }

    public boolean buildValid() {
        return blockEntity.assemblyBuildValid();
    }

    public long capacity() {
        return blockEntity.assemblyCapacity();
    }

    public long ramBuffer() {
        return blockEntity.assemblyRamBuffer();
    }

    public boolean isAutoStart() {
        return blockEntity.assemblyAutoStart();
    }

    /** Whether the case's left side is off. */
    public boolean isSidePanelOff() {
        return blockEntity.sidePanelOff();
    }

    public boolean isOnNetwork() {
        return blockEntity.assemblyOnNetwork();
    }

    public boolean hasCard() {
        return blockEntity.assemblyHasCard();
    }

    public int supercomputers() {
        return blockEntity.assemblySupercomputers();
    }

    public int datacenters() {
        return blockEntity.assemblyDatacenters();
    }

    public int lanes() {
        return blockEntity.assemblyLanes();
    }

    public boolean managerInstalled() {
        return blockEntity.assemblyManagerInstalled();
    }
}
