/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import net.minecraft.core.BlockPos;

/**
 * A multiblock's controller as its redstone ports reach it: told what signal each port that reads hears, and asked
 * what signal the ports that give give.
 */
public interface IRedstonePortHost {

    /** The port at {@code port}, reading, hears {@code signal} from round it, nought to fifteen. */
    void heard(BlockPos port, int signal);

    /** What the ports that give put out, nought to fifteen: what a comparator would read of the controller. */
    int givenSignal();
}
