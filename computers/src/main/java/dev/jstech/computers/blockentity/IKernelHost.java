/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.os.IOsHost;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * A machine as its kernel needs it, past what any host answers: whether its switch is on and its parts still make a
 * computer, how it closes down, where its screens find it, and who is told what the kernel ended. A computer is one;
 * a rack is one for whichever of its machines it is answering for.
 */
interface IKernelHost extends IOsHost {

    /** Whether the machine's switch is on, whatever its parts and its system are doing. */
    boolean isManualOn();

    /** Whether the machine's parts still make a computer; one that lost a part it cannot run without does not. */
    boolean buildValid();

    /** The machine closes down for that long, ending everything it was running, and then starts over. */
    void goDown(long now, int ticks);

    /** Where the machine's screens find it. */
    BlockPos getBlockPos();

    /**
     * The kernel ended things for their memory: the lines it logged go to the consoles watching, the windows that went
     * close on the desktops showing the machine, and a program ended in front of a terminal gives the prompt back.
     */
    void memoryFreed(ServerLevel level, List<String> lines, List<String> windows, boolean frontKilled);
}
