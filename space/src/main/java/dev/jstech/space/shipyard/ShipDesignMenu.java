/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.shipyard;

import dev.jstech.core.menu.CoreMenu;
import dev.jstech.core.menu.MenuOpening;
import dev.jstech.core.menu.MenuValidity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/**
 * The menu of a Small Shipyard's builder screen: no slots, since a ship is designed, not stored in items; its screen
 * reads the shipyard's terminal, whose design, plans and plan to build are sent to the players who see it, and changes
 * them through {@link ShipDesigner}'s calls.
 */
public final class ShipDesignMenu extends CoreMenu {

    private final ShipyardTerminalBlockEntity terminal;

    public ShipDesignMenu(final int containerId, final Inventory inventory,
                          final ShipyardTerminalBlockEntity terminal) {
        super(ShipDesigner.MENU.get(), containerId, inventory, MenuValidity.blockEntity(terminal));
        this.terminal = terminal;
    }

    /** The menu on the player's side, for the terminal whose place the server wrote. */
    public ShipDesignMenu(final int containerId, final Inventory inventory, final RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, MenuOpening.blockEntity(inventory, buf, ShipyardTerminalBlockEntity.class));
    }

    /** The shipyard's terminal. */
    public ShipyardTerminalBlockEntity terminal() {
        return this.terminal;
    }
}
