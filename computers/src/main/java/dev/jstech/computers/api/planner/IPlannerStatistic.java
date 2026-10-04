/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.api.planner;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.ApiStatus;

/**
 * Something another mod measures that a planner may weigh plans by: it shows among the planner's statistics, and it
 * may say how long a step takes, which the planner then reckons with instead of its own estimate.
 */
@ApiStatus.Experimental
public interface IPlannerStatistic {

    /** The statistic's id. */
    ResourceLocation id();

    /** Its name, as the list of statistics shows it. */
    Component name();

    /** What it reads now on the network of the Mainframe at {@code mainframe}. Called on the server's thread. */
    Component read(ServerLevel level, BlockPos mainframe);

    /** How long {@code step} takes, in ticks, or -1 to leave it to the planner. May be called off the main thread. */
    default long estimate(final PlanStep step) {
        return -1L;
    }
}
