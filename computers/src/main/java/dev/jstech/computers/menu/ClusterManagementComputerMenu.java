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
import dev.jstech.core.menu.MenuOpening;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/** The Cluster Management Computer's assembly menu: the hardware slots and the screen's numbers. */
public class ClusterManagementComputerMenu extends AbstractAssemblyComputerMenu {

    private final ClusterManagementComputerBlockEntity blockEntity;

    public ClusterManagementComputerMenu(final int containerId, final Inventory playerInventory,
                                         final ClusterManagementComputerBlockEntity be) {
        super(ComputingMenus.CLUSTER_MANAGEMENT_COMPUTER_MENU.get(), containerId, playerInventory, be,
                ClusterManagementComputerLayout.layout(), "pcie");
        this.blockEntity = be;
    }

    public static ClusterManagementComputerMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                                            final RegistryFriendlyByteBuf buf) {
        return new ClusterManagementComputerMenu(containerId, playerInventory,
                MenuOpening.blockEntity(playerInventory, buf, ClusterManagementComputerBlockEntity.class));
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