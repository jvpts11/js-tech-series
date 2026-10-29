/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.flight;

import dev.jstech.core.menu.CoreMenu;
import dev.jstech.core.menu.MenuOpening;
import dev.jstech.core.menu.MenuValidity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Flight Computer's menu: no slots, since a computer holds nothing; its screen reads the computer, whose state is
 * sent to the players who see it, presses its keys through {@link FlightComputer#PRESS}, marks its landing spot
 * through {@link FlightComputer#MARK} and asks for its landing map through {@link FlightComputer#MAP}.
 */
public final class FlightComputerMenu extends CoreMenu {

    private final FlightComputerBlockEntity computer;

    public FlightComputerMenu(final int containerId, final Inventory inventory,
                              final FlightComputerBlockEntity computer) {
        super(FlightComputer.MENU.get(), containerId, inventory, MenuValidity.blockEntity(computer));
        this.computer = computer;
    }

    /** The menu on the player's side, for the computer whose place the server wrote. */
    public FlightComputerMenu(final int containerId, final Inventory inventory, final RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, MenuOpening.blockEntity(inventory, buf, FlightComputerBlockEntity.class));
    }

    /** The computer. */
    public FlightComputerBlockEntity computer() {
        return this.computer;
    }
}
