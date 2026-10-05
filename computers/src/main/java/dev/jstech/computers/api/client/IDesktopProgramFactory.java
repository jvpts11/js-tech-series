/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.api.client;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

/** Makes a program's window each time it is opened. */
@ApiStatus.Experimental
@FunctionalInterface
public interface IDesktopProgramFactory {

    /**
     * The program, for one window.
     *
     * @param host    the computer the desktop belongs to
     * @param monitor the monitor the desktop is shown on
     * @param desktop the id of the desktop the machine runs, which names its system
     */
    DesktopProgram create(BlockPos host, BlockPos monitor, ResourceLocation desktop);
}
