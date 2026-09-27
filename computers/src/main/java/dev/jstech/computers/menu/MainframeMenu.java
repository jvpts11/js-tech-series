/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.gui.layout.MainframeLayout;
import dev.jstech.computers.registry.ComputingMenus;
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.menu.CoreMenu;
import dev.jstech.core.menu.MenuOpening;
import dev.jstech.core.menu.MenuValidity;
import dev.jstech.core.menu.PlayerSlots;
import dev.jstech.core.menu.SlotGroup;
import dev.jstech.core.network.FailoverRole;
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
 * Menu for the Mainframe: the 24 hardware slots (motherboard, CPUs, RAM, GPUs, PSU, disks, each restricted to its
 * component category by the block entity's item handler) plus the player inventory, with powered/capacity/queues/
 * buffer synced for the screen.
 */
public class MainframeMenu extends CoreMenu {

    private final MainframeBlockEntity blockEntity;

    public static final int BUTTON_POWER = 0;
    public static final int BUTTON_AUTOSTART = 1;
    public static final int BUTTON_FAILOVER = 2;

    public MainframeMenu(final int containerId, final Inventory playerInventory,
                         final MainframeBlockEntity be) {
        super(ComputingMenus.MAINFRAME_MENU.get(), containerId, playerInventory, MenuValidity.blockEntity(be));
        this.blockEntity = be;
        final GuiLayout layout = MainframeLayout.layout();
        final IItemHandler hardware = be.getInventory();

        final List<Slot> hardwareSlots = new ArrayList<>();
        hardwareSlots.add(slot(hardware, MainframeBlockEntity.MOTHERBOARD_SLOT, layout.slotAt("mobo")));
        hardwareSlots.add(slot(hardware, MainframeBlockEntity.PSU_SLOT, layout.slotAt("psu")));
        for (int i = 0; i < MainframeBlockEntity.CPU_SLOTS; i++) {
            final GuiLayout.SlotPosition at = layout.slotAt("cpu_" + i);
            hardwareSlots.add(new BoardSlot(hardware, MainframeBlockEntity.CPU_SLOTS_START + i, at.x(), at.y(), i,
                    be::boardCpuSlots));
        }
        for (int i = 0; i < MainframeBlockEntity.RAM_SLOTS; i++) {
            final GuiLayout.SlotPosition at = layout.slotAt("ram_" + i);
            hardwareSlots.add(new BoardSlot(hardware, MainframeBlockEntity.RAM_SLOTS_START + i, at.x(), at.y(), i,
                    be::boardRamSlots));
        }
        for (int i = 0; i < MainframeBlockEntity.GPU_SLOTS; i++) {
            final GuiLayout.SlotPosition at = layout.slotAt("gpu_" + i);
            hardwareSlots.add(new BoardSlot(hardware, MainframeBlockEntity.GPU_SLOTS_START + i, at.x(), at.y(), i,
                    be::boardPcieSlots));
        }
        for (int i = 0; i < MainframeBlockEntity.DISK_SLOTS; i++) {
            final GuiLayout.SlotPosition at = layout.slotAt("disk_" + i);
            hardwareSlots.add(new BoardSlot(hardware, MainframeBlockEntity.DISK_SLOTS_START + i, at.x(), at.y(), i,
                    be::boardDiskSlots));
        }
        final SlotGroup hw = slots(hardwareSlots.toArray(Slot[]::new));

        final PlayerSlots player = playerInventory(playerInventory, layout.playerInventoryAt());
        shiftClick(hw, player.all());
        shiftClick(player.all(), hw);

        data(be.fields().menuData());

        button(BUTTON_POWER, p -> blockEntity.togglePower());
        button(BUTTON_AUTOSTART, p -> blockEntity.toggleAutoStart());
        button(BUTTON_FAILOVER, p -> blockEntity.toggleFailover());
    }

    public static MainframeMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                            final RegistryFriendlyByteBuf buf) {
        return new MainframeMenu(containerId, playerInventory,
                MenuOpening.blockEntity(playerInventory, buf, MainframeBlockEntity.class));
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

    public BlockPos blockPos() {
        return blockEntity.getBlockPos();
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

    public boolean isRunning() {
        return blockEntity.assemblyRunning();
    }

    public boolean buildValid() {
        return blockEntity.assemblyBuildValid();
    }

    public long capacity() {
        return blockEntity.assemblyCapacity();
    }

    public int parallelQueues() {
        return blockEntity.assemblyParallelQueues();
    }

    public long ramBuffer() {
        return blockEntity.assemblyRamBuffer();
    }

    public boolean isAutoStart() {
        return blockEntity.assemblyAutoStart();
    }

    public boolean isManualOn() {
        return blockEntity.assemblyManualOn();
    }

    public int networkState() {
        return blockEntity.assemblyNetworkState();
    }

    public int pendingOps() {
        return blockEntity.assemblyPendingOps();
    }

    public int runningOps() {
        return blockEntity.assemblyRunningOps();
    }

    public long completedOps() {
        return blockEntity.assemblyCompletedOps();
    }

    public boolean failoverEnabled() {
        return blockEntity.assemblyFailoverEnabled();
    }

    public FailoverRole failoverRole() {
        return blockEntity.assemblyFailoverRole();
    }
}
