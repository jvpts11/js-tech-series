/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.hands;

import dev.jstech.core.machine.MachineBlockEntity;
import net.minecraft.server.level.ServerPlayer;

/** What a control of a formed structure's model does when a player presses it, on the server. */
@FunctionalInterface
public interface IHandControl {

    /** Turns its terminal's switch on, as the screen's switch does. */
    IHandControl START = (terminal, player) -> terminal.control().setOn(true);
    /** Turns its terminal's switch off, as the screen's switch does. */
    IHandControl STOP = (terminal, player) -> terminal.control().setOn(false);

    /** Does what the control does for {@code terminal}, pressed by {@code player}, who may work it. */
    void press(MachineBlockEntity terminal, ServerPlayer player);
}
