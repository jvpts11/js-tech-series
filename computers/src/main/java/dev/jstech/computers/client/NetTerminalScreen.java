/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.menu.NetTerminalMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * MC-NET bare: the whole monitor glass is the console, and the machine is the prompt.
 *
 * <p>This is a network machine with no operating space installed, which is the same thing a Linux with no
 * desktop is. It greets in its house's voice, says what is missing and how to put one back, and stands at
 * {@code SYSTEM:>}, which names the machine rather than a place, because a flat disk has no places.
 */
public final class NetTerminalScreen extends CommandPromptScreen<NetTerminalMenu> {

    public NetTerminalScreen(final NetTerminalMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected boolean netStyle() {
        return true;
    }

    @Override
    protected boolean bareTerminal() {
        return true;
    }
}
