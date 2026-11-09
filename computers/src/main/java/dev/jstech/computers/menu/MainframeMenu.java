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
import dev.jstech.core.jstechseries.network.FailoverRole;
import dev.jstech.core.menu.MenuOpening;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/**
 * Menu for the Mainframe: a cabinet's slots and buttons ({@link AbstractCabinetMenu}), with its failover switch and
 * what only a Mainframe shows synced for the screen: its queues and their speeds, its RAM buffer, its network's state
 * and its operations.
 */
public class MainframeMenu extends AbstractCabinetMenu<MainframeBlockEntity> {

    public static final int BUTTON_FAILOVER = 2;

    public MainframeMenu(final int containerId, final Inventory playerInventory, final MainframeBlockEntity be) {
        super(ComputingMenus.MAINFRAME_MENU.get(), containerId, playerInventory, be, MainframeLayout.geometry());
        button(BUTTON_FAILOVER, p -> be.toggleFailover());
    }

    public static MainframeMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                            final RegistryFriendlyByteBuf buf) {
        return new MainframeMenu(containerId, playerInventory,
                MenuOpening.blockEntity(playerInventory, buf, MainframeBlockEntity.class));
    }

    public int parallelQueues() {
        return blockEntity().assemblyParallelQueues();
    }

    /** How fast one of its queues runs, in items per tick: 0 the processor's, then a graphics card's each. */
    public int queueSpeed(final int queue) {
        return blockEntity().assemblyQueueSpeed(queue);
    }

    public long ramBuffer() {
        return blockEntity().assemblyRamBuffer();
    }

    public int networkState() {
        return blockEntity().assemblyNetworkState();
    }

    public int pendingOps() {
        return blockEntity().assemblyPendingOps();
    }

    public int runningOps() {
        return blockEntity().assemblyRunningOps();
    }

    public long completedOps() {
        return blockEntity().assemblyCompletedOps();
    }

    public boolean failoverEnabled() {
        return blockEntity().assemblyFailoverEnabled();
    }

    public FailoverRole failoverRole() {
        return blockEntity().assemblyFailoverRole();
    }
}
