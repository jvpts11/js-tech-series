/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.launch;

import dev.jstech.core.menu.CoreMenu;
import dev.jstech.core.menu.MenuOpening;
import dev.jstech.core.menu.MenuValidity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Launch Panel's menu: no slots; its console reads the panel, whose state is sent to the players who see it, and
 * presses its controls through {@link LaunchPanel#PRESS}.
 */
public final class LaunchPanelMenu extends CoreMenu {

    private final LaunchPanelBlockEntity panel;

    public LaunchPanelMenu(final int containerId, final Inventory inventory, final LaunchPanelBlockEntity panel) {
        super(LaunchPanel.MENU.get(), containerId, inventory, MenuValidity.blockEntity(panel));
        this.panel = panel;
    }

    /** The menu on the player's side, for the panel whose place the server wrote. */
    public LaunchPanelMenu(final int containerId, final Inventory inventory, final RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, MenuOpening.blockEntity(inventory, buf, LaunchPanelBlockEntity.class));
    }

    /** The panel. */
    public LaunchPanelBlockEntity panel() {
        return this.panel;
    }
}
