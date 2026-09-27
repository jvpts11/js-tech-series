/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.block.NetworkGatewayBlock;
import dev.jstech.computers.blockentity.NetworkGatewayBlockEntity;
import dev.jstech.computers.gui.layout.NetworkGatewayLayout;
import dev.jstech.computers.registry.ComputingMenus;
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.menu.CoreMenu;
import dev.jstech.core.menu.MenuOpening;
import dev.jstech.core.menu.MenuValidity;
import dev.jstech.core.menu.PlayerSlots;
import dev.jstech.core.menu.SlotGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * The Network Gateway's own screen: its nine-slot item buffer and the player's inventory. Pulled items
 * land in the buffer and pushed items leave from it; everything else about the Gateway is set on the host
 * computer, so nothing here is a control.
 */
public class NetworkGatewayMenu extends CoreMenu {

    private final NetworkGatewayBlockEntity blockEntity;

    public NetworkGatewayMenu(final int containerId, final Inventory playerInventory,
                              final NetworkGatewayBlockEntity be) {
        super(ComputingMenus.NETWORK_GATEWAY_MENU.get(), containerId, playerInventory,
                MenuValidity.block(be.getLevel(), be.getBlockPos(), NetworkGatewayBlock.class));
        this.blockEntity = be;
        final GuiLayout layout = NetworkGatewayLayout.layout();
        final Slot[] bufferSlots = new Slot[NetworkGatewayBlockEntity.BUFFER_SLOTS];
        for (int i = 0; i < bufferSlots.length; i++) {
            final GuiLayout.SlotPosition at = layout.slotAt("buffer" + i);
            bufferSlots[i] = new SlotItemHandler(be.buffer(), i, at.x(), at.y());
        }
        final SlotGroup buffer = slots(bufferSlots);
        final PlayerSlots player = playerInventory(playerInventory, layout.playerInventoryAt());
        shiftClick(buffer, player.all());
        shiftClick(player.all(), buffer);
    }

    public static NetworkGatewayMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                                 final RegistryFriendlyByteBuf buf) {
        return new NetworkGatewayMenu(containerId, playerInventory,
                MenuOpening.blockEntity(playerInventory, buf, NetworkGatewayBlockEntity.class));
    }

    public NetworkGatewayBlockEntity blockEntity() {
        return blockEntity;
    }

    public BlockPos blockEntityPos() {
        return blockEntity.getBlockPos();
    }
}
