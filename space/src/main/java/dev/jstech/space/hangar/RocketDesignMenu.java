/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.hangar;

import dev.jstech.core.menu.CoreMenu;
import dev.jstech.core.menu.MenuOpening;
import dev.jstech.core.menu.MenuValidity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/**
 * The menu of a Rocket Hangar's design screen: no slots, since a design is drawn, not stored in items; its screen reads
 * the hangar's terminal, whose design, plans and plan to build are sent to the players who see it, and changes them
 * through {@link RocketDesigner}'s calls.
 */
public final class RocketDesignMenu extends CoreMenu {

    private final HangarTerminalBlockEntity terminal;

    public RocketDesignMenu(final int containerId, final Inventory inventory,
                            final HangarTerminalBlockEntity terminal) {
        super(RocketDesigner.MENU.get(), containerId, inventory, MenuValidity.blockEntity(terminal));
        this.terminal = terminal;
    }

    /** The menu on the player's side, for the terminal whose place the server wrote. */
    public RocketDesignMenu(final int containerId, final Inventory inventory, final RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, MenuOpening.blockEntity(inventory, buf, HangarTerminalBlockEntity.class));
    }

    /** The hangar's terminal. */
    public HangarTerminalBlockEntity terminal() {
        return this.terminal;
    }
}
