/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.NetworkGatewayBlock;
import dev.jstech.computers.blockentity.NetworkGatewayBlockEntity;
import dev.jstech.computers.gui.layout.NetworkGatewayLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The Network Gateway's own screen: its nine-slot item buffer and the player's inventory. Pulled items
 * land in the buffer and pushed items leave from it; everything else about the Gateway is set on the host
 * computer, so nothing here is a control.
 */
public class NetworkGatewayMenu extends AbstractComputerMenu {

    private final NetworkGatewayBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    public NetworkGatewayMenu(final int containerId, final Inventory playerInventory,
                              final NetworkGatewayBlockEntity be) {
        super(ComputingModule.NETWORK_GATEWAY_MENU.get(), containerId);
        this.blockEntity = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        for (int i = 0; i < NetworkGatewayBlockEntity.BUFFER_SLOTS; i++) {
            addSlot(new SlotItemHandler(be.buffer(), i, NetworkGatewayLayout.bufferX(i) + 1,
                    NetworkGatewayLayout.BUFFER_Y + 1));
        }
        addPlayerInventory(playerInventory, NetworkGatewayLayout.INV_X, NetworkGatewayLayout.INV_Y);
    }

    @Nullable
    public static NetworkGatewayMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                                 final RegistryFriendlyByteBuf buf) {
        if (playerInventory.player.level().getBlockEntity(buf.readBlockPos())
                instanceof NetworkGatewayBlockEntity be) {
            return new NetworkGatewayMenu(containerId, playerInventory, be);
        }
        return null;
    }

    public NetworkGatewayBlockEntity blockEntity() {
        return blockEntity;
    }

    public BlockPos blockEntityPos() {
        return blockEntity.getBlockPos();
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        return quickMoveBetweenContainerAndPlayer(player, index, NetworkGatewayBlockEntity.BUFFER_SLOTS);
    }

    @Override
    public boolean stillValid(final Player player) {
        return access.evaluate((level, pos) -> level.getBlockState(pos).getBlock() instanceof NetworkGatewayBlock
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0, true);
    }
}
