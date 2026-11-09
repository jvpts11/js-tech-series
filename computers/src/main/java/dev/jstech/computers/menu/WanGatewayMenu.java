/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.blockentity.WanGatewayBlockEntity;
import dev.jstech.computers.gui.layout.WanGatewayLayout;
import dev.jstech.computers.registry.ComputingMenus;
import dev.jstech.core.menu.MenuOpening;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/**
 * Menu for the WAN Gateway Computer: a cabinet's slots and buttons ({@link AbstractCabinetMenu}), with what only a
 * gateway shows synced for the screen: whether it is on a network, and what the line of each of its four ports does.
 */
public class WanGatewayMenu extends AbstractCabinetMenu<WanGatewayBlockEntity> {

    public WanGatewayMenu(final int containerId, final Inventory playerInventory, final WanGatewayBlockEntity be) {
        super(ComputingMenus.WAN_GATEWAY_MENU.get(), containerId, playerInventory, be, WanGatewayLayout.geometry());
    }

    public static WanGatewayMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                             final RegistryFriendlyByteBuf buf) {
        return new WanGatewayMenu(containerId, playerInventory,
                MenuOpening.blockEntity(playerInventory, buf, WanGatewayBlockEntity.class));
    }

    public boolean networked() {
        return blockEntity().assemblyNetworked();
    }

    public boolean diskBusy() {
        return blockEntity().assemblyDiskBusy();
    }

    /** What port {@code port}'s line does, 1 to 4, as {@link WanGatewayBlockEntity#line} says. */
    public int line(final int port) {
        return blockEntity().line(port);
    }

    /** How many of its ports hold a card. */
    public int cardedPorts() {
        return blockEntity().cardedPorts();
    }

    /** How many of its ports have their line up. */
    public int portsUp() {
        return blockEntity().portsUp();
    }
}
