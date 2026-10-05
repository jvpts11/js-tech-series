/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.api.client.IOperatingSpace;
import dev.jstech.computers.menu.ComputerTerminalMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * One opening of a machine's operating space, as a space's screen is handed it: the terminal menu behind the narrow
 * face the API shows, so nothing of the menu's own reaches a space but what that face names.
 *
 * @param terminal the menu the server opened for the player at the machine
 */
record TerminalSpaceOpening(ComputerTerminalMenu terminal) implements IOperatingSpace {

    @Override
    public AbstractContainerMenu menu() {
        return this.terminal;
    }

    @Override
    public BlockPos hostPos() {
        return this.terminal.hostPos();
    }

    @Override
    public BlockPos monitorPos() {
        return this.terminal.monitorPos();
    }

    @Override
    public ResourceLocation spaceId() {
        return this.terminal.spaceId();
    }

    @Override
    public String era() {
        return this.terminal.hardwareEra().serializedName();
    }
}
