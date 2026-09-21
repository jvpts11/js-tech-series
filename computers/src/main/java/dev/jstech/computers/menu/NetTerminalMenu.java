/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.os.ConsoleIdentity;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

/**
 * The network system's console: the monitor of a machine that runs MC-NET with no operating space on it.
 *
 * <p>Same command plumbing as the other terminals and a menu type of its own, so the client opens a console
 * in that system's voice. It is what a machine whose space has been taken off comes up at, and what an
 * addon's space replaces; a machine with a space installed never opens this, because its prompt is a heading
 * inside the space instead.
 */
public class NetTerminalMenu extends CommandPromptMenu {

    public NetTerminalMenu(final int containerId, final Inventory playerInventory, final BlockPos monitorPos,
                           final BlockPos hostPos, @Nullable final HardwareEra era,
                           final ConsoleIdentity console, final long session) {
        super(ComputingModule.NET_TERMINAL_MENU.get(), containerId, playerInventory, monitorPos, hostPos,
                era, console, session);
    }

    public static NetTerminalMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                              final RegistryFriendlyByteBuf buf) {
        final OpenData data = readOpenBuffer(buf);
        return new NetTerminalMenu(containerId, playerInventory, data.monitor(), data.host(), data.era(),
                data.console(), data.session());
    }
}
