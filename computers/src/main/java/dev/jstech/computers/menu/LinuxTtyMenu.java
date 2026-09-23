/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.registry.ComputingMenus;
import dev.jstech.computers.os.ConsoleIdentity;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

/**
 * The Unix virtual console: the monitor of a system met at a Unix prompt that has no desktop environment, and
 * of a booted Arch/Gentoo live medium. Same command plumbing as the other terminals, but its own menu type so
 * the client opens the dedicated full-screen TTY instead of the MC-NET Command Prompt window.
 */
public class LinuxTtyMenu extends CommandPromptMenu {

    public LinuxTtyMenu(final int containerId, final Inventory playerInventory, final BlockPos monitorPos,
                        final BlockPos hostPos, @Nullable final HardwareEra era, final ConsoleIdentity console,
                        final long session) {
        super(ComputingMenus.LINUX_TTY_MENU.get(), containerId, playerInventory, monitorPos, hostPos,
                era, console, session);
    }

    public static LinuxTtyMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                           final RegistryFriendlyByteBuf buf) {
        final OpenData data = readOpenBuffer(buf);
        return new LinuxTtyMenu(containerId, playerInventory, data.monitor(), data.host(), data.era(),
                data.console(), data.session());
    }
}
