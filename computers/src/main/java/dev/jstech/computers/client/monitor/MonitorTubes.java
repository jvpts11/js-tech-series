/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.monitor;

import dev.jstech.computers.block.IComputerCase;
import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.core.gui.Tube;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/** What the client can tell about the tube of a screen from the block standing there. */
public final class MonitorTubes {

    private MonitorTubes() {
    }

    /**
     * The tube of the screen at {@code monitor} as this client sees it, a monitor or a computer's own glass; a colour
     * tube when there is none there.
     */
    public static Tube tubeAt(@Nullable final BlockPos monitor) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (monitor == null || minecraft.level == null) {
            return Tube.COLOUR;
        }
        final Block block = minecraft.level.getBlockState(monitor).getBlock();
        if (block instanceof MonitorBlock screen) {
            return screen.kind().tube();
        }
        return block instanceof IComputerCase own && own.glass() != null ? own.glass().tube() : Tube.COLOUR;
    }
}
