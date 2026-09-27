/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.block.ServerRouterBlock;
import dev.jstech.computers.blockentity.ServerRouterBlockEntity;
import dev.jstech.computers.datacenter.LoadBalanceMode;
import dev.jstech.computers.registry.ComputingMenus;
import dev.jstech.core.menu.CoreMenu;
import dev.jstech.core.menu.MenuOpening;
import dev.jstech.core.menu.MenuValidity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

/**
 * Menu for the Server Router's config GUI: no item slots; it exposes the live topology summary the router writes
 * into its declared fields (input face, managed-rack budget, and one row per output-face section with its
 * rack/server counts and load-balance mode), and routes a row's mode-cycle click back to the router.
 */
public class ServerRouterMenu extends CoreMenu {

    private final ServerRouterBlockEntity blockEntity;
    private final BlockPos routerPos;
    private final String initialName;

    public ServerRouterMenu(final int containerId, final Inventory playerInventory,
                            final ServerRouterBlockEntity be, final String initialName) {
        super(ComputingMenus.SERVER_ROUTER_MENU.get(), containerId, playerInventory,
                MenuValidity.block(be.getLevel(), be.getBlockPos(), ServerRouterBlock.class));
        this.blockEntity = be;
        this.routerPos = be.getBlockPos();
        this.initialName = initialName;
        data(be.fields().menuData());
        for (final Direction face : Direction.values()) {
            button(face.get3DDataValue(), player -> blockEntity.cycleLoadBalanceMode(face));
        }
    }

    public static ServerRouterMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                               final RegistryFriendlyByteBuf buf) {
        final ServerRouterBlockEntity be = MenuOpening.blockEntity(playerInventory, buf, ServerRouterBlockEntity.class);
        final String name = buf.readUtf();
        return new ServerRouterMenu(containerId, playerInventory, be, name);
    }

    public BlockPos routerPos() {
        return routerPos;
    }

    public String initialName() {
        return initialName;
    }

    @Nullable
    public Direction inputFace() {
        return blockEntity.inputFaceShown();
    }

    public int managedRacks() {
        return blockEntity.managedRacksShown();
    }

    public int maxRacks() {
        return blockEntity.maxRacksShown();
    }

    public boolean overCapacity() {
        return blockEntity.overCapacityShown();
    }

    public int sectionCount() {
        return blockEntity.sectionCountShown();
    }

    @Nullable
    public Direction sectionFace(final int i) {
        return blockEntity.sectionFaceShown(i);
    }

    public int sectionRacks(final int i) {
        return blockEntity.sectionRacksShown(i);
    }

    public int sectionServers(final int i) {
        return blockEntity.sectionServersShown(i);
    }

    public LoadBalanceMode sectionMode(final int i) {
        return blockEntity.sectionModeShown(i);
    }
}
