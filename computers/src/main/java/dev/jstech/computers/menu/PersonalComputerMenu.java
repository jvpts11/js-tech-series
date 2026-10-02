/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.gui.layout.PersonalComputerLayout;
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

/**
 * Menu for the Personal Computer: a single hardware-assembly surface (motherboard, PSU, CPU, RAM, GPU, disks, each
 * restricted to its component category and clamped to the count the installed motherboard offers) plus the player
 * inventory.
 */
public class PersonalComputerMenu extends AbstractAssemblyComputerMenu {

    private final PersonalComputerBlockEntity blockEntity;

    public static final int BUTTON_POWER = 0;
    public static final int BUTTON_AUTOSTART = 1;
    public static final int BUTTON_SIDE_PANEL = 2;

    public PersonalComputerMenu(final int containerId, final Inventory playerInventory,
                                final PersonalComputerBlockEntity be) {
        super(ComputingMenus.PERSONAL_COMPUTER_MENU.get(), containerId, playerInventory, MenuValidity.blockEntity(be));
        this.blockEntity = be;
        final GuiLayout layout = PersonalComputerLayout.layout();
        final IItemHandler hw = be.getHardware();

        final List<Slot> hardwareSlots = new ArrayList<>();
        hardwareSlots.add(slot(hw, PersonalComputerBlockEntity.MOTHERBOARD_SLOT, layout.slotAt("mobo")));
        hardwareSlots.add(slot(hw, PersonalComputerBlockEntity.PSU_SLOT, layout.slotAt("psu")));
        final GuiLayout.SlotPosition cpuAt = layout.slotAt("cpu");
        hardwareSlots.add(new BoardSlot(hw, PersonalComputerBlockEntity.CPU_SLOT, cpuAt.x(), cpuAt.y(), 0,
                be::boardCpuSlots));
        for (int i = 0; i < PersonalComputerBlockEntity.RAM_SLOTS; i++) {
            final GuiLayout.SlotPosition at = layout.slotAt("ram_" + i);
            hardwareSlots.add(new BoardSlot(hw, PersonalComputerBlockEntity.RAM_SLOTS_START + i, at.x(), at.y(), i,
                    be::boardRamSlots));
        }
        for (int i = 0; i < PersonalComputerBlockEntity.GPU_SLOTS; i++) {
            final GuiLayout.SlotPosition at = layout.slotAt("gpu_" + i);
            hardwareSlots.add(new BoardSlot(hw, PersonalComputerBlockEntity.GPU_SLOTS_START + i, at.x(), at.y(), i,
                    be::boardPcieSlots));
        }
        for (int i = 0; i < PersonalComputerBlockEntity.DISK_SLOTS; i++) {
            final GuiLayout.SlotPosition at = layout.slotAt("disk_" + i);
            hardwareSlots.add(new BoardSlot(hw, PersonalComputerBlockEntity.DISK_SLOTS_START + i, at.x(), at.y(), i,
                    be::boardDiskSlots));
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

    public static PersonalComputerMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                                   final RegistryFriendlyByteBuf buf) {
        return new PersonalComputerMenu(containerId, playerInventory,
                MenuOpening.blockEntity(playerInventory, buf, PersonalComputerBlockEntity.class));
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

    public int networkServerCount() {
        return blockEntity.assemblyServerCount();
    }
}
