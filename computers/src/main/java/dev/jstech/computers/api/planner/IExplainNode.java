/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.api.planner;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

/**
 * What another mod adds to a plan as it is shown: notes under a step, each one a node of its own in the plan's tree
 * (where a step's items come from in that mod's world, what a machine of that mod will do with them).
 */
@ApiStatus.Experimental
public interface IExplainNode {

    /** The contribution's id. */
    ResourceLocation id();

    /** The notes to show under {@code step}, or none. */
    List<Component> notes(PlanStep step);
}
