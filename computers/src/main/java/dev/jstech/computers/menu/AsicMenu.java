/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.block.AsicBlock;
import dev.jstech.computers.blockentity.AsicBlockEntity;
import dev.jstech.computers.gui.layout.AsicLayout;
import dev.jstech.computers.registry.ComputingMenus;
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.menu.CoreMenu;
import dev.jstech.core.menu.MenuOpening;
import dev.jstech.core.menu.MenuValidity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;

/**
 * An ASIC's window (computers/docs/design/teracoin.md): the inventory, the pool's button, which moves it to the
 * network's next pool, and Solo; its address comes with its own payload, typed.
 */
public class AsicMenu extends CoreMenu {

    private final AsicBlockEntity asic;

    /** The pool's button: the network's next pool. */
    public static final int BUTTON_NEXT_POOL = 0;
    /** Solo: mining alone. */
    public static final int BUTTON_SOLO = 1;

    public AsicMenu(final int containerId, final Inventory playerInventory, final AsicBlockEntity asic) {
        super(ComputingMenus.ASIC_MENU.get(), containerId, playerInventory,
                MenuValidity.block(asic.getLevel(), asic.getBlockPos(), AsicBlock.class));
        this.asic = asic;
        final GuiLayout layout = AsicLayout.layout(asic.model().boards());
        playerInventory(playerInventory, layout.playerInventoryAt());
        button(BUTTON_NEXT_POOL, player -> {
            if (player.level() instanceof ServerLevel level) {
                asic.nextPool(level);
            }
        });
        button(BUTTON_SOLO, player -> asic.setAlone());
    }

    public static AsicMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                       final RegistryFriendlyByteBuf buf) {
        return new AsicMenu(containerId, playerInventory,
                MenuOpening.blockEntity(playerInventory, buf, AsicBlockEntity.class));
    }

    public AsicBlockEntity asic() {
        return asic;
    }

    public BlockPos asicPos() {
        return asic.getBlockPos();
    }
}
